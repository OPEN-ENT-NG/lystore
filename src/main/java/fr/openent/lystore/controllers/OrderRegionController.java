package fr.openent.lystore.controllers;

import fr.openent.lystore.helpers.AttachmentHelper;
import fr.openent.lystore.helpers.FileHelper;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.model.file.Attachment;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.OrderRegionService;
import fr.openent.lystore.service.impl.DefaultOrderRegionService;
import fr.openent.lystore.service.impl.DefaultOrderService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.*;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
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

    @Post("/region/from/client/:id")
    @ApiDoc("Create an order with id order client when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createWithOrderClientAdminOrder(final HttpServerRequest request) {
        Integer idOrder = Integer.parseInt(request.getParam("id"));
        JsonObject results = new JsonObject();
        //TODO effacer storage
        FileHelper.uploadMultipleFiles("Files", request, storage, vertx , config)
                .compose(files -> {
                    Promise<JsonObject> promise = Promise.promise();
                    request.endHandler(aVoid -> {
                        JsonObject order = formatDataToJson(request);
                        results.put("newFiles", AttachmentHelper.attachmentsToJsonArray(files));
                        promise.complete(order);
                    });
                    return promise.future();
                })
                .compose(bodyOrder -> {
                    Map<String, String> namesAndIds = generateMapFilenameId(bodyOrder);
                    results.put("order",bodyOrder);
                    return FileHelper.getCopyOldFiles(namesAndIds,storage);
                })
                .onSuccess(oldFiles -> UserUtils.getUserInfos(eb, request, user -> {

                    orderRegionService.setOrderRegion(results.getJsonObject("order"), idOrder, results.getJsonArray("newFiles"),oldFiles ,user, Logging.defaultResponseHandler(eb,
                            request,
                            Contexts.ORDERREGION.toString(),
                            Actions.UPDATE.toString(),
                            idOrder.toString(),
                            new JsonObject().put("orderRegion", results.getJsonObject("order"))));
                }))
                .onFailure(err -> {
                    String message = String.format("[Lystore@%s::createAdminOrder] An error has occurred " +
                                    "during upload files: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    log.error(message, err);
                    renderError(request);
                });
    }

    private Map<String, String> generateMapFilenameId(JsonObject bodyOrder) {
        String idFilesStr = bodyOrder.getString("oldFiles");
        String[] idFileArrayStr = idFilesStr.split(",");
        List<String> idsFiles = new ArrayList<>(Arrays.asList(idFileArrayStr));
        String nameFilesStr = bodyOrder.getString("oldFilesName");
        String[] nameFileArrayStr = nameFilesStr.split("/");
        List<String> namesFiles = new ArrayList<>(Arrays.asList(nameFileArrayStr));
        Map<String, String> namesAndIds = new HashMap<>();
        for (int i = 0; i < idsFiles.size(); i++) {
            if(idsFiles.get(i).length() > 0)
                namesAndIds.put(namesFiles.get(i), idsFiles.get(i));
        }
        return namesAndIds;
    }

    @Put("/region/order/:id")
    @ApiDoc("Update an order when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateAdminOrder(final HttpServerRequest request) {
        JsonObject results = new JsonObject();
        Integer idOrder = Integer.parseInt(request.getParam("id"));
        FileHelper.uploadMultipleFiles("Files", request, storage, vertx , config)
                .compose(files -> {
                    Promise<JsonObject> promise = Promise.promise();
                    request.endHandler(aVoid -> {
                        JsonObject order = formatDataToJson(request);
                        results.put("newFiles", AttachmentHelper.attachmentsToJsonArray(files));
                        promise.complete(order);
                    });
                    return promise.future();
                })
                .compose(bodyOrder -> {
                    Promise<List<String>> promise = Promise.promise();
                    String idFilesStr  = bodyOrder.getString("oldFiles");
                    String[] idFileArrayStr = idFilesStr.split(",");
                    List<String> idsFiles = new ArrayList<>(Arrays.asList(idFileArrayStr));
                    results.put("order",bodyOrder);
                    List<String> idsToRemove = new ArrayList<>();
                    orderRegionService.getIdFilesToDelete(idsFiles,idOrder, sql ->{
                        JsonArray resultsDeletedIds = sql.body().getJsonArray("results");
                        for (Object entry : resultsDeletedIds) {
                            idsToRemove.add(((JsonArray) entry).getString(0));
                        }
                        promise.complete(idsToRemove);
                    });
                    return promise.future();
                })
                .compose(idsToRemove -> FileHelper.deleteFiles(idsToRemove,storage))
                .onSuccess(files -> UserUtils.getUserInfos(eb, request, user -> {
                            request.endHandler(aVoid -> {
                                orderRegionService.updateOrderRegion(results.getJsonObject("order"), storage,idOrder,results.getJsonArray("newFiles"), user, Logging.defaultResponseHandler(eb,
                                        request,
                                        Contexts.ORDERREGION.toString(),
                                        Actions.UPDATE.toString(),
                                        idOrder.toString(),
                                        new JsonObject().put("orderRegion", results.getJsonObject("order"))));
                            });
                        })
                )
                .onFailure(err -> {
                    String message = String.format("[Lystore@%s::updateAdminOrder] An error has occurred " +
                                    "during upload files: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    log.error(message, err);
                    renderError(request);
                });
    }


    @Post("/region/orders/")
    @ApiDoc("Create orders from a region with attachments (0 or n)")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        FileHelper.uploadMultipleFiles("Files", request, storage, vertx , config)
                .onSuccess(files -> UserUtils.getUserInfos(eb, request, user -> {
                            request.endHandler(aVoid -> {
                                JsonObject order = formatDataToJson(request);
                                orderCreate(order, files, user, request)
                                        .onSuccess(res -> renderJson(request, new JsonObject()))
                                        .onFailure(err -> {
                                            String message = String.format("[Lystore@%s::createAdminOrder] An error has occurred " +
                                                            "during upload files: %s",
                                                    this.getClass().getSimpleName(), err.getMessage());
                                            log.error(message, err);
                                            renderError(request);
                                        });
                            });
                        })
                )
                .onFailure(err -> {
                    String message = String.format("[Lystore@%s::createAdminOrder] An error has occurred " +
                                    "during upload files: %s",
                            this.getClass().getSimpleName(), err.getMessage());
                    log.error(message, err);
                    renderError(request);
                });
    }

    private Future<Void> orderCreate(JsonObject order, List<Attachment> files, UserInfos userInfos, HttpServerRequest request) {
        Promise<Void> promise = Promise.promise();

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
                orderRegionService.createOrdersRegion(order, files, userInfos, idProjectRight, orderCreated -> {
                    if (orderCreated.isRight()) {

                        Number idReturning = orderCreated.right().getValue().getInteger("id");
                        Logging.insert(eb,
                                request,
                                Contexts.ORDERREGION.toString(),
                                Actions.CREATE.toString(),
                                idReturning.toString(),
                                new JsonObject().put("order region", order));
                        promise.complete();
                    } else {
                        LOGGER.error("[Lystore@%s::createAdminOrder] An error when you want get id after create order region: %s" + orderCreated.left()); // todo revoir commentaire
                        promise.fail(orderCreated.left().getValue());
                    }
                });
            } else {
                LOGGER.error("[Lystore@%s::createAdminOrder] An error when you want get id after create project: %s" + idProject.left()); // todo revoir commentaire
                promise.fail(idProject.left().getValue());
            }
        });

        return promise.future();
    }

    private JsonObject formatDataToJson(HttpServerRequest request) {
        JsonObject body = new JsonObject();
        request.formAttributes().entries().forEach(entry -> body.put(entry.getKey(), entry.getValue()));
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

    @Deprecated
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



    @Get("/orderRegion/:id/files")
    @ApiDoc("Download specific file")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getFilesOrderRegion(HttpServerRequest request) {
        Integer orderId = Integer.valueOf(request.getParam("id"));
        orderRegionService.getFilesId(orderId , arrayResponseHandler(request));
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
