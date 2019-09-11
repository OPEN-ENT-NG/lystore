package fr.openent.lystore.helpers;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.impl.MongoDbCrudService;

public class MongoHelper extends MongoDbCrudService {
    private final EventBus eb;
    private static final Logger logger = LoggerFactory.getLogger(MongoHelper.class);

    private static final String  STATUS = "status";
    public MongoHelper(String collection ,EventBus eb) {
        super(collection);
        this.eb = eb;
    }


    public void addExport(JsonObject export, Handler<String> handler) {
        try {
            mongo.insert(this.collection, export, jsonObjectMessage -> handler.handle(jsonObjectMessage.body().getString("_id")));
        } catch (Exception e) {
            handler.handle("mongoinsertfailed");
        }
    }

    public void updateExport(String idExport,String status,Handler<String> handler){
        try {
            final JsonObject matches = new JsonObject().put("_id", idExport);
            mongo.findOne(this.collection, matches , result -> {
                if ("ok".equals(result.body().getString(STATUS))) {
                    JsonObject exportProperties = result.body().getJsonObject("result");
                    exportProperties.put("status",status);
                    mongo.save(collection, exportProperties, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            if(!event.body().getString("status").equals("ok")) {
                                handler.handle("mongoinsertfailed");
                            }else {
                                handler.handle("ok");
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            handler.handle("mongoinsertfailed");
        }
    }
}
