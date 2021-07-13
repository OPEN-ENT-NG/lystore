package fr.openent.lystore.export.instructions;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.instructions.RME.*;
import fr.openent.lystore.export.instructions.equipmentRapp.*;
import fr.openent.lystore.export.instructions.iris.IrisTab;
import fr.openent.lystore.export.instructions.notificationEquipCP.LinesBudget;
import fr.openent.lystore.export.instructions.notificationEquipCP.NotificationLycTab;
import fr.openent.lystore.export.instructions.notificationEquipCP.RecapMarketGestion;
import fr.openent.lystore.export.instructions.publipostage.Publipostage;
import fr.openent.lystore.export.instructions.subventionEquipment.Market;
import fr.openent.lystore.export.instructions.subventionEquipment.Subventions;
import fr.openent.lystore.export.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Instruction extends ExportObject {
    private final String operationsIdQuery = "WITH operations AS (" +
            "SELECT operation.id, label_operation.label, operation.id_instruction " +
            "FROM " + Lystore.lystoreSchema + ".operation " +
            "INNER JOIN " + Lystore.lystoreSchema + ".label_operation ON (operation.id_label = label_operation.id) " +
            "WHERE id_instruction = ? " +
            ")" +
            "SELECT instruction.*, array_to_json(array_agg(operations)) as operations " +
            "FROM " + Lystore.lystoreSchema + ".instruction " +
            "INNER JOIN operations ON (operations.id_instruction = instruction.id) " +
            "WHERE instruction.id = ? " +
            "GROUP BY instruction.id";
    private Integer id;
    private Logger log = LoggerFactory.getLogger(Instruction.class);

    public Instruction(ExportService exportService, String idFile, Integer instructionId) {
        super(exportService,idFile);
        this.id = instructionId;
    }

    public void exportInvestissement(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExportHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }


        getStructures().onSuccess(structures ->  Sql.getInstance().prepared(operationsIdQuery, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                ExportHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {

                JsonObject instruction = either.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    ExportHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));
                    String path = FileResolver.absolutePath("public/template/excel/templateInvestissement.xlsx");
                    Map<String, JsonObject> structuresMap = getStructureMap(structures);

                    try {
                        FileInputStream templateInputStream = new FileInputStream(path);
                        Workbook workbook = new XSSFWorkbook(templateInputStream);
//                        List<Future> futures = new ArrayList<>();
//                        Future<Boolean> lyceeFuture = Future.future();
//                        Future<Boolean> CMRFuture = Future.future();
//                        Future<Boolean> CMDfuture = Future.future();
//                        Future<Boolean> Fonctionnementfuture = Future.future();
//                        Future<Boolean> RecapEPLEfuture = Future.future();
//                        Future<Boolean> RecapImputationBudfuture = Future.future();
//                        futures.add(lyceeFuture);
//                        futures.add(CMRFuture);
//                        futures.add(CMDfuture);
//                        futures.add(Fonctionnementfuture);
//                        futures.add(RecapEPLEfuture);
//                        futures.add(RecapImputationBudfuture);
//                        futureHandler(handler, workbook, futures);
//
//                        new LyceeTab(workbook, instruction).create(getHandler(lyceeFuture));
//                        new CMRTab(workbook, instruction).create(getHandler(CMRFuture));
//                        new CMDTab(workbook, instruction).create(getHandler(CMDfuture));
//                        new FonctionnementTab(workbook, instruction).create(getHandler(Fonctionnementfuture));
//                        new RecapEPLETab(workbook, instruction).create(getHandler(RecapEPLEfuture));
//                        new RecapImputationBud(workbook, instruction).create(getHandler(RecapImputationBudfuture));
                        new LyceeTab(workbook, instruction,structuresMap).create()
                                .compose(v ->  new CMRTab(workbook, instruction,structuresMap).create())
                                .compose(v->  new CMDTab(workbook, instruction,structuresMap).create())
                                .compose(v ->  new FonctionnementTab(workbook, instruction,structuresMap).create())
                                .compose(v ->   new RecapEPLETab(workbook, instruction,structuresMap).create())
                                .compose(v ->    new RecapImputationBud(workbook, instruction,structuresMap).create())
                                .onSuccess(getFinalHandler(handler, workbook)
                                ).onFailure(failure ->{
                            handler.handle(new Either.Left<>("Error when resolving futures : " + failure.getMessage()));
                        });

                    } catch (IOException e) {
                        ExportHelper.catchError(exportService, idFile, "Xlsx Failed to read template");
                        handler.handle(new Either.Left<>("Xlsx Failed to read template"));
                    }
                }
            }
        }))).onFailure( f->{
            handler.handle(new Either.Left<>(f.getMessage()+ " getting neo"));
        });


    }

    public void exportEquipmentRapp(Handler<Either<String, Buffer>> handler, String type) {
        if (this.id == null) {
            ExportHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }


        getStructures().onSuccess(structures -> Sql.getInstance().prepared(operationsIdQuery, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                ExportHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {

                JsonObject instruction = either.right().getValue();
                String operationStr = "operations";
                Map<String, JsonObject> structuresMap = getStructureMap(structures);
                if (!instruction.containsKey(operationStr)) {
                    ExportHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    Boolean failed = false;
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));


                    Workbook workbook = new XSSFWorkbook();
                    new ComptaTab(workbook, instruction, type,structuresMap).create()
                            .compose(l->new  ListForTextTab(workbook, instruction, type,structuresMap).create())
                            .compose(listForText -> new RecapTab(workbook, instruction, type,structuresMap).create())
                            .compose(Recap-> new AnnexeDelibTab(workbook, instruction, type,structuresMap).create())
                            .compose(recapMarket ->  new RecapMarket(workbook, instruction, type,structuresMap).create())
                            .compose(v -> new VerifBudgetTab(workbook, instruction, type,structuresMap).create())
                            .onSuccess(getFinalHandler(handler, workbook)
                            ).onFailure(failure ->{
                        handler.handle(new Either.Left<>("Error when resolving futures : " + failure.getMessage()));
                    });
                }
            }
        }))).onFailure( f->{
                    handler.handle(new Either.Left<>(f.getMessage()+ " getting neo"));
                }
        );
    }



    public void exportSubvention(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            log.error("Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }
        getStructures().onSuccess(structures -> Sql.getInstance().prepared(operationsIdQuery, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(eitherInstruction -> {
            if (eitherInstruction.isLeft()) {
                log.error("Error when getting sql datas for subvention");
                handler.handle(new Either.Left<>("Error when getting sql datas for subvention"));
            } else {
                Map<String, JsonObject> structuresMap = getStructureMap(structures);
                JsonObject instruction = eitherInstruction.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    log.error("Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));

                    Workbook workbook = new XSSFWorkbook();

                    new Subventions(workbook, instruction, true,structuresMap).create()
                            .compose(l-> new Subventions(workbook, instruction, false,structuresMap).create())
                            .compose(listForText -> new Market(workbook, instruction, true,structuresMap).create())
                            .compose(Recap->   new Market(workbook, instruction, false,structuresMap).create())
                            .onSuccess(getFinalHandler(handler, workbook)
                            ).onFailure(failure ->{
                        handler.handle(new Either.Left<>("Error when resolving futures : " + failure.getMessage()));
                    });
                }
            }
        }))).onFailure( f->{
            handler.handle(new Either.Left<>(f.getMessage()+ " getting neo"));
        });


    }

    public void exportPublipostage(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExportHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }
        getStructures().onSuccess(structures ->  Sql.getInstance().prepared(operationsIdQuery, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(eitherInstruction -> {
            if (eitherInstruction.isLeft()) {
                ExportHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {
                JsonObject instruction = eitherInstruction.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    ExportHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));
                    Map<String, JsonObject> structuresMap = getStructureMap(structures);
                    Workbook workbook = new XSSFWorkbook();
                    List<Future> futures = new ArrayList<>();
                    Future<Boolean> PublipostageFuture = Future.future();

                    futures.add(PublipostageFuture);

                    futureHandler(handler, workbook, futures);

                    new Publipostage(workbook, instruction,structuresMap).create(getHandler(PublipostageFuture));
                }
            }
        }))).onFailure(f->{
            handler.handle(new Either.Left<>(f.getMessage()+ " getting neo"));
        });
    }

    public void exportNotficationCp(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExportHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }


        getStructures().onSuccess(structures ->
                Sql.getInstance().prepared(operationsIdQuery, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
                    if (either.isLeft()) {
                        ExportHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                        handler.handle(new Either.Left<>("Error when getting sql datas "));
                    } else {
                        Map<String, JsonObject> structuresMap = getStructureMap(structures);
                        JsonObject instruction = either.right().getValue();
                        String operationStr = "operations";
                        if (!instruction.containsKey(operationStr)) {
                            ExportHelper.catchError(exportService, idFile, "Error when getting operations");
                            handler.handle(new Either.Left<>("Error when getting operations"));
                        } else {
                            instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));

                            Workbook workbook = new XSSFWorkbook();
//                    List<Future> futures = new ArrayList<>();
//                    Future<Boolean> LinesBudgetFuture = Future.future();
//                    Future<Boolean> RecapMarketGestionFuture = Future.future();
//                    Future<Boolean> NotifcationLyceeFuture = Future.future();
//
//                    futures.add(LinesBudgetFuture);
//                    futures.add(RecapMarketGestionFuture);
//                    futures.add(NotifcationLyceeFuture);
//
//                    futureHandler(handler, workbook, futures);
                            new NotificationLycTab(workbook, instruction,structuresMap).create()
                                    .compose(RM -> new RecapMarketGestion(workbook, instruction,structuresMap).create())
                                    .compose(LB ->  new LinesBudget(workbook, instruction,structuresMap).create())
                                    .onSuccess(getFinalHandler(handler, workbook)
                                    ).onFailure(failure ->{
                                handler.handle(new Either.Left<>("Error when resolving futures : " + failure.getMessage()));
                            });
                        }
                    }
                }))).onFailure( f->{
            handler.handle(new Either.Left<>(f.getMessage()+ " getting neo"));
        });
    }

    public void exportIris(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExportHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }
        getStructures().onSuccess(structures ->Sql.getInstance().prepared(operationsIdQuery, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                ExportHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {
                Map<String, JsonObject> structuresMap = getStructureMap(structures);
                JsonObject instruction = either.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    ExportHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));

                    Workbook workbook = new XSSFWorkbook();

                    new IrisTab(workbook, instruction,structuresMap).create()
                            .onSuccess(getFinalHandler(handler, workbook))
                            .onFailure(failure ->{
                                handler.handle(new Either.Left<>("Error when resolving futures : " + failure.getMessage()));
                            });;

                }
            }
        }))).onFailure( f->{
            handler.handle(new Either.Left<>(f.getMessage()+ " getting neo"));
        });

    }



}
