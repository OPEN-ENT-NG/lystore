package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.service.CampaignService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.Map;

public class DefaultCampaignService extends SqlCrudService implements CampaignService {
    private static final Logger log = LoggerFactory.getLogger(DefaultCampaignService.class);


    public DefaultCampaignService(String schema, String table) {
        super(schema, table);
    }

    public void listCampaigns(Handler<Either<String, JsonArray>> handler) {
        Promise<JsonArray> campaignPromise = Promise.promise();
        Promise<JsonArray> equipmentPromise = Promise.promise();
        Promise<JsonArray> pursePromise = Promise.promise();
        Promise<JsonArray> basketPromise = Promise.promise();
        Promise<JsonArray> orderPromise = Promise.promise();



        Future.all(campaignPromise.future(), equipmentPromise.future(), pursePromise.future(), orderPromise.future(), basketPromise.future()).onComplete(event -> {
            if (event.succeeded()) {
                JsonArray campaigns = campaignPromise.future().result();
                JsonArray equipments = equipmentPromise.future().result();
                JsonArray baskets = basketPromise.future().result();
                JsonArray purses = pursePromise.future().result();
                JsonArray orders =  orderPromise.future().result();

                JsonObject campaignMap = new JsonObject();
                JsonObject object, campaign;
                for (int i = 0; i < campaigns.size(); i++) {
                    object = campaigns.getJsonObject(i);
                    object.put("nb_orders_waiting", 0).put("nb_orders_valid", 0).put("nb_orders_sent", 0)
                            .put("nb_orders", 0).put("nb_baskets",0);
                    campaignMap.put(object.getInteger("id").toString(), object);
                }

                for (int i = 0; i < purses.size(); i++) {
                    object = purses.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("purse_amount", object.getString("purse"));
                    }catch (NullPointerException e){
                        log.info("A purse is present on this structure but the structure is not linked to the campaign");
                    }
                }

                for (int i = 0; i < orders.size(); i++) {
                    object = orders.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("nb_orders",object.getLong("count") + campaign.getLong("nb_orders"));
                        String status =  object.getString("status").toLowerCase();
                        if(!status.equals("done") && !status.equals("rejected") && !status.equals("valid") && !status.equals("sent")) {
                            campaign.put("nb_orders_waiting", campaign.getLong("nb_orders_waiting") + object.getLong("count"));
                        }
                        else
                            campaign.put("nb_orders_" + status , object.getLong("count"));

                    }catch (NullPointerException e){
                        log.warn("An order is present on this structure but the structure is not linked to the campaign");
                    }
                }

                for (int i = 0; i < equipments.size(); i++) {
                    object = equipments.getJsonObject(i);
                    campaign = campaignMap.getJsonObject(object.getInteger("id").toString());
                    try {
                        campaign.put("nb_equipments", object.getLong("nb_equipments"));
                    }catch (NullPointerException e){
                        log.info("The campaign reffered doesn't exist anymore id_campaign " + object.getInteger("id"));
                    }
                }
                for (int i = 0; i < baskets.size(); i++) {
                    object = baskets.getJsonObject(i);
                    campaign = campaignMap.getJsonObject(object.getInteger("id").toString());
                    try {
                        campaign.put("nb_baskets", object.getLong("nb_baskets"));
                    }catch (NullPointerException e){
                        log.info("The campaign reffered doesn't exist anymore id_campaign " + object.getInteger("id"));
                    }
                }

                JsonArray campaignList = new JsonArray();
                for (Map.Entry<String, Object> aCampaign : campaignMap) {
                    campaignList.add(aCampaign.getValue());
                }

                handler.handle(new Either.Right<>(campaignList));

            } else {
                handler.handle(new Either.Left<>("An error occurred when retrieving campaigns"));
            }
        });

        getCampaignsInfo(handlerPromise(campaignPromise));
        getCampaignEquipmentCount(handlerPromise(equipmentPromise));
        getBasketCount(handlerPromise(basketPromise));
        getCampaignsPurses(handlerPromise(pursePromise));
        getCampaignOrderStatusCount(handlerPromise(orderPromise));
    }

    private void getBasketCount(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT campaign.id, COUNT(DISTINCT basket_equipment.id) as nb_baskets " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".basket_equipment ON (campaign.id = basket_equipment.id_campaign) " +
                "GROUP BY campaign.id";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }


    private Handler<Either<String, JsonArray>> handlerPromise(Promise<JsonArray> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                log.error(event.left().getValue());
                promise.fail(event.left().getValue());
            }
        };
    }

    private void getCampaignEquipmentCount(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT campaign.id, COUNT(DISTINCT rel_equipment_tag.id_equipment) as nb_equipments " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tag ON (rel_group_campaign.id_tag = tag.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (tag.id = rel_equipment_tag.id_tag) " +
                "WHERE rel_equipment_tag.id_equipment  not in (SELECT id from  "
                                    + Lystore.lystoreSchema + ".equipment where equipment.status != 'UNAVAILABLE' OR equipment.status = 'OUT_OF_STOCK'  )" +
                "GROUP BY campaign.id";
         Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsPurses(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT SUM(amount) as purse, purse.id_campaign " +
                "FROM " + Lystore.lystoreSchema + ".purse " +
                "GROUP BY id_campaign;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsPurses(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT amount, initial_amount, id_campaign as id_campaign " +
                "FROM " + Lystore.lystoreSchema + ".purse " +
                "WHERE id_structure = ?";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    private void getCampaignOrderStatusCount(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT count(*), id_campaign, status " +
                "FROM " + Lystore.lystoreSchema + ".allorders " +
                "WHERE override_region is not true " +
                "GROUP BY id_campaign, status;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignOrderStatusCount(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT campaign.id as id_campaign, COUNT(order_client_equipment.id) as nb_order " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".campaign ON (order_client_equipment.id_campaign = campaign.id) " +
                "WHERE id_structure = ? " +
                "GROUP BY campaign.id;";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsInfo(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT campaign.*, COUNT(DISTINCT rel_group_structure.id_structure) as nb_structures ,  orders_limit_date.min_date , orders_limit_date.max_date " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (rel_group_structure.id_structure_group = rel_group_campaign.id_structure_group) " +
                "INNER JOIN (SELECT campaign.id , " +
                "   min(ord.creation_date) as min_date , " +
                "   max(ord.creation_date) as max_date " +
                "   FROM " + Lystore.lystoreSchema + ".campaign " +
                "   LEFT JOIN " + Lystore.lystoreSchema + ".allOrders ord ON (ord.id_campaign = campaign.id) " +
                "   group by campaign.id " +
                "     ) as orders_limit_date on orders_limit_date.id = campaign.id " +
               " GROUP BY campaign.id,campaign.name,campaign.description,campaign.image,campaign.purse_enabled,campaign.automatic_close " +
                ",campaign.start_date,campaign.end_date,campaign.priority_enabled,campaign.priority_field , min_date ,max_date " +
                " ORDER BY campaign.start_date DESC ;";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsInfo(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT campaign.*, count(DISTINCT rel_group_structure.id_structure) as nb_structures, count(DISTINCT rel_equipment_tag.id_equipment) as nb_equiments " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (rel_group_campaign.id_tag = rel_equipment_tag.id_tag) " +
                "WHERE rel_group_structure.id_structure = ? " +
                "GROUP BY campaign.id,campaign.name,campaign.description,campaign.image,campaign.purse_enabled,campaign.automatic_close " +
                ",campaign.start_date,campaign.end_date,campaign.priority_enabled,campaign.priority_field " +
                " ORDER BY campaign.start_date DESC ;";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    public void listCampaigns(String idStructure,  Handler<Either<String, JsonArray>> handler) {
        Promise<JsonArray> campaignPromise = Promise.promise();
        Promise<JsonArray> pursePromise = Promise.promise();
        Promise<JsonArray> basketPromise = Promise.promise();
        Promise<JsonArray> orderPromise = Promise.promise();

        Future.all(campaignPromise.future(), pursePromise.future(), basketPromise.future(), orderPromise.future()).onComplete(event -> {
            if (event.succeeded()) {
                JsonArray campaigns = campaignPromise.future().result();
                JsonArray baskets = basketPromise.future().result();
                JsonArray purses = pursePromise.future().result();
                JsonArray orders = orderPromise.future().result();

                JsonObject campaignMap = new JsonObject();
                JsonObject object, campaign;
                for (int i = 0; i < campaigns.size(); i++) {
                    campaign = campaigns.getJsonObject(i);
                    campaignMap.put(campaign.getInteger("id").toString(), campaign);
                }

                for (int i = 0; i < baskets.size(); i++) {
                    object = baskets.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("nb_panier", object.getLong("nb_panier"));
                    }catch (NullPointerException e){
                        log.info("A basket is present on this structure but the structure is not linked to the campaign");
                    }
                }

                for (int i = 0; i < purses.size(); i++) {
                    object = purses.getJsonObject(i);
                    campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                    try {
                        campaign.put("purse_amount", object.getString("amount"));
                        campaign.put("initial_purse_amount",object.getString("initial_amount"));
                    }catch (NullPointerException e){
                        log.info("A purse is present on this structure but the structure is not linked to the campaign");
                    }
                }
                for (int i = 0; i < orders.size(); i++) {
                    object = orders.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("nb_order", object.getLong("nb_order"));
                    }catch (NullPointerException e){
                        log.info("An order is present on this structure but the structure is not linked to the campaign");
                    }
                }

                JsonArray campaignList = new JsonArray();
                for (Map.Entry<String, Object> aCampaign : campaignMap) {
                    campaignList.add(aCampaign.getValue());
                }

                handler.handle(new Either.Right<>(campaignList));
            } else {
                handler.handle(new Either.Left<>("[DefaultCampaignService@listCampaigns] An error occured. CompositeFuture returned failed :" + event.cause()));
            }
        });

        getCampaignsInfo(idStructure, handlerPromise(campaignPromise));
        getCampaignsPurses(idStructure, handlerPromise(pursePromise));
        getCampaignOrderStatusCount(idStructure, handlerPromise(orderPromise));
        getBasketCampaigns(idStructure, handlerPromise(basketPromise));
    }

    private void getBasketCampaigns(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT COUNT(basket_equipment.id) as nb_panier, campaign.id as id_campaign " +
                "FROM " + Lystore.lystoreSchema + ".basket_equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".campaign ON (campaign.id = basket_equipment.id_campaign) " +
                "WHERE id_structure = ? " +
                "GROUP BY campaign.id;";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    public void getCampaign(Integer id, Handler<Either<String, JsonObject>> handler){
        String query = " SELECT  " +
                "  campaign.*,  " +
                "  array_to_json( " +
                "    array_agg(groupe) " +
                "  ) as groups,  " +
                "  orders_info.max_date,  " +
                "  orders_info.min_date  " +
                "FROM  " +
                "    " + Lystore.lystoreSchema + ".campaign campaign  " +
                "  LEFT JOIN ( " +
                "    SELECT  " +
                "      rel_group_campaign.id_campaign,  " +
                "      structure_group.*,  " +
                "      array_to_json( " +
                "        array_agg(id_tag) " +
                "      ) as tags  " +
                "    FROM  " +
                "        " + Lystore.lystoreSchema + ".structure_group  " +
                "      INNER JOIN   " + Lystore.lystoreSchema + ".rel_group_campaign ON structure_group.id = rel_group_campaign.id_structure_group  " +
                "    WHERE  " +
                "      rel_group_campaign.id_campaign = ?  " +
                "    GROUP BY  " +
                "      ( " +
                "        rel_group_campaign.id_campaign,  " +
                "        structure_group.id " +
                "      ) " +
                "  ) as groupe ON groupe.id_campaign = campaign.id  " +
                "  LEFT JOIN ( " +
                "    SELECT  " +
                "      Max(orders.creation_date) as max_date,  " +
                "      MIN(orders.creation_date) as min_date,  " +
                "      id_campaign  " +
                "    FROM  " +
                "        " + Lystore.lystoreSchema + ".campaign cc  " +
                "      inner join   " + Lystore.lystoreSchema + ".allorders orders on cc.id = orders.id_campaign  " +
                "    WHERE  " +
                "      id_campaign = ?  " +
                "    Group by  " +
                "      id_campaign " +
                "  ) as orders_info on orders_info.id_campaign = campaign.id  " +
                "where  " +
                "  campaign.id = ?  " +
                "group By  " +
                "  ( " +
                "    campaign.id, orders_info.min_date,  " +
                "    orders_info.max_date " +
                "  );  " ;

        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(id).add(id).add(id), SqlResult.validUniqueResultHandler(handler));
    }
    public void create(final JsonObject campaign, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".campaign_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getInteger("id");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getCampaignCreationStatement(id, campaign));


                        JsonArray groups = campaign.getJsonArray("groups");
                        statements.add(getCampaignTagsGroupsRelationshipStatement(id, (JsonArray) groups));
                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                handler.handle(getTransactionHandler(event, id));
                            }
                        });
                    } catch (ClassCastException e) {
                        log.error("An error occurred when casting tags ids", e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                } else {
                    log.error("An error occurred when selecting next val");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }

    public void update(final Integer id, JsonObject campaign,final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getCampaignUpdateStatement(id, campaign))
                .add(getCampaignTagGroupRelationshipDeletion(id));
        JsonArray groups = campaign.getJsonArray("groups");
        statements.add(getCampaignTagsGroupsRelationshipStatement(id, (JsonArray) groups));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event, id));
            }
        });
    }

    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getCampaignsGroupRelationshipDeletion(ids))
                .add(getBasketCampaignsDeletion(ids))
                .add(getCampaignsDeletion(ids));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event,ids.get(0)));
            }
        });
    }

    private JsonObject getBasketCampaignsDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".basket_equipment ")
                .append(" WHERE id_campaign in  ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put("statement", query.toString())
                .put("values", value)
                .put("action", "prepared");
    }

    @Override
    public void getCampaignStructures(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct id_structure FROM lystore.campaign " +
                "INNER JOIN lystore.rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN lystore.rel_group_structure " +
                "ON (rel_group_structure.id_structure_group = rel_group_campaign.id_structure_group) " +
                "WHERE campaign.id = ?;";

        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(campaignId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void updatePreference(Integer campaignId,Integer projectId, String structureId,
                                 JsonArray projectOrders, Handler<Either<String, JsonObject>> handler) {
        String query= "UPDATE " + Lystore.lystoreSchema + ".project SET "+
                "preference = ? " +
                "WHERE id = ?; " +
                "UPDATE " + Lystore.lystoreSchema + ".project SET "+
                "preference = ? " +
                "WHERE id = ?; ";
        JsonArray values = new JsonArray();
        for(Object object : projectOrders){
            values.add(((JsonObject) object).getInteger("preference"));
            values.add(((JsonObject) object).getInteger("id"));
        }
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        statements.add(new JsonObject()
                .put("statement",query)
                .put("values",values)
                .put("action","prepared"));
        sql.transaction(statements, jsonObjectMessage -> handler.handle(getTransactionHandler(jsonObjectMessage,projectId)));
    }

    @Override
    public void getStructures(Integer idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id_structure as id " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group) " +
                "WHERE id = ? " +
                "GROUP BY id_structure";
        JsonArray params = new JsonArray()
                .add(idCampaign);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    public void updateAccessibility(final Integer id, final JsonObject campaign,
                                    final Handler<Either<String, JsonObject>> handler) {
        String options = "";
        String finalCondition = "";
        options += "start_date = ? ,";
        if (campaign.containsKey(LystoreBDD.END_DATE) && campaign.getValue(LystoreBDD.END_DATE) != null) {
            options += "end_date = ? ,";
        } else {
            options += "end_date = NULL ,";
        }
        options += "automatic_close = false ";

        String query = "UPDATE "+ this.resourceTable  +" SET " + options + " WHERE id = ? " + finalCondition + ";";
        JsonArray params = new JsonArray()
                .add(campaign.getString(LystoreBDD.START_DATE));
        if (campaign.containsKey(LystoreBDD.END_DATE) && campaign.getValue(LystoreBDD.END_DATE) != null)
            params.add(campaign.getString(LystoreBDD.END_DATE));
        params.add(id);
        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    private JsonObject getCampaignTagsGroupsRelationshipStatement(Number id, JsonArray groups) {
        StringBuilder insertTagCampaignRelationshipQuery = new StringBuilder("INSERT INTO " +
                Lystore.lystoreSchema + ".rel_group_campaign" +
                "(id_campaign, id_structure_group, id_tag) VALUES ");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for(int j = 0; j < groups.size(); j++ ){
            JsonObject group =  groups.getJsonObject(j);
            JsonArray tags = group.getJsonArray("tags");
            for (int i = 0; i < tags.size(); i++) {
                insertTagCampaignRelationshipQuery.append(" (?, ?, ?)");
                if(i!=tags.size()-1 || j!= groups.size()-1){
                    insertTagCampaignRelationshipQuery.append(",");
                }
                params.add(id)
                        .add(group.getInteger("id"))
                        .add(tags.getInteger(i));
            }
        }
        return new JsonObject()
                .put("statement", insertTagCampaignRelationshipQuery.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getCampaignTagGroupRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_group_campaign " +
                " WHERE id_campaign = ?;";

        return new JsonObject()
                .put("statement", query)
                .put("values", new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put("action", "prepared");
    }

    /**
     * Returns the update statement.
     *
     * @param id        resource Id
     * @param campaign campaign to update
     * @return Update statement
     */
    private JsonObject getCampaignUpdateStatement(Number id, JsonObject campaign) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".campaign " +
                "SET  name=?, " +
                "description=?," +
                " image=?, " +
                "purse_enabled=?, " +
                "priority_enabled=?," +
                " priority_field=?," +
                "start_date = ?," +
                " end_date = ?, " +
                "automatic_close = ?" +
                "WHERE id = ?";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaign.getString("name"))
                .add(campaign.getString("description"))
                .add(campaign.getString("image"))
                .add(campaign.getBoolean("purse_enabled"))
                .add(campaign.getBoolean("priority_enabled"))
                .add(campaign.getString("priority_field"))
                .add(campaign.getString("start_date"))
                .add(campaign.getString("end_date"))
                .add(campaign.getBoolean("automatic_close"))
                .add(id);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getCampaignCreationStatement(Number id, JsonObject campaign) {
        String insertCampaignQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".campaign(id, name, description, image, " +
                        " purse_enabled, priority_enabled, priority_field, start_date, end_date,automatic_close )" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?,?) RETURNING id; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(campaign.getString("name"))
                .add(campaign.getString("description"))
                .add(campaign.getString("image"))
                .add(campaign.getBoolean("purse_enabled"))
                .add(campaign.getBoolean("priority_enabled"))
                .add(campaign.getString("priority_field"))
                .add(campaign.getString("start_date"))
                .add(campaign.getString("end_date"))
                .add(campaign.getBoolean("automatic_close"));

        return new JsonObject()
                .put("statement", insertCampaignQuery)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getCampaignsGroupRelationshipDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".rel_group_campaign ")
                .append(" WHERE id_campaign in  ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put("statement", query.toString())
                .put("values", value)
                .put("action", "prepared");
    }

    private JsonObject getCampaignsDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".campaign ")
                .append(" WHERE id in  ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put("statement", query.toString())
                .put("values", value)
                .put("action", "prepared");
    }
    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @param id    resource Id
     * @return Transaction handler
     */
    private static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event, Number id) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .put("id", id);
            either = new Either.Right<>(returns);
        } else {
            log.error("An error occurred when launching campaign transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

}
