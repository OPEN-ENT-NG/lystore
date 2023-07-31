package fr.openent.lystore.controllers;

import fr.openent.lystore.factory.ServiceFactory;
import fr.openent.lystore.service.StructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;


/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class StructureController extends ControllerHelper {

    private StructureService structureService;

    public StructureController(ServiceFactory serviceFactory){
        super();
        this.structureService = serviceFactory.structureService();
    }

    @Get("/structures")
    @ApiDoc("Returns all structures")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructures(HttpServerRequest request){
        structureService.getStructures(arrayResponseHandler(request));
    }
    @Get("/structures/type")
    @ApiDoc("Returns all structure's type")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructureTypes(HttpServerRequest request){
        structureService.getStructureTypes(arrayResponseHandler(request));
    }

}
