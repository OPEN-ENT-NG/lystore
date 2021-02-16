package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.service.LabelOperationService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;

public class DefaultLabelOperationService extends SqlCrudService implements LabelOperationService {

    private static final Logger log = LoggerFactory.getLogger (DefaultLabelOperationService.class);

    public DefaultLabelOperationService(String table) {
        super(table);
    }

    private String getLabelTextFilter(List<String> filters) {
        String filter = "";
        if (filters.size() > 0) {
            filter = "WHERE ";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) {
                    filter += "AND ";
                }
                filter += "(LOWER(label_operation.label) ~ LOWER(?))";
            }
        }
        return filter;
    }

    public void getLabels (List<String> filters, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> getLabelsInfoFuture = Future.future();
        Future<JsonArray> getMaxOrdersDateLabelFuture = Future.future();

        List<Future> listFuture = new ArrayList<>();

        listFuture.add(getLabelsInfoFuture);
        listFuture.add(getMaxOrdersDateLabelFuture);

        CompositeFuture.all(listFuture).setHandler(makeLabelsDataArray(handler, getLabelsInfoFuture, getMaxOrdersDateLabelFuture));

        getLabelsInfo(filters, FutureHelper.handlerJsonArray(getLabelsInfoFuture));
        getMaxOrdersDateLabel(FutureHelper.handlerJsonArray(getMaxOrdersDateLabelFuture));
    }

    private Handler<AsyncResult<CompositeFuture>> makeLabelsDataArray(Handler<Either<String, JsonArray>> handler, Future<JsonArray> getLabelsInfoFuture, Future<JsonArray> getMaxOrdersDateLabelFuture) {

        return new Handler<AsyncResult<CompositeFuture>>() {
            @Override
            public void handle(AsyncResult<CompositeFuture> event) {
                if (event.failed()) {
                    log.error("makeLabelsDataArray failed");
                    handler.handle(new Either.Left<>("future failed"));
                    return;
                }

                JsonArray labels = getLabelsInfoFuture.result();
                JsonArray getMaxOrdersDateLabel = getMaxOrdersDateLabelFuture.result();

                for (int i = 0; i < labels.size(); i++) {
                    JsonObject label = labels.getJsonObject(i);
                    for (int k = 0; k < getMaxOrdersDateLabel.size(); k++) {
                        JsonObject maxOrderDate = getMaxOrdersDateLabel.getJsonObject(k);
                        if (label.getInteger("id").equals(maxOrderDate.getInteger("id_label"))) {
                            label.put("max_creation_date", maxOrderDate.getString("max_creation_date"));
                        }
                    }
                }
                handler.handle(new Either.Right<>(labels));
            }
        };
    }

    private void getLabelsInfo(List<String> filters, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter);
            }
        }

        String query = "SELECT label_operation.id, label_operation.label, label_operation.start_date, label_operation.end_date, COUNT(operation.id) AS is_used " +
                "FROM " + Lystore.lystoreSchema + ".label_operation " +
                "LEFT OUTER JOIN " +  Lystore.lystoreSchema + ".operation ON label_operation.id = operation.id_label " +
                getLabelTextFilter(filters) + " " +
                "GROUP BY label_operation.id;";

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private void getMaxOrdersDateLabel(Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();


        String query = "SELECT MAX(allOrders.creation_date) AS max_creation_date, operation.id_label " +
                "FROM " + Lystore.lystoreSchema + ".allOrders " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation ON operation.id = allOrders.id_operation " +
                "GROUP BY id_label;";

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void createLabelOperation(JsonObject labelOperation, Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " +
                Lystore.lystoreSchema + " .label_operation(label, start_date, end_date) " +
                "VALUES (?,?,?) RETURNING id";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(labelOperation.getString("label"))
                .add(labelOperation.getString("start_date"))
                .add(labelOperation.getString("end_date"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateLabelOperation(Integer id, JsonObject labelOperation, Handler<Either<String, JsonObject>> handler){
        String query = "UPDATE " + Lystore.lystoreSchema + ".label_operation " +
                " SET id = ?, " +
                " label = ?, " +
                " start_date = ?, " +
                " end_date = ? " +
                " WHERE id = ? " +
                " RETURNING id;";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
                .add(labelOperation.getInteger("id"))
                .add(labelOperation.getString("label"))
                .add(labelOperation.getString("start_date"))
                .add(labelOperation.getString("end_date"))
                .add(id);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void deleteLabelOperation(JsonArray labelOperationIds, Handler<Either<String, JsonObject>> handler){
        String query = "DELETE FROM " +
                Lystore.lystoreSchema +
                ".label_operation " + "WHERE id IN " +
                Sql.listPrepared(labelOperationIds.getList());
        JsonArray values = new JsonArray();
        for(int i = 0; i < labelOperationIds.size(); i++){
            values.add(labelOperationIds.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void checkIfLabelUsed(JsonArray labelOperationIds, Handler<Either <String, JsonArray>> handler) {
        String query = "SELECT 1 as isUsed FROM " + Lystore.lystoreSchema + ".operation WHERE operation.id_label IN " + Sql.listPrepared(labelOperationIds.getList());
        JsonArray values =  new JsonArray();
        for(int i = 0; i < labelOperationIds.size(); i++){
            values.add(labelOperationIds.getValue(i));
        }
        sql.prepared(query,values,SqlResult.validResultHandler(handler));
    }
}
