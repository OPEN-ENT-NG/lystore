package fr.openent.lystore.export;

import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.export.campaign.CampaignExport;
import fr.openent.lystore.export.instructions.Instruction;
import fr.openent.lystore.export.validOrders.ValidOrders;
import fr.openent.lystore.export.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.ServiceFactory;
import fr.openent.lystore.service.impl.DefaultExportServiceService;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import java.util.ArrayList;
import java.util.List;

import static fr.openent.lystore.Lystore.*;
import static fr.openent.lystore.constants.ParametersConstants.REGION_TYPE_NAME;

public class ExportLystoreWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private Instruction instruction;
    private ValidOrders validOrders;
    private Storage storage;
    private ExportService exportService ;
    private String idNewFile;
    private boolean isWorking = false;
    private boolean isSleeping = true;

    private final String XLSXHEADER= "application/vnd.ms-excel";
    private final String PDFHEADER = "application/pdf";
    private CampaignExport campaign;
    public static String url;
    private ServiceFactory serviceFactory;
    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportLystoreWorker.class.getSimpleName(), this);
        this.config = CONFIG;
        this.storage = STORAGE;
        this.vertx = vertx;
        this.serviceFactory = new ServiceFactory(vertx, storage, Neo4j.getInstance(), Sql.getInstance(),
                MongoDb.getInstance(), config ,eb);
        this.exportService = serviceFactory.exportService();

    }

    @Override
    public void handle(Message<JsonObject> eventMessage) {
        eventMessage.reply(new JsonObject().put("status", "ok"));

        if (isSleeping) {
            logger.info("Calling Worker");
            isSleeping = false;
            processExport();
        }
    }




    private void processExport(){
        isWorking = true;
        Handler<Either<String,Boolean>> exportHandler = event -> {
            logger.info("exportHandler");
            if (event.isRight()) {
                isWorking = false;
                logger.info("export to Waiting");
                processExport();
            } else {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx " + event.left().getValue());
            }
        };

        exportService.getWaitingExport(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                    int time  = 1080;
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    if(isWorking) {
                                        ExportHelper.catchErrorTimeout(exportService, idNewFile, "error when creating xlsx : Timed out");
                                        processExport();
                                    }
                                }
                            },
                            time * 1000
                    );
                    JsonObject waitingOrder = event.right().getValue();
                    chooseExport( waitingOrder,exportHandler);
                }else{
                    isSleeping = true;
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    processExport();
                                }
                            },
                            3600*1000
                    );
                }
            }
        });
    }

    private void chooseExport(JsonObject body, Handler<Either<String, Boolean>> exportHandler) {
        final String action = body.getString("action", "");
        String fileName = body.getString("filename");
        idNewFile = body.getString("_id");
        Integer object_id = -1;
        String string_object_id ="";
        List<Integer> ids= new ArrayList<Integer>();
        JsonObject params = body.getJsonObject("externalParams");
        if(action.equals(ExportTypes.CAMPAIGN_ORDERS)){
            for(Object o : body.getJsonArray("ids")){
                ids.add(Integer.parseInt((String)o));
            }
        }
        if(body.containsKey("url")){
            this.url = body.getString("url");
        }
        try {
            object_id = Integer.parseInt(body.getString("object_id"));
            string_object_id = object_id.toString();
        }catch (ClassCastException ce){
            object_id = body.getInteger("object_id");
            string_object_id = object_id.toString();
        }catch (NumberFormatException ce){
            string_object_id = body.getString("object_id");

        }
        logger.info("Export Type : " + action);
        switch (action) {
            case ExportTypes.EQUIPMENT_INSTRUCTION:
                exportEquipment(
                        object_id,
                        body.getString("type"),
                        fileName,exportHandler );
                break;
            case ExportTypes.RME:
                exportRME(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.NOTIFICATION_CP:
                exportNotificationCp(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.PUBLIPOSTAGE:
                exportPublipostage(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.SUBVENTION:
                exportSubvention(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.IRIS:
                exportIris(object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.LIST_LYCEE:
                exportListLycOrders(string_object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_BEFORE_VALIDATION:
                exportBCOrders(params,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_DURING_VALIDATION:
                exportBCOrdersDuringValidation(params,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_AFTER_VALIDATION:
                exportBCOrdersAfterValidation(string_object_id,
                        fileName,
                        exportHandler);
                break;
            case ExportTypes.BC_AFTER_VALIDATION_STRUCT:
                exportBCOrdersAfterValidationStruct(string_object_id,fileName,exportHandler);
                break;
            case ExportTypes.BC_BEFORE_VALIDATION_STRUCT:
                exportBCOrdersBeforeValidationStruct(params,fileName,exportHandler);
                break;
            case ExportTypes.CAMPAIGN_ORDERS:
                exportCampaignOrder(object_id,
                        fileName,
                        ids,
                        body.getString(CommonConstants.HOST),
                        body.getString(CommonConstants.LANGUAGE),
                        exportHandler);
                break;
            default:
                ExportHelper.catchError(exportService, idNewFile, "Invalid action in worker : " + action,exportHandler);
                break;
        }
    }

    private void exportCampaignOrder(Integer campaignId, String titleFile, List<Integer> ids, String host, String language, Handler<Either<String, Boolean>> exportHandler) {

        logger.info("Export orders from campaign : ");

        this.campaign = new CampaignExport(exportService, idNewFile, campaignId, ids, I18n.getInstance().translate(config.getString(REGION_TYPE_NAME),host,language));
        this.campaign.exportOrders(event1 -> {
            saveExportHandler(titleFile, exportHandler, event1, "error when creating export order Campaign xlsx :", XLSXHEADER);
        });
    }

    private void exportBCOrdersBeforeValidationStruct(JsonObject params, String titleFile, Handler<Either<String, Boolean>> exportHandler) {

        logger.info("Export BC per structures from Orders before validation started : ");

        this.validOrders = new ValidOrders(exportService, params, idNewFile, this.eb, this.vertx, this.config, this.storage, this.serviceFactory);
        this.validOrders.exportBCBeforeValidationByStructures(event1 -> {
            saveExportHandler(titleFile, exportHandler, event1, "error when creating BCOrdersBeforeValidationStruct PDF ", PDFHEADER);
        });
    }

    private void exportBCOrdersAfterValidationStruct(String object_id, String titleFile, Handler<Either<String, Boolean>> exportHandler) {

        logger.info("Export BC per structures from Orders after validation started BC : " + object_id);

        this.validOrders = new ValidOrders(exportService, object_id, idNewFile, this.eb, this.vertx, this.config, false,
                this.storage, this.serviceFactory);
        this.validOrders.exportBCAfterValidationByStructures(event1 -> {
            saveExportHandler(titleFile, exportHandler, event1, "error when creating BCOrdersAfterValidationStruct PDF ", PDFHEADER);
        });
    }

    private void exportBCOrdersAfterValidation(String object_id, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC per structures from Orders after validation started BC : " + object_id);

        this.validOrders = new ValidOrders(exportService, object_id, idNewFile, this.eb, this.vertx, this.config, false,
                this.storage, this.serviceFactory);
        this.validOrders.exportBCAfterValidation(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating BCOrdersAfterValidation PDF ", PDFHEADER);
        });
    }

    private void exportBCOrdersDuringValidation(JsonObject params, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC from Orders during validation started");
        this.validOrders = new ValidOrders(exportService, params, idNewFile, this.eb, this.vertx, this.config, this.storage, this.serviceFactory);
        this.validOrders.exportBCDuringValidation(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating BCOrdersDuringValidation PDF ", PDFHEADER);
        });
    }

    private void exportBCOrders(JsonObject params, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC from Orders started");
        this.validOrders = new ValidOrders(exportService, params, idNewFile, this.eb, this.vertx, this.config, this.storage, this.serviceFactory);
        this.validOrders.exportBC(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating BCorders PDF ", PDFHEADER);
        });
    }

    private void exportListLycOrders(String object_id, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export list lycee from Orders started");
        this.validOrders = new ValidOrders(exportService, object_id, idNewFile, this.eb, this.vertx, this.config, true,
                this.storage, this.serviceFactory);
        this.validOrders.exportListLycee(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating ListLycOrder xlsx :", XLSXHEADER);
        });
    }


    private void exportIris(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Iris started");
        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportIris(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating IRIS xlsx :", XLSXHEADER);
        });
    }

    private void exportNotificationCp(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export NotificationCP started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportNotficationCp(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating NotificationCP xlsx :", XLSXHEADER);
        });
    }

    private void exportSubvention(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Subvention started");
        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportSubvention(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating Subvention xlsx :", XLSXHEADER);
        });
    }

    private void exportPublipostage(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Publipostage started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportPublipostage(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating Publipostage xlsx :", XLSXHEADER);
        });
    }

    private void exportRME(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export RME started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportInvestissement(event -> {
            saveExportHandler(titleFile, handler, event, "error when creating RME xlsx :", XLSXHEADER);
        });
    }



    private void exportEquipment(int instructionId, String type, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Equipment started");
        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportEquipmentRapp(event1 -> {
            saveExportHandler(titleFile, handler, event1, "error when creating ExportEquipment xlsx :", XLSXHEADER);
        }, type);
    }

    private void saveBuffer(Buffer buff, String fileName,Handler<Either<String,Boolean>> handler,String fileType) {
        storage.writeBuffer(buff, fileType, fileName, file -> {
            if (!"ok".equals(file.getString("status"))) {
                ExportHelper.catchError(exportService, idNewFile, "An error occurred when inserting file ",handler);
                handler.handle(new Either.Left<>("An error occurred when inserting file"));
            } else {
                logger.info(fileName + " insert in storage");
                exportService.updateWhenSuccess(file.getString("_id"), idNewFile,handler);
            }
        });
    }

    private void saveExportHandler(String titleFile, Handler<Either<String, Boolean>> handler, Either<String, Buffer> event1, String errorMessage, String fileType) {
        if (event1.isLeft()) {
            ExportHelper.catchError(exportService, idNewFile, errorMessage +"\n" + event1.left().getValue(), handler);
        } else {
            logger.info(titleFile + " created ");
            Buffer buffer = event1.right().getValue();
            saveBuffer(buffer, titleFile, handler, fileType);
        }
    }

}
