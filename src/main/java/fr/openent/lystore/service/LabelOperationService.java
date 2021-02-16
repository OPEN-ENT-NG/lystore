package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface LabelOperationService {

    void getLabels (List<String> filters, Handler<Either<String, JsonArray>> handler);

    void createLabelOperation(JsonObject labelOperation, Handler<Either<String, JsonObject>> handler);

    void updateLabelOperation(Integer id, JsonObject labelOperation, Handler<Either<String, JsonObject>> handler);

    void deleteLabelOperation(JsonArray labelOperationIds, Handler<Either<String, JsonObject>> handler);

    void checkIfLabelUsed(JsonArray labelOperationIds, Handler<Either <String, JsonArray>> handler);
}
