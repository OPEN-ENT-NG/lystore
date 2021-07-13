package fr.openent.lystore.export;

import fr.openent.lystore.model.Project;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportObject {
    protected String idFile;
    protected ExportService exportService;
    protected Logger log = LoggerFactory.getLogger(ExportObject.class);

    public ExportObject(ExportService exportService, String idNewFile) {
        this.exportService = exportService;
        this.idFile = idNewFile;
    }
    protected void futureHandler(Handler<Either<String, Buffer>> handler, Workbook workbook, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                try {
                    ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
                    workbook.write(fileOut);
                    Buffer buff = new BufferImpl();
                    buff.appendBytes(fileOut.toByteArray());
                    handler.handle(new Either.Right<>(buff));
                } catch (IOException e) {
                    handler.handle(new Either.Left<>(e.getMessage()));
                }
            } else {
                handler.handle(new Either.Left<>("Error when resolving futures : " + event.cause().getMessage()));
            }
        });
    }

    protected Handler<Either<String, Boolean>> getHandler(Future<Boolean> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                future.fail(event.left().getValue());
            }
        };
    }

    protected Future<JsonArray> getStructures() {
        Promise<JsonArray> promise = Promise.promise();
        String query = "" +
                "MATCH (s:Structure) " +
                "RETURN " +
                "s.id as id," +
                " s.UAI as uai," +
                " s.name as name," +
                " s.address + ' ,' + s.zipCode +' ' + s.city as address,  " +
                "s.zipCode as zipCode," +
                " s.city as city," +
                " s.type as type," +
                " s.phone as phone";
        Neo4j.getInstance().execute(query, new JsonObject(), Neo4jResult.validResultHandler(h->{
            if(h.isRight()){
                promise.complete(h.right().getValue());
            }else{
                promise.fail(h.left().getValue());
            }
        }));
        return promise.future();
    }
    protected Map<String, JsonObject> getStructureMap(JsonArray structures) {
        Map<String, JsonObject> structuresMap = new HashMap<>();
        for (int i = 0; i < structures.size(); i++) {
            structuresMap.put(structures.getJsonObject(i).getString("id"), structures.getJsonObject(i));
        }
        return structuresMap;
    }


    protected Handler<Boolean> getFinalHandler(Handler<Either<String, Buffer>> handler, Workbook workbook) {
        return event -> {
            try {
                ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
                workbook.write(fileOut);
                Buffer buff = new BufferImpl();
                buff.appendBytes(fileOut.toByteArray());
                handler.handle(new Either.Right<>(buff));
            } catch (IOException e) {
                handler.handle(new Either.Left<>(e.getMessage()));
            }
        };
    }

}
