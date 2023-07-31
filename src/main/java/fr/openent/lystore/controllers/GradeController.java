package fr.openent.lystore.controllers;

import fr.openent.lystore.service.GradeService;
import fr.openent.lystore.factory.ServiceFactory;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class GradeController extends ControllerHelper {
    private final GradeService gradeService;

    public GradeController(ServiceFactory serviceFactory) {
        super();
        gradeService = serviceFactory.gradeService();
    }

    @Get("/grades")
    @ApiDoc("get all grades")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getGrades(HttpServerRequest request) {
        gradeService.getGrades(arrayResponseHandler(request));
    }


}
