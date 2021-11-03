package fr.openent.lystore.controllers;

import fr.openent.lystore.helpers.FileHelper;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.OrderRegionService;
import fr.openent.lystore.service.impl.DefaultOrderRegionService;
import fr.openent.lystore.service.impl.DefaultOrderService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class OrderRegionController extends BaseController {


    private OrderRegionService orderRegionService;
    private Storage storage;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderService.class);

    public static final String UTF8_BOM = "\uFEFF";


    public OrderRegionController(Storage storage) {
        this.orderRegionService = new DefaultOrderRegionService("equipment");
        this.storage = storage;

    }

//    @Post("/order/upload/file")
//    @ApiDoc("Upload a file for a specific order")
//    @SecuredAction(value = "", type = ActionType.RESOURCE)
//    @ResourceFilter(ManagerRight.class)
//    public void uploadFile(HttpServerRequest request) {
//        storage.writeUploadFile(request, entries -> {
//            if (!"ok".equals(entries.getString("status"))) {
//                renderError(request);
//                return;
//            }
//            try {
//                String fileId = entries.getString("_id");
//                String filename = entries.getJsonObject("metadata").getString("filename");
//                orderRegionService.addFileToOrder(fileId, filename, event -> {
//                    if (event.isRight()) {
//                        JsonObject response = new JsonObject()
//                                .put("id", fileId)
//                                .put("filename", filename);
//                        request.response().setStatusCode(201).putHeader("Content-Type", "application/json").end(response.toString());
//                    } else {
//                        deleteFile(fileId);
//                        renderError(request);
//                    }
//                });
//            } catch (NumberFormatException e) {
//                renderError(request);
//            }
//        });
//    }

    @Post("/region/order")
    @ApiDoc("Create an order with id order client when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createWithOrderClientAdminOrder(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos event) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject order) {
                        RequestUtils.bodyToJson(request, orderRegion -> orderRegionService.setOrderRegion(order, event, Logging.defaultResponseHandler(eb,
                                request,
                                Contexts.ORDERREGION.toString(),
                                Actions.CREATE.toString(),
                                null,
                                orderRegion)));
                    }
                });
            }

        });
    }

    @Put("/region/order/:id")
    @ApiDoc("Update an order when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateAdminOrder(final HttpServerRequest request) {
        Integer idOrder = Integer.parseInt(request.getParam("id"));
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos event) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject order) {
                        RequestUtils.bodyToJson(request, orderRegion -> orderRegionService.updateOrderRegion(order, idOrder, event, Logging.defaultResponseHandler(eb,
                                request,
                                Contexts.ORDERREGION.toString(),
                                Actions.UPDATE.toString(),
                                idOrder.toString(),
                                new JsonObject().put("orderRegion", orderRegion))));
                    }
                });
            }
        });
    }

    @Post("/region/orders/")
    @ApiDoc("Create orders from a region")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        String totalFiles = request.getHeader("Files");
        if (Integer.parseInt(totalFiles) != 0) {
//            storage.writeUploadFile(request, resultUpload -> {
//                if (!"ok".equals(resultUpload.getString("status"))) {
//                    String message = "[Presences@DefaultStatementAbsenceController:createWithFile] Failed to save file.";
//                    log.error(message + " " + resultUpload.getString("message"));
//                    renderError(request);
//                    return;
//                }
//
//                cptFiles.set(cptFiles.get() + 1);
//                String file_id = resultUpload.getString("_id");
//                JsonObject metadata = resultUpload.getJsonObject("metadata").put("file_id", file_id);
//                JsonObject file = metadata;
//                files.add(file);
//
//                if (cptFiles.get() == Integer.parseInt(totalFiles)) {
//                renderJson(request, resultUpload);
//                    orderCreate(request, files);
//                }
//            });
            FileHelper.uploadMultipleFiles(totalFiles, request, storage, vertx.fileSystem())
                    .onSuccess(res -> {
                        // todo récupérer Jsonobject Payload (RequestUtils.bodyToJson ?)
                        orderCreate(request, new ArrayList());
                    })
                    .onFailure(err -> {
                        String message = String.format("[Lystore@%s::createAdminOrder] An error has occurred " +
                                        "during upload files: %s",
                                this.getClass().getSimpleName(), err.getMessage());
                        log.error(message, err);
                        renderError(request);
                    });
        } else {
            request.pause();
            request.setExpectMultipart(true);
            ArrayList noFiles = new ArrayList();
            orderCreate(request, noFiles);
        }
    }

    private void orderCreate(final HttpServerRequest request, ArrayList files) {
        try {
            JsonObject order = formatDataToJson(request, files);
            UserUtils.getUserInfos(eb, request, user -> {
                if (!order.isEmpty()) {
                    Integer id_title = Integer.parseInt(order.getString("title_id"));
                    orderRegionService.createProject(id_title, idProject -> {
                        if (idProject.isRight()) {
                            Integer idProjectRight = idProject.right().getValue().getInteger("id");
                            Logging.insert(eb,
                                    request,
                                    Contexts.PROJECT.toString(),
                                    Actions.CREATE.toString(),
                                    idProjectRight.toString(),
                                    new JsonObject().put("id", idProjectRight).put("id_title", id_title));
                            orderRegionService.createOrdersRegion(order, user, idProjectRight, orderCreated -> {
                                if (orderCreated.isRight()) {
                                    Number idReturning = orderCreated.right().getValue().getInteger("id");
                                    Logging.insert(eb,
                                            request,
                                            Contexts.ORDERREGION.toString(),
                                            Actions.CREATE.toString(),
                                            idReturning.toString(),
                                            new JsonObject().put("order region", order));
                                } else {
                                    LOGGER.error("An error when you want get id after create order region " + orderCreated.left());
                                    request.response().setStatusCode(400).end();
                                }
                            });

                            request.response().setStatusCode(201).end();
                        } else {
                            LOGGER.error("An error when you want get id after create project " + idProject.left());
                            request.response().setStatusCode(400).end();
                        }
                    });
                }
            });
        } catch (Exception e) {
            LOGGER.error("An error when you want create order region and project", e);
            request.response().setStatusCode(400).end();
        }
    }

    private JsonObject formatDataToJson(HttpServerRequest request, ArrayList files) {
        JsonObject body = new JsonObject();
        request.formAttributes().entries().forEach(entry -> {
            body.put(entry.getKey(), entry.getValue()).put("files", files);
        });
        return body;
    }

    @Delete("/region/:id/order")
    @ApiDoc("delete order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteOrderRegion(final HttpServerRequest request) {
        Integer idRegion = Integer.parseInt(request.getParam("id"));
        orderRegionService.deleteOneOrderRegion(idRegion, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDERREGION.toString(),
                Actions.DELETE.toString(),
                idRegion.toString(),
                new JsonObject().put("idRegion", idRegion)));
    }

    @Get("/orderRegion/:id/order")
    @ApiDoc("get order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOneOrder(HttpServerRequest request) {
        Integer idOrder = Integer.parseInt(request.getParam("id"));
        orderRegionService.getOneOrderRegion(idOrder, defaultResponseHandler(request));
    }

    @Put("/order/region/:idOperation/operation")
    @ApiDoc("update operation in orders region")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperation(final HttpServerRequest request) {
        final Integer idOperation = Integer.parseInt(request.params().get("idOperation"));
        RequestUtils.bodyToJsonArray(request, idsOrders -> orderRegionService.updateOperation(idOperation, idsOrders, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDER.toString(),
                Actions.UPDATE.toString(),
                idOperation.toString(),
                new JsonObject().put("ids", idsOrders))));
    }

    @Get("/order/region/create/file/:fileId")
    @ApiDoc("Download specific file")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getFileOrderRegion(HttpServerRequest request) {
        String fileId = request.getParam("fileId");
        orderRegionService.getFileOrderRegion(fileId, event -> {
            if (event.isRight()) {
                storage.sendFile(fileId, event.right().getValue().getString("filename"), request, false, new JsonObject());
            } else {
                notFound(request);
            }
        });
    }

    /**
     * //     * Delete file from storage based on identifier
     * //     *
     * //     * @param fileId File identifier to delete
     * //
     */
    private void deleteFile(String fileId) {
        storage.removeFile(fileId, e -> {
            if (!"ok".equals(e.getString("status"))) {
                log.error("[Lystore@uploadFile] An error occurred while removing " + fileId + " file.");
            }
        });
    }
}
