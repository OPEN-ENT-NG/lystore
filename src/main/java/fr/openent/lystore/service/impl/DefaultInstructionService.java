package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.EnumConstant;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.model.InstructionStatus;
import fr.openent.lystore.service.InstructionService;
import fr.openent.lystore.service.OrderRegionService;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.openent.lystore.service.impl.DefaultOrderService.getNextValidationNumber;
import static fr.wseduc.webutils.http.Renders.getHost;

public class DefaultInstructionService  extends SqlCrudService implements InstructionService {

    private static final Logger log = LoggerFactory.getLogger (DefaultOrderService.class);
    private DefaultOperationService operationService = new DefaultOperationService(Lystore.lystoreSchema, "operation");
    private final OrderService orderService = new DefaultOrderService(Lystore.lystoreSchema, LystoreBDD.ORDER_CLIENT_EQUIPMENT , null);
    private final OrderRegionService orderRegionService = new DefaultOrderRegionService(LystoreBDD.ORDER_REGION_EQUIPMENT);

    public DefaultInstructionService(
            String schema, String table) {
        super(schema, table);
    }

    public void getExercises (Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema +".exercise";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler) );
    }


    private String getTextFilter(List<String> filters, HttpServerRequest request) {
        String filter = "";
        if (filters.size() > 0) {
            filter = "WHERE ";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) {
                    filter += "AND ";
                }
                filter += "(LOWER(instruction.object) ~ LOWER(?) OR " +
                        "LOWER(instruction.service_number) ~ LOWER(?) OR " +
                        "LOWER(totalOp::VARCHAR(255)) ~ LOWER(?) OR " +
                        "LOWER(instruction.cp_number) ~ LOWER(?) OR " +
                        "to_char(instruction.date_cp, 'DD/MM/YY') ~ ? OR " +
                        "LOWER(CASE ";
                for(InstructionStatus status : InstructionStatus.values()) {
                    filter += "WHEN instruction.cp_adopted = '" + status.toString().toUpperCase() + "' " +
                            "THEN '" + I18n.getInstance().translate("lystore.instruction.status." + status.toString().toLowerCase(), getHost(request), I18n.acceptLanguage(request)) + "' ";
                }
                    filter += "END) ~ LOWER(?) OR " +
                        "LOWER(exercise.year) ~ LOWER(?)) ";
            }
        }
        return filter;
    }

    public void getInstructions(List<String> filters, HttpServerRequest request, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter).add(filter).add(filter).add(filter).add(filter);
            }
        }
        String query =  "WITH values AS (" +
                "SELECT AllOp.id AS operation_id, operation.id_instruction, operation.id AS op_id, AllOp.amount " +
                "FROM " + Lystore.lystoreSchema +".operation " +
                "INNER JOIN " + Lystore.lystoreSchema +".allOperationOrders as AllOp ON AllOp.id = operation.id " +
                "WHERE operation.id_instruction IS NOT NULL) " +
                "SELECT instruction.*, " +
                "to_json(exercise.*) AS exercise, " +
                "array_to_json(array_agg( o.id )) AS operations, " +
                "totalOp " +
                "FROM " + Lystore.lystoreSchema +".instruction " +
                "INNER JOIN " + Lystore.lystoreSchema +".exercise exercise ON exercise.id = instruction.id_exercise " +
                "LEFT JOIN " + Lystore.lystoreSchema +".operation o ON o.id_instruction = instruction.id " +
                "LEFT JOIN (SELECT Sum(amount) AS totalOp, " +
                "id_instruction from values " +
                "GROUP BY id_instruction " +
                ") AS totalOperation ON totalOperation.id_instruction = instruction.id " +
                getTextFilter(filters, request) +
                " GROUP BY (instruction.id, exercise.id, totalOp);";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(instructionsEither -> {
            try{
                if (instructionsEither.isRight()) {
                    JsonArray instructions = instructionsEither.right().getValue();
                    if(instructions.size() == 0){
                        handler.handle(new Either.Right<>(instructions));
                        return;
                    }
                    JsonArray idsInstructions = SqlQueryUtils.getArrayAllIdsResult(instructions);
                    Promise<JsonArray> getSumOperationsPromise = Promise.promise();

                    List<Future<JsonArray>> futursArray = new ArrayList<>();
                    futursArray.add(getSumOperationsPromise.future());

                    Future.join(futursArray).onComplete(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray getSumOperations = getSumOperationsPromise.future().result();
                        JsonArray instructionsResult =  SqlQueryUtils.addDataByIdJoin(instructions, getSumOperations,"amount");
                        handler.handle(new Either.Right<>(instructionsResult));
                    });

                    SqlUtils.getSumOperations(idsInstructions, FutureHelper.handlerJsonArray(getSumOperationsPromise));

                } else {
                    handler.handle(new Either.Left<>("404"));
                }
            } catch( Exception e){
                log.error("An error when you want get all instructions", e);
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    public void getOperationOfInstruction(Integer IdInstruction, Handler<Either<String, JsonArray>> handler) {
        JsonArray idInstructionParams = new JsonArray().add(IdInstruction);

        String queryOperation = "" +
                "SELECT operation.*,  " +
                "       to_json(label.*) AS label  " +
                "FROM " + Lystore.lystoreSchema +".operation  " +
                "INNER JOIN " + Lystore.lystoreSchema +".label_operation label ON label.id = operation.id_label  " +
                "WHERE id_instruction = ? " +
                "GROUP BY (operation.id,  " +
                "          label.*)";

        sql.getInstance().prepared(queryOperation, idInstructionParams, SqlResult.validResultHandler(eventOperation -> {
            try{
                if (eventOperation.isRight()) {
                    JsonArray operations = eventOperation.right().getValue();
                    if (operations.size() == 0) {
                        handler.handle(new Either.Right<>(operations));
                        return;
                    }
                    JsonArray idsOperations = SqlQueryUtils.getArrayAllIdsResult(operations);

                    Promise<JsonArray> getCountOrderInOperationPromise = Promise.promise();
                    Promise<JsonArray> getAllPriceOperationPromise = Promise.promise();
                    Promise<JsonArray> getNumberOrderSubventionPromise = Promise.promise();

                    Future.all( getCountOrderInOperationPromise.future(), getAllPriceOperationPromise.future(), getNumberOrderSubventionPromise.future() ).onComplete(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray operationsFinal = new JsonArray();
                        JsonArray getNbrOrder = getCountOrderInOperationPromise.future().result();
                        JsonArray getAmountsDemands = getAllPriceOperationPromise.future().result();
                        JsonArray getNumberSubvention = getNumberOrderSubventionPromise.future().result();

                        for (int i = 0; i < operations.size(); i++) {
                            JsonObject operation = operations.getJsonObject(i);
                            for (int j = 0; j < getNbrOrder.size(); j++) {
                                JsonObject countOrders = getNbrOrder.getJsonObject(j);
                                if (operation.getInteger("id").equals(countOrders.getInteger("id"))) {
                                    operation.put("nb_orders", countOrders.getString("nb_orders"));
                                }
                            }
                            for (int k = 0; k < getAmountsDemands.size(); k++) {
                                JsonObject amountDemand = getAmountsDemands.getJsonObject(k);
                                if (operation.getInteger("id").equals(amountDemand.getInteger("id"))) {
                                    operation.put("amount", amountDemand.getString("amount"));
                                }
                            }
                            for (int m = 0; m < getNumberSubvention.size(); m++) {
                                JsonObject numberSubvention = getNumberSubvention.getJsonObject(m);
                                if (operation.getInteger("id").equals(numberSubvention.getInteger("id_operation"))) {
                                    operation.put("number_sub", numberSubvention.getString("number_sub"));
                                }
                            }

                            operationsFinal.add(operation);
                        }
                        handler.handle(new Either.Right<>(operationsFinal));
                    });

                    SqlUtils.getCountOrderInOperation(idsOperations,  FutureHelper.handlerJsonArray(getCountOrderInOperationPromise));
                    SqlUtils.getAllPriceOperation(idsOperations,  FutureHelper.handlerJsonArray(getAllPriceOperationPromise));
                    operationService.getNumberOrderSubvention(idsOperations,  FutureHelper.handlerJsonArray(getNumberOrderSubventionPromise));
                }
            } catch ( Exception e){
                log.error("An error when you want get all instructions", e);
                handler.handle(new Either.Left<>("404"));
            }

        }));
    }


    public void create(JsonObject instruction, Handler<Either<String, JsonObject>> handler) {

        String query = "";
        query = "INSERT INTO " + Lystore.lystoreSchema + ".instruction" +
                " ( id_exercise, object, service_number, cp_number, submitted_to_cp, date_cp, comment, cp_adopted) " +
                "VALUES (? ,? ,? ,? ,? ,? , ? ,";
        query += instruction.getString(LystoreBDD.CP_ADOPTED) != null ? "? " : "NULL ";
        query += ")" +
                "RETURNING id; ";


        String object = instruction.getString(LystoreBDD.OBJECT);
        if (object.length() > 80) {
            object = object.substring(0, 79);
        }
        JsonArray params = new JsonArray()
                .add(instruction.getInteger(LystoreBDD.ID_EXERCISE))
                .add(object)
                .add(instruction.getString(LystoreBDD.SERVICE_NUMBER))
                .add(instruction.getString(LystoreBDD.CP_NUMBER))
                .add(instruction.getBoolean(LystoreBDD.SUBMITTED_TO_CP))
                .add(instruction.getString(LystoreBDD.DATE_CP))
                .add(instruction.getString(LystoreBDD.COMMENT));
        if (instruction.getString(LystoreBDD.CP_ADOPTED) != null) {
            params.add(instruction.getString(LystoreBDD.CP_ADOPTED));
        }
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void checkCpValue(Number id, String cp_adopted, Handler<Either<String, JsonObject>> handler) {
        switch (cp_adopted){
            case EnumConstant.VALID_CP_STATUS :
                handleCpAdopted(id,  handler);
                break;
            case EnumConstant.REJECTED_CP_STATUS :
                handleCpRejected(id,  handler);
                break;
            default:
                handler.handle(new Either.Right<>(new JsonObject()));
                break;
        }
    }

    private void handleCpRejected(Number id,  Handler<Either<String, JsonObject>> handler) {
        String queryGetOrders = "SELECT distinct orders.id as " + LystoreBDD.ID_ORDER + ", " +
                "CASE WHEN orders.override_region is null" +
                " then '" + CommonConstants.REGION + "' " +
                " else '" + CommonConstants.EPLE + "'" +
                " END as " + LystoreBDD.ORDER_ORIGIN + " " +
                "from  " + Lystore.lystoreSchema + ".allOrders orders " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".operation on operation.id = orders.id_operation " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".instruction on instruction.id = operation.id_instruction and instruction.id = ? " +
                "WHERE override_region IS NOT true " +
                "; ";

        sql.prepared(queryGetOrders, new JsonArray().add(id), SqlResult.validResultHandler(event -> {
                    JsonArray sqlResults = event.right().getValue();
                    generateOrdersRejectStatements(sqlResults,  handler, id);
                })
        );
    }

    private void generateOrdersRejectStatements(JsonArray sqlResults, Handler<Either<String, JsonObject>> handler, Number id) {
        JsonArray statements = new JsonArray();
        sqlResults.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .forEach(result -> {
                    Integer orderId = result.getInteger(LystoreBDD.ID_ORDER);
                    String type = result.getString(LystoreBDD.ORDER_ORIGIN);
                    if (type.equals(CommonConstants.EPLE)) {
                        orderService.addRejectedOrderStatements(statements, orderId, "");
                    } else {
                        statements.add(orderRegionService.getUpdateRejectOrderRegionStatement(orderId));
                    }
                });
        if (statements.size() > 0)
            sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event, id)));
        else
            handler.handle(new Either.Right<>(new JsonObject()));
    }

    private void handleCpAdopted(Number id,  Handler<Either<String, JsonObject>> handler) {
        String queryGetOrders=  "SELECT distinct orders.id,orders.id_contract, " +
                "CASE WHEN orders.override_region is null then 'REGION' else 'EPLE' END " +
                "from  " +  Lystore.lystoreSchema + ".allOrders orders " +
                "INNER JOIN  " +  Lystore.lystoreSchema + ".contract on contract.id = orders.id_contract " +
                "INNER join  " +  Lystore.lystoreSchema + ".contract_type on contract.id_contract_type = contract_type.id " +
                "INNER JOIN  " +  Lystore.lystoreSchema + ".operation on operation.id = orders.id_operation " +
                "INNER JOIN  " +  Lystore.lystoreSchema + ".instruction on instruction.id = operation.id_instruction and instruction.id = ? " +
                "WHERE code != '236'    AND override_region IS NOT true " +
                "order by id_contract ; ";
        Map<Integer,JsonArray> mapMarket = new HashMap<>();

        sql.prepared(queryGetOrders, new JsonArray().add(id) ,new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (event.body().containsKey("status") && "ok".equals(event.body().getString("status"))) {
                    JsonArray sqlResults = event.body().getJsonArray("results");
                    for(Object o : sqlResults){
                        JsonArray sqlResult = (JsonArray)o;
                        Integer idMarket = sqlResult.getInteger(1);
                        if(mapMarket.containsKey(idMarket)){
                            mapMarket.put(idMarket,mapMarket.get(idMarket).add(sqlResult));
                        }else{
                            mapMarket.put(idMarket,new JsonArray().add(sqlResult));
                        }
                    }
                }
                JsonArray statements = new JsonArray();
                generateOrdersUpdateStatements(mapMarket, statements, handler, id);
            }
        });

    }
    private void generateOrdersUpdateStatements(Map<Integer, JsonArray> mapMarket, JsonArray statements,
                                                Handler<Either<String, JsonObject>> handler, Number id) {
        List<Promise<JsonArray>> promises = new ArrayList<>();
         for (Map.Entry<Integer, JsonArray> entry : mapMarket.entrySet()) {
            Promise<JsonArray> promise = Promise.promise();
            promises.add(promise);
        }
        Future.all(FutureHelper.promisesToFutures(promises)).onComplete(new Handler<AsyncResult<CompositeFuture>>() {
            @Override
            public void handle(AsyncResult<CompositeFuture> event) {
                if (event.succeeded()) {
                    List<JsonArray> resultsList = event.result().list();
                    for (JsonArray objects : resultsList) {
                        statements.addAll(objects);
                    }
                    if(statements.size() > 0 ) {
                        sql.transaction(statements, transactionEvent -> handler.handle(SqlQueryUtils.getTransactionHandler(transactionEvent, id)));
                    }else {
                        handler.handle(new Either.Right<>(new JsonObject()));
                    }
                }
            }
        });

        int i = 0;
        for (Map.Entry<Integer, JsonArray> entry : mapMarket.entrySet()) {
            createNewValidationNumber(id,entry,FutureHelper.handlerJsonArray(promises.get(i++)));
        }
    }

    private void createNewValidationNumber(Number idInstruction,Map.Entry<Integer, JsonArray> entry, Handler<Either<String, JsonArray>> handler) {
        String getIdQuery = getNextValidationNumber();
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    JsonArray statements = new JsonArray();
                    JsonArray orders = entry.getValue();
                    final String numberOrder = event.right().getValue().getString("numberorder");
                    for (Object o : orders) {
                        JsonArray order = (JsonArray) o;
                        if (order.getString(2).equals("REGION")) {
                            statements.add(getUpdateOrdersStatementRegion(order.getInteger(0), numberOrder));
                        } else {
                            statements.add(getUpdateOrdersStatementClient(order.getInteger(0), numberOrder));
                        }
                    }
                    handler.handle(new Either.Right<>(statements));
                }else{
                    log.error("error When creating new validation number");
                    handler.handle(new Either.Left<>("error When creating new validation number"));
                }
            }

        }));
    }


    private JsonObject getUpdateOrdersStatementRegion(Number id, String numberOrder) {
        String statement =
                "UPDATE " +
                        Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                        "SET " +
                        "  status = 'VALID', number_validation = ? " +
                        "where id = ?  AND STATUS NOT IN ('VALID', 'DONE', 'SENT');";


        JsonArray params = new JsonArray().add(numberOrder).add(id);
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");

    }
    private JsonObject getUpdateOrdersStatementClient(Number id, String numberOrder) {
        String statement =
                "UPDATE " +
                        Lystore.lystoreSchema + ".order_client_equipment " +
                        "SET " +
                        "  status = 'VALID', number_validation = ? " +
                        "where id = ? AND STATUS NOT IN ('VALID', 'DONE', 'SENT');";


        JsonArray params = new JsonArray().add(numberOrder).add(id);
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");

    }

    public void updateInstruction(Integer id, JsonObject instruction, Handler<Either<String, JsonObject>> handler) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".instruction " +
                "SET " +
                "id_exercise = ? ," +
                "object = ? , " +
                "service_number = ? , " +
                "cp_number = ? , " +
                "submitted_to_cp = ? , " +
                "date_cp = ? , " +
                "comment = ? ,  ";
        query += instruction.getString(LystoreBDD.CP_ADOPTED) != null ? "cp_adopted = ? " : "cp_adopted = NULL ";
        query += "WHERE id = ? " +
                "RETURNING id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(instruction.getInteger(LystoreBDD.ID_EXERCISE))
                .add(instruction.getString(LystoreBDD.OBJECT))
                .add(instruction.getString(LystoreBDD.SERVICE_NUMBER))
                .add(instruction.getString(LystoreBDD.CP_NUMBER))
                .add(instruction.getBoolean(LystoreBDD.SUBMITTED_TO_CP))
                .add(instruction.getString(LystoreBDD.DATE_CP))
                .add(instruction.getString(LystoreBDD.COMMENT));
        if (instruction.getString(LystoreBDD.CP_ADOPTED) != null) {
            params.add(instruction.getString(LystoreBDD.CP_ADOPTED));
        }
        params.add(id);
        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    public  void deleteInstruction(JsonArray instructionIds, Handler<Either<String, JsonObject>> handler){
        JsonArray values = new JsonArray();
        for (int i = 0; i < instructionIds.size(); i++) {
            values.add(instructionIds.getValue(i));
        }
        String query = "DELETE FROM " +
                Lystore.lystoreSchema +
                ".instruction " +
                "WHERE id IN " +
                Sql.listPrepared(instructionIds.getList()) +
                " RETURNING id";
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }
}
