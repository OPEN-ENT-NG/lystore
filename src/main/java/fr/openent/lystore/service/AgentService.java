package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface AgentService {

    /**
     * List all agents in database
     * @param handler Function handler returning data
     */
    public void getAgents (Handler<Either<String, JsonArray>> handler);

    /**
     * Create an agent based on agent object
     * @param agent object containing data
     * @param handler Function handler returning data
     */
    public void createAgent (JsonObject agent, Handler<Either<String, JsonObject>> handler);

    /**
     * Update an agent based on agent object
     * @param id Agent id to update
     * @param agent agent object
     * @param handler Function handler returning data
     */
    public void updateAgent (Integer id, JsonObject agent, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete an Agent based on ids
     * @param ids agent ids to delete
     * @param handler Function handler returning data
     */
    public void deleteAgent (List<Integer> ids, Handler<Either<String, JsonObject>> handler);
}
