package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface BasketService {
    /**
     * Create a basket item
     * @param basket basket item to create
     * @param handler function handler returning data
     */
     void create(JsonObject basket, Handler<Either<String, JsonObject>> handler);

    /**
     * List  basket list of a campaign and a structure
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param handler function handler returning data
     */
     void listBasket(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler);

    /**
     * Delete a basket item
     * @param idBasket id of the basket item
     * @param handler function handler returning data
     */
     void delete(Integer idBasket, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a basket's amount
     * @param idBasket id of a basket item
     * @param amount the new amount
     * @param handler function handler returning data
     */
     void updateAmount(Integer idBasket, Integer amount, Handler<Either<String, JsonObject>> handler );

    /**
     * transform basket to an order
     * @param request the request
     * @param baskets list of basket's items to transform
     * @param idCampaign the id of the campaign
     * @param idStructure the id of the structure
     * @param nameStructure the name of the structure
     * @param handler function handler returning data
     */
    void takeOrder(HttpServerRequest request, JsonArray baskets, Integer idCampaign,
                   String idStructure, String nameStructure , Handler<Either<String, JsonObject>> handler );
    /**
     * List  basket list of a campaign and a structure to transform to an order
     * @param idCampaign id of the campaign
     * @param idStructure id of the structure
     * @param handler function handler returning data
     */
    void listebasketItemForOrder( Integer idCampaign, String idStructure,  Handler<Either<String, JsonArray>> handler );
}