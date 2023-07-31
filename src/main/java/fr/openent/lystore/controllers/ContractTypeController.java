package fr.openent.lystore.controllers;

import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ContractTypeService;
import fr.openent.lystore.factory.ServiceFactory;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ContractTypeController extends ContractController {

    private ContractTypeService contractTypeService;

    public ContractTypeController(ServiceFactory serviceFactory) {
        super(serviceFactory);
        this.contractTypeService = serviceFactory.contractTypeService();
    }

    @Get("/contract/types")
    @ApiDoc("List all market types in database")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void listMarketTypes (HttpServerRequest request) {
        contractTypeService.listContractTypes(arrayResponseHandler(request));
    }
}
