package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.model.Structure;
import fr.openent.lystore.model.Title;
import fr.openent.lystore.service.TitleService;
import fr.openent.lystore.utils.LystoreUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultTitleService extends SqlCrudService implements TitleService {

    private static final Logger log = LoggerFactory.getLogger(DefaultTitleService.class);

    public DefaultTitleService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void getTitles(Integer idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT rel_title_campaign_structure.id_structure, array_to_json(array_agg(title.*)) as titles " +
                "FROM " + Lystore.lystoreSchema + ".rel_title_campaign_structure " +
                "INNER JOIN " + Lystore.lystoreSchema + ".title ON (rel_title_campaign_structure.id_title = title.id) " +
                "WHERE rel_title_campaign_structure.id_campaign = ?" +
                "GROUP BY id_structure;";

        sql.prepared(query, new JsonArray().add(idCampaign), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getTitles(Integer idCampaign, String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT title.id, title.name " +
                "FROM " + Lystore.lystoreSchema + ".title " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_title_campaign_structure ON (title.id = rel_title_campaign_structure.id_title) " +
                "WHERE rel_title_campaign_structure.id_campaign = ? " +
                "AND rel_title_campaign_structure.id_structure = ?";

        JsonArray params = new JsonArray()
                .add(idCampaign)
                .add(structureId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getTitlesAdmin(Integer idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct title.id, title.name " +
                "FROM " + Lystore.lystoreSchema + ".title " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_title_campaign_structure ON (title.id = rel_title_campaign_structure.id_title) " +
                "WHERE rel_title_campaign_structure.id_campaign = ? ";

        JsonArray params = new JsonArray()
                .add(idCampaign);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getList(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct title.id, title.name " +
                "FROM " + Lystore.lystoreSchema + ".title " ;

        JsonArray params = new JsonArray();

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void importTitlesForCampaign(Integer idCampaign, JsonObject importMap, JsonObject newTitlesMap, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        Set<String> importKeys = importMap.getMap().keySet();
        Set<String> newTitlesKeys = newTitlesMap.getMap().keySet();
        JsonArray structures;
        if (!importKeys.isEmpty()) {
            for (String key : importKeys) {
                structures = importMap.getJsonArray(key);
                if (!structures.isEmpty()) {
                    JsonArray params = new JsonArray();
                    StringBuilder query = new StringBuilder("INSERT INTO " + Lystore.lystoreSchema + ".rel_title_campaign_structure (id_title, id_campaign, id_structure) " +
                            "VALUES ");
                    for (int i = 0; i < structures.size(); i++) {
                        query.append("(?, ?, ?),");
                        params.add(Integer.parseInt(key))
                                .add(idCampaign)
                                .add(structures.getString(i));
                    }

                    statements.add(new JsonObject()
                            .put("statement", query.toString().substring(0, query.length() - 1))
                            .put("values", params)
                            .put("action", "prepared"));
                }
            }
        }

        if (!newTitlesKeys.isEmpty()) {
            for (String key : newTitlesKeys) {
                structures = newTitlesMap.getJsonArray(key);
                if (!structures.isEmpty()) {
                    JsonArray params = new JsonArray();
                    StringBuilder query = new StringBuilder("WITH title_insert AS (INSERT INTO " + Lystore.lystoreSchema + ".title (name) VALUES (?) RETURNING id) " +
                            "INSERT INTO " + Lystore.lystoreSchema + ".rel_title_campaign_structure (id_title, id_campaign, id_structure) VALUES ");
                    params.add(key);
                    for (int i = 0; i < structures.size(); i++) {
                        query.append("((SELECT title_insert.id FROM title_insert), ? , ?),");
                        params.add(idCampaign)
                                .add(structures.getString(i));
                    }

                    statements.add(new JsonObject()
                            .put("statement", query.toString().substring(0, query.length() - 1))
                            .put("values", params)
                            .put("action", "prepared"));
                }
            }
        }
        if (!statements.isEmpty()) {
            Sql.getInstance().transaction(statements, message -> {
                if ("ok".equals(message.body().getString("status"))) {
                    handler.handle(new Either.Right<>(message.body()));
                } else {
                    handler.handle(new Either.Left<>(message.body().getString("message")));
                }
            });
        } else {
            handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
        }
    }

    @Override
    public void getTitles(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, name " +
                "FROM " + Lystore.lystoreSchema + ".title;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getRelationForCampaign(Integer idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT rel_title_campaign_structure.id_structure, title.name " +
                "FROM " + Lystore.lystoreSchema + ".rel_title_campaign_structure " +
                "INNER JOIN " + Lystore.lystoreSchema + ".title ON (rel_title_campaign_structure.id_title = title.id) " +
                "WHERE id_campaign = ?";
        JsonArray params = new JsonArray().add(idCampaign);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void deleteRelation(Integer idCampaign, Integer idTitle, String idStructure, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_title_campaign_structure " +
                "WHERE id_campaign = ? " +
                "AND id_title = ? " +
                "AND id_structure = ?;";

        JsonArray params = new JsonArray()
                .add(idCampaign)
                .add(idTitle)
                .add(idStructure);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }


    @Override
    public Future<Void> deleteTitlesRelations(Integer idCampaign, JsonArray structuresJA) {
        Promise<Void> promise = Promise.promise();
        JsonArray statements = new JsonArray();

        List<Structure> structures = initStructuresTitleRelation(structuresJA);
        if(structures.size() > 0) {
            structures.forEach(structure ->
                    structure.getTitles().forEach(title ->
                            statements.add(getDeletionStatement(idCampaign, title.getId(), structure.getId()))
                    )
            );
            Sql.getInstance().transaction(statements, message -> {
                if (CommonConstants.OK.equals(message.body().getString(CommonConstants.STATUS))) {
                    promise.complete();
                } else {
                    promise.fail(message.body().getString(CommonConstants.MESSAGE));
                }
            });
        }else {
            promise.fail(LystoreUtils.generateErrorMessage(this.getClass(),"delteTitlesRelations","Structures Array is empty", ""));
        }
        return promise.future();
    }

    private List<Structure> initStructuresTitleRelation(JsonArray structuresJA) {
        try{
            return structuresJA.stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .map(structureParams -> {
                        Structure structure = new Structure();
                        structure.setId(structureParams.getString(LystoreBDD.ID_STRUCTURE));
                        structureParams.getJsonArray(LystoreBDD.TITLES, new JsonArray())
                                .stream()
                                .filter(JsonObject.class::isInstance)
                                .map(JsonObject.class::cast)
                                .forEach(titleObject -> structure.addTitle(new Title(titleObject)));
                        return structure;
                    })
                    .collect(Collectors.toList());
        }catch (Exception e){
            log.error(LystoreUtils.generateErrorMessage(this.getClass(),
                    "initStructuresTitleRelation",
                    "error when setting structures",
                    e.getMessage()));
            return new ArrayList<>();
        }
    }

    private JsonObject getDeletionStatement(Integer idCampaign, int idTitle, String idStructure) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_title_campaign_structure " +
                "WHERE id_campaign = ? " +
                "AND id_title = ? " +
                "AND id_structure = ?;";

        JsonArray params = new JsonArray()
                .add(idCampaign)
                .add(idTitle)
                .add(idStructure);

        return new JsonObject()
                .put(CommonConstants.STATEMENT, query)
                .put(CommonConstants.VALUES, params)
                .put(CommonConstants.ACTION, CommonConstants.PREPARED);
    }
}
