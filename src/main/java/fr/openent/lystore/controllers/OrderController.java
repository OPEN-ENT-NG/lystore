package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.impl.DefaultOrderService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;



public class OrderController extends ControllerHelper {
    private final OrderService orderService;

    public OrderController (){
        super();
        this.orderService = new DefaultOrderService(Lystore.lystoreSchema, "order_client_equipment");
    }

    @Get("/orders/:idCampaign/:idStructure")
    @ApiDoc("Get the list of orders by idCampaign and idstructure")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listOrdersByCampaignByStructure(final HttpServerRequest request){
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
            String idStructure = request.params().get("idStructure");
            orderService.listOrder(idCampaign,idStructure, arrayResponseHandler(request));
    }catch (ClassCastException e ){
            log.error("An error occured when casting campaign id ",e);
        }
    }
}