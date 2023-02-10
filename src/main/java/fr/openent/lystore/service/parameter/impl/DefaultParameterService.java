package fr.openent.lystore.service.parameter.impl;

import fr.openent.lystore.model.parameter.BCOptions;
import fr.openent.lystore.service.parameter.ParameterService;
import fr.openent.lystore.utils.LystoreUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.lystore.constants.ParametersConstants.*;

public class DefaultParameterService extends SqlCrudService implements ParameterService {
    public DefaultParameterService(String schema, String table) {
        super(schema, table);
    }

    private Logger log = LoggerFactory.getLogger(DefaultActiveStructureService.class);

    @Override
    public void getBcOptions(Handler<Either<String, JsonObject>> handler) {
        getBcOptions()
                .onSuccess(success -> handler.handle(new Either.Right<>(success.toJson())))
                .onFailure(fail -> handler.handle(new Either.Left<>(fail.getMessage())));
    }

    public Future<BCOptions> getBcOptions() {
        Promise<BCOptions> promise = Promise.promise();
        String query = "SELECT name, address, signature, image as img from " + this.resourceTable;
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                try {
                    JsonObject result = event.right().getValue().getJsonObject(0);
                    promise.complete(new BCOptions(result));
                } catch (Exception e) {
                    log.error(LystoreUtils.generateErrorMessage(DefaultParameterService.class,
                            "getBcOptions","error when casting sql result", e));
                    promise.fail(
                            LystoreUtils.generateErrorMessage(DefaultParameterService.class,
                                    "getBcOptions","error when getting sql result", e));
                }
            } else {
                promise.fail(LystoreUtils.generateErrorMessage(DefaultParameterService.class,
                        "getBcOptions","error when getting sql result", event.left().getValue()));
            }
        }));
        return promise.future();
    }



    @Override
    public void putBcOptions(JsonObject parameter, Handler<Either<String, JsonArray>> handler) {
        String query = "UPDATE " + this.resourceTable + " SET name = ?, address = ?, signature= ?, image= ? ;";
        JsonArray params = new JsonArray();
        params.add(getStringWithBrakeLine(parameter.getJsonObject(NAME)))
                .add(getStringWithBrakeLine(parameter.getJsonObject(ADDRESS)))
                .add(getStringWithBrakeLine(parameter.getJsonObject(SIGNATURE)))
                .add(parameter.getString(IMG));
        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }

    private String getStringWithBrakeLine(JsonObject jsonObject) {
        StringBuilder result = new StringBuilder();
        List<String> list = jsonObject.stream().map(entry -> {
            if (entry.getValue().getClass() == String.class) {
                return (String) entry.getValue();
            } else {
                return "";
            }
        }).collect(Collectors.toList());

        for (String elem : list) {
            result.append(elem).append("\n");
        }
        return result.toString();
    }
}
