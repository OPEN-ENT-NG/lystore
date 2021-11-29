package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.model.file.Attachment;
import fr.openent.lystore.service.OrderRegionService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

import static fr.openent.lystore.utils.OrderUtils.safeGetDouble;
import static org.entcore.common.email.impl.PostgresEmailHelper.logger;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    public DefaultOrderRegionService(String table) {
        super(table);;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderRegionService.class);


    @Override
    public void setOrderRegion(JsonObject orderRegion, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".order-region-equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                try {
                    final Number id = event.right().getValue().getInteger("id");
                    JsonArray statements = new JsonArray()
                            .add(setOrderRegionStatement(id, orderRegion, user));

                    sql.transaction(statements, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
                        }
                    });
                } catch (ClassCastException e){
                    LOGGER.error("An error occurred when casting ids", e);
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }

    private JsonObject setOrderRegionStatement(Number id, JsonObject order, UserInfos user) {
        String statement = "";
        statement = "" +
                "INSERT INTO  " + Lystore.lystoreSchema + ".\"order-region-equipment\" AS ore " +
                "(id, " +
                "price, " +
                "amount, " +
                "creation_date, " +
                "owner_name, " +
                "owner_id, " +
                "equipment_key, " +
                "name, " +
                "comment, " +
                "id_order_client_equipment, " +
                "rank, ";
        statement += order.containsKey("id_operation") ? "id_operation, " : "";
        statement += "status, " +
                "id_campaign, " +
                "id_structure, " +
                "summary, " +
                "description, " +
                "image, " +
                "technical_spec, " +
                "id_contract, " +
                "cause_status, " +
                "number_validation, " +
                "id_order, " +
                "id_project," +
                "id_type ) ";

        statement += "SELECT " +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ,";
        statement += order.getInteger("rank") != -1 ? "?, " : "NULL, ";
        statement += order.containsKey("id_operation") ? "?, " : "";
        statement += " 'IN PROGRESS', " +
                "       id_campaign, " +
                "       id_structure, " +
                "       summary, " +
                "       description, " +
                "       image, " +
                "       technical_spec, " +
                "       ? as id_contract, " +
                "       cause_status, " +
                "       number_validation, " +
                "       id_order, " +
                "       id_project," +
                "       ? " +
                "FROM  " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id = ? " +
                "RETURNING id;";

        JsonArray params = new JsonArray()
                .add(id)
                .add(order.getDouble("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getInteger("equipment_key"))
                .add(order.getString("name"))
                .add(order.getString("comment"))
                .add(order.getInteger("id_order_client_equipment"));
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        if (order.containsKey("id_operation")) {
            params.add(order.getInteger("id_operation"));
        }
        params.add(order.getInteger("id_contract"));
        params.add(order.getInteger("id_type"));
        params.add(order.getInteger("id_order_client_equipment"));

        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject updateIdOrderRegionEquipment(Number id, JsonArray files) {

        String statement = "UPDATE " + Lystore.lystoreSchema + ".order_region_file " +
                "SET id_order_region_equipment = ? " +
                "WHERE id IN  " + Sql.listPrepared(files) + ";";

        JsonArray params = new JsonArray().add(id);
        for(Object file : files){
            JsonObject fileJO = (JsonObject)file;
            params.add(fileJO.getString("id"));
        }
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    public void updateOrderRegion(JsonObject order, int idOrder, List<Attachment> files, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        statements.add(deletePreviousFilesStatement(idOrder));
        statements.add(getUpdateOrderStatement(order,idOrder,user));

        for(Attachment file : files){
            statements.add(addFileToOrder(idOrder, file));
        }
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, idOrder));
            }
        });
    }

    private JsonObject deletePreviousFilesStatement(int idOrder) {
        String statement = "DELETE FROM " + Lystore.lystoreSchema + ".order_region_file" +
                " WHERE id_order_region_equipment =  ? ;" ;

        JsonArray params = new JsonArray()
                .add(idOrder);

        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getUpdateOrderStatement(JsonObject order, int idOrder, UserInfos user) {
        String statement = "";
        JsonArray params = new JsonArray();
        statement = "" +
                "UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                " SET " +
                "price = ?, " +
                "amount = ?, " +
                "modification_date = NOW() , " +
                "owner_name = ? , " +
                "owner_id = ?, " +
                "name = ?, " +
                "equipment_key = ?," +
                "id_type = ?, " +
                "cause_status = 'IN PROGRESS', ";

        statement += order.containsKey("rank") && order.getInteger("rank") != -1 ? "rank=?," : "rank = NULL, ";
        statement += order.containsKey("id_operation") ? "id_operation = ?, " : "";
        statement += order.containsKey("id_contract") ? "id_contract = ?, " : "";
        statement += "comment = ? " +
                "WHERE id = ? " +
                "RETURNING id;";

        params.add(safeGetDouble(order,"price"));
        params.add(Integer.parseInt(order.getString("amount")));
        params.add(user.getUsername());
        params.add(user.getUserId());
        params.add(order.getString("equipment_name"));
        params.add(Integer.parseInt(order.getString("equipment_key")));
        params.add(order.getInteger("id_type"));
        //TODO RANK
        if ( order.containsKey("rank") && order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        if (order.containsKey("id_operation")) {
            params.add(Integer.parseInt(order.getString("id_operation")));
        }
        if (order.containsKey("id_contract")) {
            params.add(Integer.parseInt(order.getString("id_contract")));
        }
        params.add(order.getString("comment"))
                .add(idOrder);
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void linkOrderToOperation(Integer id_order_client_equipment, Integer id_operation, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET  " +
                "status = ? ,id_operation = ? " +
                "WHERE id = ? " +
                "RETURNING id;";

        values.add("IN PROGRESS");
        values.add(id_operation);
        values.add(id_order_client_equipment);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));

    }

    public void createOrdersRegion(JsonObject order, List<Attachment> files, UserInfos user, Number id_project, Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".order-region-equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            try {
                final Number id = event.right().getValue().getInteger("id");
                JsonArray statements = new JsonArray()
                        .add(createOrderRegionStatement(id, order, user, id_project));

                for(Attachment file : files){
                    statements.add(addFileToOrder(id, file));
                }

                sql.transaction(statements, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
                    }
                });
            } catch(ClassCastException e) {
                LOGGER.error("An error occurred when casting ids", e);
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    private JsonObject createOrderRegionStatement(Number id, JsonObject order, UserInfos user, Number id_project) {
        String statement = "" +
                " INSERT INTO lystore.\"order-region-equipment\" ";

        if (order.getInteger("rank", -1) != -1) {
            statement += " ( id, price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment,  id_project,  id_operation, rank) " +
                    "  VALUES (?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ; ";
        } else {
            statement += " ( id, price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment,  id_project,  id_operation, id_type) " +
                    "  VALUES (?, ?, ?, now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ; ";
        }

        JsonArray params = new JsonArray()
                .add(id)
                .add(order.getString("price"))
                .add(order.getString("amount"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getString("equipment_name"))
                .add(order.getString("summary"))
                .add(order.getString("description"))
                .add(order.getString("image"))
                .add(order.getJsonArray("technical_specs"))
                .add("IN PROGRESS")
                .add(order.getString("id_contract"))
                .add(order.getString("equipment_key"))
                .add(order.getString("id_campaign"))
                .add(order.getString("id_structure"))
                .add(order.getString("comment"))
                .add(id_project)
                .add(order.getString("id_operation"))
                .add(order.getString("id_type"));

        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject addFileToOrder(Number id, Attachment file) {
        String statement = "INSERT INTO " + Lystore.lystoreSchema + ".order_region_file (id, id_order_region_equipment, filename) " +
                "VALUES (?, ?, ?) ;" ;

        JsonArray params = new JsonArray()
                .add(file.id())
                .add(id)
                .add(file.metadata().filename());

        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    public void createProject( Integer id_title, Handler<Either<String, JsonObject>> handler) {
        JsonArray params;

        String queryProjectEquipment = "" +
                "INSERT INTO lystore.project " +
                "( id_title ) VALUES " +
                "( ? )  RETURNING id ;";
        params = new fr.wseduc.webutils.collections.JsonArray();

        params.add(id_title);

        Sql.getInstance().prepared(queryProjectEquipment, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "DELETE FROM " +
                Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                "WHERE id = ? " +
                "RETURNING id";
        sql.prepared(query, new JsonArray().add(idOrderRegion), SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOneOrderRegion(int idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "SELECT ore.*, " +
                "       ore.price AS price_single_ttc, " +
                "       to_json(contract.*) contract, " +
                "       to_json(ct.*) contract_type, " +
                "       to_json(campaign.*) campaign, " +
                "       to_json(prj.*) AS project, " +
                "       to_json(tt.*) AS title, " +
                "       to_json(oce.*) AS order_parent " +
                "FROM  " + Lystore.lystoreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".order_client_equipment AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN  " + Lystore.lystoreSchema + ".contract ON ore.id_contract = contract.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ct ON ct.id = contract.id_contract_type " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".project AS prj ON ore.id_project = prj.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".title AS tt ON tt.id = prj.id_title " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.id = ? " +
                "GROUP BY ( prj.id, " +
                "          ore.id, " +
                "          contract.id, " +
                "          ct.id, " +
                "          campaign.id, " +
                "          tt.id, " +
                "          oce.id )";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateOperation(Integer idOperation, JsonArray idsOrders, Handler<Either<String, JsonObject>> handler) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                " SET id_operation = " +
                idOperation +
                " WHERE id IN " +
                Sql.listPrepared(idsOrders.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < idsOrders.size(); i++) {
            values.add(idsOrders.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getFileOrderRegion(String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema + ".order_region_file WHERE id = ?";
        JsonArray params = new JsonArray()
                .add(fileId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultsHandler(event -> {
            if (event.isRight() && event.right().getValue().size() > 0) {
                handler.handle(new Either.Right<>(event.right().getValue().getJsonObject(0)));
            } else {
                handler.handle(new Either.Left<>("Not found"));
            }
        }));
    }
    @Override
    public void getFilesId(Integer idOrder , Handler<Either<String,JsonArray>> handler){
        String query = "SELECT * from " + Lystore.lystoreSchema + ".order_region_file where id_order_region_equipment = ? ;";
        JsonArray params = new JsonArray().add(idOrder);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight() && event.right().getValue().size() > 0) {
                handler.handle(new Either.Right<>(event.right().getValue()));
            } else {
                handler.handle(new Either.Left<>("Not found"));
            }
        }));
    }
}
