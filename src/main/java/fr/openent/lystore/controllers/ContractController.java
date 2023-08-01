package fr.openent.lystore.controllers;

import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ContractService;
import fr.openent.lystore.factory.ServiceFactory;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ContractController extends ControllerHelper {

    private ContractService contractService;

    public ContractController(ServiceFactory serviceFactory) {
        super();
        this.contractService = serviceFactory.contractService();
    }

    @Get("/contracts")
    @ApiDoc("Display all contracts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getContracts (HttpServerRequest request) {
        contractService.getContracts(arrayResponseHandler(request));
    }

    @Post("/contract")
    @ApiDoc("Create a contract")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void createContract (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "contract",
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject contract) {
                        contractService.createContract(contract,
                                Logging.defaultResponseHandler(eb,
                                        request,
                                        Contexts.CONTRACT.toString(),
                                        Actions.CREATE.toString(),
                                        null,
                                        contract));
                    }
                });
    }

    @Put("/contract/:id")
    @ApiDoc("Update a contract")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateContract (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "contract",
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject contract) {
                        try {
                            contractService.updateContract(contract,
                                    Integer.parseInt(request.params().get("id")),
                                    Logging.defaultResponseHandler(eb,
                                            request,
                                            Contexts.CONTRACT.toString(),
                                            Actions.UPDATE.toString(),
                                            request.params().get("id"),
                                            contract));
                        } catch (ClassCastException e) {
                            log.error("An error occurred when casting contract id", e);
                            badRequest(request);
                        }
                    }
                });
    }

    @Delete("/contract")
    @ApiDoc("Delete one or more contracts")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteContracts (HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                contractService.deleteContract(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.CONTRACT.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("An error occurred when casting contract id", e);
            badRequest(request);
        }
    }
}
