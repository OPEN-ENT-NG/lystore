package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.TagService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class DefaultTagService extends SqlCrudService implements TagService {

    private Sql sql;

    public DefaultTagService(String schema, String table) {
        super(schema, table);
        this.sql = Sql.getInstance();
    }

    public void getAll(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, name, color, count(rel_equipment_tag.id_equipment) as nb_equipments " +
                "FROM " + Lystore.LYSTORE_SCHEMA + ".tag " +
                "INNER JOIN " + Lystore.LYSTORE_SCHEMA + ".rel_equipment_tag on (tag.id = rel_equipment_tag.id_tag) " +
                "GROUP BY id " +
                "UNION " +
                "SELECT id, name, color, count(rel_equipment_tag.id_equipment) as nb_equipments " +
                "FROM " + Lystore.LYSTORE_SCHEMA + ".tag " +
                "LEFT JOIN " + Lystore.LYSTORE_SCHEMA + ".rel_equipment_tag on (tag.id = rel_equipment_tag.id_tag) " +
                "GROUP BY id;";

        this.sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void create(JsonObject tag, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.LYSTORE_SCHEMA + ".tag(name, color) " +
                "VALUES (?, ?) RETURNING id;";

        JsonArray params = new JsonArray()
                .addString(tag.getString("name"))
                .add(tag.getString("color"));

        this.sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void update(Integer id, JsonObject tag, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.LYSTORE_SCHEMA + ".tag " +
                "SET name = ?, color = ? " +
                "WHERE id = ?;";

        JsonArray params = new JsonArray()
                .addString(tag.getString("name"))
                .addString(tag.getString("color"))
                .addNumber(id);

        this.sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    public void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        SqlUtils.deleteIds("tag", ids, handler);
    }
}
