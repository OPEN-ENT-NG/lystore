package fr.openent.lystore.export.instructions.subventionEquipment;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.Map;

public class Market extends TabHelper {
    private StructureService structureService;
    private ArrayList<Integer> codes = new ArrayList<>();
    private int arraylength = 5;
    private int lineNumber = 0;
    private final String MARKET_LABEL = "Code Marché (Dotation)";
    private final String AMOUNT = "Quantité accordé";
    private final String TOTAL = "Somme Montant Accordé";
    private final String ANNEXE_TEXT = "ANNEXE au rapport";
    private final String TOTAL_TITLE = "Montant total dotations financières au titre du présent rapport";
    private boolean isCMR;
    Double totalSubv = 0.d;


    public Market(Workbook workbook, JsonObject instruction, boolean isCMR, Map<String, JsonObject> structuresMap) {
        super(workbook, instruction, (isCMR) ? "ANN. 1 Rapport CMR Marchés" : "ANN. 1 Rapport PUB. Marchés",structuresMap);
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.isCMR = isCMR;
    }



    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        fillPage(structures);
        HandleCatchResult(false, "", new JsonArray(), handler);
    }

    @Override
    protected void fillPage(Map<String, JsonObject> structures){
        setStructures(structures);
        setTitle();
        writeArray();
    }

    private void setTitle() {
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            totalSubv += Double.parseDouble(data.getString("totalprice"));
        }
        excel.insertBlackTitleHeaderBorderlessCenter(0, lineNumber, ANNEXE_TEXT);
        sizeMergeRegionWithStyle(lineNumber, 0, 2, excel.blackTitleHeaderBorderlessCenteredStyle);
        lineNumber++;
        excel.insertBlackTitleHeaderBorderlessCenter(0, lineNumber, TOTAL_TITLE);
        sizeMergeRegionWithStyle(lineNumber, 0, 2, excel.blackTitleHeaderBorderlessCenteredStyle);
        lineNumber++;
        excel.insertBlueTitleHeaderBorderlessCenterDoubleCurrency(0, lineNumber, totalSubv);
        sizeMergeRegionWithStyle(lineNumber, 0, 2, excel.blackTitleHeaderBorderlessCenteredStyle);
        lineNumber += 2;

    }

    private void writeArray() {
        for (int i = 0; i < datas.size(); i++) {
            JsonObject campaignData = datas.getJsonObject(i);
            JsonArray orders = campaignData.getJsonArray("actionsJO");
            orders = sortByCity(orders, false);
            String campaign = campaignData.getString("campaign");
            lineNumber++;
            excel.insertUnderscoreHeader(0, lineNumber, campaign);
            lineNumber += 2;
            Integer initLine = lineNumber;
            String previousIdStruct = "";
            String market = "";
            Integer previousMarketId = orders.getJsonObject(0).getInteger("market_id");
            String previousMarket = orders.getJsonObject(0).getString("market");
            for (int j = 0; j < orders.size(); j++) {
                JsonObject order = orders.getJsonObject(j);
                String idStructure = order.getString("id_structure");
                market = order.getString("market");
                Integer marketId = order.getInteger("market_id");
                if (!idStructure.equals(previousIdStruct)) {
                    if (j != 0) {
                        excel.insertLabelBold(0, lineNumber, previousMarket);
                        excel.setTotalXWithStyle(initLine, lineNumber - 1, 1, lineNumber, excel.tabIntStyleCenterBold);
                        excel.setTotalX(initLine, lineNumber - 1, 2, lineNumber);
                        initLine = lineNumber + 1;
                        previousMarketId = marketId;
                        previousMarket = market;
                        lineNumber+=2;
                    }
                    String zip = order.getString("zipCode").substring(0, 2);
                    String structString = zip + " - " +
                            order.getString("city") + " - " + order.getString("nameEtab") + "(" + order.getString("uai") + ")";
                    excel.insertHeader(0, lineNumber, structString);
                    sizeMergeRegion(lineNumber, 0, 2);
                    previousIdStruct = idStructure;
                    lineNumber++;
                    setLabels();
                }
//
                if (previousMarketId != marketId) {
                    excel.insertLabelBold(0, lineNumber, previousMarket);
                    excel.setTotalXWithStyle(initLine, lineNumber - 1, 1, lineNumber, excel.tabIntStyleCenterBold);
                    excel.setTotalX(initLine, lineNumber - 1, 2, lineNumber);
                    initLine = lineNumber + 1;
                    previousMarketId = marketId;
                    previousMarket = market;
                    lineNumber++;
                }
                excel.insertCellTab(0, lineNumber, formatStrToCell(order.getString("name_equipment"), 10));
                excel.insertCellTabDouble(1, lineNumber, order.getInteger("amount")*1.d);
                excel.insertCellTabDouble(2, lineNumber, safeGetDouble(order,"total", "Market"));
                lineNumber++;
            }
            excel.insertLabelBold(0, lineNumber, market);
            excel.setTotalXWithStyle(initLine, lineNumber - 1, 1, lineNumber, excel.tabIntStyleCenterBold);
            excel.setTotalX(initLine, lineNumber - 1, 2, lineNumber);
            initLine = lineNumber + 2;
//            excel.insertCellTabDoubleWithPrice(3, lineNumber, Double.parseDouble(campaignData.getString("totalprice")));
            lineNumber += 2;
            if(i <= 1){
                excel.autoSize(4);
            }
        }


    }


    @Override
    protected void setLabels() {
        excel.insertHeader(0, lineNumber, MARKET_LABEL);
        excel.insertHeader(1, lineNumber, AMOUNT);
        excel.insertHeader(2, lineNumber, TOTAL);
        lineNumber++;
    }





    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "       With values as  (             " +
                "     SELECT  orders.id ,orders.\"price TTC\",  " +
                getTotalPriceTTCWithOptions() +
                "             as Total, contract.name as market, contract_type.code as code,    " +
                "             program.name as program,         CASE WHEN orders.id_order_client_equipment is not null  " +
                "             THEN  (select oce.name FROM " + Lystore.lystoreSchema + ".order_client_equipment oce    " +
                "              where oce.id = orders.id_order_client_equipment limit 1)     " +
                "             ELSE ''      " +
                "             END as old_name,     " +
                "             orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,  orders.id as id,  " +
                "             program_action.id_program, orders.amount ,contract.id as market_id,   campaign.name as campaign, orders.comment, project.room, orders.isregion, " +
                "             project.stair,project.building,    " +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  true as isregion, ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation ," +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             UNION      " +
                "             (select oce.id ,  false as isregion," +
                "             CASE WHEN price_proposal is null then  price + (price*tax_amount/100)  else price_proposal end as \"price TTC\", " +
                "             amount, creation_date, null as modification_date, name,  " +
                "             summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, " +
                "             id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action,  " +
                "             id_operation, override_region           from " + Lystore.lystoreSchema + ".order_client_equipment  oce) " +
                "             ) as orders       " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id   and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id)                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id" +
                " AND   contract_type.code != '236')      " + // a modifier pour non subventions
                "             INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".project ON orders.id_project = project.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        if (isCMR)
            query += "   AND (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "')  ";
        else {
            query +=
                    "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "')  " +
                            "     OR                    " +
                            " (spa.structure_type = '" + LYCEE + "' AND " +
                            "   ( specific_structures.type is null OR  specific_structures.type ='" + LYCEE + "') ))    ";
        }
        query += "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +
                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region , orders.comment,campaign.name , orders.id," +
                "               orders.isregion, " +
                "              project.room,project.stair, project.building " +
                "             order by campaign,id_structure,code,market_id, id_structure,program,code " +
                "  )    SELECT  values.campaign as campaign,    array_to_json(array_agg(values))as actions ,SUM(values.total) as totalPrice " +
                "  from  values      " +
                "  Group by values.campaign   " +
                "  Order by values.campaign   ;";


        sqlHandler(handler);

    }
}
