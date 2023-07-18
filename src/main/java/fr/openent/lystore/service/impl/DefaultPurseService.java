package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.model.Purse;
import fr.openent.lystore.model.Structure;
import fr.openent.lystore.model.utils.Domain;
import fr.openent.lystore.service.PurseService;
import fr.openent.lystore.utils.LystoreUtils;
import fr.openent.lystore.utils.OrderUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.openent.lystore.constants.LystoreBDD.TOTAL_ORDER;

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
    public Future<List<Purse>> getPursesByCampaignId(Integer campaignId) {
        Promise<List<Purse>> promise = Promise.promise();
        String query = "   " +
                " WITH orders as (SELECT oce.id_structure, " +
                "               oce.id_campaign, " +
                "               Sum(( (SELECT CASE " +
                "                               WHEN oce.price_proposal IS NOT NULL THEN 0 " +
                "                               WHEN Sum(oco.price + ( oco.price * oco.tax_amount " +
                "                                                      / 100 " +
                "                                                    ) * " +
                "                                                    oco.amount " +
                "                                    ) IS " +
                "                                    NULL THEN 0 " +
                "                               ELSE Sum(Round(oco.price + ( " +
                "                                              oco.price * oco.tax_amount " +
                "                                              / 100 ) " +
                "                                                          * " +
                "                                        oco.amount, 2)) " +
                "                             END " +
                "                      FROM     " + Lystore.lystoreSchema + ".order_client_options oco " +
                "                      WHERE  oco.id_order_client_equipment = oce.id) " +
                "                     + Round((oce.price + oce.price * oce.tax_amount /100), 2) ) " +
                "                   * " +
                "                   oce.amount) " +
                "               AS total_order " +
                "        FROM     " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "               INNER JOIN   " + Lystore.lystoreSchema + ".purse " +
                "                       ON purse.id_campaign = oce.id_campaign " +
                "                          AND oce.id_structure = purse.id_structure " +
                "        GROUP  BY oce.id_structure, " +
                "                  oce.id_campaign " +
                "        ORDER  BY id_structure) " +
                "SELECT purse.*, " +
                "       orders.total_order " +
                "FROM      " + Lystore.lystoreSchema + ".purse " +
                "left join orders " +
                "               ON orders.id_structure = purse.id_structure AND  orders.id_campaign = purse.id_campaign " +
                "WHERE  " +
                "        purse.id_campaign = ? ;  ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaignId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                List<Purse> purses = event.right().getValue().stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .map(Purse::new)
                        .collect(Collectors.toList());
                promise.complete(purses);
            } else {
                log.error(LystoreUtils.generateErrorMessage(this.getClass(),"getPursesByCampaignId",
                        "error when getting purses" , event.left().getValue()));
                promise.fail(event.left().getValue());
            }
        }));
        return promise.future();
    }

    @Override
    @Deprecated
    public void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        getPursesByCampaignId(campaignId)
                .onSuccess(result ->
                        handler.handle(new Either.Right<>(new JsonArray(result.stream().map(Purse::toJsonObject).collect(Collectors.toList())))))
                .onFailure(error -> {
                    log.error(LystoreUtils.generateErrorMessage(this.getClass(),"getPursesByCampaignId",
                            "error when getting purses" , error));
                    handler.handle(new Either.Left<>(error.getMessage()));
                });
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

    public void update(Integer id, double totalOrder, double initialAmount, Handler<Either<String, JsonObject>> handler) {

        String query = "UPDATE lystore.purse " +
                " SET amount =  ? , initial_amount = ?" +
                " WHERE id = ? returning * ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(initialAmount - totalOrder)
                .add(initialAmount)
                .add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
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

    private static String getCSVLine(Structure structure, Purse purse) {
        DecimalFormat df = new DecimalFormat("####0.00");
        return structure.getUAI()
                + ";" + structure.getName()
                + ";" + df.format(purse.getAmount())
                + ";" + df.format(purse.getInitialAmount())
                + ";" + df.format(purse.getTotalOrder()) + "\n";
    }


    private static String getCSVHeader(Domain domain) {
        return I18n.getInstance().translate("UAI", domain.getHost(), domain.getLang()) + ";" +
                I18n.getInstance().translate("lystore.name", domain.getHost(), domain.getLang()) + ";" +
                I18n.getInstance().translate("purse", domain.getHost(), domain.getLang()) + ";" +
                I18n.getInstance().translate("lystore.campaign.purse.init", domain.getHost(), domain.getLang()) + ";" +
                I18n.getInstance().translate("lystore.campaign.purse.total_order", domain.getHost(), domain.getLang()) + ";" +
                "\n";
    }

    /**
     * Launch export. Build CSV based on values parameter
     * @param values values to export
     * @param domain domain to get i18n
     */
    @Override
    public Future<String> getExport(Map<Structure, Purse> values, Domain domain) {
        Promise<String> promise = Promise.promise();
        StringBuilder exportString = new StringBuilder(getCSVHeader(domain));
        for (Map.Entry<Structure, Purse> entry : values.entrySet()) {
            if(entry.getValue() != null )
                exportString.append(getCSVLine(entry.getKey(), entry.getValue()));
        }
        promise.complete(exportString.toString());
        return promise.future();
    }

    @Override
    public Future<Double> getTotalOrder(int id) {
        Promise<Double> promise = Promise.promise();
        String query = "SELECT  Sum(( (SELECT CASE " +
                "                               WHEN oce.price_proposal IS NOT NULL THEN 0 " +
                "                               WHEN Sum(oco.price + ( oco.price * oco.tax_amount " +
                "                                                      / 100 " +
                "                                                    ) * " +
                "                                                    oco.amount " +
                "                                    ) IS " +
                "                                    NULL THEN 0 " +
                "                               ELSE Sum(Round(oco.price + ( " +
                "                                              oco.price * oco.tax_amount " +
                "                                              / 100 )  * oco.amount, 2)) " +
                "                             END " +
                "                      FROM   " + Lystore.lystoreSchema + ".order_client_options oco " +
                "                      WHERE  oco.id_order_client_equipment = oce.id) " +
                "                     + Round((oce.price + oce.price * oce.tax_amount /100), 2) ) " +
                "                   * " +
                "                   oce.amount ) " +
                "               AS total_order " +
                "        FROM   " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "               INNER JOIN " + Lystore.lystoreSchema + ".purse " +
                "                       ON purse.id_campaign = oce.id_campaign " +
                "                          AND oce.id_structure = purse.id_structure " +
                "  Where purse.id = ?       " +
                "        GROUP  BY oce.id_structure, " +
                "                  oce.id_campaign";
        JsonArray params = new JsonArray();
        params.add(id);

        Sql.getInstance().prepared(query,params, SqlResult.validUniqueResultHandler(event -> {
            promise.complete(OrderUtils.safeGetDouble(event.right().getValue(),TOTAL_ORDER));
        }));

        return promise.future();
    }

}
