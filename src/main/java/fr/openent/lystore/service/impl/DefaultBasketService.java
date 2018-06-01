package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.BasketService;
import fr.openent.lystore.service.NotificationService;
import fr.openent.lystore.service.PurseService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.http.Renders.getHost;

public class DefaultBasketService extends SqlCrudService implements BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBasketService.class);

    private static NotificationService notificationService;
    private PurseService purseService;

    public DefaultBasketService(String schema, String table, Vertx vertx, JsonObject slackConfiguration) {
        super(schema, table);
        this.purseService = new DefaultPurseService();
        notificationService = new SlackService(
                vertx,
                slackConfiguration.getString("api-uri"),
                slackConfiguration.getString("token"),
                slackConfiguration.getString("bot-username"),
                slackConfiguration.getString("channel")
        );
    }

    public void listBasket(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT basket.id, basket.amount, basket.processing_date," +
                "     basket.id_campaign, basket.id_structure " +
                "    , array_to_json(array_agg( e.* )) as equipment " +
                "    , array_to_json(array_agg(DISTINCT ep.*)) as options " +
                "    FROM " + Lystore.lystoreSchema + ".basket_equipment basket " +
                "    LEFT JOIN " + Lystore.lystoreSchema + ".basket_option " +
                "    ON basket_option.id_basket_equipment = basket.id " +
                "    LEFT JOIN " +
                "       (Select equipment_option.*, tax.value tax_amount " +
                "       FROM lystore.equipment_option " +
                "       INNER JOIN  lystore.tax on tax.id = equipment_option.id_tax ) ep "+
                "    ON basket_option.id_option = ep.id " +
                "    INNER JOIN " +
                "       (Select equipment.*, tax.value tax_amount " +
                "       FROM lystore.equipment INNER JOIN  lystore.tax " +
                "       ON tax.id = equipment.id_tax ) as e"+
                "    ON e.id = basket.id_equipment " +
                "    WHERE basket.id_campaign = ? " +
                "    AND basket.id_structure = ? " +
                "    GROUP BY " +
                "    (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure ); ";
        values.add(idCampaign).add(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }
    public void create(final JsonObject basket, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".basket_equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getInteger("id");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getBasketEquipmentCreationStatement(id, basket));

                        JsonArray options = basket.getJsonArray("options");
                        int i = 0;
                        while (null != options && i < options.size() ) {
                            statements.add(getBasketEquipmentOptionCreationStatement(id, options.getInteger(i)));
                            i++;
                        }

                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
                            }
                        });
                    } catch (ClassCastException e) {
                        LOGGER.error("An error occurred when casting tags ids", e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                } else {
                    LOGGER.error("An error occurred when selecting next val");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }
    public void delete(final Integer idBasket,final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getOptionsBasketDeletion(idBasket))
                .add(getEquipmentBasketDeletion(idBasket));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, idBasket));
            }
        });
    }
    public void updateAmount(Integer idBasket, Integer amount, Handler<Either<String, JsonObject>> handler ) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".basket_equipment " +
                " SET  amount = ? " +
                " WHERE id = ?; ";
        values.add(amount).add(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler) );
    }
    public void listebasketItemForOrder( Integer idCampaign, String idStructure,
                                         Handler<Either<String, JsonArray>> handler ){
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT  basket.id id_basket, basket.amount, basket.processing_date,  basket.id_campaign, " +
                "basket.id_structure, e.id id_equipment, e.name,e.summary, e.description, e.price, e.image, " +
                "e.id_contract, e.status, jsonb(e.technical_specs) technical_specs, e.tax_amount, " +
                " nextval('lystore.order_client_equipment_id_seq' ) as id_order, " +
                "Case Count(ep) " +
                "when 0 " +
                "then ROUND((e.price + ((e.price *  e.tax_amount) /100)), 2) * basket.amount " +
                "else (ROUND(e.price + ((e.price *  e.tax_amount) /100), 2) + SUM(ep.total_option_price)) * basket.amount " +
                "END as total_price, array_to_json(array_agg(DISTINCT ep.*)) as options  " +
                "FROM  lystore.basket_equipment basket  " +
                "LEFT JOIN lystore.basket_option ON basket_option.id_basket_equipment = basket.id " +
                "LEFT JOIN (Select equipment_option.*, tax.value tax_amount, " +
                "ROUND(equipment_option.price + ((equipment_option.price * tax.value )/100), 2) as total_option_price " +
                "FROM lystore.equipment_option  " +
                "INNER JOIN  lystore.tax ON tax.id = equipment_option.id_tax ) " +
                "ep ON basket_option.id_option = ep.id " +
                "INNER JOIN (Select equipment.*,  tax.value tax_amount  FROM lystore.equipment " +
                "INNER JOIN  lystore.tax  ON tax.id = equipment.id_tax " +
                "WHERE equipment.status = 'AVAILABLE') as e ON e.id = basket.id_equipment " +
                "WHERE basket.id_campaign = ?   AND basket.id_structure = ? " +
                "GROUP BY (basket.id, basket.amount, basket.processing_date,basket.id_campaign, basket.id_structure, " +
                "e.id, e.price, e.name, e.summary, e.description, e.price, e.image, e.id_contract, e.status," +
                " jsonb(e.technical_specs),  e.tax_amount );";
        values.add(idCampaign).add(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void takeOrder(final HttpServerRequest request, final JsonArray baskets, Integer idCampaign,
                          String idStructure, final String nameStructure ,
                          final Handler<Either<String, JsonObject>> handler ){
        try {
            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
            JsonObject basket;
            for (int i = 0; i < baskets.size(); i++) {
                basket = baskets.getJsonObject(i);
                statements.add(purseService.updatePurseAmountStatement(Float.valueOf(basket.getString("total_price")),
                        idCampaign , idStructure ,"-"));
                statements.add(getInsertEquipmentOrderStatement( basket));
                if(! "[null]".equals( basket.getString("options"))) {
                    statements.add(getInsertEquipmentOptionsStatement(basket));
                    statements.add(getDeletionBasketsOptionsStatments(basket.getInteger("id_basket")));
                }
            }
            statements.add(getDeletionBasketsEquipmentStatments(idCampaign, idStructure));

            sql.transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    JsonObject results = event.body().getJsonArray("results")
                            .getJsonObject(event.body().getJsonArray("results").size()-1);
                    JsonArray objectResult = results.getJsonArray("results").getJsonArray(0);
                    getTransactionHandler(request, nameStructure, getTotalPriceOfBasketList(baskets),
                            event, new JsonObject(objectResult.getString(0) ), handler);
                }
            });
        }catch (ClassCastException e) {
            LOGGER.error("An error occurred when casting baskets elements", e);
            handler.handle(new Either.Left<String, JsonObject>(""));
        }
    }
    private JsonObject getOptionsBasketDeletion(Integer idBasket) {
        String insertBasketEquipmentRelationshipQuery =
                "DELETE FROM " + Lystore.lystoreSchema + ".basket_option " +
                        " WHERE id_basket_equipment = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idBasket);

        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getEquipmentBasketDeletion(Integer idBasket) {
        String insertBasketEquipmentRelationshipQuery =
                "DELETE FROM " + Lystore.lystoreSchema + ".basket_equipment " +
                        "WHERE id= ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idBasket);

        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Returns a basket equipment insert statement
     *
     * @param id    basket Id
     * @param basket basket Object
     * @return basket equipment relationship transaction statement
     */
    private JsonObject getBasketEquipmentCreationStatement(Number id, JsonObject basket) {
        String insertBasketEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".basket_equipment(" +
                        "id, amount, processing_date, id_equipment, id_campaign, id_structure)" +
                        "VALUES (?, ?, ?, ?, ?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(basket.getInteger("amount"))
                .add(basket.getString("processing_date"))
                .add(basket.getInteger("equipment"))
                .add(basket.getInteger("id_campaign"))
                .add(basket.getString("id_structure"));

        return new JsonObject()
                .put("statement", insertBasketEquipmentRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }

    /**
     * Returns a basket equipment-option insert statement
     * @param id id of equipment-basket
     * @param option id of options
     * @return basket equipment-option relationship transaction statement
     */
    private JsonObject getBasketEquipmentOptionCreationStatement(Number id, Number option) {
        String insertBasketEquipmentOptionRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".basket_option " +
                        "(id_basket_equipment, id_option) " +
                        " VALUES (?, ?);";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(option);
        return new JsonObject()
                .put("statement", insertBasketEquipmentOptionRelationshipQuery)
                .put("values", params)
                .put("action", "prepared");
    }


    private static JsonObject getInsertEquipmentOrderStatement (JsonObject basket){

        StringBuilder queryEquipmentOrder = new StringBuilder()
                .append(" INSERT INTO lystore.order_client_equipment ")
                .append(" (id, price, tax_amount, amount,  id_campaign, id_structure, name, summary," +
                        " description, image, technical_spec, status, " +
                        " id_contract, equipment_key ) VALUES ")
                .append(" (?, ?, ?,  ?, ?, ?, ?, ?, ?, ?, to_json(?::text), ?, ?, ?); ");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(basket.getInteger("id_order"))
                .add(Float.valueOf(basket.getString("price")))
                .add(Float.valueOf(basket.getString("tax_amount")))
                .add(basket.getInteger("amount"))
                .add(basket.getInteger("id_campaign"))
                .add(basket.getString("id_structure"))
                .add(basket.getString("name"))
                .add(basket.getString("summary"))
                .add(basket.getString("description"))
                .add(basket.getString("image"))
                .add(basket.getString("technical_specs"))
                .add("WAITING")
                .add(basket.getInteger("id_contract"))
                .add(basket.getInteger("id_equipment")) ;

        return new JsonObject()
                .put("statement", queryEquipmentOrder.toString())
                .put("values", params)
                .put("action", "prepared");

    }

    private static JsonObject getInsertEquipmentOptionsStatement (JsonObject basket){
        JsonArray options = new fr.wseduc.webutils.collections.JsonArray(basket.getString("options"))  ;
        StringBuilder queryEOptionEquipmentOrder = new StringBuilder()
                .append( " INSERT INTO " + Lystore.lystoreSchema + ".order_client_options " )
                .append(" ( tax_amount, price, id_order_client_equipment, name, amount, required) VALUES ");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (int i=0; i<options.size(); i++){
            queryEOptionEquipmentOrder.append("( ?, ?, ?, ?, ?, ?)");
            queryEOptionEquipmentOrder.append( i == options.size()-1 ? "; " : ", ");
            JsonObject option = options.getJsonObject(i);
            params.add( option.getFloat("tax_amount"))
                    .add(option.getFloat("price"))
                    .add( basket.getInteger("id_order"))
                    .add(option.getString("name"))
                    .add(option.getInteger("amount"))
                    .add(option.getBoolean("required"));
        }
        return new JsonObject()
                .put("statement", queryEOptionEquipmentOrder.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getDeletionBasketsOptionsStatments (Number idBasketEquipment){
        StringBuilder queryEquipmentOrder = new StringBuilder()
                .append( " DELETE FROM " + Lystore.lystoreSchema + ".basket_option " )
                .append( "WHERE id_basket_equipment = ? ;");

        return new JsonObject()
                .put("statement", queryEquipmentOrder.toString())
                .put("values",  new fr.wseduc.webutils.collections.JsonArray().add(idBasketEquipment))
                .put("action", "prepared");
    }
    private static JsonObject getDeletionBasketsEquipmentStatments(Integer idCampaign, String idStructure){
        StringBuilder queryEquipmentOrder = new StringBuilder()
                .append( " DELETE FROM " + Lystore.lystoreSchema + ".basket_equipment " )
                .append( " WHERE id_campaign = ? AND id_structure = ? RETURNING ")
                .append(getReturningQueryOfTakeOrder()) ;
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure)
                .add(idCampaign).add(idStructure)
                .add(idCampaign).add(idStructure);
        return new JsonObject()
                .put("statement", queryEquipmentOrder.toString())
                .put("values", params )
                .put("action", "prepared");
    }
    private static String getReturningQueryOfTakeOrder() {
        return "( SELECT row_to_json(row(p.amount, count(o.id ) )) " +
                " FROM " + Lystore.lystoreSchema + ".purse p, " + Lystore.lystoreSchema + ".order_client_equipment o " +
                " where p.id_campaign = ? " +
                " AND p.id_structure = ? " +
                " AND  o.id_campaign = ? " +
                " AND o.id_structure = ? " +
                " GROUP BY(p.amount) )";
    }
    /**
     * Returns the amount of purse from an order transactions.
     *
     * @param event PostgreSQL event
     * @param basicBDObject
     * @return Transaction handler
     */
    private static void getTransactionHandler(HttpServerRequest request, String nameStructure, Float totalPrice,
                          Message<JsonObject> event, JsonObject basicBDObject,
                                              Handler<Either<String, JsonObject>> handler) {
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
                JsonObject returns = new JsonObject()
                        .put("amount", basicBDObject.getInteger("f1"))
                        .put("nb_order", basicBDObject.getInteger("f2"));
                DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                final double cons = 100.0;
                Number total =  Math.round(totalPrice * cons)/cons;
                handler.handle( new Either.Right<String, JsonObject>(returns));
                notificationService.sendMessage(
                        I18n.getInstance().translate( "the.structure" ,
                                getHost(request) , I18n.acceptLanguage(request) )
                                +" "+ nameStructure +" "+
                                I18n.getInstance().translate( "lystore.slack.order.message1" ,
                                        getHost(request) , I18n.acceptLanguage(request) )
                                +" "+ total.toString()  +" "+
                                I18n.getInstance().translate( "money.symbol" ,
                                        getHost(request) , I18n.acceptLanguage(request) )
                                + " " +
                                I18n.getInstance().translate( "determiner.male" ,
                                        getHost(request) , I18n.acceptLanguage(request) )
                                +" "+ format.format(new Date())+" ");
        } else {
            LOGGER.error("An error occurred when launching 'order' transaction");
            handler.handle(new Either.Left<String, JsonObject>(""));
        }
    }

    private static Float getTotalPriceOfBasketList(JsonArray baskets) {
        Float total = Float.valueOf(0);
        for(int i = 0; i < baskets.size(); i++) {
            total += Float.valueOf((baskets.getJsonObject(i)).getString("total_price"));
        }
        return total;
    }
}
