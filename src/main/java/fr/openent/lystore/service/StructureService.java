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

    /** get structure by id
     *
     * @param ids list of structure identifier (JsonArray containing of list {@link String}
     * @param handler JsonArray of Structure (see Structure model)
     */
    void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler);

    /** get structure by id
     *
     * @param ids list of structure identifier (JsonArray containing of list {@link String}
     * @return Future of list {@link Structure}
     */
    Future<List<Structure>> getStructureById(JsonArray ids);
}
