package fr.openent.lystore.service;

import fr.openent.lystore.model.Structure;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface StructureService {

    /**
     * list all Structures in database
     * @param handler function handler returning data
     */
    void  getStructures(Handler<Either<String,JsonArray>> handler);

    void getStructureTypes(Handler<Either<String,JsonArray>> handler);

    void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler);

    /**
     * renvoie un JsonArray
     * @param ids
     * @param handler
     */
    void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler);

    /**
     *  renvoi une list<Structure>
     * @param ids
     * @return
     */
    Future<List<Structure>> getStructureById(JsonArray ids);
}
