package fr.openent.lystore.export.campaign.extractionOrder;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

public class ExtractionOrder extends TabHelper {
    Integer id_campaign;
    public ExtractionOrder(Workbook workbook, Integer id) {
        super(workbook,"Extraction");
        id_campaign = id;
    }

    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }

    @Override
    protected  void fillPage(JsonArray structures){
        setStructuresFromDatas(structures);
        setLabels();
        setDatas();
        excel.autoSize(40);
    }

    private void setDatas() {
        log.info(datas.getJsonObject(0));
        for(int i = 0; i < datas.size() ; i++) {
            JsonObject data =  datas.getJsonObject(i);
            excel.insertCellTab(0, 5+i, data.getString("uai"));
            excel.insertCellTab(1, 5+i, data.getString("type"));
            excel.insertCellTab(2, 5+i, data.getString("nameEtab"));
            excel.insertCellTab(3, 5+i, data.getString("zipCode").substring(0,2));
            excel.insertCellTab(4, 5+i, data.getString("city"));
            excel.insertCellTab(5, 5+i, data.getString("cite_mixte"));
            excel.insertCellTab(6, 5+i, data.getString("academy"));
            excel.insertCellTabInt(7, 5+i, 7);
            excel.insertCellTabInt(8, 5+i, 8);
            excel.insertCellTabInt(9, 5+i,9);
            excel.insertCellTabInt(10, 5+i, 10);
            excel.insertCellTabInt(11, 5+i, 11);
            excel.insertCellTabInt(12, 5+i, 12);
            excel.insertCellTabInt(13, 5+i, 13);
            excel.insertCellTabInt(14, 5+i,14);
            excel.insertCellTab(15, 5+i, data.getString("status"));
            excel.insertCellTabInt(16, 5+i, 16);
            excel.insertCellTab(17, 5+i, data.getString("priority_order"));
            excel.insertCellTab(18, 5+i, data.getString("comment"));
            excel.insertCellTabInt(19, 5+i, 19);
            //TODO check pour ORE
            excel.insertCellTab(20,5+i,data.getString("equipment_name"));
            excel.insertCellTabDouble(21,5+i,safeGetDouble(data,"quantity","ExtractionOrder"));
            excel.insertCellTabDouble(22,5+i,safeGetDouble(data,"priceht","ExtractionOrder"));
            excel.insertCellTabDouble(23,5+i,safeGetDouble(data,"tva","ExtractionOrder"));
            excel.insertCellTabDouble(24,5+i,safeGetDouble(data,"quantity","ExtractionOrder"));
            excel.insertCellTabInt(25,5+i,25);
            excel.insertCellTabInt(26,5+i,26);
            excel.insertCellTabInt(27,5+i,27);
            excel.insertCellTabInt(28,5+i,28);
            excel.insertCellTabInt(29,5+i,29);
            excel.insertCellTabInt(30,5+i,30);
            excel.insertCellTabInt(31,5+i,31);
            excel.insertCellTabInt(32,5+i,32);
            excel.insertCellTabInt(34,5+i,34);
            excel.insertCellTabInt(35,5+i,35);
            excel.insertCellTabInt(36,5+i,36);
            excel.insertCellTabInt(37,5+i,37);
            excel.insertCellTabInt(38,5+i,38);
            excel.insertCellTabInt(39,5+i,39);
            excel.insertCellTabInt(40,5+i,40);
            excel.insertCellTabInt(41,5+i,41);

        }
    }


    @Override
    protected void setLabels() {
        excel.insertBlackTitleHeaderBorderless(1,1,"Lsytore - Extraction Demande ");
        excel.insertBlackTitleHeaderBorderless(1,2,"Campagne : " + datas.getJsonObject(0).getString("campaign_name"));
        excel.insertBlackTitleHeader(0,3,"Informations des structures");
        sizeMergeRegion(3,0,6);
        setStructuresInfoLabel();
        excel.insertBlackTitleHeader(7,3,"Projet");
        sizeMergeRegion(3,7,19);
        setProjectLabel();
        excel.insertBlackTitleHeader(20,3,"Équipements");
        sizeMergeRegion(3,20,25);
        setEquipmentLabel();
        excel.insertBlackTitleHeader(26,3,"Éléments comptables");
        sizeMergeRegion(3,26,35);
        setAccountingElementsLabel();
        excel.insertBlackTitleHeader(36,3,"Gestion");
        sizeMergeRegion(3,36,41);
        setManagementLabel();
    }

    private void setManagementLabel() {
        excel.insertWithStyle(36,4,"Exercice",excel.labelOnGreen);
        excel.insertWithStyle(37,4,"Opération",excel.labelOnGreen);
        excel.insertWithStyle(38,4,"Rapport",excel.labelOnGreen);
        excel.insertWithStyle(39,4,"Numéro CP",excel.labelOnGreen);
        excel.insertWithStyle(40,4,"Date CP",excel.labelOnGreen);
        excel.insertWithStyle(41,4,"Statut Rapport CP",excel.labelOnGreen);

    }

    private void setAccountingElementsLabel() {
        excel.insertWithStyle(26,4,"Marché support",excel.labelOnYellow);
        excel.insertWithStyle(27,4,"Correspondant région",excel.labelOnYellow);
        excel.insertWithStyle(28,4,"Nature Comptable",excel.labelOnYellow);
        excel.insertWithStyle(29,4,"Libellé Nature Comptable",excel.labelOnYellow);
        excel.insertWithStyle(30,4,"Chapître budgétaire",excel.labelOnYellow);
        excel.insertWithStyle(31,4,"Code fonctionnel",excel.labelOnYellow);
        excel.insertWithStyle(32,4,"Programme",excel.labelOnYellow);
        excel.insertWithStyle(33,4,"Libellé programme",excel.labelOnYellow);
        excel.insertWithStyle(34,4,"Action",excel.labelOnYellow);
        excel.insertWithStyle(35,4,"Libellé Action",excel.labelOnYellow);
    }

    private void setEquipmentLabel() {
        excel.insertWithStyle(20,4,"Equipement",excel.labelOnOrange);
        excel.insertWithStyle(21,4,"Quantité proposée",excel.labelOnOrange);
        excel.insertWithStyle(22,4,"Prix unitaire HT",excel.labelOnOrange);
        excel.insertWithStyle(23,4,"Taux TVA",excel.labelOnOrange);
        excel.insertWithStyle(24,4,"Prix unitaire modifié TTC",excel.labelOnOrange);
        excel.insertWithStyle(25,4,"Somme Montant proposée TTC",excel.labelOnOrange);

    }

    private void setProjectLabel() {
        excel.insertWithStyle(7,4,"Projet",excel.labelOnBlueGrey);
        excel.insertWithStyle(8,4,"Commentaire projet",excel.labelOnBlueGrey);
        excel.insertWithStyle(9,4,"Regroupements",excel.labelOnBlueGrey);
        excel.insertWithStyle(10,4,"Labels équipements",excel.labelOnBlueGrey);
        excel.insertWithStyle(11,4,"Salle",excel.labelOnBlueGrey);
        excel.insertWithStyle(12,4,"Batiment",excel.labelOnBlueGrey);
        excel.insertWithStyle(13,4,"Origine EPLE/REGION",excel.labelOnBlueGrey);
        excel.insertWithStyle(14,4,"Date demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(15,4,"Status Demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(16,4,"Priorité Projet",excel.labelOnBlueGrey);
        excel.insertWithStyle(17,4,"Priorité Demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(18,4,"Commentaire demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(19,4,"Pièce jointe",excel.labelOnBlueGrey);
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
        query = " SELECT DISTINCT orders.id," +
                "      campaign.name as campaign_name ," +
                "    orders.\"price TTC\" as priceTTC," +
                "    orders.amount as quantity," +
                "   orders.name as equipment_name ," +
                "    orders.priceHT as priceHT," +
                "    orders.tax_amount as TVA, "+
                "    orders.id_structure," +
                "    orders.status, " +
                "    orders.comment ," +
                " CASE " +
                "   orders.prio IS NULL" +
                " THEN ' '" +
                " ELSE orders.prio END  as priority_order, " +
                "     CASE  " +
                "      WHEN ss.type IS NULL THEN ' '  " +
                "      ELSE ss.type  " +
                "    END   AS cite_mixte , " +
                "     SUM(Round(( (SELECT CASE  " +
                "               WHEN orders.price_proposal IS NOT NULL THEN 0  " +
                "               WHEN orders.override_region IS NULL THEN 0  " +
                "               WHEN Sum(oco.price + ( (  " +
                "                      oco.price * oco.tax_amount ) / 100 )  " +
                "                                  *  oco.amount) IS  NULL " +
                "                       THEN 0  " +
                "                       ELSE Sum(oco.price + ( ( oco.price * oco.tax_amount ) / 100 )  * oco.amount)  " +
                "                      END  " +
                "               FROM    " + Lystore.lystoreSchema +".order_client_options oco  " +
                "        WHERE  oco.id_order_client_equipment = orders.id)  + orders.\"price TTC\" ) * orders.amount, 2)) AS Total " +
                "FROM    " + Lystore.lystoreSchema +".allorders orders  " +
                "       INNER JOIN  " + Lystore.lystoreSchema +".campaign  " +
                "               ON campaign.id = orders.id_campaign  " +
                "                  AND id_campaign = ?  " +
                "       LEFT JOIN  " + Lystore.lystoreSchema +".specific_structures ss  " +
                "              ON ss.id = orders.id_structure  " +
                "GROUP  BY " +
                "         orders.id," +
                "         id_structure,  " +
                "          campaign.NAME,  " +
                "          ss.type," +
                " orders.\"price TTC\" ," +
                "   quantity," +
                "   TVA," +
                "   priceHT," +
                " equipment_name," +
                " orders.status," +
                "  orders.comment , " +
                " priority_order ; ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(id_campaign);
        sqlHandler(handler,params);
    }
}
