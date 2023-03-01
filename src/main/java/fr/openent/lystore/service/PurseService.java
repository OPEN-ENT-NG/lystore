package fr.openent.lystore.service;

import fr.openent.lystore.model.Purse;
import fr.openent.lystore.model.Structure;
import fr.openent.lystore.model.utils.Domain;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface PurseService {

    /**
     * Launch purse import
     * @param campaignId Campaign id
     * @param statementsValues Object containing structure ids as key and purse amount as value
     * @param handler Function handler
     */
    void launchImport(Integer campaignId, JsonObject statementsValues, Handler<Either<String, JsonObject>> handler);

    /**
     * Get all the purses of a campaign
     * @param campaignId
     * @return
     */
    Future<List<Purse>> getPursesByCampaignId(Integer campaignId);

    /**
     * Get purses by campaign id
     * @param campaignId campaign id
     * @param handler handler function returning data
     */
    void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler);

    /**
     * Update a purse based on his id
     * @param id Purse id
     * @param purse purse object
     * @param handler Function handler returning data
     */
     void update(Integer id, JsonObject purse, Handler<Either<String, JsonObject>> handler);

    /**
     * get statement to decrease or increase an amount of Purse
     * @param price total price of an equipment (with options)
     * @param idCampaign Campaign id
     * @param idStructure Structure id
     * @param operation "+" or "-"
     * @return Statment
     */
     JsonObject updatePurseAmountStatement(Double price,Integer idCampaign, String idStructure, String operation);

    void checkPurses(Integer id, Handler<Either<String, JsonArray>> handler);

     Future<String> getExport(Map<Structure, Purse> values, Domain domain);
}
