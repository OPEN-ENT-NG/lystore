package fr.openent.lystore.controllers;

import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.LabelOperationService;
import fr.openent.lystore.service.ServiceFactory;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class LabelOperationController extends ControllerHelper {
    private final LabelOperationService labelOperationService;

    public LabelOperationController(ServiceFactory serviceFactory) {
        super();
        this.labelOperationService = serviceFactory.labelOperationService();
    }

    @Get("/labels/")
    @ApiDoc("Returns all labels in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getLabels (HttpServerRequest request) {
        labelOperationService.getLabels(request.params().getAll("q"), arrayResponseHandler(request));
    }

    @Post("/labelOperation/manage")
    @ApiDoc("Create a label operation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void createLabelOperation(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "manageLabel", labelOperation -> labelOperationService.createLabelOperation(labelOperation,
                Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.LABEL_OPERATION.toString(),
                        Actions.CREATE.toString(),
                        null,
                        labelOperation)));
    }

    @Put("/labelOperation/manage/:idLabelOperation")
    @ApiDoc("Update a label operation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateLabelOperation(final HttpServerRequest request) {
        final Integer idLabelOperation = Integer.parseInt(request.params().get("idLabelOperation"));
        RequestUtils.bodyToJson(request, pathPrefix + "manageLabel", labelOperation -> labelOperationService.updateLabelOperation(
                idLabelOperation, labelOperation, Logging.defaultResponseHandler(
                        eb,
                        request,
                        Contexts.LABEL_OPERATION.toString(),
                        Actions.UPDATE.toString(),
                        idLabelOperation.toString(),
                        labelOperation
                )
        ));
    }

    @Delete("/labelOperations/manage")
    @ApiDoc("Delete label operation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteLabelOperation(final HttpServerRequest request) {
        RequestUtils.bodyToJsonArray(request, labelOperationIds ->
                labelOperationService.checkIfLabelUsed(labelOperationIds, new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if(event.isRight()){
                            if(event.right().getValue().size() == 0) {
                                labelOperationService.deleteLabelOperation(labelOperationIds, Logging.defaultResponseHandler(eb,
                                        request,
                                        Contexts.LABEL_OPERATION.toString(),
                                        Actions.DELETE.toString(),
                                        labelOperationIds.toString(),
                                        new JsonObject().put("ids", labelOperationIds)));
                            } else {
                                request.response().setStatusMessage("Cannot delete anymore : an operation might have been created with one of the labels")
                                        .setStatusCode(202).end();
                            }

                        }else{
                            log.error("[Lystore] OperationController Error when checking label_operation used in operations" );
                            badRequest(request);
                        }
                    }
                }));
    }
}
