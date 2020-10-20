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

public class RecapStructOrder  extends TabHelper {
    List<Integer> ids_campaigns;

    public RecapStructOrder(Workbook workbook, List<Integer> ids) {
        super(workbook,"Récap_Extraction");
        ids_campaigns = ids;
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }

    @Override
    protected  void fillPage(JsonArray structures){
        setStructuresFromDatas(structures);
        setLabels();
//        setDatas();
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
            excel.insertWithStyle(7, 5+i, data.getInteger("nb_orders"),excel.tabIntStyleCenterBold);
            excel.insertCellTabDoubleWithPrice(8, 5+i, safeGetDouble(data,"total","RecapStructOrder"));
            if(i == 10)
            excel.autoSize(40);
        }
        if(datas.size() < 10){
            excel.autoSize(40);
        }
    }


    @Override
    protected void setLabels() {
        excel.insertBlackTitleHeaderBorderless(1,1,"Lsytore - Extraction Demande ");
        excel.insertBlackTitleHeader(0,3,"Informations des structures");
        sizeMergeRegion(3,0,6);
        excel.insertBlackTitleHeader(7,3,"");

        sizeMergeRegion(3,7,8);
        setStructuresInfoLabel();
    }

    private void setStructuresInfoLabel() {
        excel.insertWithStyle(0,4,"Identifiant Campagne",excel.labelOnBlueGrey);
        excel.insertWithStyle(1,4,"Libellé Campagne",excel.labelOnBlueGrey);
        excel.insertWithStyle(2,4,"Origine Demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(3,4,"Status Demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(4,4,"Nombre Demandes",excel.labelOnBlueGrey);
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
        query = " SELECT DISTINCT orders.id_structure,  " +
                "                Count(orders.id)                                   AS nb_orders,  " +
                "                campaign.NAME,  " +
                "                CASE  " +
                "                  WHEN ss.type IS NULL THEN ' '  " +
                "                  ELSE ss.type  " +
                "                END                                                AS cite_mixte  " +
                "                ,  " +
                "                SUM(Round(( (SELECT CASE  " +
                "                                  WHEN orders.price_proposal IS NOT NULL THEN 0  " +
                "                                  WHEN orders.override_region IS NULL THEN 0  " +
                "                                  WHEN Sum(oco.price + ( (  " +
                "                                           oco.price * oco.tax_amount ) / 100 )  " +
                "                                                       *  " +
                "                                                       oco.amount) IS  " +
                "                                       NULL THEN 0  " +
                "                                  ELSE Sum(oco.price + ( (  " +
                "                                           oco.price * oco.tax_amount ) / 100 )  " +
                "                                                       *  " +
                "                                                       oco.amount)  " +
                "                                END  " +
                "                         FROM    " + Lystore.lystoreSchema +".order_client_options oco  " +
                "                         WHERE  oco.id_order_client_equipment = orders.id)  " +
                "                        + orders.\"price TTC\" ) * orders.amount, 2)) AS Total " +
                "FROM    " + Lystore.lystoreSchema +".allorders orders  " +
                "       INNER JOIN  " + Lystore.lystoreSchema +".campaign  " +
                "               ON campaign.id = orders.id_campaign  " +
                "                  AND id_campaign IN "+ Sql.listPrepared(ids_campaigns) +
                "       LEFT JOIN  " + Lystore.lystoreSchema +".specific_structures ss  " +
                "              ON ss.id = orders.id_structure  " +
                "GROUP  BY id_structure,  " +
                "          campaign.NAME,  " +
                "          ss.type "
              ;
//                "    ,orders.\"price TTC\", " +
//                "    orders.price_proposal, " +
//                "    orders.override_region, " +
//                "    orders.amount "
        ;

        JsonArray params = new JsonArray(ids_campaigns);
        sqlHandler(handler,params);
    }
}
