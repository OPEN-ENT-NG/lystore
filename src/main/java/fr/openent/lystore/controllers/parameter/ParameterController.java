package fr.openent.lystore.controllers.parameter;

import fr.openent.lystore.security.LystoreManagerAdminAccesOrSuperAdminRight;
import fr.openent.lystore.service.ServiceFactory;
import fr.openent.lystore.service.parameter.ParameterService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

import static fr.openent.lystore.constants.ParametersConstants.BCOPTIONS;

public class ParameterController extends ControllerHelper {
    final ParameterService parameterService;

    public ParameterController(ServiceFactory serviceFactory) {
        parameterService = serviceFactory.parameterService();
    }

    @Get("/parameter/bc/options")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(LystoreManagerAdminAccesOrSuperAdminRight.class)
    @ApiDoc("get bc options")
    public void getBcOptions(final HttpServerRequest request) {
        parameterService.getBcOptions(DefaultResponseHandler.defaultResponseHandler(request));
    }

    @Put("/parameter/bc/options")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("update bs options")
    public void putBcOptions(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + BCOPTIONS,
                parameter -> parameterService.putBcOptions(parameter, DefaultResponseHandler.arrayResponseHandler(request)));
    }
}
