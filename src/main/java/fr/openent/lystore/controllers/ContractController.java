package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ContractService;
import fr.openent.lystore.service.impl.DefaultContractService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class ContractController extends ControllerHelper {

    private ContractService contractService;

    public ContractController () {
        super();
        this.contractService = new DefaultContractService(Lystore.LYSTORE_SCHEMA, "contract");
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
                    public void handle(JsonObject contract) {
                        contractService.createContract(contract, defaultResponseHandler(request));
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
                    public void handle(JsonObject contract) {
                        try {
                            contractService.updateContract(contract,
                                    Integer.parseInt(request.params().get("id")),
                                    defaultResponseHandler(request));
                        } catch (ClassCastException e) {
                            log.error("E014 : An error occurred when casting contract id");
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
            if (params.size() > 0) {
                List<Integer> ids = new ArrayList<Integer>();
                for (String param : params) {
                    ids.add(Integer.parseInt(param));
                }
                contractService.deleteContract(ids, defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        }
        catch (ClassCastException e) {
            log.error("E015 : An error occurred when casting contract id");
            badRequest(request);
        }
    }
}
