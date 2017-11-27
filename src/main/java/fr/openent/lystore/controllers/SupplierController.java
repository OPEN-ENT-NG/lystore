package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.DefaultSupplierService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class SupplierController extends ControllerHelper {

    private SupplierService supplierService;

    public SupplierController() {
        super();
        this.supplierService = new DefaultSupplierService(Lystore.LYSTORE_SCHEMA, "supplier");
    }

    @Get("/suppliers")
    @ApiDoc("Returns all holders in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getHolders (HttpServerRequest request) {
        supplierService.getSuppliers(arrayResponseHandler(request));
    }

    //TODO Gérer la sécurité
    @Post("/supplier")
    @ApiDoc("Create a holder")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void createHolder (final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            public void handle(final UserInfos user) {
                RequestUtils.bodyToJson(request, pathPrefix + "supplier", new Handler<JsonObject>() {
                    public void handle(JsonObject body) {
                        supplierService.createSupplier(body, defaultResponseHandler(request));
                    }
                });
            }
        });
    }

    //TODO Gérer la sécurité
    @Put("/supplier/:id")
    @ApiDoc("Update a holder based on provided id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void updateHolder (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "holder", new Handler<JsonObject>() {
            public void handle(JsonObject body) {
                try {
                    supplierService.updateSupplier(Integer.parseInt(request.params().get("id")), body,
                            defaultResponseHandler(request));
                } catch (ClassCastException e) {
                    badRequest(request);
                }
            }
        });
    }

    //TODO Gérer la sécurité
    @Delete("/supplier")
    @ApiDoc("Delete a holder based on provided id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void deleteHolder (HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (params.size() > 0) {
                List<Integer> ids = new ArrayList<Integer>();
                for (int i = 0; i < params.size(); i++) {
                    ids.add(Integer.parseInt(params.get(i)));
                }
                supplierService.deleteSupplier(ids, defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        }
        catch (ClassCastException e) {
            badRequest(request);
        }
    }
}
