package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.service.InstructionService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;

import java.util.ArrayList;
import java.util.List;

import static fr.openent.lystore.service.impl.DefaultOrderService.getNextValidationNumber;

public class DefaultInstructionService  extends SqlCrudService implements InstructionService {

    private static final Logger log = LoggerFactory.getLogger (DefaultOrderService.class);
    private DefaultOperationService operationService = new DefaultOperationService(Lystore.lystoreSchema, "operation");

    public DefaultInstructionService(
            String schema, String table) {
        super(schema, table);
    }

    public void getExercises (Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema +".exercise";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler) );
    }


    private String getTextFilter(List<String> filters) {
        String filter = "";
        if (filters.size() > 0) {
            filter = "WHERE ";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) {
                    filter += "AND ";
                }
                filter += "(LOWER(instruction.object) ~ LOWER(?) OR " +
                        "LOWER(instruction.service_number) ~ LOWER(?) OR " +
                        "LOWER(instruction.cp_number) ~ LOWER(?) OR " +
                        "LOWER(exercise.year) ~ LOWER(?)) ";
            }
        }
        return filter;
    }

    public void getInstructions(List<String> filters, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter).add(filter);
            }
        }
        String query =  "SELECT instruction.*, " +
                "to_json(exercise.*) AS exercise, " +
                "array_to_json(array_agg( o.id )) AS operations " +
                "FROM  " + Lystore.lystoreSchema +".instruction " +
                "INNER JOIN " + Lystore.lystoreSchema +".exercise exercise ON exercise.id = instruction.id_exercise " +
                "LEFT JOIN " + Lystore.lystoreSchema +".operation o ON o.id_instruction = instruction.id " +
                getTextFilter(filters) +
                " GROUP BY (instruction.id, exercise.id);";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(instructionsEither -> {
            try{
                if (instructionsEither.isRight()) {
                    JsonArray instructions = instructionsEither.right().getValue();
                    if(instructions.size() == 0){
                        handler.handle(new Either.Right<>(instructions));
                        return;
                    }
                    JsonArray idsInstructions = SqlQueryUtils.getArrayAllIdsResult(instructions);
                    Future<JsonArray> getSumOperationsFutur = Future.future();

                    List<Future> futursArray = new ArrayList<>();
                    futursArray.add(getSumOperationsFutur);

                    CompositeFuture.join( futursArray ).setHandler(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray getSumOperations = getSumOperationsFutur.result();
                        JsonArray instructionsResult =  SqlQueryUtils.addDataByIdJoin(instructions, getSumOperations,"amount");
                        handler.handle(new Either.Right<>(instructionsResult));
                    });

                    SqlUtils.getSumOperations(idsInstructions, FutureHelper.handlerJsonArray(getSumOperationsFutur));

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

                    Future<JsonArray> getCountOrderInOperationFuture = Future.future();
                    Future<JsonArray> getAllPriceOperationFuture = Future.future();
                    Future<JsonArray> getNumberOrderSubventionFuture = Future.future();

                    CompositeFuture.all( getCountOrderInOperationFuture, getAllPriceOperationFuture, getNumberOrderSubventionFuture ).setHandler(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray operationsFinal = new JsonArray();
                        JsonArray getNbrOrder = getCountOrderInOperationFuture.result();
                        JsonArray getAmountsDemands = getAllPriceOperationFuture.result();
                        JsonArray getNumberSubvention = getNumberOrderSubventionFuture.result();

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

                    SqlUtils.getCountOrderInOperation(idsOperations,  FutureHelper.handlerJsonArray(getCountOrderInOperationFuture));
                    SqlUtils.getAllPriceOperation(idsOperations,  FutureHelper.handlerJsonArray(getAllPriceOperationFuture));
                    operationService.getNumberOrderSubvention(idsOperations,  FutureHelper.handlerJsonArray(getNumberOrderSubventionFuture));
                }
            } catch ( Exception e){
                log.error("An error when you want get all instructions", e);
                handler.handle(new Either.Left<>("404"));
            }

        }));
    }



    public void create(JsonObject instruction, Handler<Either<String, JsonObject>> handler){
        String getIdQuery = "Select nextval('"+ Lystore.lystoreSchema + ".instruction_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler( new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()) {
                    try{
                        final Number id = event.right().getValue().getInteger("id");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getInstructionCreationStatement(id,instruction));

                        handleAdoptedCP(id, statements, instruction, handler);

                    }catch(ClassCastException e){
                        log.error("An error occured when casting structures ids " + e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                }else{
                    log.error("An error occurred when selecting next val");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));




    }

    private void handleAdoptedCP(Number id, JsonArray statements, JsonObject instruction, Handler<Either<String, JsonObject>> handler) {
        if(instruction.getBoolean("cp_adopted")){
            String getIdQuery = getNextValidationNumber();
            sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> event) {
                    final String numberOrder = event.right().getValue().getString("numberorder");
                    statements.add( getUpdateOrdersStatementClient(id,numberOrder));
                    statements.add(getUpdateOrdersStatementRegion(id,numberOrder));
                    sql.transaction(statements, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            handler.handle(SqlQueryUtils.getTransactionHandler(event,id));
                        }
                    });
                }
            }));
        }


    }



    private JsonObject getUpdateOrdersStatementRegion(Number id, String numberOrder) {
        String statement =
                "UPDATE " +
                        Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                        "SET " +
                        "  status = 'VALID', number_validation = ? " +
                        "FROM " +
                        "  ( " +
                        "    SELECT " +
                        "      \"order-region-equipment\".id, " +
                        "      \"order-region-equipment\".status, " +
                        "      \"order-region-equipment\".id_operation, " +
                        "      contract_type.code " +
                        "    FROM " +
                        "      lystore.\"order-region-equipment\" " +
                        "      inner join " + Lystore.lystoreSchema +".operation on \"order-region-equipment\".id_operation = operation.id " +
                        "      inner join " + Lystore.lystoreSchema +".instruction on operation.id_instruction = instruction.id AND instruction.id = ? " +
                        "      inner join " + Lystore.lystoreSchema +".contract on contract.id = \"order-region-equipment\".id_contract " +
                        "      inner join " + Lystore.lystoreSchema +".contract_type on contract.id_contract_type = contract_type.id " +
                        "  ) as ore " +
                        "where " +
                        "  ore.id_operation is not null " +
                        "  and ore.code != '236' " +
                        "  and \"order-region-equipment\".id = ore.id "+
                        "  and ore.status = 'IN PROGRESS';";


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
                        "FROM " +
                        "  ( " +
                        "    SELECT " +
                        "      order_client_equipment.id, " +
                        "      order_client_equipment.status, " +
                        "      order_client_equipment.id_operation, " +
                        "      contract_type.code " +
                        "    FROM " +
                        "      lystore.order_client_equipment " +
                        "      inner join " + Lystore.lystoreSchema +".operation on order_client_equipment.id_operation = operation.id " +
                        "      inner join " + Lystore.lystoreSchema +".instruction on operation.id_instruction = instruction.id AND instruction.id = ? " +
                        "      inner join " + Lystore.lystoreSchema +".contract on contract.id = order_client_equipment.id_contract " +
                        "      inner join " + Lystore.lystoreSchema +".contract_type on contract.id_contract_type = contract_type.id " +
                        "  ) as oce " +
                        "where " +
                        "  oce.id_operation is not null " +
                        "  and oce.code != '236' " +
                        "  and order_client_equipment.id = oce.id " +
                        "and oce.status IN ('IN PROGRESS');";

        JsonArray params = new JsonArray().add(numberOrder).add(id);
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");

    }
    private JsonObject getInstructionCreationStatement(Number id, JsonObject instruction) {

        String statement = "INSERT INTO " + Lystore.lystoreSchema +".instruction (" +
                "id, "+
                "id_exercise," +
                "object, " +
                "service_number, " +
                "cp_number, " +
                "submitted_to_cp, " +
                "date_cp, " +
                "comment" +
                ",cp_adopted" +
                ") " +
                "VALUES (? ,? ,? ,? ,? ,? ,? ," +
                "? ," +
                "? ) " +
                "RETURNING id; ";


        String object = instruction.getString("object");
        if(object.length() > 80){
            object = object.substring(0,79);
        }
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(instruction.getInteger("id_exercise"))
                .add(object)
                .add(instruction.getString("service_number"))
                .add(instruction.getString("cp_number"))
                .add(instruction.getBoolean("submitted_to_cp"))
                .add(instruction.getString("date_cp"))
                .add(instruction.getString("comment"))
                .add(instruction.getBoolean("cp_adopted"))
                ;

        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    public  void updateInstruction(Integer id, JsonObject instruction, Handler<Either<String, JsonObject>> handler){
        try{

            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                    .add(getUpdateInstructionStatement(id,instruction));
            handleAdoptedCP(id, statements, instruction, handler);

        }catch(ClassCastException e){
            log.error("An error occured when casting structures ids " + e);
            handler.handle(new Either.Left<String, JsonObject>(""));
        }
    }

    private JsonObject getUpdateInstructionStatement(Integer id, JsonObject instruction) {
        String statement = " UPDATE " + Lystore.lystoreSchema + ".instruction " +
                "SET " +
                "id_exercise = ? ," +
                "object = ? , " +
                "service_number = ? , " +
                "cp_number = ? , " +
                "submitted_to_cp = ? , " +
                "date_cp = ? , " +
                "comment = ?  " +
                ",cp_adopted = ?  " +
                "WHERE id = ? " +
                "RETURNING id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(instruction.getInteger("id_exercise"))
                .add(instruction.getString("object"))
                .add(instruction.getString("service_number"))
                .add(instruction.getString("cp_number"))
                .add(instruction.getBoolean("submitted_to_cp"))
                .add(instruction.getString("date_cp"))
                .add(instruction.getString("comment"))
                .add(instruction.getBoolean("cp_adopted"))
                .add(id);

        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
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
