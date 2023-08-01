package fr.openent.lystore.controllers;

import fr.openent.lystore.factory.ServiceFactory;
import fr.openent.lystore.service.TaxService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class TaxController extends ControllerHelper {

    private final TaxService taxService;

    public TaxController(ServiceFactory serviceFactory) {
        super();
        this.taxService = serviceFactory.taxService();
    }

    @Get("/taxes")
    @ApiDoc("List all taxes in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listTaxes (HttpServerRequest request) {
        taxService.list(arrayResponseHandler(request));
    }
}
