package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.AgentService;
import fr.openent.lystore.service.ServiceFactory;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class AgentController extends ControllerHelper {

    private AgentService agentService;

    public AgentController(ServiceFactory serviceFactory) {
        super();
        this.agentService = serviceFactory.agentService();
    }

    @Get("/agents")
    @ApiDoc("Returns all agents in database")
    @SecuredAction(Lystore.MANAGER_RIGHT)
    public void getAgents (HttpServerRequest request) {
        agentService.getAgents(arrayResponseHandler(request));
    }

    @Post("/agent")
    @ApiDoc("Create an agent")
    @SecuredAction(Lystore.ADMINISTRATOR_RIGHT)
    public void createAgent (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "agent", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject body) {
                agentService.createAgent(body, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.AGENT.toString(),
                        Actions.CREATE.toString(),
                        null,
                        body));
            }
        });
    }

    @Put("/agent/:id")
    @ApiDoc("Update an agent based on provided id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateAgent (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "agent", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject body) {
                try {
                    agentService.updateAgent(Integer.parseInt(request.params().get("id")), body,
                            Logging.defaultResponseHandler(eb,
                                    request,
                                    Contexts.AGENT.toString(),
                                    Actions.UPDATE.toString(),
                                    request.params().get("id"),
                                    body));
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting agent id", e);
                    badRequest(request);
                }
            }
        });
    }


    @Delete("/agent")
    @ApiDoc("Delete and agent based on provided id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteAgent (HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (!params.isEmpty()) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                agentService.deleteAgent(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.AGENT.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("An error occurred when casting agent id", e);
            badRequest(request);
        }
    }
}
