package fr.openent.lystore.service.parameter.impl;

import fr.openent.lystore.service.parameter.ActiveStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

public class DefaultActiveStructureService implements ActiveStructureService {

    private Logger log = LoggerFactory.getLogger(DefaultActiveStructureService.class);
    EventBus eb;
    private final String LystoreGroupName = "Lystore";

    public DefaultActiveStructureService(EventBus eb) {
        this.eb = eb;
    }


    @Override
    public void undeployStructureLystore(String structureId, Handler<Either<String, JsonObject>> handler) {
        String query = "MATCH (s:Structure {id:{structureId}})--(g:ManualGroup{name: {groupName} })" +
                "DETACH DELETE g ;";
        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("groupName", LystoreGroupName);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validUniqueResultHandler(handler));
    }
    @Override
    public void getStructuresLystore(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE HAS(s.UAI) OPTIONAL MATCH (s)<-[:DEPENDS]-(g:ManualGroup{name: {groupName} })" +
                "RETURN DISTINCT s.UAI as uai, s.name as name, s.id as structureId, (HAS(g.id)) as deployed, g.id as id";

        JsonObject params = new JsonObject().put("groupName", LystoreGroupName);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void createLystoreGroupToStructure(JsonObject body, Handler<Either<String, JsonObject>> handler) {
        String query = "MATCH (s:Structure {id:{structureId}}) " +
                "OPTIONAL MATCH (s)<-[:DEPENDS]-(g:ManualGroup{name: {groupName}})" +
                " RETURN g.id as groupId";

        JsonObject creationParams = new JsonObject()
                .put("structureId", body.getString("structureId"))
                .put("groupName", LystoreGroupName);
        Neo4j.getInstance().execute(query, creationParams, Neo4jResult.validUniqueResultHandler(either -> {
                    if (either.isLeft()) {
                        handler.handle(new Either.Left<>("Failed to get  structure during deploy"));
                        return;
                    }
//
                    JsonObject creationResult = either.right().getValue();
                    log.info(either.right().getValue());
                    if (!(null == creationResult.getValue("groupId"))) {
                        handler.handle(new Either.Right<>(new JsonObject()));
                        return;
                    }

                    body.put("groupDisplayName", LystoreGroupName);
//                    .put("lockDelete",true); //A CHANGER
//            body.put("lockDelete",true);
                    JsonObject action = new JsonObject()
                            .put("action", "manual-create-group")
                            .put("structureId", body.getString("structureId"))
                            .put("group", body);
                    log.info(body);

                    eb.request("entcore.feeder", action, (Handler<AsyncResult<Message<JsonObject>>>) createGarResult -> {
                        if (createGarResult.failed()) {
                            handler.handle(new Either.Left<>("Failed to create lystore group"));
                            return;
                        }
                        String groupId = createGarResult.result().body()
                                .getJsonArray("results")
                                .getJsonArray(0)
                                .getJsonObject(0).getString("id");
                        String groupLockQuery =  "MAtch (g:Group {id:{groupId}}) set g.lockDelete = true return g";

                        JsonObject paramsLock = new JsonObject()
                                .put("groupId", groupId);
                        Neo4j.getInstance().execute(groupLockQuery, paramsLock, Neo4jResult.validUniqueResultHandler(result ->
                                {
                                    if(result.isRight())
                                        log.info("group locked success");
                                    else
                                        log.error(result.left().getValue());
                                })
                        );
                        String queryRole = "MATCH (r:Role)" +
                                " WHERE r.distributions = {linkName} RETURN r.id as id";

                        JsonArray arrayRole = new JsonArray().add("Admin Lystore");
                        Neo4j.getInstance().execute(queryRole, new JsonObject().put("linkName",arrayRole),
                                Neo4jResult.validUniqueResultHandler(linkResult -> {
                                    if (linkResult.isLeft()) {
                                        handler.handle(new Either.Left<>("Failed to fetch role id"));
                                    }
                                    String roleId = linkResult.right().getValue().getString("id");
                                    String queryLink = "MATCH (r:Role), (g:Group) " +
                                            "WHERE r.id = {roleId} and g.id = {groupId}" +
                                            "CREATE UNIQUE (g)-[:AUTHORIZED]->(r)" ;
                                    JsonObject params = new JsonObject()
                                            .put("groupId", groupId)
                                            .put("roleId", roleId);
                                    Neo4j.getInstance().execute(queryLink, params, Neo4jResult.validUniqueResultHandler(handler));
                                }));
                    });
                }
        ));
    }

}
