package fr.openent.lystore.controllers;

import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ProgramService;
import fr.openent.lystore.factory.ServiceFactory;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ProgramController extends ContractController {

    private ProgramService programService;

    public ProgramController(ServiceFactory serviceFactory) {
        super(serviceFactory);
        this.programService = serviceFactory.programService();
    }

    @Get("/programs")
    @ApiDoc("List all programs in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void listPrograms (HttpServerRequest request) {
        programService.listPrograms(arrayResponseHandler(request));
    }
}
