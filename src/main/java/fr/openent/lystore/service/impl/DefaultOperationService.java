package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.service.OperationService;
import fr.openent.lystore.utils.LystoreUtils;
import fr.openent.lystore.utils.OrderUtils;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultOperationService extends SqlCrudService implements OperationService {

    public DefaultOperationService(String schema, String table) {
        super(schema, table);
    }
    private static final Logger log = LoggerFactory.getLogger (DefaultOrderService.class);

    private String getTextFilter(List<String> filters) {
        String filter = "";
        if (filters.size() > 0) {
            filter = "WHERE ";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) {
                    filter += "AND ";
                }
                filter += "(LOWER(label.label) ~ LOWER(?) OR LOWER(o_region.order_number) ~ LOWER(?) OR  ";
                filter += " LOWER(o_client.order_number) ~ LOWER(?)) ";

            }
        }
        return filter;
    }

    @Override
    public void listOperations(List<String> filters, Handler<Either<String, JsonArray>> arrayResponseHandler) {
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter);
            }
        }

        //TODO trop long => essayez avec des statements
        String queryOperation = "" +
                "SELECT operation.*,  " +
                "       to_json(label.*) AS label  " +
                "FROM    " + Lystore.lystoreSchema +".operation  " +
                "INNER JOIN    " + Lystore.lystoreSchema +".label_operation label ON label.id = operation.id_label  " +
                getTextFilter(filters) + " " +
                "GROUP BY (operation.id,  " +
                "          label.*)";
        Sql.getInstance().prepared(queryOperation, params, SqlResult.validResultHandler(arrayResponseHandler));
    }

    public void getOperations(List<String> filters, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter);
            }
        }

        /* old
        SELECT
  operation.*,
  to_json(label.*) AS label,
  array_to_json(
    array_agg(o_client.order_number)
  ) AS bc_number,
  array_to_json(
    array_agg(DISTINCT oce.program)
  ) AS programs,
  array_to_json(
    array_agg(DISTINCT c_client.name)
  ) AS contracts
FROM
  lystore.operation
  INNER JOIN lystore.label_operation label ON label.id = operation.id_label
  LEFT JOIN lystore.allOrders oce ON oce.id_operation = operation.id
  AND oce.override_region IS false
  AND oce.status = 'IN PROGRESS'
  LEFT JOIN lystore.order o_client ON o_client.id = oce.id_order
  LEFT JOIN lystore.contract c_client ON c_client.id = oce.id_contract
GROUP BY
  (operation.id, label.*)
         */

        String queryOperation = "" +
                "SELECT  " +
                "  operation.*,  " +
                "  to_json(label.*) AS label,  " +
                "  array_to_json( " +
                "    array_agg(order_summary.order_number) " +
                "  ) AS bc_number,  " +
                "  array_to_json( " +
                "    array_agg(DISTINCT order_summary.program) " +
                "  ) AS programs,  " +
                "  array_to_json( " +
                "    array_agg(DISTINCT order_summary.name) " +
                "  ) AS contracts  " +
                "FROM  " +
                Lystore.lystoreSchema + ".operation  " +
                "  INNER JOIN " + Lystore.lystoreSchema + ".label_operation label ON label.id = operation.id_label  " +
                "  LEFT JOIN ( " +
                "    SELECT  " +
                "      c_client.name,  " +
                "      oce.program,  " +
                "      o_client.order_number,  " +
                "      oce.id_operation  " +
                "    from  " +
                "      lystore.allOrders oce  " +
                "      INNER JOIN " + Lystore.lystoreSchema + ".contract c_client ON c_client.id = oce.id_contract  " +
                "      LEFT JOIN " + Lystore.lystoreSchema +".order o_client ON o_client.id = oce.id_order  " +
                "    WHERE  " +
                "      oce.override_region IS false  " +
                "      AND  oce.status IN ('IN PROGRESS','VALID','DONE') " +
                "  ) order_summary ON order_summary.id_operation = operation.id " +
                "GROUP BY  " +
                "  (operation.id, label.*) ";
//        Sql.getInstance().prepared(queryOperation, params, SqlResult.validResultHandler(handler));

        Sql.getInstance().prepared(queryOperation, params, SqlResult.validResultHandler(operationsEither -> {
            try {
                if (operationsEither.isRight()) {
                    JsonArray operations = operationsEither.right().getValue();
                    if (operations.size() == 0) {
                        handler.handle(new Either.Right<>(operations));
                        return;
                    }
                    JsonArray idsOperations = SqlQueryUtils.getArrayAllIdsResult(operations);

                    Future<JsonArray> getCountOrderInOperationFuture = Future.future();
                    Future<JsonArray> getInstructionForOperationFuture = Future.future();
                    Future<JsonArray> getAllPriceOperationFuture = Future.future();
                    Future<JsonArray> getNumberOrderSubventionFuture = Future.future();

                    List<Future> listFuture = new ArrayList<>();

                    listFuture.add(getCountOrderInOperationFuture);
                    listFuture.add(getInstructionForOperationFuture);
                    listFuture.add(getAllPriceOperationFuture);
                    listFuture.add(getNumberOrderSubventionFuture);
                    CompositeFuture.all(listFuture).setHandler(makeOperationsDataArray(handler, operations, getCountOrderInOperationFuture, getInstructionForOperationFuture, getAllPriceOperationFuture, getNumberOrderSubventionFuture));

                    SqlUtils.getCountOrderInOperation(idsOperations, FutureHelper.handlerJsonArray(getCountOrderInOperationFuture));
                    getInstructionForOperation(idsOperations, FutureHelper.handlerJsonArray(getInstructionForOperationFuture));
                    getAllPriceOperation(idsOperations,FutureHelper.handlerJsonArray(getAllPriceOperationFuture));
                    getNumberOrderSubvention(idsOperations,  FutureHelper.handlerJsonArray(getNumberOrderSubventionFuture));

                } else {
                    handler.handle(new Either.Left<>("404"));
                }
            } catch( Exception e){
                log.error("An error when you want get all operation", e);
                handler.handle(new Either.Left<>("An error when you want get all operation" + e));
            }
        }));
    }

    private Handler<AsyncResult<CompositeFuture>> makeOperationsDataArray(Handler<Either<String, JsonArray>> handler, JsonArray operations, Future<JsonArray> getCountOrderInOperationFuture, Future<JsonArray> getInstructionForOperationFuture, Future<JsonArray> getAllPriceOperationFuture, Future<JsonArray> getNumberOrderSubventionFuture) {
        return asyncEvent -> {
            if (asyncEvent.failed()) {
                String message = "Failed to retrieve operation";
                handler.handle(new Either.Left<>(message));
                return;
            }
            JsonArray getOrderCount = getCountOrderInOperationFuture.result();
            JsonArray getInstruction = getInstructionForOperationFuture.result();
            JsonArray getSumPriceOperation = getAllPriceOperationFuture.result();
            JsonArray getNumberSubvention = getNumberOrderSubventionFuture.result();

            JsonArray operationFinalSend = new JsonArray();
            for (int i = 0; i < operations.size(); i++) {
                JsonObject operation = operations.getJsonObject(i);
                for (int j = 0; j < getOrderCount.size(); j++) {
                    JsonObject countOrders = getOrderCount.getJsonObject(j);
                    if (operation.getInteger("id").equals(countOrders.getInteger("id"))) {
                        operation.put("nb_orders", countOrders.getString("nb_orders"));
                    }
                }
                for (int k = 0; k < getInstruction.size(); k++) {
                    if(!operation.containsKey("instruction"))
                        operation.put("instruction","{}");
                    JsonObject instruction = getInstruction.getJsonObject(k);
                    if (operation.getInteger("id").equals(instruction.getInteger("id_operation"))) {
                        operation.put("instruction", instruction.getString("instruction"));
                    }
                }
                for (int n = 0; n < getSumPriceOperation.size(); n++) {
                    JsonObject sumPriceOperation = getSumPriceOperation.getJsonObject(n);
                    if (operation.getInteger("id").equals(sumPriceOperation.getInteger("id"))) {
                        operation.put("amount", sumPriceOperation.getString("amount"));
                    }
                }

                for (int m = 0; m < getNumberSubvention.size(); m++) {
                    JsonObject numberSubvention = getNumberSubvention.getJsonObject(m);
                    if (operation.getInteger("id").equals(numberSubvention.getInteger("id_operation"))) {
                        operation.put("number_sub", numberSubvention.getString("number_sub"));
                    }
                }
                operationFinalSend.add(operation);
            }
            handler.handle(new Either.Right<>(operationFinalSend));
        };
    }

    private void getAllPriceOperation(JsonArray idsOperations, Handler<Either<String, JsonArray>> handler) {
        String queryGetTotalOperation = "SELECT  id, Sum(amount) as amount from " + Lystore.lystoreSchema + ".allOperationOrders " +
                " WHERE id IN " +
                Sql.listPrepared(idsOperations.getList()) + " " +
                "Group by id;";

        Sql.getInstance().prepared(queryGetTotalOperation, idsOperations, SqlResult.validResultHandler(handler));
    }

    public void getNumberOrderSubvention (JsonArray idsOperations, Handler<Either<String, JsonArray>> handler ){
        String queryGetContractClient = "" +
                "WITH operations AS " +
                "  (SELECT ore.id_operation, " +
                "          COUNT (ct) AS number_sub " +
                "   FROM " + Lystore.lystoreSchema +".\"order-region-equipment\" ore " +
                "   INNER JOIN " + Lystore.lystoreSchema +".contract c_region ON c_region.id = ore.id_contract " +
                "   INNER JOIN " + Lystore.lystoreSchema +".contract_type ct ON ct.id = c_region.id_contract_type " +
                "   AND ct.code = '236' " +
                "   WHERE ore.id_operation IN " +
                Sql.listPrepared(idsOperations.getList()) + " " +
                "   GROUP BY (ore.id_operation) " +
                "   UNION ALL SELECT oce.id_operation, " +
                "                    COUNT(*) AS number_sub " +
                "   FROM " + Lystore.lystoreSchema +".order_client_equipment oce " +
                "   INNER JOIN " + Lystore.lystoreSchema +".contract c_client ON c_client.id = oce.id_contract " +
                "   INNER JOIN " + Lystore.lystoreSchema +".contract_type ct ON ct.id = c_client.id_contract_type " +
                "   AND ct.code = '236' " +
                "   WHERE oce.id_operation IN " +
                Sql.listPrepared(idsOperations.getList()) + " " +
                "     AND oce.override_region IS FALSE " +
                "     AND oce.status IN ('IN PROGRESS','VALID','DONE') " +
                "   GROUP BY (oce.id_operation)) " +
                "SELECT operations.id_operation, " +
                "       SUM(operations.number_sub) AS number_sub " +
                "FROM operations " +
                "GROUP BY (operations.id_operation)";

        JsonArray params = SqlQueryUtils.multiplyArray(2, idsOperations);

        Sql.getInstance().prepared(queryGetContractClient, params, SqlResult.validResultHandler(handler));
    }

    private void getInstructionForOperation(JsonArray idsOperations, Handler<Either<String, JsonArray>> handler){
        String queryGetTotalOperation = "SELECT " +
                "o.id AS id_operation, " +
                "to_json(i.*) AS instruction " +
                "FROM " + Lystore.lystoreSchema + ".instruction AS i " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation o on i.id = o.id_instruction " +
                "WHERE o.id IN " +

                Sql.listPrepared(idsOperations.getList());

        Sql.getInstance().prepared(queryGetTotalOperation, idsOperations, SqlResult.validResultHandler(handler));
    }


    public void create(JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " +
                Lystore.lystoreSchema + ".operation(id_label, status, date_operation) " +
                "VALUES (?, ?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"))
                .add(operation.getString("date_operation"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public  void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_label = ?, " +
                "status = ?, " +
                "id_instruction = ?, " +
                "date_operation = ? " +
                "WHERE id = ? " +
                "RETURNING id";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"))
                .add(operation.getInteger("id_instruction"))
                .add(operation.getString("date_operation"))
                .add(id);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public  void addInstructionId(Integer instructionId, JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(updateIdInstructionAdd(operationIds,instructionId))
                .add(updateOperationOrdersRegionAdd(operationIds))
                .add(updateOperationOrdersClientAdd(operationIds))
                ;
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event,operationIds.getInteger(0)));
            }
        });
    }

    public  void removeInstructionId( JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(updateIdInstructionRemove(operationIds))
                .add(updateOperationOrdersRegionRemove(operationIds))
                .add(updateOperationOrdersClientRemove(operationIds));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event,operationIds.getInteger(0)));
            }
        });
    }

    private JsonObject updateOperationOrdersRegionAdd(JsonArray operationIds) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                "SET status = '" + LystoreBDD.WAITING_FOR_ACCEPTANCE + "' " +
                " WHERE id_operation IN " +
                Sql.listPrepared(operationIds.getList()) +
                " AND STATUS NOT IN ('" + LystoreBDD.VALID + "', '" + LystoreBDD.DONE + "'," +
                " '" + LystoreBDD.SENT + "', '" + LystoreBDD.REJECTED + "') " +
                " ;";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
                .put("action", "prepared");
        return statement;
    }

    private JsonObject updateOperationOrdersClientAdd(JsonArray operationIds) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET status = '" + LystoreBDD.WAITING_FOR_ACCEPTANCE + "' " +
                " WHERE id_operation IN " +
                Sql.listPrepared(operationIds.getList()) +
                " AND STATUS NOT IN ('" + LystoreBDD.VALID + "', '" + LystoreBDD.DONE + "'," +
                " '" + LystoreBDD.SENT + "', '" + LystoreBDD.REJECTED + "') " +
                " ;";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
                .put("action", "prepared");
        return statement;
    }

    private JsonObject updateIdInstructionAdd(JsonArray operationIds, Integer instructionId) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_instruction = ?" +
                " WHERE id IN " +
                Sql.listPrepared(operationIds.getList()) +
                ";";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", new JsonArray().add(instructionId).addAll(operationIds))
                .put("action", "prepared");
        return statement;
    }

    private JsonObject updateOperationOrdersRegionRemove(JsonArray operationIds) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                "SET status = 'IN PROGRESS' " +
                " WHERE id_operation IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
                .put("action", "prepared");
        return statement;
    }

    private JsonObject updateOperationOrdersClientRemove(JsonArray operationIds) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET status = 'IN PROGRESS' " +
                " WHERE id_operation IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
                .put("action", "prepared");
        return statement;
    }

    private JsonObject updateIdInstructionRemove(JsonArray operationIds) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_instruction = null" +
                " WHERE id IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
                .put("action", "prepared");
        return statement;
    }
    public  void deleteOperation(JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        String query = "DELETE FROM " +
                Lystore.lystoreSchema +
                ".operation" + " WHERE id IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < operationIds.size(); i++) {
            values.add(operationIds.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOperationOrders(Integer operationId, Handler<Either<String, JsonArray>> handler) {
        getOrderByOperation(operationId, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isRight()) {
                    JsonArray ordersClientsByOperation = event.right().getValue();
                    for (int i = 0 ; i<ordersClientsByOperation.size() ; i++){
                        JsonObject order =  ordersClientsByOperation.getJsonObject(i);
                        if(order.getBoolean("override_region")!= null)
                            ordersClientsByOperation.getJsonObject(i).put("typeOrder", "client");
                        else{
                            ordersClientsByOperation.getJsonObject(i).put("typeOrder", "region");
                        }
                    }
                    handler.handle(new Either.Right<>(ordersClientsByOperation));
                }
                else {
                    handler.handle(new Either.Left<>(event.left().getValue()));
                }
            }});
    }

    @Override
    public void deleteOrdersOperation(JsonArray ordersIds, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        JsonArray ordersRegionIds = ordersIds.getJsonObject(0).getJsonArray("ordersRegionId");
        JsonArray ordersClientsIds = ordersIds.getJsonObject(0).getJsonArray("ordersClientId");

        if(!ordersRegionIds.isEmpty())
            statements.add(getDeletionRegion(ordersRegionIds));
        if(!ordersClientsIds.isEmpty())
            statements.add(getClientsChangement(ordersClientsIds));

        if (!statements.isEmpty()) {
            Sql.getInstance().transaction(statements, message -> {
                if ("ok".equals(message.body().getString("status"))) {
                    handler.handle(new Either.Right<>(message.body()));
                } else {
                    handler.handle(new Either.Left<>(message.body().getString("message")));
                }
            });
        } else {
            handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
        }
    }

    private void getCreationDate(int idOperation, Handler<Either<String, JsonArray>> handler) {
        String queryGetCreationDate = "SELECT allOrders.creation_date from " + Lystore.lystoreSchema + ".allOrders " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation ON operation.id = allOrders.id_operation; ";

        Sql.getInstance().prepared(queryGetCreationDate, new JsonArray().add(idOperation), SqlResult.validResultHandler(handler));
    }


    private JsonObject getClientsChangement(JsonArray ordersClientsIds) {
        String query= "UPDATE "+Lystore.lystoreSchema+".order_client_equipment " +
                " SET status='WAITING'," +
                "id_operation = NULL " +
                " WHERE id in " + Sql.listPrepared(ordersClientsIds.getList());
        return new JsonObject()
                .put("statement", query)
                .put("values", ordersClientsIds)
                .put("action", "prepared");
    }

    private JsonObject getDeletionRegion(JsonArray ordersRegionIds) {
        String query = "DELETE from "+Lystore.lystoreSchema+".\"order-region-equipment\" " +
                " WHERE id in"+Sql.listPrepared(ordersRegionIds.getList()) ;
        return new JsonObject()
                .put("statement", query)
                .put("values", ordersRegionIds)
                .put("action", "prepared");
    }

    private void getOrderByOperation(int idOperation, Handler<Either<String, JsonArray>> handler) {
        String queryGOrderClient = "" +
                "SELECT     orders.id, " +
                "           instruction.cp_adopted AS instruction_cp_adopted, " +
                "           ct.code, " +
                "           orders.override_region, " +
                "           Round(( " +
                "                    ( " +
                "                    SELECT " +
                "                           CASE " +
                "                                  WHEN orders.price_proposal IS NOT NULL THEN 0 " +
                "                                  WHEN orders.override_region IS NULL THEN 0 " +
                "                                  WHEN Sum((oco.price + ( oco.price * oco.tax_amount ) / 100 ) * oco.amount) IS NULL THEN 0 " +
                "                                  ELSE Sum((oco.price + ( oco.price * oco.tax_amount ) / 100 ) * oco.amount) " +
                "                           END " +
                "                    FROM   lystore.order_client_options oco " +
                "                    WHERE  oco.id_order_client_equipment = orders.id AND orders.override_region is false) " +
                "           + orders.\"price TTC\" ) , 2) AS price_ttc, " +
                "           orders.priceht                                                                                        AS price, " +
                "           orders.creation_date, " +
                "           orders.amount, " +
                "           orders.NAME, " +
                "           orders.price_proposal, " +
                "           orders.tax_amount, " +
                "           orders.id_structure, " +
                "           orders.status," +
                "           orders.prio as rank, " +
                "           c.NAME                               AS contract_name, " +
                "           Array_to_json(Array_agg(order_opts)) AS options, " +
                "           ( " +
                "                  SELECT " +
                "                         CASE " +
                "                                WHEN orders.override_region IS NULL THEN Array_to_json(Array_agg(DISTINCT file_region.*)) " +
                "                                ELSE Array_to_json(Array_agg(DISTINCT file_client.*)) " +
                "                                 END) AS files , " +
                "           To_json(project.*)                                       AS project, " +
                "           To_json(tt.*)                                            AS title, " +
                "           To_json(campaign.*)                                      AS campaign, " +
                "           To_json(c.*)                                             AS contract, " +
                "           To_json(ct.*)                                            AS contract_type, " +
                "           Array_to_json(Array_agg( DISTINCT structure_group.NAME)) AS structure_groups " +

                "FROM   " + Lystore.lystoreSchema + ".allorders orders  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract c ON orders.id_contract = c.id  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ct ON c.id_contract_type = ct.id  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".campaign campaign ON orders.id_campaign = campaign.id  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation o ON (orders.id_operation = o.id)" +
                "INNER JOIN lystore.rel_group_campaign ON (orders.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN lystore.rel_group_structure ON (orders.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN lystore.structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".project on orders.id_project = project.id   " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".title as tt ON tt.id = project.id_title  " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".order_client_options order_opts  " +
                "      ON (orders.id = order_opts.id_order_client_equipment AND orders.override_region = false ) " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".instruction on o.id_instruction = instruction.id  " +

                " LEFT JOIN " + Lystore.lystoreSchema + ".order_file as file_client " +
                " ON ( orders.id = file_client.id_order_client_equipment AND orders.override_region is false )" +
                " LEFT JOIN " + Lystore.lystoreSchema + ".order_region_file as file_region " +
                " ON orders.id = file_region.id_order_region_equipment AND orders.override_region is null " +

                "WHERE" +
                " orders.override_region is not true   " +
                "  AND o.id = ? " +
                "GROUP BY (orders.id,  " +
                "          orders.\"price TTC\",  " +
                "          orders.name," +
                "          orders.priceht , " +
                "          orders.tax_amount , " +
                "          orders.id_structure,  " +
                "          c.name," +
                "          orders.prio, " +
                " orders.price_proposal," +
                " orders.override_region," +
                "orders.amount," +
                "orders.creation_date," +
                "orders.status," +
                "instruction_cp_adopted," +
                "ct.code ,campaign.* ,project.id ,tt.id ,c.id, ct.id) " +
                "ORDER BY override_region;";


        Sql.getInstance().prepared(queryGOrderClient, new JsonArray().add(idOperation), SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                handler.handle(new Either.Right<>(new JsonArray(event.right().getValue().stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .map(elem -> {
                            elem.put(LystoreBDD.CAMPAIGN, new JsonObject(elem.getString(LystoreBDD.CAMPAIGN)));
                            if(elem.getValue(LystoreBDD.PROJECT) != null ){
                                elem.put(LystoreBDD.PROJECT, new JsonObject(elem.getString(LystoreBDD.PROJECT)));
                            }else{
                                elem.put(LystoreBDD.PROJECT, new JsonObject());
                            }
                            if(elem.getValue(LystoreBDD.TITLE) != null ){
                                elem.put(LystoreBDD.TITLE, new JsonObject(elem.getString(LystoreBDD.TITLE)));
                            }else{
                                elem.put(LystoreBDD.TITLE, new JsonObject());
                            }
                            if(elem.getValue(LystoreBDD.OPTIONS) != null ){
                                elem.put(LystoreBDD.OPTIONS,  new JsonArray(elem.getString(LystoreBDD.OPTIONS)));
                            }else{
                                elem.put(LystoreBDD.OPTIONS, new JsonArray());
                            }
                            if(elem.getValue(LystoreBDD.FILES) != null ){
                                elem.put(LystoreBDD.FILES,  new JsonArray(elem.getString(LystoreBDD.FILES)));
                            }else{
                                elem.put(LystoreBDD.FILES, new JsonArray());
                            }
                            elem.put(LystoreBDD.PRICE, OrderUtils.safeGetDouble(elem, LystoreBDD.PRICE));
//                            elem.put(LystoreBDD.PRICE_PROPOSAL, OrderUtils.safeGetDouble(elem, LystoreBDD.PRICE_PROPOSAL));
                            elem.put(LystoreBDD.TAX_AMOUNT, OrderUtils.safeGetDouble(elem, LystoreBDD.TAX_AMOUNT));
                            elem.put(LystoreBDD.PRICETTC, OrderUtils.safeGetDouble(elem, LystoreBDD.PRICE_TTC));
                            elem.put(LystoreBDD.CONTRACT, new JsonObject(elem.getString(LystoreBDD.CONTRACT)));
                            elem.put(LystoreBDD.CONTRACT_TYPE, new JsonObject(elem.getString(LystoreBDD.CONTRACT_TYPE)));
                            return elem;
                        })
                        .collect(Collectors.toList()))));
            } else {
                handler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(
                                this.getClass(),
                                "listOrder",
                                "error when getting data",
                                event.left().getValue()))
                );
            }
        }));
    }

}
