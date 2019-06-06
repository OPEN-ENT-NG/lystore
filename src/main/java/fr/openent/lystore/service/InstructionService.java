package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface InstructionService {

    void getExercises(Handler<Either<String, JsonArray>> handler);

    void getInstructions(List<String> filters, Handler<Either<String, JsonArray>> handler);

 /*   void create(JsonObject operation,  Handler<Either<String, JsonObject>> handler);

  /*  void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler);*/

    void deleteInstruction(JsonArray instructionIds,  Handler<Either<String, JsonObject>> handler);
}
