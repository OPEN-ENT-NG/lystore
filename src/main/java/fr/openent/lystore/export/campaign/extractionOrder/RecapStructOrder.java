package fr.openent.lystore.export.campaign.extractionOrder;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.model.Campaign;
import fr.openent.lystore.model.Campaign.SummaryOrder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;

import java.text.SimpleDateFormat;
import java.util.*;

public class RecapStructOrder  extends TabHelper {
    List<Integer> ids_campaigns;
    ArrayList<Campaign> campaigns = new ArrayList<>();
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
        initObjects();
        setLabels();
        setDatas();
    }

    private void initObjects() {
        Map<Long, Campaign> campaignMap = new HashMap<>();
        for(int i = 0 ; i < datas.size() ; i ++){
            JsonObject data = datas.getJsonObject(i);
            Long idCampaign  = data.getLong("id_campaign");
            Campaign campaign ;
            if(!campaignMap.containsKey(idCampaign)){
                campaign = new Campaign();
                campaign.setId(idCampaign.toString());
                campaign.setName(data.getString("name"));
                campaignMap.put(idCampaign,campaign);
                campaigns.add(campaign);
            }else {
                campaign = campaignMap.get(idCampaign);
            }
            SummaryOrder summaryOrder = new SummaryOrder();
            summaryOrder.setNumberOrders(data.getLong("nb_orders"));
            summaryOrder.setOrigin(data.getString("order_origin"));
            summaryOrder.setStatus(data.getString("status"));
            campaign.addSummaryOrder(summaryOrder);
        }
    }

    private void setDatas() {
        int nbLine = 3;
        int startSum = nbLine;
        for(int i = 0; i < campaigns.size() ; i++){
            Campaign campaign = campaigns.get(i);
            for(int j = 0 ;j < campaign.getSummaryOrders().size(); j++) {
                SummaryOrder  summaryOrder = campaign.getSummaryOrders().get(j);
                excel.insertWithStyle(0, nbLine, campaign.getId(),excel.tabStringStyleRight);
                excel.insertWithStyle(1, nbLine, campaign.getName(),excel.tabStringStyle);
                excel.insertWithStyle(2, nbLine, summaryOrder.getOrigin(),excel.tabStringStyle);
                excel.insertWithStyle(3, nbLine, summaryOrder.getStatus(),excel.tabStringStyle);
                excel.insertWithStyle(4, nbLine, summaryOrder.getNumberOrders(),excel.tabIntStyleRight);
                nbLine ++ ;
                if(nbLine == 10){
                    excel.autoSize(40);
                }
            }
            excel.insertCellTab(0, nbLine,EMPTY);
            excel.insertWithStyle(1, nbLine,"Total " + campaign.getName(),excel.labelBoldStyle);
            excel.insertCellTab(2, nbLine,EMPTY);
            excel.insertCellTab(3, nbLine,EMPTY);
            excel.setTotalXWithStyle(startSum,nbLine-1,4,nbLine,excel.tabIntRedBoldRight);
            startSum = nbLine++;
//            nbLine++;

        }
        if(nbLine < 10){
            excel.autoSize(40);
        }
    }

    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        boolean errorCatch = false;
        String errorSTR = "" ;
        try {
            fillPage(new JsonArray());
        }catch (Exception e){
            errorCatch = true;
            errorSTR = e.getMessage();
            log.error("------------------------------ERROR---------------------------");
            for (StackTraceElement elem : e.getStackTrace()) {
                log.error("\t\t"+ elem);
            }
            log.error("-------------------------END ERROR---------------------------");

        }
        HandleCatchResult(errorCatch, errorSTR, new JsonArray(), handler);
    }

    @Override
    protected void setLabels() {
        SimpleDateFormat formatterDateExcel = new SimpleDateFormat("dd.MM.yyyy");
        Date orderDate =  new Date();
        excel.insertBlackTitleHeaderBorderless(1,1,"Lystore - Extraction Demande - " + formatterDateExcel.format(orderDate));
        setStructuresInfoLabel();
    }

    private void setStructuresInfoLabel() {
        excel.insertWithStyle(0,2,"Identifiant Campagne",excel.labelOnBlueGrey);
        excel.insertWithStyle(1,2,"Libellé Campagne",excel.labelOnBlueGrey);
        excel.insertWithStyle(2,2,"Origine Demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(3,2,"Status Demande",excel.labelOnBlueGrey);
        excel.insertWithStyle(4,2,"Nombre Demandes",excel.labelOnBlueGrey);
    }



    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = " SELECT  " +
                "  DISTINCT orders.id_campaign,  " +
                "  Count(orders.id) AS nb_orders,  " +
                "  campaign.NAME,  " +
                "  CASE when orders.override_region IS NULL THEN 'REGION' ELSE 'EPLE' END as order_origin,  " +
                "  orders.status  " +
                "FROM  " +
                  Lystore.lystoreSchema + ".allorders orders  " +
                "  INNER JOIN " +  Lystore.lystoreSchema + ".campaign ON campaign.id = orders.id_campaign  " +
                "  AND id_campaign IN " + Sql.listPrepared(ids_campaigns) +
                "WHERE  " +
                "  orders.override_region IS NOT TRUE  " +
                "GROUP BY  " +
                "  orders.override_region,  " +
                "  orders.status,  " +
                "  id_campaign,  " +
                "  campaign.NAME; " 
               
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
