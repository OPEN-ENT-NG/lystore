package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.service.OperationService;
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
log.info("add la");
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(updateIdInstructionAdd(operationIds,instructionId));
                //.add(updateOperationOrdersRegionAdd(operationIds))
                //.add(updateOperationOrdersClientAdd(operationIds));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event,operationIds.getInteger(0)));
            }
        });
    }

    public  void removeInstructionId( JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        log.info("LA REMOVE");
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
                "SET status = 'WAITING_FOR_ACCEPTANCE' " +
                " WHERE id_operation IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
                .put("action", "prepared");
        return statement;
    }

    private JsonObject updateOperationOrdersClientAdd(JsonArray operationIds) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET status = 'WAITING_FOR_ACCEPTANCE' " +
                " WHERE id_operation IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
                .put("action", "prepared");
        return statement;
    }

    private JsonObject updateIdInstructionAdd(JsonArray operationIds, Integer instructionId) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_instruction = " +
                instructionId +
                " WHERE id IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";

        JsonObject statement = new JsonObject().put("statement", query)
                .put("values", operationIds)
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

        Future<JsonArray> getOrderRegionByOperationFuture = Future.future();
        Future<JsonArray> getOrderClientByOperationFuture = Future.future();

        getOrderRegionByOperation(operationId, FutureHelper.handlerJsonArray(getOrderRegionByOperationFuture));
        getOrderClientByOperation(operationId, FutureHelper.handlerJsonArray(getOrderClientByOperationFuture));

        CompositeFuture.all( getOrderRegionByOperationFuture, getOrderClientByOperationFuture).setHandler(asyncEvent -> {
            if (asyncEvent.failed()) {
                String message = "Failed to retrieve order of operation";
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray ordersRegionsByOperation = getOrderRegionByOperationFuture.result();
            JsonArray ordersClientsByOperation = getOrderClientByOperationFuture.result();

            for (int i = 0 ; i<ordersClientsByOperation.size() ; i++){
                ordersClientsByOperation.getJsonObject(i).put("typeOrder", "client");
            }

            for (int i = 0 ; i<ordersRegionsByOperation.size() ; i++){
                ordersClientsByOperation.add(ordersRegionsByOperation.getJsonObject(i).put("typeOrder", "region"));
            }

            handler.handle(new Either.Right<>(ordersClientsByOperation));
        });
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

    private void getOrderRegionByOperation(int idOperation, Handler<Either<String, JsonArray>> handler){
        String queryGetOrderRegion = "" +
                "SELECT ore.id, " +
                "       ore.id_order_client_equipment, " +
                "       ore.creation_date, " +
                "       ore.amount, " +
                "       ore.name, " +
                "       ore.id_structure, " +
                "       ore.status, " +
                "       ore.price * ore.amount AS price, " +
                "       c.name AS contract_name " +
                "FROM  " + Lystore.lystoreSchema +".\"order-region-equipment\" ore " +
                "INNER JOIN  " + Lystore.lystoreSchema +".contract c ON ore.id_contract = c.id " +
                "INNER JOIN  " + Lystore.lystoreSchema +".operation o ON (ore.id_operation = o.id) " +
                "WHERE o.id = ? " +
                "GROUP BY (ore.id, " +
                "          ore.price, " +
                "          ore.name, " +
                "          ore.id_structure, " +
                "          c.name);";

        Sql.getInstance().prepared(queryGetOrderRegion, new JsonArray().add(idOperation), SqlResult.validResultHandler(handler));
    }


    private void getOrderClientByOperation(int idOperation, Handler<Either<String, JsonArray>> handler){
        String queryGOrderClient = "" +
                "SELECT oce.id,  " +
                "       (  " +
                "               (SELECT " +
                "                  CASE " +
                "                  WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)  IS NULL THEN 0 " +
                "                  WHEN oce.price_proposal IS NOT NULL THEN 0 " +
                "                  ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) " +
                "                  END " +
                "               FROM " + Lystore.lystoreSchema +".order_client_options oco " +
                "               WHERE id_order_client_equipment = oce.id) + " +
                "                                                         (CASE  " +
                "                                                             WHEN oce.price_proposal IS NOT NULL THEN (oce.price_proposal)  " +
                "                                                             ELSE (oce.price + ((oce.price * oce.tax_amount) /100))  " +
                "                                                         END))  * oce.amount AS price,  " +
                "       oce.creation_date,  " +
                "       oce.amount,  " +
                "       oce.name,  " +
                "       oce.id_structure,  " +
                "       oce.status,  " +
                "       c.name AS contract_name  " +
                "FROM   " + Lystore.lystoreSchema +".order_client_equipment oce  " +
                "INNER JOIN   " + Lystore.lystoreSchema +".contract c ON oce.id_contract = c.id  " +
                "INNER JOIN   " + Lystore.lystoreSchema +".operation o ON (oce.id_operation = o.id)  " +
                "WHERE  " +
                "   o.id = ? " +
                "  AND oce.override_region IS FALSE  " +
                "GROUP BY (oce.id,  " +
                "          oce.price,  " +
                "          oce.name,  " +
                "          oce.id_structure,  " +
                "          c.name);";

        Sql.getInstance().prepared(queryGOrderClient, new JsonArray().add(idOperation), SqlResult.validResultHandler(handler));
    }

}
