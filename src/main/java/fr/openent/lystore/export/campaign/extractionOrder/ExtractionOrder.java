package fr.openent.lystore.export.campaign.extractionOrder;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportLystoreWorker;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.export.helpers.ExportHelper;
import fr.openent.lystore.model.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExtractionOrder extends TabHelper {
    List<Integer> ids_campaigns;
    List<Order> orders = new ArrayList<>();
    public ExtractionOrder(Workbook workbook, List<Integer> ids ) {
        super(workbook,"Extraction");
        ids_campaigns = ids;
    }

    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }

    @Override
    protected  void fillPage(JsonArray structures){
        setStructuresFromDatas(structures);
        datas = sortByUai(datas);
        setLabels();
        initObjects();
        datas = new JsonArray();
        log.info("orders number " + orders.size());
        setDatas();
    }

    private void initObjects() {
        Map<String,Structure> structures = new HashMap<>();
        Map<Long, Campaign> campaigns = new HashMap<>();
        Map<Long, Project> projects = new HashMap<>();
        Map<Long, Market> markets = new HashMap<>();
        Map<Long, AccountingProgram> programs = new HashMap<>();
        Map<Long, AccountingProgramAction> programActions = new HashMap<>();
        Map<Long, Operation> operations = new HashMap<>();
        Map<Long, Instruction> instructions = new HashMap<>();
        for (Object jo : datas){
            JsonObject data = (JsonObject)jo;
            Order order = new Order();
            String idStructure = data.getString("id_structure");
            Long idCampaign = data.getLong("campaign_id");
            Long idProject = data.getLong("project_id");
            Long idMarket = data.getLong("market_id");
            Long idProgram =data.getLong("program_id");
            Long idProgramAction = data.getLong("accounting_nature_id");
            Long idOperation = data.getLong("id_operation");
            boolean isValid = data.getBoolean("isvalid");
            setOrderStructure(structures, data, order, idStructure);
            setOrderCampaign(campaigns,data,order,idCampaign);
            setOrderProject(projects,data,order,idProject);
            setOrderData(data,order);
            setOrderMarket(markets,data,order,idMarket);
            setOrderProgram(programs,data,order,idProgram);
            setOrderProgramAction(programActions,data,order,idProgramAction);
            if(idOperation != -1){
                setOrderOperation(operations,instructions,data,order,idOperation);
            }
            if(isValid){
                setOrderBCAndValid(order,data);
            }
            orders.add(order);
        }
    }

    private void setOrderBCAndValid(Order order, JsonObject data) {
        order.setNumberValidation(data.getString("order_validation_number"));
        order.setAtLeastValid(true);
        if(data.getBoolean("hasbc")){
            order.setHasBC(true);
            order.setProgram(data.getString("order_program"));
            BCOrder bcOrder = new BCOrder();
            bcOrder.setDateCreation(getFormatDate(data.getString("bc_date_creation")));
            bcOrder.setEngagementNumber(data.getString("bc_engagement_number"));
            bcOrder.setNumber(data.getString("bc_number"));
            order.setBcOrder(bcOrder);
        }
    }

    private void setOrderOperation(Map<Long, Operation> operations, Map<Long, Instruction> instructions, JsonObject data, Order order, Long idOperation) {

        if(!operations.containsKey(idOperation)){
            Operation operation = new Operation();
            Long idInstruction = data.getLong("instruction_id");
            if(idInstruction != -1){
                if(!instructions.containsKey(idInstruction)) {
                    Instruction instruction = new Instruction();
                    instruction.setId(idInstruction.toString());
                    instruction.setDate_cp(getFormatDate(data.getString("date_cp")));
                    instruction.setExercise(data.getString("exercise_year"));
                    instruction.setCp_number(data.getString("cp_number"));
                    instructions.put(idInstruction, instruction);
                    instruction.setObject(data.getString("label_instruction"));
                }
                operation.setInstruction(instructions.get(idInstruction));
            }
            operation.setId(idOperation.toString());
            operation.setLabel(data.getString("label_operation"));
            operation.setDate_operation(getFormatDate(data.getString("date_operation")));
            operation.setStatus(Boolean.parseBoolean(data.getString("operation_status")));
            order.setOperation(operation);

        }else {
            order.setOperation(operations.get(idOperation));
        }
    }

    private void setOrderProgramAction(Map<Long, AccountingProgramAction> programActions, JsonObject data, Order order, Long idProgramAction) {
        if(!programActions.containsKey(idProgramAction)){
            AccountingProgramAction accountingProgramAction = new AccountingProgramAction();
            accountingProgramAction.setId(idProgramAction.toString());
            accountingProgramAction.setNumber(data.getString("action_number"));
            accountingProgramAction.setDescription(data.getString("action_description"));
            order.setProgramAction(accountingProgramAction);

        }else {
            order.setProgramAction(programActions.get(idProgramAction));
        }
    }


    private void setOrderProgram(Map<Long, AccountingProgram> programs, JsonObject data, Order order, Long idProgram) {
        if(!programs.containsKey(idProgram)){
            AccountingProgram program = new AccountingProgram();
            program.setId(idProgram.toString());
            program.setName(data.getString("program_name"));
            program.setProgramChapter(data.getLong("program_chapter").toString());
            program.setFunctionnalCode(data.getValue("functional_code").toString());
            program.setSection(data.getString("program_section"));
            program.setLabel(data.getString("program_label"));
            program.setChapter(data.getValue("program_chapter").toString());
            order.setAccountingProgram(program);

        }else {
            order.setAccountingProgram(programs.get(idProgram));
        }
    }

    private void setOrderMarket(Map<Long, Market> markets, JsonObject data, Order order, Long idMarket) {
        if(!markets.containsKey(idMarket)){
            Market market = new Market();
            market.setId(idMarket.toString());
            market.setName(data.getString("market_name"));
            market.setMarket_number(data.getString("market_number"));
            market.setAgent(data.getString("market_agent"));
            market.setRegion_supplier(data.getString("market_supplier"));
            market.setAccoutingCode(data.getString("accounting_code"));
            market.setAccoutingNature(data.getString("accounting_nature"));
            order.setMarket(market);

        }else {
            order.setMarket(markets.get(idMarket));
        }
    }

    private void setOrderData(JsonObject data, Order order) {

        if(data.getString("order_origin").equals("REGION"))
            order.setId("R-" + data.getInteger("id").toString());
        else
            order.setId("E-" + data.getInteger("id").toString());

        order.setOrigin(data.getString("order_origin"));
        order.setCreationDate(getFormatDate(data.getString("orders_date")));
        order.setStatus(data.getString("status"));
        order.setComment(data.getString("comment"));
        order.setName(data.getString("equipment_name"));
        order.setPriceHT(safeGetDouble(data,"priceht","ExtractionOrder"));
        order.setTax_amount(safeGetDouble(data,"tva","ExtractionOrder"));
        order.setName(data.getString("equipment_name"));
        order.setAmount(data.getInteger("quantity"));
        order.setTotalTTC(safeGetDouble(data,"total","ExtractionOrder"));
        if(Integer.parseInt(data.getString("priority_order"))!= -1)
            order.setRank(Integer.parseInt(data.getString("priority_order")));
        if(safeGetDouble(data,"price_proposal","ExtractionOrder") != -1.d)
            order.setPriceProposal(safeGetDouble(data,"price_proposal","ExtractionOrder"));
        if(data.getBoolean("has_file")){
            JsonArray filesArray= data.getJsonArray("filesid");
            for(int i = 0 ; i < filesArray.size(); i ++){
                //METTRE URL ICI
                order.addFilenames(ExportLystoreWorker.url + "/lystore/order/" + order.getId().split("-")[1] +
                        "/file/" + filesArray.getJsonArray(i).getString(1));
            }
        }
        if(data.getLong("id_order_client_equipment") != -1){
            order.setIdOrderClientEquipment(data.getLong("id_order_client_equipment"));
        }
        if(data.getBoolean("has_options")){
            order.setHasOptions(true);
            order.setOptionAmount(safeGetDouble(data,"options_amount","ExtractionOrder"));
            JsonArray options= data.getJsonArray("options_names");
            for(int i = 0 ; i < options.size(); i ++){
                order.addOption(options.getJsonArray(i).getString(1));
            }
        }
    }

    private void setOrderProject(Map<Long, Project> projects, JsonObject data, Order order, Long idProject) {
        if(!projects.containsKey(idProject)){
            Project project = new Project();
            project.setId(idProject.toString());
            project.setName(data.getString("project_name"));
            project.setComment(data.getString("project_comment"));
            project.setBuilding(data.getString("project_building"));
            project.setRoom(data.getString("project_room"));

            for (int j = 0 ; j< data.getJsonArray("structure_groups").size() ; j++){
                project.addStructureGroup(data.getJsonArray("structure_groups").getJsonArray(j).getString(1));
            }
            for (int j = 0 ; j< data.getJsonArray("tags_name").size() ; j++){
                project.addTag(data.getJsonArray("tags_name").getJsonArray(j).getString(1));

            }
            projects.put(idProject,project);
            if(data.getInteger("priority_project").equals(-1))
                project.setRank(data.getInteger("priority_project"));

            order.setProject(project);

        }else {
            order.setProject(projects.get(idProject));
        }
    }

    private void setOrderCampaign(Map<Long, Campaign> campaigns, JsonObject data, Order order, Long idCampaign) {
        if(!campaigns.containsKey(idCampaign)){
            Campaign campaign = new Campaign();
            campaign.setId(idCampaign.toString());
            campaign.setOpen(data.getBoolean("campaign_open"));
            campaign.setName(data.getString("campaign_name"));
            try {
                Double purse = safeGetDouble(data, "purse_amount", "ExtractionOrder");
                if (purse != -1.d)
                    campaign.setPurse(purse);
                else
                    campaign.setHasPurse(false);
            }catch (NullPointerException e){
                data.getValue("purse_amount");
            }
            if(data.getBoolean("has_campaign_start"))
                campaign.setStartDate(getFormatDate(data.getString("campaign_start_date")));
            else
                campaign.setStartDate("");
            if(data.getBoolean("has_campaign_end"))
                campaign.setEndDate(getFormatDate(data.getString("campaign_end_date")));
            else
                campaign.setEndDate("");


            campaigns.put(idCampaign,campaign);
            order.setCampaign(campaign);
        }else {
            order.setCampaign(campaigns.get(idCampaign));
        }
    }

    private void setOrderStructure(Map<String, Structure> structures, JsonObject data, Order order, String idStructure) {
        if(!structures.containsKey(idStructure)){
            Structure structure = new Structure();
            structure.setId(idStructure);
            structure.setAcademy(data.getString("academy"));
            structure.setUAI(data.getString("uai"));
            structure.setType(data.getString("type"));
            structure.setName(data.getString("nameEtab"));
            structure.setZipCode(data.getString("zipCode"));
            structure.setCity(data.getString("city"));
            structure.setCiteMixte(data.getString("cite_mixte"));
            structures.put(idStructure,structure);
            order.setStructure(structure);
        }else {
            order.setStructure(structures.get(idStructure));
        }
    }

    private void setDatas() {
        int nbOrder = orders.size();
        for(int i = 0; i < nbOrder ; i++) {
            Order order = orders.get(0);
            insertStruturesInfosFromData(i, order.getStructure());
            setProjectAndCampaignsDatas(i, order);
            insertEquipmementDatas(i, order);
            insertAccountingDatas(i, order);
            setManagementInstructionDatas(i, order);
            setBCManagementData(i,order);
            if (i == 10) {
                excel.autoSize(60);
            }
            orders.remove(0);
        }
        if (nbOrder < 10) {
            excel.autoSize(60);
        }
    }

    private void setBCManagementData(int i, Order order) {
        if(order.isAtLeastValid()) {
            excel.insertCellTab(55,5+i,order.getNumberValidation());
            if(order.hasBC()){
                BCOrder bcOrder = order.getBcOrder();
                excel.insertCellTab(58,5+i,order.getProgram());
                excel.insertCellTab(56,5+i,bcOrder.getNumber());
                excel.insertCellTab(57,5+i,bcOrder.getEngagementNumber());
                excel.insertCellTab(59,5+i,bcOrder.getDateCreation());
            }else{
                for(int j = 1; j <= 4; j++){
                    excel.insertCellTab(55+j, 5 + i, EMPTY);
                }
            }
        }else {
            for(int j = 0; j <= 4; j++){
                excel.insertCellTab(55+j, 5 + i, EMPTY);
            }
        }

    }

    private void setManagementInstructionDatas(int i, Order order) {
        if(order.hasOperation()) {
            Operation operation = order.getOperation();
            excel.insertCellTab(48, 5 + i, operation.getLabel());
            excel.insertCellTab(49, 5 + i, operation.getDate_operation());
            excel.insertCellTab(50, 5 + i, (operation.getStatus() ? "Ouverte" : "Fermée"));
            if(operation.hasInstruction()){
                Instruction instruction = operation.getInstruction();
                excel.insertCellTab(47, 5 + i, instruction.getExercise());
                excel.insertCellTab(51,5 + i, instruction.getObject());
                excel.insertCellTab(52,5 + i, instruction.getCp_number());
                excel.insertCellTab(53,5 + i,instruction.getDate_cp());
            }else{
                excel.insertCellTab(47, 5 + i, EMPTY);
                excel.insertCellTab(51,5 + i, EMPTY);
                excel.insertCellTab(52,5 + i, EMPTY);
                excel.insertCellTab(53,5 + i, EMPTY);

            }
            excel.insertCellTab(54, 5 + i, "");
        }else{
            for(int j = 0; j <= 7; j++){
                excel.insertCellTab(47+j, 5 + i, EMPTY);
            }
        }
    }

    private void insertAccountingDatas(int i, Order order) {
        Market market = order.getMarket();
        AccountingProgram program = order.getAccountingProgram();
        AccountingProgramAction programAction = order.getProgramAction();
        excel.insertCellTab(35,5+i,market.getName());
        excel.insertCellTab(36,5+i,market.getMarket_number());
        excel.insertCellTab(37,5+i,market.getRegion_supplier());
        excel.insertCellTab(38,5+i,market.getAgent());
        excel.insertCellTab(39,5+i,market.getAccoutingCode());
        excel.insertCellTab(40,5+i,market.getAccoutingNature());
        excel.insertCellTab(41,5+i,program.getChapter() + " " + program.getSection());
        excel.insertCellTab(42,5+i,program.getFunctionnalCode());
        excel.insertCellTab(43,5+i,program.getName());
        excel.insertCellTab(44,5+i,program.getLabel());
        excel.insertCellTab(45,5+i,programAction.getNumber());
        excel.insertCellTab(46,5+i,programAction.getDescription());
    }

    private void insertEquipmementDatas(int i, Order order) {
        excel.insertCellTab(18,5+i, order.getId());
        excel.insertCellTab(19,5+i, order.getOrigin());
        excel.insertCellTab(20,5+i, (order.hasidOrderClientEquipment()) ? "E-" + order.getIdOrderClientEquipment() : EMPTY);
        excel.insertCellTab(21,5+i, order.getCreationDate());
        excel.insertCellTab(22,5+i, OrderStatus.valueOf(order.getStatus().replace(" ", "_")));
        excel.insertWithStyle(23,5+i,
                (order.getProject().hasRank() && order.getProject().getRank() !=-1.d ? order.getProject().getRank(): EMPTY ),excel.tabIntStyleRightBold);
        excel.insertWithStyle(24,5+i,(order.hasRank() && order.getRank() !=-1.d ? order.getRank() : EMPTY ),excel.tabIntStyleRightBold);
        excel.insertCellTab(25,5+i, order.getComment());
        if(order.hasFilename())
            excel.insertCellTab(26,5+i,order.getFilenames().toString());
        else
            excel.insertCellTab(26,5+i,EMPTY);
        excel.insertCellTab(27,5+i,order.getName());
        excel.insertCellTabInt(28,5+i,order.getAmount());
        excel.insertCellTabDouble(29,5+i,order.getPriceHT());
        excel.insertCellTabDouble(30,5+i,order.getTax_amount());
        excel.insertWithStyle(31,5+i,(order.hasPriceProposal() && order.getPriceProposal() != -1.d ? order.getPriceProposal() : EMPTY ),excel.tabCurrencyStyle);
        excel.insertCellTabDoubleWithPrice(32,5+i,order.getTotalTTC());
        if(order.hasOptions()) {
            excel.insertCellTab(33, 5 + i, order.getOptionsNames().toString());
            excel.insertWithStyle(34, 5 + i, order.getOptionAmount(),excel.tabCurrencyStyle);
        }else{
            excel.insertCellTab(33, 5 + i, EMPTY);
            excel.insertCellTab(34, 5 + i, EMPTY);
        }
    }

    private void setProjectAndCampaignsDatas(int i, Order order) {
        Campaign campaign = order.getCampaign();
        Project project = order.getProject();
        excel.insertCellTab(7,5+i,campaign.getName());
        if(campaign.hasPurse())
            excel.insertCellTabDoubleWithPrice(8,5+i,campaign.getPurse());
        else
            excel.insertCellTab(8,5+i,EMPTY);
        excel.insertCellTab(9,5+i,campaign.getStartDate());
        excel.insertCellTab(10,5+i,campaign.getEndDate());
        excel.insertCellTab(11,5+i,campaign.isOpen() ? "Ouverte" : "Fermée");
        excel.insertCellTab(12, 5+i, project.getName());
        excel.insertCellTab(13, 5+i, project.getComment());
        excel.insertCellTab(14, 5+i  , project.getStructureGroupString());
        excel.insertCellTab(15, 5+i  , project.getTags().toString());
        excel.insertCellTab(16,5+i,project.getRoom());
        excel.insertCellTab(17,5+i,project.getBuilding());
    }

    private void insertStruturesInfosFromData(int i, Structure structure) {
        excel.insertCellTab(0, 5+i, structure.getUAI());
        excel.insertCellTab(1, 5+i, structure.getType());
        excel.insertCellTab(2, 5+i, structure.getName());
        excel.insertCellTab(3, 5+i, structure.getZipCode().substring(0,2));
        excel.insertCellTab(4, 5+i, structure.getCity());
        excel.insertCellTab(5, 5+i, structure.getCiteMixte());
        excel.insertCellTab(6, 5+i, structure.getAcademy());
    }


    @Override
    protected void setLabels() {
        SimpleDateFormat formatterDateExcel = new SimpleDateFormat("dd.MM.yyyy");
        Date orderDate =  new Date();

        excel.insertBlackTitleHeaderBorderless(1,1,"Lystore - Extraction Demande - " + formatterDateExcel.format(orderDate));
        excel.insertBlackTitleHeader(0,3,"Informations des structures");
        sizeMergeRegion(3,0,6);
        setStructuresInfoLabel();
        excel.insertBlackTitleHeader(7,3,"Campagne et Projet");
        sizeMergeRegion(3,7,17);
        setProjectLabel();
        excel.insertBlackTitleHeader(18,3,"Demande d'équipements");
        sizeMergeRegion(3,18,32);
        setEquipmentLabel();
        excel.insertBlackTitleHeader(33,3,"Options Equipements");
        sizeMergeRegion(3,33,34);
        setOptionsEquipmentLabel();
        excel.insertBlackTitleHeader(35,3,"Éléments comptables");
        sizeMergeRegion(3,35,46);
        setAccountingElementsLabel();
        excel.insertBlackTitleHeader(47,3,"Gestion des rapports CP");
        sizeMergeRegion(3,47,54);
        setManagementInstructionLabel();
        excel.insertBlackTitleHeader(55,3,"Gestion des bons de commandes");
        sizeMergeRegion(3,55,59);
        setBCManagementLabel();
    }

    private void setBCManagementLabel() {
        excel.insertWithStyle(55,4,"Numéro de Validation",excel.labelOnOrange);
        excel.insertWithStyle(56,4,"N° Bon de Commande",excel.labelOnOrange);
        excel.insertWithStyle(57,4,"N° Engagement",excel.labelOnOrange);
        excel.insertWithStyle(58,4,"Programme",excel.labelOnOrange);
        excel.insertWithStyle(59,4,"Date création",excel.labelOnOrange);
    }

    private void setOptionsEquipmentLabel() {
        excel.insertWithStyle(33,4,"Options",excel.labelOnOrange);
        excel.insertWithStyle(34,4,"Montant total TTC Options",excel.labelOnOrange);
    }

    private void setManagementInstructionLabel() {
        excel.insertWithStyle(47,4,"Exercice",excel.labelOnGreen);
        excel.insertWithStyle(48,4,"Opération",excel.labelOnGreen);
        excel.insertWithStyle(49,4,"Date Opération",excel.labelOnGreen);
        excel.insertWithStyle(50,4,"Status Opération",excel.labelOnGreen);
        excel.insertWithStyle(51,4,"Rapport",excel.labelOnGreen);
        excel.insertWithStyle(52,4,"Numéro CP",excel.labelOnGreen);
        excel.insertWithStyle(53,4,"Date CP",excel.labelOnGreen);
        excel.insertWithStyle(54,4,"Statut du rapport CP",excel.labelOnGreen);

    }

    private void setAccountingElementsLabel() {
        excel.insertWithStyle(35,4,"Marché support",excel.labelOnYellow);
        excel.insertWithStyle(36,4,"Numéro Marché",excel.labelOnYellow);
        excel.insertWithStyle(37,4,"Titulaire Marché",excel.labelOnYellow);
        excel.insertWithStyle(38,4,"Correspondant Région",excel.labelOnYellow);
        excel.insertWithStyle(39,4,"Nature Compable",excel.labelOnYellow);
        excel.insertWithStyle(40,4,"Libellé Nature Compable",excel.labelOnYellow);
        excel.insertWithStyle(41,4,"Chapitre Budgétaire",excel.labelOnYellow);
        excel.insertWithStyle(42,4,"Code fonctionnel",excel.labelOnYellow);
        excel.insertWithStyle(43,4,"Programme",excel.labelOnYellow);
        excel.insertWithStyle(44,4,"Libellé Programme",excel.labelOnYellow);
        excel.insertWithStyle(45,4,"Action",excel.labelOnYellow);
        excel.insertWithStyle(46,4,"Libellé Action",excel.labelOnYellow);
    }

    private void setEquipmentLabel() {
        excel.insertWithStyle(18,4,"N° Demande",excel.labelOnOrange);
        excel.insertWithStyle(19,4,"Origine EPLE/REGION",excel.labelOnOrange);
        excel.insertWithStyle(20,4,"Demande Modif.",excel.labelOnOrange);
        excel.insertWithStyle(21,4,"Date création",excel.labelOnOrange);
        excel.insertWithStyle(22,4,"Status Demande",excel.labelOnOrange);
        excel.insertWithStyle(23,4,"Priorité Projet",excel.labelOnOrange);
        excel.insertWithStyle(24,4,"Priorité Demande",excel.labelOnOrange);
        excel.insertWithStyle(25,4,"Commentare demande",excel.labelOnOrange);
        excel.insertWithStyle(26,4,"Pièces jointes",excel.labelOnOrange);
        excel.insertWithStyle(27,4,"Équipement",excel.labelOnOrange);
        excel.insertWithStyle(28,4,"Quantité",excel.labelOnOrange);
        excel.insertWithStyle(29,4,"Prix unitaire HT",excel.labelOnOrange);
        excel.insertWithStyle(30,4,"Taux TVA",excel.labelOnOrange);
        excel.insertWithStyle(31,4,"Somme Montant proposé TTC",excel.labelOnOrange);
        excel.insertWithStyle(32,4,"Montant total TTC",excel.labelOnOrange);


    }

    private void setProjectLabel() {
        excel.insertWithStyle(7,4,"Campagne",excel.labelOnBlueGrey);
        excel.insertWithStyle(8,4,"Cagnotte initiale",excel.labelOnBlueGrey);
        excel.insertWithStyle(9,4,"Date Début",excel.labelOnBlueGrey);
        excel.insertWithStyle(10,4,"Date Fin",excel.labelOnBlueGrey);
        excel.insertWithStyle(11,4,"Statut Campagne",excel.labelOnBlueGrey);
        excel.insertWithStyle(12,4,"Projet",excel.labelOnBlueGrey);
        excel.insertWithStyle(13,4,"Commentaire projet",excel.labelOnBlueGrey);
        excel.insertWithStyle(14,4,"Regroupements",excel.labelOnBlueGrey);
        excel.insertWithStyle(15,4,"Labels équipements",excel.labelOnBlueGrey);
        excel.insertWithStyle(16,4,"Salle",excel.labelOnBlueGrey);
        excel.insertWithStyle(17,4,"Batiment",excel.labelOnBlueGrey);
    }

    private void setStructuresInfoLabel() {
        excel.insertWithStyle(0,4,"UAI",excel.labelOnGrey);
        excel.insertWithStyle(1,4,"Type",excel.labelOnGrey);
        excel.insertWithStyle(2,4,"Dénomination",excel.labelOnGrey);
        excel.insertWithStyle(3,4,"Dpt",excel.labelOnGrey);
        excel.insertWithStyle(4,4,"Commune",excel.labelOnGrey);
        excel.insertWithStyle(5,4,"Cité Mixte",excel.labelOnGrey);
        excel.insertWithStyle(6,4,"Académie",excel.labelOnGrey);
    }

    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        ArrayList<String> structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            if(!structuresId.contains(data.getString("id_structure")))
                structuresId.add(structuresId.size(), data.getString("id_structure"));
        }
        getStructures(new JsonArray(structuresId),getStructureHandler(structuresId,handler));
    }
    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = " WITH info_group_and_tag as ( " +
                "  SELECT  " +
                "    tag.name,  " +
                "    rgc.id_campaign as id_campaign,  " +
                "    rgs.id_structure as id_struct,  " +
                "    structure_group.name as group_name  " +
                "  FROM  " +
                "    " + Lystore.lystoreSchema + ".tag  " +
                "    INNER JOIN " +  Lystore.lystoreSchema + ".rel_group_campaign rgc on tag.id = rgc.id_tag  " +
                "    INNER JOIN " +  Lystore.lystoreSchema + ".structure_group on rgc.id_structure_group = structure_group.id  " +
                "    INNER JOIN " +  Lystore.lystoreSchema + ".rel_group_structure rgs on structure_group.id = rgs.id_structure_group " +
                ")  " +
                "SELECT  " +
                "  DISTINCT orders.id,  " +
                "  orders.creation_date as orders_date,  " +
                "  campaign.name as campaign_name,  " +
                "  campaign.id as campaign_id,  " +
                "  orders.\"price TTC\" as priceTTC,  " +
                "  orders.amount as quantity,  " +
                "  orders.name as equipment_name,  " +
                "  orders.id_structure,  " +
                "  orders.status,  " +
                "  contract_type.name as accounting_nature,  " +
                "  contract_type.code as accounting_code,  " +
                "  contract_type.id as accounting_nature_id,  " +
                "  market.name as market_name,  " +
                "  market.reference as market_number,  " +
                "  market.id as market_id,  " +
                "  supplier.name as market_supplier,  " +
                "  agent.name as market_agent,  " +
                "  program.name as program_name,  " +
                "  program.label as program_label,  " +
                "  program.chapter as program_chapter,  " +
                "  program.functional_code as functional_code,  " +
                "  program.id as program_id,  " +
                "  program.section as program_section,  " +
                "  program_action.action as action_number,  " +
                "  program_action.description as action_description,  " +
                "  instruction_operation.label as label_operation,  " +
                "  instruction_operation.status as operation_status,  " +
                "  instruction_operation.date_operation as date_operation,  " +
                "  instruction_operation.cp_number as cp_number,  " +
                "  instruction_operation.date_cp as date_cp,  " +
                "  instruction_operation.year as exercise_year,  " +
                "  title.name as project_name,  " +
                "  project.id as project_id,  " +
                "  CASE when project.description is NULL THEN '' ELSE project.description END as project_comment,  " +
                "  CASE when orders.id_order is NULL  " +
                "  OR orders.program IS NULL then FALSE else TRUE END as hasBC,  " +
                "  CASE when orders.number_validation is NULL then FALSE else TRUE END as isValid,  " +
                "  CASE when project.room is NULL THEN '' ELSE project.room END as project_room,  " +
                "  CASE when project.building is NULL THEN '' ELSE project.building END as project_building,  " +
                "  CASE when orders.override_region IS NULL THEN 'REGION' ELSE 'EPLE' END as order_origin,  " +
                "  CASE when orders.id_order_client_equipment IS NULL THEN -1 ELSE orders.id_order_client_equipment END as id_order_client_equipment,  " +
                "  campaign.start_date as campaign_start_date,  " +
                "  campaign.end_date campaign_end_date,  " +
                "  orders.number_validation as order_validation_number,  " +
                "  orders.program as order_program,  " +
                "  orders.action as program_action,  " +
                "  CASE WHEN campaign.purse_enabled IS TRUE THEN ( " +
                "    SELECT  " +
                "      purse.initial_amount  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".purse  " +
                "    WHERE  " +
                "      purse.id_campaign = campaign.id  " +
                "      AND purse.id_structure = orders.id_structure " +
                "  ) ELSE -1 END as purse_amount,  " +
                "  CASE WHEN orders.comment IS NULL THEN '' ELSE orders.comment END as comment,  " +
                "  array_agg(info_group_and_tag.name) as tags_name,  " +
                "  array_agg( " +
                "    DISTINCT info_group_and_tag.group_name " +
                "  ) as structure_groups,  " +
                "  campaign.accessible as campaign_open,  " +
                "  CASE WHEN orders.prio IS NULL THEN -1 ELSE orders.prio END as priority_order,  " +
                "  CASE WHEN project.preference IS NULL THEN -1 ELSE project.preference END as priority_project,  " +
                "  CASE WHEN ss.type IS NULL  " +
                "  OR ss.type = 'LYC' THEN ' ' ELSE ss.type END AS cite_mixte,  " +
                "  CASE WHEN orders.tax_amount = -1 THEN 20 ELSE orders.tax_amount END as TVA,  " +
                "  CASE WHEN orders.priceHT = -1 THEN orders.\"price TTC\" / 1.2 ELSE orders.priceHT END as priceHT,  " +
                "  CASE WHEN orders.price_proposal IS NULL THEN -1 ELSE orders.price_proposal END as price_proposal,  " +
                "  CASE WHEN instruction_operation.id_operation IS NULL THEN -1 ELSE instruction_operation.id_operation END as id_operation,  " +
                "  CASE WHEN instruction_operation.cp_number IS NULL THEN '' ELSE instruction_operation.cp_number END as cp_number,  " +
                "  CASE WHEN instruction_operation.label IS NULL THEN '' ELSE instruction_operation.label END as label_operation,  " +
                "  CASE WHEN instruction_operation.object IS NULL THEN '' ELSE instruction_operation.object END as label_instruction,  " +
                "  CASE WHEN instruction_operation.instruction_id IS NULL THEN -1 ELSE instruction_operation.instruction_id END as instruction_id,  " +
                "  CASE WHEN campaign.start_date IS NULL THEN FALSE ELSE TRUE END as has_campaign_start,  " +
                "  CASE WHEN campaign.end_date IS NULL THEN FALSE ELSE TRUE END as has_campaign_end,  " +
                "  ( " +
                "    SELECT  " +
                "      array_agg(equipment_type.name  || ' : ' || oco.name )  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".order_client_options oco " +
                "    INNER JOIN " +  Lystore.lystoreSchema + ". equipment_type " +
                "    ON equipment_type.id = oco.id_type " +
                "    WHERE  " +
                "      oco.id_order_client_equipment = orders.id  " +
                "      AND orders.override_region is false " +
                "  ) as options_names,  " +
                "  ( " +
                "    SELECT  " +
                "      SUM( " +
                "        oco.price + ( " +
                "          (oco.price * oco.tax_amount) / 100 " +
                "        ) * oco.amount * orders.amount " +
                "      )  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".order_client_options oco  " +
                "    WHERE  " +
                "      oco.id_order_client_equipment = orders.id  " +
                "      AND orders.override_region is false " +
                "  ) as options_amount,  " +
                "  CASE WHEN( " +
                "    SELECT  " +
                "      DISTINCT 1  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".order_client_options oco  " +
                "    WHERE  " +
                "      oco.id_order_client_equipment = orders.id  " +
                "      AND orders.override_region is false " +
                "  ) IS NULL THEN FALSE ELSE TRUE END as has_options,  " +
                "  ( " +
                "    SELECT  " +
                "      order_bc.engagement_number  " +
                "    from  " +
                "      " +  Lystore.lystoreSchema + ".order order_bc  " +
                "    where  " +
                "      order_bc.id = orders.id_order " +
                "  ) as bc_engagement_number,  " +
                "  ( " +
                "    SELECT  " +
                "      order_bc.date_creation  " +
                "    from  " +
                "      " +  Lystore.lystoreSchema + ".order order_bc  " +
                "    where  " +
                "      order_bc.id = orders.id_order " +
                "  ) as bc_date_creation,  " +
                "  ( " +
                "    SELECT  " +
                "      order_bc.order_number  " +
                "    from  " +
                "      " +  Lystore.lystoreSchema + ".order order_bc  " +
                "    where  " +
                "      order_bc.id = orders.id_order " +
                "  ) as bc_number,  " +
                "  Round( " +
                "    orders.\"price TTC\" * orders.amount,  " +
                "    2 " +
                "  ) AS Total,  " +
                "  CASE WHEN( " +
                "    SELECT  " +
                "      DISTINCT 1  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".order_file  " +
                "    WHERE  " +
                "      order_file.id_order_client_equipment = orders.id " +
                "  ) IS NULL THEN FALSE ELSE TRUE END as has_file,  " +
                "  ( " +
                "    SELECT  " +
                "      array_agg(filename)  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".order_file  " +
                "    WHERE  " +
                "      order_file.id_order_client_equipment = orders.id  " +
                "      AND orders.override_region is false " +
                "  ) as filenames,  " +
                "  ( " +
                "    SELECT  " +
                "      array_agg(id)  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".order_file  " +
                "    WHERE  " +
                "      order_file.id_order_client_equipment = orders.id  " +
                "      AND orders.override_region is false " +
                "  ) as filesid  " +
                "FROM  " +
                "  " +  Lystore.lystoreSchema + ".allorders orders  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".campaign ON campaign.id = orders.id_campaign  " +
                "  AND id_campaign = ?  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".project ON orders.id_project = project.id  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".title ON project.id_title = title.id  " +
                "  INNER JOIN info_group_and_tag ON ( " +
                "    info_group_and_tag.id_campaign = campaign.id  " +
                "    AND orders.id_structure = info_group_and_tag.id_struct " +
                "  )  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".contract market ON (orders.id_contract = market.id)  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".contract_type ON (market.id_contract_type = contract_type.id " +           "  )  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".agent ON (market.id_agent = agent.id)  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".supplier ON (market.id_supplier = supplier.id)  " +
                "  LEFT JOIN " +  Lystore.lystoreSchema + ".specific_structures ss ON (ss.id = orders.id_structure )" +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".structure_program_action spg ON  " +
                "    contract_type.id = spg.contract_type_id " +
                "   AND (spg.structure_type = ss.type OR (spg.structure_type ='" + LYCEE + "' AND ss.type is NULL ))  " +

                "  INNER JOIN " +  Lystore.lystoreSchema + ".program_action ON (program_action.id = spg.program_action_id)" +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".program ON ( " +
                "    program.id = program_action.id_program " +
                "  )  " +
                "  LEFT JOIN ( " +
                "    SELECT  " +
                "      DISTINCT label_operation.label,  " +
                "      operation.id as id_operation,  " +
                "      operation.date_operation,  " +
                "      instruction.date_cp,  " +
                "      instruction.object,  " +
                "      instruction.cp_number,  " +
                "      instruction.year,  " +
                "      operation.status,  " +
                "      instruction.id as instruction_id  " +
                "    FROM  " +
                "      " +  Lystore.lystoreSchema + ".operation  " +
                "      INNER JOIN " +  Lystore.lystoreSchema + ".label_operation ON ( " +
                "        operation.id_label = label_operation.id " +
                "      )  " +
                "      LEFT JOIN ( " +
                "        Select  " +
                "          DISTINCT instruction.id,  " +
                "          instruction.date_cp,  " +
                "          instruction.object,  " +
                "          instruction.cp_number,  " +
                "          exercise.year  " +
                "        FROM  " +
                "          " +  Lystore.lystoreSchema + ".instruction  " +
                "          INNER JOIN " +  Lystore.lystoreSchema + ".exercise on instruction.id_exercise = exercise.id " +
                "      ) as instruction on operation.id_instruction = instruction.id " +
                "  ) as instruction_operation ON ( " +
                "    instruction_operation.id_operation = orders.id_operation  " +
                "    AND orders.id_operation IS NOT NULL " +
                "  )  " +
                "WHERE  " +
                "  orders.override_region is not true  " +
                "GROUP BY  " +
                "  orders.id,  " +
                "  id_structure,  " +
                "  campaign.NAME,  " +
                "  ss.type,  " +
                "  orders.\"price TTC\",  " +
                "  quantity,  " +
                "  TVA,  " +
                "  priceHT,  " +
                "  equipment_name,  " +
                "  orders.status,  " +
                "  orders.comment,  " +
                "  priority_order,  " +
                "  campaign.accessible,  " +
                "  campaign.start_date,  " +
                "  campaign.end_date,  " +
                "  campaign.purse_enabled,  " +
                "  campaign.id,  " +
                "  title.name,  " +
                "  project.description,  " +
                "  project.room,  " +
                "  project.building,  " +
                "  orders.override_region,  " +
                "  project.preference,  " +
                "  orders.creation_date,  " +
                "  orders.price_proposal,  " +
                "  contract_type.name,  " +
                "  market.name,  " +
                "  market.reference,  " +
                "  contract_type.code,  " +
                "  supplier.name,  " +
                "  agent.name,  " +
                "  program.name,  " +
                "  program.chapter,  " +
                "  program.functional_code,  " +
                "  program.section,  " +
                "  program_action.action,  " +
                "  program_action.description,  " +
                "  instruction_operation.label,  " +
                "  instruction_operation.cp_number,  " +
                "  instruction_operation.object,  " +
                "  instruction_operation.status,  " +
                "  campaign.id,  " +
                "  project.id,  " +
                "  market.id,  " +
                "  contract_type.id,  " +
                "  program.id,  " +
                "  program_action.id,  " +
                "  instruction_operation.id_operation,  " +
                "  instruction_operation.instruction_id,  " +
                "  instruction_operation.date_operation,  " +
                "  instruction_operation.cp_number,  " +
                "  instruction_operation.date_cp,  " +
                "  instruction_operation.year,  " +
                "  orders.number_validation,  " +
                "  orders.program,  " +
                "  orders.action,  " +
                "  orders.id_order,  " +
                "  orders.id_order_client_equipment;   ";

        launchSQLFutures(handler);
    }

    private void launchSQLFutures(Handler<Either<String,JsonArray>> handler) {
        List<Future> futures = new ArrayList<>();
        datas = new JsonArray();
        for(Integer id : ids_campaigns){
            Future<JsonObject> getDatasForOneCampaignFuture = Future.future();
            futures.add(getDatasForOneCampaignFuture);
        }
        getDatasForOneCampaignFutureHandler(handler,futures);
        for (int i = 0 ; i < ids_campaigns.size() ; i++){
            Future future = futures.get(i);
            Integer id = ids_campaigns.get(i);
            sqlHandler(getHandler(future),new JsonArray().add(id));
        }
    }

    protected void getDatasForOneCampaignFutureHandler(Handler<Either<String, JsonArray>> handler, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray results =new JsonArray();
                List<JsonArray> resultsList = event.result().list();
                for (JsonArray objects : resultsList) {
                    for(Object jo : objects){
                        results.add((JsonObject)jo);
                    }
                }
                datas = results;
                handler.handle(new Either.Right(datas));

            } else {
                handler.handle(new Either.Left<>("Error when resolving futures : " + event.cause().getMessage()));
            }
        });
    }
    @Override
    protected void sqlHandler(Handler<Either<String,JsonArray>> handler, JsonArray params){
        Sql.getInstance().prepared(query, params, new DeliveryOptions().setSendTimeout(Lystore.timeout * 1000000000L),SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));
    }

}
