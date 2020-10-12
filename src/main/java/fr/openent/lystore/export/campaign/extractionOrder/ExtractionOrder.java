package fr.openent.lystore.export.campaign.extractionOrder;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;

import java.util.ArrayList;
import java.util.List;

public class ExtractionOrder extends TabHelper {
    Integer id_campaign;
    List<Integer> ids_campaigns;

    public ExtractionOrder(Workbook workbook, List<Integer> ids) {
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
        setLabels();
        datas =sortByUai(datas);
        excel.autoSize(40);
        setDatas();
    }

    private void setDatas() {
        log.info(datas.getJsonObject(0));
        for(int i = 0; i < datas.size() ; i++) {
            JsonObject data =  datas.getJsonObject(i);
            insertDates(i, data);
            insertStruturesInfosFromData(i, data);
            setProjectAndCampaignsDatas(i, data);
            //TODO check pour ORE
            insertEquipmementDatas(i, data);
            excel.insertCellTabInt(35,5+i,41);
            excel.insertCellTabInt(36,5+i,42);
            excel.insertCellTabInt(37,5+i,43);
            excel.insertCellTabInt(38,5+i,44);
            excel.insertCellTabInt(39,5+i,45);
        }
    }

    private void insertEquipmementDatas(int i, JsonObject data) {
        if(data.getString("order_origin").equals("REGION"))
            excel.insertCellTab(18,5+i,  "R-" + data.getInteger("id").toString());
        else
            excel.insertCellTab(18,5+i,  "E-" + data.getInteger("id").toString());
        excel.insertCellTab(19,5+i,data.getString("order_origin"));
        excel.insertCellTab(20,5+i,"TODO");
        excel.insertCellTab(21,5+i,data.getString("orders_date"));
        excel.insertCellTab(22,5+i,data.getString("status"));
        excel.insertCellTab(23,5+i,(!data.getString("priority_order").equals("-1") ? data.getString("priority_order"): "" ));
        excel.insertCellTab(24,5+i,(data.getLong("priority_project") != -1 ? data.getLong("priority_project").toString(): "" ));
        excel.insertCellTab(25,5+i,data.getString("comment"));
        excel.insertCellTab(26,5+i,data.getString("filename"));
        excel.insertCellTab(27,5+i,data.getString("equipment_name"));
        excel.insertCellTabDouble(28,5+i,safeGetDouble(data,"quantity","ExtractionOrder"));
        excel.insertCellTabDouble(29,5+i,safeGetDouble(data,"priceht","ExtractionOrder"));
        excel.insertCellTabDouble(30,5+i,safeGetDouble(data,"tva","ExtractionOrder"));
        excel.insertWithStyle(31,5+i,
                (safeGetDouble(data,"price_proposal","ExtractionOrder")!=-1.d ? safeGetDouble(data,"price_proposal","ExtractionOrder") : ""),excel.tabCurrencyStyle );
        excel.insertCellTabDouble(32,5+i,safeGetDouble(data,"total","ExtractionOrder"));
        excel.insertCellTabInt(33,5+i,35);
        excel.insertCellTabInt(34,5+i,36);
        excel.insertCellTabInt(35,5+i,37);
    }

    private void setProjectAndCampaignsDatas(int i, JsonObject data) {
        excel.insertCellTab(7,5+i,data.getString("campaign_name"));
        Double purse_amount = safeGetDouble(data,"purse_amount","ExtractionOrder");
        if(purse_amount != -1.d)
            excel.insertCellTabDoubleWithPrice(8,5+i,purse_amount);
        else
            excel.insertCellTab(8,5+i,"");
        excel.insertCellTab(11,5+i,data.getBoolean("campaign_open") ? "Ouverte" : "Fermée");
        excel.insertCellTab(12, 5+i, data.getString("project_name"));
        excel.insertCellTab(13, 5+i, data.getString("project_comment"));


        StringBuilder structure_groups = new StringBuilder();
        structure_groups.append("{") ;
        for (int j = 0 ; j< data.getJsonArray("structure_groups").size() ; j++){
            structure_groups.append(data.getJsonArray("structure_groups").getJsonArray(j).getString(1));
            if(j!= data.getJsonArray("structure_groups").size()-1){
                structure_groups.append(",");
            }
        }
        structure_groups.append("}");
        excel.insertCellTab(14, 5+i  , structure_groups.toString());

        StringBuilder tags = new StringBuilder();
        tags.append("{") ;
        for (int j = 0 ; j< data.getJsonArray("tags_name").size() ; j++){
            tags.append(data.getJsonArray("tags_name").getJsonArray(j).getString(1));
            if(j!= data.getJsonArray("tags_name").size()-1){
                tags.append(",");
            }
        }
        tags.append("}");
        excel.insertCellTab(15, 5+i  , tags.toString());

        excel.insertCellTab(16,5+i,data.getString("project_room"));
        excel.insertCellTab(17,5+i,data.getString("project_building"));
    }

    private void insertStruturesInfosFromData(int i, JsonObject data) {
        excel.insertCellTab(0, 5+i, data.getString("uai"));
        excel.insertCellTab(1, 5+i, data.getString("type"));
        excel.insertCellTab(2, 5+i, data.getString("nameEtab"));
        excel.insertCellTab(3, 5+i, data.getString("zipCode").substring(0,2));
        excel.insertCellTab(4, 5+i, data.getString("city"));
        excel.insertCellTab(5, 5+i, data.getString("cite_mixte"));
        excel.insertCellTab(6, 5+i, data.getString("academy"));
    }

    private void insertDates(int i, JsonObject data) {
        try{
            excel.insertCellTab(10,5+i,data.getString("campaign_end_date"));
        }catch (NullPointerException ignored){
            excel.insertCellTab(10,5+i,"");
        }
        try{
            excel.insertCellTab(9,5+i,data.getString("campaign_start_date"));
        }catch (NullPointerException ignored){
            excel.insertCellTab(9,5+i,"");
        }
    }


    @Override
    protected void setLabels() {
        excel.insertBlackTitleHeaderBorderless(1,1,"Lsytore - Extraction Demande ");

        excel.insertBlackTitleHeaderBorderless(1,2,"Campagne : " + datas.getJsonObject(0).getString("campaign_name"));
        sizeMergeRegion(2,0,3);
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
        excel.insertBlackTitleHeader(24,3,"Éléments comptables");
        sizeMergeRegion(3,35,33);
        setAccountingElementsLabel();
//        excel.insertBlackTitleHeader(34,3,"Gestion");
//        sizeMergeRegion(3,34,39);
//        setManagementLabel();
    }

    private void setOptionsEquipmentLabel() {
        excel.insertWithStyle(33,4,"Options",excel.labelOnOrange);
        excel.insertWithStyle(34,4,"Montant total TTC Options",excel.labelOnOrange);
    }

    private void setManagementLabel() {
        excel.insertWithStyle(34,4,"Exercice",excel.labelOnGreen);
        excel.insertWithStyle(35,4,"Opération",excel.labelOnGreen);
        excel.insertWithStyle(36,4,"Rapport",excel.labelOnGreen);
        excel.insertWithStyle(37,4,"Numéro CP",excel.labelOnGreen);
        excel.insertWithStyle(38,4,"Date CP",excel.labelOnGreen);
        excel.insertWithStyle(39,4,"Statut Rapport CP",excel.labelOnGreen);

    }

    private void setAccountingElementsLabel() {
        excel.insertWithStyle(35,4,"Marché support",excel.labelOnYellow);
        excel.insertWithStyle(36,4,"Numéro Marché",excel.labelOnYellow);
        excel.insertWithStyle(37,4,"Titulaire Marché",excel.labelOnYellow);
        excel.insertWithStyle(38,4,"Correspondant Région",excel.labelOnYellow);
        excel.insertWithStyle(39,4,"Nature Compable",excel.labelOnYellow);
        excel.insertWithStyle(40,4,"Chapitre Budgétaire",excel.labelOnYellow);
        excel.insertWithStyle(41,4,"Code fonctionnel",excel.labelOnYellow);
        excel.insertWithStyle(42,4,"Programme",excel.labelOnYellow);
        excel.insertWithStyle(43,4,"Libellé Programme",excel.labelOnYellow);
        excel.insertWithStyle(44,4,"Action",excel.labelOnYellow);
        excel.insertWithStyle(44,4,"Libellé Action",excel.labelOnYellow);
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
        query = " WITH info_group_and_tag as (SELECT tag.name , rgc.id_campaign as id_campaign , rgs.id_structure as id_struct, structure_group.name as group_name " +
                "     FROM lystore.tag  " +
                "     INNER JOIN lystore.rel_group_campaign rgc " +
                "              on tag.id = rgc.id_tag  " +
                "     INNER JOIN lystore.structure_group on rgc.id_structure_group = structure_group.id " +
                "     INNER JOIN lystore.rel_group_structure rgs on structure_group.id = rgs.id_structure_group " +
                ") " +
                "SELECT  " +
                "  DISTINCT orders.id," +
                "  orders.creation_date as orders_date," +
                "  campaign.name as campaign_name,  " +
                "  orders.\"price TTC\" as priceTTC,  " +
                "  orders.amount as quantity,  " +
                "  orders.name as equipment_name,  " +
                "  orders.id_structure,  " +
                "  orders.status,  " +
                "  title.name as project_name,  " +
                "  CASE when project.description is NULL THEN '' ELSE project.description END as project_comment,  " +
                "  CASE when project.room is NULL THEN '' ELSE project.room END as project_room,  " +
                "  CASE when project.building is NULL THEN '' ELSE project.building END as project_building,  " +
                "  CASE when orders.override_region IS NULL THEN 'REGION' ELSE 'EPLE' END as order_origin, "+
                "CASE WHEN(SELECT filename FROM lystore.order_file WHERE order_file.id_order_client_equipment = orders.id AND  orders.override_region is false) IS NULL THEN '' \n" +
                "  ELSE (SELECT filename FROM lystore.order_file WHERE order_file.id_order_client_equipment = orders.id AND  orders.override_region is false) END\n" +
                "  as filename,"+
                "  campaign.start_date as campaign_start_date,  " +
                "  campaign.end_date campaign_end_date,  " +
                "  CASE WHEN campaign.purse_enabled IS TRUE THEN ( " +
                "    SELECT  " +
                "      purse.initial_amount  " +
                "    FROM  " +
                "      lystore.purse  " +
                "    WHERE  " +
                "      purse.id_campaign = campaign.id  " +
                "      AND purse.id_structure = orders.id_structure " +
                "  ) ELSE -1 END as purse_amount,  " +
                "  CASE WHEN orders.comment IS NULL THEN '' ELSE orders.comment END as comment,  " +
                "  array_agg(info_group_and_tag.name) as tags_name, " +
                "  array_agg(DISTINCT info_group_and_tag.group_name) as structure_groups, " +
                "  campaign.accessible as campaign_open,  " +
                "  CASE WHEN orders.prio IS NULL THEN -1 ELSE orders.prio END as priority_order,  " +
                "  CASE WHEN project.preference IS NULL THEN -1 ELSE project.preference END as priority_project,  " +
                "  CASE WHEN ss.type IS NULL THEN ' ' ELSE ss.type END AS cite_mixte," +
                "  CASE WHEN orders.tax_amount = -1 THEN 20 ELSE orders.tax_amount END as TVA,  " +
                "  CASE WHEN orders.priceHT = -1 THEN orders.\"price TTC\"/1.2 ELSE orders.priceHT END as priceHT,  " +
                " CASE WHEN orders.price_proposal IS NULL THEN -1 ELSE orders.price_proposal END as price_proposal,  " +
                "    Round( " +
               " orders.\"price TTC\"  * orders.amount,  2 " +
                "  ) AS Total," +
                "CASE WHEN(SELECT filename FROM lystore.order_file WHERE order_file.id_order_client_equipment = orders.id AND  orders.override_region is false) IS NULL THEN ''  " +
                "  ELSE (SELECT filename FROM lystore.order_file WHERE order_file.id_order_client_equipment = orders.id AND  orders.override_region is false) END " +
                "  as filename, " +
                "FROM  " +
                "  lystore.allorders orders  " +
                "  INNER JOIN lystore.campaign ON campaign.id = orders.id_campaign  " +
                "  AND id_campaign IN  " + Sql.listPrepared(ids_campaigns) +
                "  INNER JOIN lystore.project ON orders.id_project = project.id  " +
                "  INNER JOIN lystore.title ON project.id_title = title.id  " +
                "  INNER JOIN info_group_and_tag ON (info_group_and_tag.id_campaign  = campaign.id AND orders.id_structure = info_group_and_tag.id_struct ) " +
                "  LEFT JOIN lystore.specific_structures ss ON ss.id = orders.id_structure  " +
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
                "  project.building," +
                "   orders.override_region," +
                "   project.preference," +
                " orders.creation_date," +
                "orders.price_proposal ; ";

        JsonArray params = new JsonArray(ids_campaigns);
        sqlHandler(handler,params);
    }
}
