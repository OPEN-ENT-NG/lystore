package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.export.validOrders.BC.BCExportDuringValidation;
import fr.openent.lystore.model.Structure;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.utils.LystoreUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.VertxException;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.lystore.constants.CommonConstants.ID;

/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class DefaultStructureService extends SqlCrudService implements StructureService {

    private Neo4j neo4j;
    public DefaultStructureService(String schema){
        super(schema, "");
        this.neo4j = Neo4j.getInstance();
    }

    @Override
    public void getStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IS NOT NULL "+
                "RETURN left(s.zipCode, 2) as department, s.id as id, s.name as name,s.city as city,s.UAI as uai, s.academy as academy, s.type as type_etab";
        neo4j.execute(query, new JsonObject(), Neo4jResult.validResultHandler(handler));
    }

    public void getStructureTypes(Handler<Either<String,JsonArray>> handler) {
        String query = "SELECT * FROM "+ Lystore.lystoreSchema+".specific_structures";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
    @Override
    public void getStructureByUAI(JsonArray uais, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.UAI IN {uais} return s.id as id, s.UAI as uai";

        Neo4j.getInstance().execute(query,
                new JsonObject().put("uais", uais),
                Neo4jResult.validResultHandler(handler));
    }

    @Override
    @Deprecated
    public void getStructureById(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        getStructureById(ids)
                .onSuccess(result -> {
                    JsonArray results = new JsonArray(
                            result.stream().map(Structure::toJsonObject).collect(Collectors.toList()));
                    handler.handle(new Either.Right<>(results));
                })
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    @Override
    public Future<List<Structure>> getStructureById(JsonArray ids) {
        Promise<List<Structure>> promise = Promise.promise();
        String query = "MATCH (s:Structure) WHERE s.id IN {ids} return s.id as id, s.UAI as uai," +
                " s.name as name, s.phone as phone, s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                "s.zipCode  as zipCode, s.city as city, s.type as type ";
        Neo4j.getInstance().execute(query,
                new JsonObject().put("ids", ids),
                Neo4jResult.validResultHandler(event -> {
                    if (event.isRight()) {
                        promise.complete(event.right().getValue().stream().map(structureObject -> {
                            JsonObject structureJo = (JsonObject) structureObject;
                            Structure structure = new Structure();
                            structure.setId(structureJo.getString(ID));
                            structure.setAcademy(structureJo.getString(LystoreBDD.ACADEMY));
                            structure.setUAI(structureJo.getString(LystoreBDD.UAI));
                            structure.setType(structureJo.getString(LystoreBDD.TYPE));
                            structure.setName(structureJo.getString(LystoreBDD.NAME));
                            structure.setZipCode(structureJo.getString(LystoreBDD.ZIPCODE));
                            structure.setCity(structureJo.getString(LystoreBDD.CITY));
                            structure.setType(structureJo.getString(LystoreBDD.TYPE));
                            structure.setAddress(structureJo.getString(LystoreBDD.ADDRESS));
                            return structure;
                        }).collect(Collectors.toList()));
                    } else {
                        promise.fail(LystoreUtils.generateErrorMessage(DefaultStructureService.class, "getStructureById",
                                "Error when getting structures",
                                event.left().getValue()));
                    }
                }));
        return promise.future();
    }
}
