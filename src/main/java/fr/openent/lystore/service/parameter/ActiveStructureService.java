package fr.openent.lystore.service.parameter;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ActiveStructureService {
    void undeployStructureLystore(String structureId, Handler<Either<String, JsonObject>> handler);

    void getStructuresLystore(Handler<Either<String, JsonArray>> arrayResponseHandler);

    void createLystoreGroupToStructure(JsonObject body, Handler<Either<String, JsonObject>> handler);
}
