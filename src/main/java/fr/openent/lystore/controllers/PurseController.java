package fr.openent.lystore.controllers;

import com.opencsv.CSVReader;
import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.model.Purse;
import fr.openent.lystore.model.Structure;
import fr.openent.lystore.model.utils.Domain;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.CampaignService;
import fr.openent.lystore.service.PurseService;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultCampaignService;
import fr.openent.lystore.service.impl.DefaultPurseService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.openent.lystore.constants.CommonConstants.ID;
import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class PurseController extends ControllerHelper {


    private final Storage storage;
    private StructureService structureService;
    private PurseService purseService;
    private CampaignService campaignService;
    private String fileId ;
    public PurseController(Vertx vertx, Storage storage) {
        super();
        this.storage = storage;
        this.structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.purseService = new DefaultPurseService();
        this.campaignService = new DefaultCampaignService(Lystore.lystoreSchema, "campaign");
    }

    @Post("/campaign/:id/purses/import")
    @ApiDoc("Import purse for a specific campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void purse(final HttpServerRequest request) {
        storage.writeUploadFile(request, entries -> {
            if (!"ok".equals(entries.getString("status"))) {
                renderError(request);
                return;
            }
            String fileId = entries.getString("_id");
            String filename = entries.getJsonObject("metadata").getString("filename");
            parseCsv(request,fileId,filename);
        });
//        final String importId = UUID.randomUUID().toString();
//        final String path = config.getString("import-folder", "/tmp") + File.separator + importId;
//        importCSVHelper.getParsedCSV(request, path, new Handler<Either<String, Buffer>>() {
//            @Override
//            public void handle(Either<String, Buffer> event) {
//                request.resume();
//                if (event.isRight()) {
//                    Buffer content = event.right().getValue();
//                    storage.writeBuffer(content,"temp",".csv", storageEvent->{
//                        fileId =storageEvent.getString("_id");
//                        log.info("_id : " + fileId);
//                        parseCsv(request, path, content);
//                    });
//                } else {
////                    renderError(request);
//                }
////                request.resume();
////
//            }
//        });
    }

    /**
     * Parse CSV file
     *
     * @param request Http request
     * @param filename    Directory path
     */
//    private void parseCsv(final HttpServerRequest request, final String path, Buffer content) {
    private void parseCsv(final HttpServerRequest request, final String fileId, String filename) {
        storage.readFile(fileId,event -> {
            CSVReader csv = new CSVReader(new InputStreamReader(
                    new ByteArrayInputStream(event.getBytes())),
                    ';', '"', 1);
            String[] values;
            JsonArray uais = new fr.wseduc.webutils.collections.JsonArray();
            JsonObject amounts = new JsonObject();
            try {
                while ((values = csv.readNext()) != null) {
                    amounts.put(values[0], values[1]);
                    uais.add(values[0]);
                }
                if (uais.size() > 0) {
                    log.info(uais);
                    log.info(amounts);
                    matchUAIID(request, filename, uais, amounts, event.toString());
                } else {
                    returnErrorMessage(request, new Throwable("missing.uai"), filename);
                }
            } catch (IOException e) {
                log.error("[Lystore@CSVImport]: csv exception", e);
                returnErrorMessage(request, e.getCause(), filename);
            }
        });
    }

    /**
     * Match ids between structure campaign ids and provided ids.
     *
     * @param realIds     provided ids
     * @param expectedIds expected ids
     * @return JsonArray containing structure campaign ids specified in CSV file.
     */
    private static JsonArray deleteWrongIds(JsonArray realIds, JsonArray expectedIds) {
        JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray correctIds = new fr.wseduc.webutils.collections.JsonArray();
        JsonObject structure;
        for (int i = 0; i < expectedIds.size(); i++) {
            structure = expectedIds.getJsonObject(i);
            ids.add(structure.getString("id_structure"));
        }
        for (int j = 0; j < realIds.size(); j++) {
            structure = realIds.getJsonObject(j);
            if (ids.contains(structure.getString("id"))) {
                correctIds.add(structure);
            }
        }

        return correctIds;
    }

    /**
     * Match structure UAI with its Neo4j id.
     *
     * @param request Http request
     * @param path    Directory path
     * @param uais    UAIs list
     * @param amount  Object containing UAI as key and purse amount as value
     */
    private void matchUAIID(final HttpServerRequest request, final String path, JsonArray uais,
                            final JsonObject amount, final String contentFile) {
        structureService.getStructureByUAI(uais, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    final JsonArray ids = event.right().getValue();
                    campaignService.getCampaignStructures(Integer.parseInt(request.params().get("id")),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.isRight()) {
                                        JsonArray correctIds = deleteWrongIds(ids, event.right().getValue());
                                        if (correctIds.size() == 0) {
                                            returnErrorMessage(request, new Throwable("lystore.statements.empty"), path);
                                            return;
                                        }
                                        JsonObject statementsValues = new JsonObject();
                                        JsonObject id;
                                        for (int i = 0; i < correctIds.size(); i++) {
                                            id = correctIds.getJsonObject(i);
                                            statementsValues.put(id.getString("id"),
                                                    amount.getString(id.getString("uai")));
                                        }
                                        launchImport(request, path, statementsValues, contentFile);
                                    } else {
                                        returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                                    }
                                }
                            });
                } else {
                    returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                }
            }
        });
    }

    /**
     * Launch database import
     *
     * @param request          Http request
     * @param path             Directory path
     * @param statementsValues Object containing statement values
     */
    private void launchImport(final HttpServerRequest request, final String path,
                              JsonObject statementsValues, final String contentFile) {
        try {
            final Integer campaignId = Integer.parseInt(request.params().get("id"));
            purseService.launchImport(campaignId,
                    statementsValues, new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if (event.isRight()) {
                                Renders.renderJson(request, event.right().getValue());
                                JsonObject contentObject = new JsonObject().put("content", contentFile);
                                Logging.insert(eb, request, Contexts.PURSE.toString(),
                                        Actions.IMPORT.toString(), campaignId.toString(), contentObject);
                                deleteImportPath(vertx, path);
                            } else {
                                returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                            }
                        }
                    });
        } catch (NumberFormatException e) {
            log.error("[Lystore@launchImport] : An error occurred when parsing campaign id", e);
            returnErrorMessage(request, e.getCause(), path);
        }
    }

    /**
     * End http request and returns message error. It delete the directory.
     *
     * @param request Http request
     * @param cause   Throwable message
     * @param path    Directory path to delete
     */
    private void returnErrorMessage(HttpServerRequest request, Throwable cause, String path) {
        renderErrorMessage(request, cause);
        deleteImportPath(vertx, path);
    }

    /**
     * Render a message error based on cause message
     *
     * @param request Http request
     * @param cause   Cause error
     */
    private static void renderErrorMessage(HttpServerRequest request, Throwable cause) {
        renderError(request, new JsonObject().put("message", cause.getMessage()));
    }

    @Get("/campaign/:id/purses/export")
    @ApiDoc("Export purses for a specific campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void export(final HttpServerRequest request) {
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("id"));
            Domain domain = new Domain(getHost(request), I18n.acceptLanguage(request));
            List<Purse> purseArrayList = new ArrayList<>();
            purseService.getPursesByCampaignId(idCampaign)
                    .compose(purses -> {
                        purseArrayList.addAll(purses);
                        return structureService.getStructureById(
                                new JsonArray(purses.stream()
                                        .map(purse -> purse.getStructure().getId())
                                        .collect(Collectors.toList())));
                    })
                    .compose(structures -> {
                        Map<Structure, Purse> values = new HashMap<>();
                        structures.forEach(structure -> values.put(structure,
                                purseArrayList.stream().filter(purse ->
                                        purse.getStructure().getId().equals(structure.getId())).findFirst().orElse(null)));

                        return purseService.getExport(values, domain);
                    })
                    .onSuccess(fileStr -> request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=" + getFileExportName(request))
                            .end(fileStr));
        } catch (NumberFormatException e) {
            log.error("[Lystore@CSVExport] : An error occurred when casting campaign id", e);
            badRequest(request);
        }
    }

    @Put("/purse/:id")
    @ApiDoc("Update a purse based on his id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateHolder(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "purse", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject body) {
                try {
                    purseService.update(Integer.parseInt(request.params().get("id")), body, new Handler<Either<String, JsonObject>>() {
                                @Override
                                public void handle(Either<String, JsonObject> event) {
                                    if(event.isRight()){
                                        Logging.defaultResponseHandler(eb,
                                                request,
                                                Contexts.PURSE.toString(),
                                                Actions.UPDATE.toString(),
                                                request.params().get("id"),
                                                body).handle(new Either.Right<>(event.right().getValue()));
                                    }else{
                                        if(event.left().getValue().contains("Check_amount_positive")){
                                            request.response().setStatusMessage("Amount negative").setStatusCode(202).end();
                                        }else{
                                            badRequest(request);
                                        }
                                    }
                                }
                            }
                    );
//                    purseService.update(Integer.parseInt(request.params().get("id")), body, Logging.defaultResponseHandler(eb,
//                                                request,
//                                                Contexts.PURSE.toString(),
//                                                Actions.UPDATE.toString(),
//                                                request.params().get("id"),
//                                                body));
                } catch (NumberFormatException e) {
                    log.error("An error occurred when casting purse id", e);
                    badRequest(request);
                }

            }
        });
    }

    @Get("/campaign/:id/purse/check")
    @ApiDoc("Get purses checks")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void checkPurse(final HttpServerRequest request) {
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("id"));
            purseService.checkPurses(idCampaign, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if(event.isRight())
                    {
                        request.response().setStatusCode(201).end(event.right().getValue().toString());
                    }else{
                        badRequest(request);
                    }
                }
            });

        } catch (NumberFormatException e) {
            log.error("[Lystore@purses] : An error occurred when casting campaign id", e);
            badRequest(request);
        }
    }

    @Get("/campaign/:id/purses/list")
    @ApiDoc("Get purses for a specific campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void list(final HttpServerRequest request) {
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("id"));
            purseService.getPursesByCampaignId(idCampaign, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        JsonArray ids = new fr.wseduc.webutils.collections.JsonArray();
                        JsonArray purses = event.right().getValue();
                        JsonObject purse;
                        for (int i = 0; i < purses.size(); i++) {
                            purse = purses.getJsonObject(i);
                            ids.add(purse.getString("id_structure"));
                        }
                        retrieveStructuresData(ids, purses, request);
                    } else {
                        badRequest(request);
                    }
                }
            });
        } catch (NumberFormatException e) {
            log.error("[Lystore@purses] : An error occurred when casting campaign id", e);
            badRequest(request);
        }
    }

    /**
     * Retrieve structure uais and name based on ids list
     * @param ids JsonArray containing ids list
     * @param purses JsonArray containing purses list
     * @param request Http request
     */
    private void retrieveStructuresData(JsonArray ids, final JsonArray purses, final HttpServerRequest request) {
        structureService.getStructureById(ids, event -> {
            if (event.isRight()) {
                JsonArray structures = event.right().getValue();
                structures.stream().forEach(structureO ->{
                    JsonObject structure =  (JsonObject) structureO;
                    purses.stream().forEach(purseO ->{
                        JsonObject purse = (JsonObject) purseO;
                        if(purse.getString(LystoreBDD.ID_STRUCTURE).equals(structure.getString(ID))) {
                            purse.put(LystoreBDD.NAME, structure.getString(LystoreBDD.NAME));
                            purse.put(LystoreBDD.UAI, structure.getString(LystoreBDD.UAI));
                            Double amount = purse.getDouble(LystoreBDD.AMOUNT);
                            purse.put(LystoreBDD.AMOUNT,amount);
                        }
                    });
                });
                Renders.renderJson(request, purses);
            } else {
                renderError(request, new JsonObject().put(CommonConstants.MESSAGE,
                        event.left().getValue()));
            }
        });
    }

    /**
     * Get File Export Name. It use internationalization to build the name.
     * @param request Http request
     * @return File name
     */
    private static String getFileExportName(HttpServerRequest request) {
        return I18n.getInstance().translate("campaign", getHost(request), I18n.acceptLanguage(request)) +
                "-" + request.params().get("id") + "-" +
                I18n.getInstance().translate("purse", getHost(request), I18n.acceptLanguage(request)) +
                ".csv";
    }
}
