package fr.openent.lystore.controllers;

import fr.openent.lystore.service.ParameterService;
import fr.openent.lystore.service.impl.DefaultParameterService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

public class SettingController  extends ControllerHelper {

    ParameterService parameterService;
    public SettingController(EventBus eb) {
        super();
        this.parameterService = new DefaultParameterService(eb);
    }

    @Get("/parameter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Render parameter view")
    public void setting(HttpServerRequest request) {
        renderView(request, null, "parameter.html", null);
    }

    @Get("/structures/lystore")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("get gar structure")
    public void getStructureGar(final HttpServerRequest request) {
        parameterService.getStructuresLystore(DefaultResponseHandler.arrayResponseHandler(request));
    }
    @Post("/structure/lystore/group")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Create group to gar structure")
    public void createGarGroupToStructure(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, parameter -> {
            parameterService.createLystoreGroupToStructure(parameter, DefaultResponseHandler.defaultResponseHandler(request));
        });

    }

    @Delete("/structures/:id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Undeploy given structure")
    public void undeployStructure(HttpServerRequest request) {
        String structureId = request.getParam("id");
        parameterService.undeployStructureLystore(structureId, DefaultResponseHandler.defaultResponseHandler(request));
    }

}
