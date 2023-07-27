package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface InstructionService {

    void getExercises(Handler<Either<String, JsonArray>> handler);

    void getInstructions(List<String> filters, HttpServerRequest request, Handler<Either<String, JsonArray>> handler);

    void create(JsonObject instruction,  Handler<Either<String, JsonObject>> handler);

    void checkCpValue(Number id, String cp_adopted, OrderService orderService, OrderRegionService orderRegionService,
                      Handler<Either<String, JsonObject>> handler);

    void updateInstruction(Integer id, JsonObject instruction, Handler<Either<String, JsonObject>> handler);

    void deleteInstruction(JsonArray instructionIds,  Handler<Either<String, JsonObject>> handler);

    void getOperationOfInstruction(Integer IdInstruction, OperationService operationService,  Handler<Either<String, JsonArray>> handler);
}
