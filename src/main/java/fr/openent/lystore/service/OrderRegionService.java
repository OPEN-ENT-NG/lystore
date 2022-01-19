package fr.openent.lystore.service;

import fr.openent.lystore.model.file.Attachment;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;

public interface OrderRegionService {
    void setOrderRegion(JsonObject order, int idOrderClient, JsonArray files, ArrayList<String> oldFiles, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void updateOrderRegion(JsonObject order, Storage storage, int idOrder, JsonArray files, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void createOrdersRegion(JsonObject order, List<Attachment> files, UserInfos event, Number id_project, Handler<Either<String, JsonObject>> handler);

    void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void getOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void updateOperation(Integer id, JsonArray orders, Handler<Either<String, JsonObject>> handler);

    public void linkOrderToOperation(Integer id_order_client_equipment, Integer id_operation, Handler<Either<String, JsonObject>> handler);

    void createProject (Integer idProject,  Handler<Either<String, JsonObject>> handler);

    void getFilesId(Integer idOrder , Handler<Either<String,JsonArray>> handler);

    void getFileOrderRegion(String fileId, Handler<Either<String, JsonObject>> handler);

    void getIdFilesToDelete(List<String> idsFiles, Integer idOrder, Handler<Message<JsonObject>>handler);
}
