package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ImportCSVHelper;
import fr.openent.lystore.service.PurseService;
import fr.wseduc.webutils.Either;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultPurseService implements PurseService {
    private Boolean invalidDatas= false;
    private static final Logger log = LoggerFactory.getLogger(PurseService.class);

    @Override
    public void launchImport(Integer campaignId, JsonObject statementsValues,
                             final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String[] fields = statementsValues.fieldNames().toArray(new String[0]);
        invalidDatas = false;
        for (String field : fields) {
            statements.add(getImportStatement(campaignId, field,
                    statementsValues.getString(field)));

        }
        if(invalidDatas){
            handler.handle(new Either.Left<String, JsonObject>
                    ("lystore.invalid.data.to.insert"));
        }else  if (statements.size() > 0) {
            Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    if (message.body().containsKey("status") &&
                            "ok".equals(message.body().getString("status"))) {
                        handler.handle(new Either.Right<String, JsonObject>(
                                new JsonObject().put("status", "ok")));
                    } else {
                        handler.handle(new Either.Left<String, JsonObject>
                                ("lystore.statements.error"));
                    }
                }
            });
        } else {
            handler.handle(new Either.Left<String, JsonObject>
                    ("lystore.statements.empty"));
        }
    }

    @Override
    public void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema + ".purse" +
                " WHERE id_campaign = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaignId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private JsonObject getImportStatement(Integer campaignId, String structureId, String amount) {
        String statement = "INSERT INTO " + Lystore.lystoreSchema + ".purse(id_structure, amount, id_campaign, initial_amount) " +
                "VALUES (?, ?, ?,?) " +
                "ON CONFLICT (id_structure, id_campaign) DO UPDATE " +
                "SET amount = ?, " +
                " initial_amount = ? " +
                "WHERE purse.id_structure = ? " +
                "AND purse.id_campaign = ?;";
        JsonArray params =  new fr.wseduc.webutils.collections.JsonArray();
        try {
            params.add(structureId)
                    .add(Double.parseDouble(amount))
                    .add(campaignId)
                    .add(Double.parseDouble(amount))
                    .add(Double.parseDouble(amount))
                    .add(Double.parseDouble(amount))
                    .add(structureId)
                    .add(campaignId);

        }catch (NumberFormatException e){
            invalidDatas = true;
        }
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    public void update(Integer id, JsonObject purse, Handler<Either<String, JsonObject>> handler) {

        String query = "UPDATE lystore.purse " +
                "SET amount = amount + ( ? - initial_amount)  , initial_amount = ?" +
                "WHERE id = ? returning * ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(purse.getDouble("amount"))
                .add(purse.getDouble("amount"))
                .add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
//        Sql.getInstance().prepared(query, params, new Handler<Message<JsonObject>>() {
//            @Override
//            public void handle(Message<JsonObject> event) {
//                log.info(event.body());
//                String status = event.body().getString("status");
//
//                if(status.equals("ok")){
//                    handler
//                }else{
//                    String message = event.body().getString("message");
//                    if(message.contains("Check_amount_positive"))
//                        log.info("Pas de chance");
//                    else{
//                        log.info("plop");
//                    }
//                }
//            }
//        });
    }

    @Override
    public JsonObject updatePurseAmountStatement(Double price, Integer idCampaign, String idStructure,String operation) {
        final double cons = 100.0;
        String updateQuery = "UPDATE  " + Lystore.lystoreSchema + ".purse " +
                "SET amount = amount " +  operation + " ?  " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(Math.round(price * cons)/cons)
                .add(idCampaign)
                .add(idStructure);

        return new JsonObject()
                .put("statement", updateQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void checkPurses(Integer id, Handler<Either<String, JsonArray>> handler) {
        String query = "   " +
                "   SELECT initial_amount, amount ,initial_amount - (amount + orders.total_order )as difference , orders.total_order,purse.id_structure ,purse.id_campaign " +
                " " +
                "FROM  " +
                "( " +
                " SELECT " +
                "     oce.id_structure, " +
                "      oce.id_campaign, " +
                "       SUM( " +
                "        ( " +
                "            ( " +
                "                SELECT " +
                "                CASE WHEN oce.price_proposal is not null " +
                "              THEN 0 WHEN SUM( " +
                "                    oco.price + (oco.price * oco.tax_amount / 100) * oco.amount " +
                "                ) is NULL THEN 0 ELSE SUM( " +
                "                    ROUND(oco.price + (oco.price * oco.tax_amount / 100) * oco.amount,2) " +
                "                ) END " +
                "                FROM " +
                "                lystore.order_client_options oco " +
                "                where " +
                "                oco.id_order_client_equipment = oce.id " +
                "            ) + ROUND((oce.price + oce.price * oce.tax_amount /100),2) " +
                "        ) * oce.amount " +
                "       ) as total_order " +
                "       from " +
                "       lystore.order_client_equipment oce " +
                "       inner join lystore.purse ON purse.id_campaign = oce.id_campaign " +
                "       and oce.id_structure = purse.id_structure " +
                "    group by " +
                "    oce.id_structure, " +
                "    oce.id_campaign " +
                "    order by " +
                "    id_structure " +
                "    ) as orders " +
                "    INNER JOIN lystore.purse ON orders.id_structure = purse.id_structure  " +
                "WHERE  " +
                "   orders.id_campaign = purse.id_campaign  " +
                "  AND purse.id_campaign = ? " +
                "  order by difference; " +
                "   ";
        Sql.getInstance().prepared(query,new JsonArray().add(id), new DeliveryOptions().setSendTimeout(Lystore.timeout * 1000000000L),SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight())
                {
                    JsonArray results = event.right().getValue();
                    for(int i =0;i<results.size();i++){
                       JsonObject result = results.getJsonObject(i);
                       try {
                           result.put("substraction",  Double.parseDouble(result.getString("substraction")));
                       }catch (NullPointerException e){
                            result.put("substraction",0.d);
                       }
                    }
                    handler.handle(new Either.Right<>(results));
                }else {
                    handler.handle(new Either.Left<>("Error in SQL when checking purse"));
                }
            }
        }));
    }
}
