package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.EquipmentService;
import fr.openent.lystore.service.impl.DefaultEquipmentService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class EquipmentController extends ControllerHelper {

    private final EquipmentService equipmentService;

    public EquipmentController () {
        super();
        this.equipmentService = new DefaultEquipmentService(Lystore.lystoreSchema, "equipment");
    }

    @Get("/equipments")
    @ApiDoc("List all equipments in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @Override
    public void list(HttpServerRequest request) {
        equipmentService.listEquipments(arrayResponseHandler(request));
    }

    @Get("/equipment/:id")
    @ApiDoc("Get an equipment")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void equipment(final HttpServerRequest request) {
        try {
            Integer idEquipment = request.params().contains("id")
                    ? Integer.parseInt(request.params().get("id"))
                    : null;

            equipmentService.equipment(idEquipment, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }
    @Get("/equipments/campaign/:idCampaign")
    @ApiDoc("List equipments of campaign in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listEquipmentFromCampaign(final HttpServerRequest request) {
        try {
            Integer idCampaign = request.params().contains("idCampaign")
                    ? Integer.parseInt(request.params().get("idCampaign"))
                    : null;
            String idStructure = request.params().contains("idStructure")
                    ? request.params().get("idStructure")
                    : null;
            equipmentService.listEquipments(idCampaign, idStructure, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Post("/equipment")
    @ApiDoc("Create an equipment")
    @SecuredAction(value =  "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "equipment", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject equipment) {
                equipmentService.create(equipment, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.EQUIPMENT.toString(),
                        Actions.CREATE.toString(),
                        null,
                        equipment));
            }
        });
    }

    @Put("/equipment/:id")
    @ApiDoc("Update an equipment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "equipment", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject equipment) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    equipmentService.update(id, equipment, Logging.defaultResponseHandler(eb,
                            request,
                            Contexts.EQUIPMENT.toString(),
                            Actions.UPDATE.toString(),
                            request.params().get("id"),
                            equipment));
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting equipment id", e);
                }
            }
        });
    }

    @Delete("/equipment")
    @ApiDoc("Delete an equipment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                equipmentService.delete(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.EQUIPMENT.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("An error occurred when casting equipment(s) id(s)", e);
            badRequest(request);
        }
    }
}
