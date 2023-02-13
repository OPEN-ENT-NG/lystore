package fr.openent.lystore.service.parameter;

import fr.openent.lystore.model.parameter.BCOptions;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ParameterService {
    public Future<BCOptions> getBcOptions();

    void getBcOptions(Handler<Either<String, JsonObject>> arrayResponseHandler);

    void putBcOptions(JsonObject parameter, Handler<Either<String, JsonArray>> arrayResponseHandler);
}
