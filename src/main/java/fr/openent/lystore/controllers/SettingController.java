package fr.openent.lystore.controllers;

import fr.openent.lystore.service.ParameterService;
import fr.openent.lystore.service.impl.DefaultParameterService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;

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

}
