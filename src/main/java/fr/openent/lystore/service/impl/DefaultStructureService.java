package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.StructureService;
import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class DefaultStructureService implements StructureService {

    private Neo4j neo4j;

    public DefaultStructureService(){
        this.neo4j = Neo4j.getInstance();
    }
    @Override
    public void getStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) RETURN s.id as id, s.name as name,s.city as city,s.UAI as uai";
        neo4j.execute(query, new JsonObject(), Neo4jResult.validResultHandler(handler));
    }
    @Override
    public void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IN {uais} return s.id as id, s.UAI as uai";

        Neo4j.getInstance().execute(query,
                new JsonObject().putArray("uais", uais),
                Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.id IN {ids} return s.id as id, s.UAI as uai";

        Neo4j.getInstance().execute(query,
                new JsonObject().putArray("ids", ids),
                Neo4jResult.validResultHandler(handler));
    }
}
