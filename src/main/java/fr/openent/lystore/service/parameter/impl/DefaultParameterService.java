package fr.openent.lystore.service.parameter.impl;

import fr.openent.lystore.model.parameter.*;
import fr.openent.lystore.service.parameter.ParameterService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.stream.Collectors;

import static fr.openent.lystore.constants.ParametersConstants.*;

public class DefaultParameterService extends SqlCrudService implements ParameterService {
    public DefaultParameterService(String schema, String table) {
        super(schema, table);
    }

    private Logger log = LoggerFactory.getLogger(DefaultActiveStructureService.class);

    @Override
    public void getBcOptions(Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT name, address, signature, image as img from " + this.resourceTable;
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(event -> {
            if(event.isRight()) {
                JsonObject result = event.right().getValue().getJsonObject(0);
                setBcOptionsToSend(handler, result);
            }else {
                handler.handle(new Either.Left<>(event.left().getValue()));
            }
        }));
    }

    private void setBcOptionsToSend(Handler<Either<String, JsonObject>> handler, JsonObject result) {
        BCOptionsAddress adress = new BCOptionsAddress();
        BCOptionsName name = new BCOptionsName();
        BCOptionsSignature signature = new BCOptionsSignature();
        BCOptions bcOptions = new BCOptions();
        bcOptions.setAddress(adress);
        bcOptions.setName(name);
        bcOptions.setSignature(signature);
        bcOptions.setImg(result.getString(IMG));
        setBCOptionsName(result, name);
        setBCOptionsAddress(adress, result);
        setBCOptionsSignature(signature, result);
        handler.handle(new Either.Right<>(bcOptions.toJson()));
    }

    private void setBCOptionsSignature(BCOptionsSignature signature, JsonObject result) {
        setBCOptionsFormData(signature, result, SIGNATURE);
    }

    private void setBCOptionsAddress(BCOptionsAddress adress, JsonObject result) {
        setBCOptionsFormData(adress, result, ADDRESS);
    }

    private String[] setBCOptionsFormData(BCOptionsFormData optionsFormData, JsonObject result, String key) {
        String[] split = result.getString(key).split("\n");
        try {
            optionsFormData.setLine1(split[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            optionsFormData.setLine1("");
        }
        try {
            optionsFormData.setLine2(split[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            optionsFormData.setLine2("");
        }
        return split;
    }

    private void setBCOptionsName(JsonObject result, BCOptionsName name) {
        String[] split = setBCOptionsFormData(name, result, NAME);
        try {
            name.setLine3(split[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
            name.setLine3("");
        }
        try {
            name.setLine4(split[3]);
        } catch (ArrayIndexOutOfBoundsException e) {
            name.setLine4("");
        }
    }

    @Override
    public void putBcOptions(JsonObject parameter, Handler<Either<String, JsonArray>> handler) {
        String query = "UPDATE " + this.resourceTable + " SET name = ?, address = ?, signature= ?, image= ? ;";
        JsonArray params = new JsonArray();
        params.add(getStringWithBrakeLine(parameter.getJsonObject(NAME)))
                .add(getStringWithBrakeLine(parameter.getJsonObject(ADDRESS)))
                .add(getStringWithBrakeLine(parameter.getJsonObject(SIGNATURE)))
                .add(parameter.getString(IMG));
        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }

    private String getStringWithBrakeLine(JsonObject jsonObject) {
        StringBuilder result = new StringBuilder();
        List<String> list = jsonObject.stream().map(entry -> {
            if (entry.getValue().getClass() == String.class) {
                return (String) entry.getValue();
            } else {
                return "";
            }
        }).collect(Collectors.toList());

        for (String elem : list) {
            result.append(elem).append("\n");
        }
        result.delete(result.length() - 2, result.length() - 1);
        return result.toString();
    }
}
