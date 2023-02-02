package fr.openent.lystore.service.parameter;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ParameterService {
    void getBcOptions(Handler<Either<String, JsonObject>> arrayResponseHandler);

    void putBcOptions(JsonObject parameter, Handler<Either<String, JsonArray>> arrayResponseHandler);
}
