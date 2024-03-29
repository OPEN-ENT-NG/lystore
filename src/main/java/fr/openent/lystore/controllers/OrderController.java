package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.ExportConstants;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.constants.ParametersConstants;
import fr.openent.lystore.export.ExportTypes;
import fr.openent.lystore.export.helpers.ExportHelper;
import fr.openent.lystore.helpers.LystoreEmailFactoryHelper;
import fr.openent.lystore.helpers.OrderHelper;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.model.parameter.BCOptions;
import fr.openent.lystore.security.AccessOrderCommentRight;
import fr.openent.lystore.security.AccessOrderRight;
import fr.openent.lystore.security.AccessUpdateOrderOnClosedCampaigne;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.*;
import fr.openent.lystore.service.impl.*;
import fr.openent.lystore.service.parameter.ParameterService;
import fr.openent.lystore.service.parameter.impl.DefaultParameterService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.bus.WorkspaceHelper;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.text.*;
import java.util.*;

import static fr.openent.lystore.constants.ParametersConstants.REGION_TYPE_NAME;
import static fr.openent.lystore.helpers.OrderHelper.*;
import static fr.openent.lystore.utils.LystoreUtils.generateErrorMessage;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;


public class OrderController extends ControllerHelper {

    private static final String NULL_DATA = "Pas d'informations";
    private Storage storage;
    private OrderService orderService;
    private StructureService structureService;
    private SupplierService supplierService;
    private ExportPDFService exportPDFService;
    private ContractService contractService;
    private AgentService agentService;
    private ProgramService programService;
    private ExportService exportService;
    LystoreEmailFactoryHelper notificationHelpDeskEmailFactory;
    LystoreEmailFactoryHelper notificationEmailFactory;
    public static final String UTF8_BOM = "\uFEFF";
    ParameterService parameterService;
    WorkspaceHelper workspaceHelper;
    private static DecimalFormat decimals = new DecimalFormat("0.00");

    public OrderController (Storage storage, Vertx vertx, JsonObject config, EventBus eb) {
        this.storage = storage;
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.orderService = new DefaultOrderService(Lystore.lystoreSchema, "order_client_equipment", emailSender);
        this.exportPDFService = new DefaultExportPDFService(vertx, config);
        this.structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
        this.contractService = new DefaultContractService(Lystore.lystoreSchema, "contract");
        this.agentService = new DefaultAgentService(Lystore.lystoreSchema, "agent");
        this.programService = new DefaultProgramService(Lystore.lystoreSchema, "program");
        exportService = new DefaultExportServiceService(storage);
        workspaceHelper  = new WorkspaceHelper(eb,storage);
        this.parameterService = new DefaultParameterService(Lystore.lystoreSchema, LystoreBDD.PARAMETER_BC_OPTIONS);

        String emailNotificationHelpDeskSender = config.getJsonObject("mail",new JsonObject()).getString("notificationHelpDeskMail","cesame.lystore@monlycee.net");
        this.notificationHelpDeskEmailFactory = new LystoreEmailFactoryHelper(vertx, config,emailNotificationHelpDeskSender);
        String emailNotificationSender = config.getJsonObject("mail",new JsonObject()).getString("notificationMail","ne-pas-repondre@ent.iledefrance.fr");
        this.notificationEmailFactory = new LystoreEmailFactoryHelper(vertx, config,emailNotificationSender);

    }

    @Get("/orders/:idCampaign/:idStructure")
    @ApiDoc("Get the list of orders by idCampaign and idstructure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void listOrdersByCampaignByStructure(final HttpServerRequest request){
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
            String idStructure = request.params().get("idStructure");
            orderService.listOrder(idCampaign,idStructure, arrayResponseHandler(request));
        }catch (ClassCastException e ){
            log.error("An error occured when casting campaign id ",e);
        }
    }
    @Put("/order/rank/move")
    @ApiDoc("Update the rank of tow orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void updatePriority(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, campaign -> {
            if (!campaign.containsKey("orders")) {
                badRequest(request);
                return;
            }
            JsonArray orders = campaign.getJsonArray("orders");
            try{
                orderService.updateRank(orders, defaultResponseHandler(request));
            }catch(Exception e){
                log.error(" An error occurred when casting campaign id", e);
            }
        });
    }

    @Get("/orders")
    @ApiDoc("Get the list of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void listOrders (final HttpServerRequest request){
        if (request.params().contains("status")) {
            List<String> queries = request.params().getAll("q");
            final String status = request.params().get("status");
            switch (status.toLowerCase()){
                case "valid":
                    getValidOrders(request, status);
                    break;
                case "sent":
                    orderService.listOrderSent(status, queries, arrayResponseHandler(request));
                    break;
                case "waiting":
                    List<String> idCampaigns = request.params().getAll("idCampaign");
                    orderService.listOrderWaiting(idCampaigns, queries, arrayResponseHandler(request));
                    break;
                default:
                    orderService.listOrder(status, queries, arrayResponseHandler(request));
                    break;

            }
        } else {
            badRequest(request);
        }
    }

    private void getValidOrders(HttpServerRequest request, String status) {
        final JsonArray statusList = new fr.wseduc.webutils.collections.JsonArray().add(status).add("SENT").add("DONE");
        orderService.getOrdersGroupByValidationNumber(statusList, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    final JsonArray orders = event.right().getValue();
                    orderService.getOrdersDetailsIndexedByValidationNumber(statusList, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray equipments = event.right().getValue();
                                JsonObject mapNumberEquipments = initNumbersMap(orders);
                                mapNumberEquipments = mapNumbersEquipments(equipments, mapNumberEquipments);
                                JsonObject order;
                                for (int i = 0; i < orders.size(); i++) {
                                    order = orders.getJsonObject(i);
                                    order.put("price",
                                            decimals.format(
                                                    roundWith2Decimals(getTotalOrder(mapNumberEquipments.getJsonArray(order.getString("number_validation")))))
                                                    .replace(".", ","));
                                }
                                List<String> queries = request.params().getAll("q");
                                renderJson(request, orderService.filterValidOrders(orders,queries));
                            } else {
                                badRequest(request);
                            }
                        }
                    });
                } else {
                    badRequest(request);
                }

            }
        });
    }

    @Get("/order")
    @ApiDoc("Get the pdf of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOrderPDF (final HttpServerRequest request) {
        final String orderNumber = request.params().get("bc_number");
        ExportHelper.makeExport(request,eb,exportService,Lystore.ORDERSSENT,  Lystore.PDF,ExportTypes.BC_AFTER_VALIDATION, "_BC_" + orderNumber);
    }

    @Get("/order/struct")
    @ApiDoc("Get the pdf of orders by structure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOrderPDFStruct (final HttpServerRequest request) {
        final String orderNumber = request.params().get("bc_number");
        try {
            if(!request.getParam("bc_number").isEmpty()) {
                ExportHelper.makeExport(request, eb, exportService, Lystore.ORDERSSENT, Lystore.PDF, ExportTypes.BC_AFTER_VALIDATION_STRUCT, "_STRUCTURES_BC_" + orderNumber);
            }

        }catch (NullPointerException e){
            ExportHelper.makeExport(request,eb,exportService,Lystore.ORDERSSENT,  Lystore.PDF,ExportTypes.BC_BEFORE_VALIDATION_STRUCT, "_STRUCTURES_BC" );
        }
//
    }
    /**
     * Init map with numbers validation
     * @param orders order list containing numbers
     * @return Map containing numbers validation as key and an empty array as value
     */
    private JsonObject initNumbersMap (JsonArray orders) {
        JsonObject map = new JsonObject();
        JsonObject item;
        for (int i = 0; i < orders.size(); i++) {
            item = orders.getJsonObject(i);
            try {
                map.put(item.getString("number_validation"), new fr.wseduc.webutils.collections.JsonArray());
            }catch (NullPointerException e){
                log.error("Number validation is null");
            }
        }
        return map;
    }

    /**
     * Map equipments with numbers validation
     * @param equipments Equipments list
     * @param numbers Numbers maps
     * @return Map containing number validations as key and an array containing equipments as value
     */
    private JsonObject mapNumbersEquipments (JsonArray equipments, JsonObject numbers) {
        JsonObject equipment;
        JsonArray equipmentList;
        for (int i = 0; i < equipments.size(); i++) {
            equipment = equipments.getJsonObject(i);
            equipmentList = numbers.getJsonArray(equipment.getString("number_validation"));
            numbers.put(equipment.getString("number_validation"), equipmentList.add(equipment));
        }
        return numbers;
    }

    @Delete("/order/:idOrder/:idStructure/:idCampaign")
    @ApiDoc("Delete a order item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessUpdateOrderOnClosedCampaigne.class)
    public void deleteOrder(final HttpServerRequest request){
        try {
            final Integer idOrder = Integer.parseInt(request.params().get("idOrder"));
            final String idStructure = request.params().get("idStructure");
            orderService.deletableOrder(idOrder, new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> deletableEvent) {
                    if (deletableEvent.isRight() && deletableEvent.right().getValue().getInteger("count") == 0) {
                        orderService.orderForDelete(idOrder, new Handler<Either<String, JsonObject>>() {
                            @Override
                            public void handle(Either<String, JsonObject> order) {
                                if (order.isRight()) {
                                    orderService.deleteOrder(idOrder, order.right().getValue(), idStructure,
                                            Logging.defaultResponseHandler(eb, request, Contexts.ORDER.toString(),
                                                    Actions.DELETE.toString(), "idOrder", order.right().getValue()));
                                }
                            }
                        });
                    } else {
                        badRequest(request);
                    }
                }
            });


        } catch (ClassCastException e){
            log.error("An error occurred when casting order id", e);
            badRequest(request);
        }
    }

    @Get("/orders/export/:idCampaign/:idStructure")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void export (final HttpServerRequest request){
        Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
        String idStructure = request.params().get("idStructure");
        orderService.listExport(idCampaign, idStructure, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isRight()){
                    request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                            .end(generateExport(request, event.right().getValue()));

                }else{
                    log.error("An error occurred when collecting orders");
                    renderError(request);
                }
            }
        });

    }

    private static String generateExport (HttpServerRequest request, JsonArray orders)  {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < orders.size(); i++) {
            report.append(generateExportLine(request, orders.getJsonObject(i)));
        }
        return report.toString();
    }

    private static String getExportHeader(HttpServerRequest request){
        if(request.params().contains("idCampaign")) {
            return I18n.getInstance().translate("creation.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("name.equipment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("price.equipment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("status", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("name.project", getHost(request), I18n.acceptLanguage(request))
                    + "\n";
        }else if (request.params().contains("id")) {
            return I18n.getInstance().translate("Structure", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("contract", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("supplier", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("creation.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("campaign", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("price.equipment", getHost(request), I18n.acceptLanguage(request))
                    + "\n";

        }else{ return ""; }
    }


    private static String generateExportLine(HttpServerRequest request, JsonObject order)  {
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat formatterDateCsv = new SimpleDateFormat("dd/MM/yyyy");
        Date orderDate = null;

        if(request.params().contains("idCampaign")) {
            try {
                orderDate = formatterDate.parse( order.getString("equipment_creation_date"));

            } catch (ParseException e) {
                log.error( "Error current format date" + e);
            }
            return formatterDateCsv.format(orderDate) + ";" +
                    order.getString("equipment_name") + ";" +
                    order.getInteger("equipment_quantity") + ";" +
                    order.getString("price_total_equipment") + " " + I18n.getInstance().
                    translate("money.symbol", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate(order.getString("equipment_status"), getHost(request),
                            I18n.acceptLanguage(request)) + ";" +
                    order.getString("project_name") + ";" +

                    "\n";
        }else if (request.params().contains("id")){
            try {
                orderDate = formatterDate.parse( order.getString("date"));

            } catch (ParseException e) {
                log.error( "Error current format date" + e);
            }
            return order.getString("uaiNameStructure") + ";" +
                    order.getString("namecontract") + ";" +
                    order.getString("namesupplier") + ";" +
                    order.getInteger("qty") + ";" +
                    formatterDateCsv.format(orderDate) + ";" +
                    order.getString("namecampaign") + ";" +
                    order.getString("pricetotal") + " " + I18n.getInstance().
                    translate("money.symbol", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    "\n";
        }else {return " ";}
    }


    @Put("/orders/valid")
    @ApiDoc("validate orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void validateOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                    @Override
                    public void handle(UserInfos userInfos) {
                        try {
                            List<String> params = new ArrayList<>();
                            for (Object id: orders.getJsonArray("ids") ) {
                                params.add( id.toString());
                            }

                            List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                            final String url = request.headers().get("Referer");
                            orderService.validateOrders(request, userInfos , ids, url,
                                    Logging.defaultResponsesHandler(eb,
                                            request,
                                            Contexts.ORDER.toString(),
                                            Actions.UPDATE.toString(),
                                            params,
                                            null));
                        } catch (ClassCastException e) {
                            log.error("An error occurred when casting order id", e);
                        }
                    }
                });
            }
        });

    }


    private void logSendingOrder(JsonArray ids, final HttpServerRequest request) {
        orderService.getOrderByValidatioNumber(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray orders = event.right().getValue();
                    JsonObject order;
                    for (int i = 0; i < orders.size(); i++) {
                        order = orders.getJsonObject(i);
                        Logging.insert(eb, request, Contexts.ORDER.toString(), Actions.UPDATE.toString(),
                                order.getInteger("id").toString(), order);
                    }
                }
            }
        });
    }



    private void sentOrders(HttpServerRequest request,
                            final JsonArray numberValidations, final String engagementNumber, final Number programId, final String dateCreation,
                            final String orderNumber) {
        programService.getProgramById(programId, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> programEvent) {
                if (programEvent.isRight()) {
                    log.info(numberValidations);
                    JsonObject program = programEvent.right().getValue();
                    orderService.updateStatusToSent(numberValidations.getList(), "SENT", engagementNumber, program.getString("name"),
                            dateCreation, orderNumber,  new Handler<Either<String, JsonObject>>() {
                                @Override
                                public void handle(Either<String, JsonObject> event) {
                                    if (event.isRight()) {
                                        //TODO DECOMMENTER
                                        log.info("HANDLE");
//                                        logSendingOrder(numberValidations,request);
                                        ExportHelper.makeExport(request,eb,exportService,Lystore.ORDERSSENT,  Lystore.PDF,ExportTypes.BC_DURING_VALIDATION, "_BC");
                                    } else {
                                        badRequest(request);
                                    }
                                }
                            });
                } else {
                    badRequest(request);
                }
            }
        });
    }

    @Put("/orders/sent")
    @ApiDoc("send orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void sendOrders (final HttpServerRequest request){
//        ExportHelper.makeExport(request,eb,exportService,Lystore.ORDERS,  Lystore.PDF,"exportBCOrders", "_BC");
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                final JsonArray validationNumbers = orders.getJsonArray("ids");
                final String nbrBc = orders.getString("bc_number");
                final String nbrEngagement = orders.getString("engagement_number");
                final String dateGeneration = orders.getString("dateGeneration");
                Number supplierId = orders.getInteger("supplierId");
                final Number programId = orders.getInteger("id_program");
                getOrdersData(request, nbrBc, nbrEngagement, dateGeneration, supplierId, validationNumbers,
                        new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject data) {
                                data.put("print_order", true);
                                sentOrders(request,validationNumbers,nbrEngagement,programId,dateGeneration,nbrBc);
                            }
                        });
            }
        });

    }

    @Put("/orders/inprogress")
    @ApiDoc("send orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void setOrdersInProgress(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                final JsonArray ids = orders.getJsonArray("ids");
                orderService.setInProgress(ids, defaultResponseHandler(request));
            }
        });
    }

    @Get("/orders/valid/export/:file")
    @ApiDoc("Export valid orders based on validation number and type file")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void csvExport(final HttpServerRequest request) {
        if (request.params().contains("number_validation")) {
            List<String> validationNumbers = request.params().getAll("number_validation");
            switch (request.params().get("file")) {
                case "structure_list": {
                    exportStructuresList(request);
                    break;
                }
                case "certificates": {
                    exportDocuments(request, false, true, validationNumbers);
                    break;
                }
                case "order": {
                    exportDocuments(request, true, false, validationNumbers);
                    break;
                }
                default: {
                    badRequest(request);
                }
            }
        } else {
            badRequest(request);
        }
    }

    private void exportDocuments(final HttpServerRequest request, final Boolean printOrder,
                                 final Boolean printCertificates, final List<String> validationNumbers) {
        if (printOrder) {
            ExportHelper.makeExport(request, eb, exportService, Lystore.ORDERS, Lystore.PDF, ExportTypes.BC_BEFORE_VALIDATION, ExportConstants.BC_SUFFIX);
        } else {
            generateCSF(request, printCertificates, validationNumbers);
        }
    }

    private void generateCSF(HttpServerRequest request, Boolean printCertificates, List<String> validationNumbers) {
        parameterService.getBcOptions().onSuccess(bcOptions ->
                supplierService.getSupplierByValidationNumbers(new JsonArray(validationNumbers), event -> {
                    if (event.isRight()) {
                        JsonObject supplier = event.right().getValue();

                        Promise<String> get64BaseImgFromWorkspace = Promise.promise();

                        String[] imgIdString = bcOptions.getImg().split("/");
                        String imgId = imgIdString[imgIdString.length - 1];
                        workspaceHelper.readDocument(imgId, document -> {
                            if (document == null) {
                                log.error("Cannot load image in getBase64File for id : " + imgId);
                                get64BaseImgFromWorkspace.complete("");
                            } else {
                                String base64 = Base64.getEncoder().encodeToString(document.getData().getBytes());
                                get64BaseImgFromWorkspace.complete(base64);
                            }
                        });
                        get64BaseImgFromWorkspace.future().onSuccess(img ->
                                getOrdersData(request, "", "", "", supplier.getInteger(CommonConstants.ID),
                                new JsonArray(validationNumbers),
                                data -> {
                                    callExportCSF(request, printCertificates, validationNumbers, bcOptions, img, data);
                                }));
                    } else {
                        generateErrorMessage(this.getClass() , "exportDocuments",
                                "An error occurred when collecting supplier Id" , event.left().getValue());
                        badRequest(request);
                    }
                }));
    }

    private void callExportCSF(HttpServerRequest request, Boolean printCertificates, List<String> validationNumbers, BCOptions bcOptions, String img, JsonObject data) {
        data.put(ExportConstants.PRINT_CERTIFICATES, printCertificates)
                .put(ParametersConstants.BC_OPTIONS, bcOptions.toJson())
                .put(ExportConstants.LOGO_DATA, img);
        exportPDFService.generatePDF(request, data,
                ExportConstants.CSF_FILENAME,  ExportConstants.CSF_PREFIX,
                pdf -> request.response()
                        .putHeader(ExportConstants.CONTENT_TYPE, ExportConstants.PDF_CONTENT_TYPE)
                        .putHeader(ExportConstants.CONTENT_DISPOSITION, ExportConstants.FILE_CONTENT_DISPOSITION
                                + generateExportName(validationNumbers, "" +
                                (printCertificates ? ExportConstants.CSF : "")) + ExportConstants.PDF_EXTENSION)
                        .end(pdf)
        );
    }

    private String generateExportName(List<String> validationNumbers, String prefix) {
        String exportName = prefix;
        for (int i = 0; i < validationNumbers.size(); i++) {
            exportName = exportName + "_" + validationNumbers.get(i);
        }

        return exportName;
    }

    private void exportStructuresList(final HttpServerRequest request) {
        ExportHelper.makeExport(request, eb, exportService,Lystore.ORDERS,  Lystore.XLSX,ExportTypes.LIST_LYCEE, "_list_bdc");
    }

    @Delete("/orders/valid")
    @ApiDoc("Delete valid orders. Cancel validation. All orders are back to validation state")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void cancelValidOrder(HttpServerRequest request) {
        if (request.params().contains("number_validation")) {
            List<String> numbers = request.params().getAll("number_validation");
            orderService.cancelValidation(new fr.wseduc.webutils.collections.JsonArray(numbers), defaultResponseHandler(request));
        } else {
            badRequest(request);
        }
    }

    @Put("/order/:idOrder/comment")
    @ApiDoc("Update an order's comment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderCommentRight.class)
    public void updateComment(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject order) {
                if (!order.containsKey("comment")) {
                    badRequest(request);
                    return;
                }
                try {
                    Integer id = Integer.parseInt(request.params().get("idOrder"));
                    String comment = order.getString("comment");
                    orderService.updateComment(id, comment, defaultResponseHandler(request));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
    }
//
//
//    private void renderValidOrdersCSVExport(HttpServerRequest request, JsonArray equipments) {
//        StringBuilder export = new StringBuilder(UTF8_BOM).append(getValidOrdersCSVExportHeader(request));
//        for (int i = 0; i < equipments.size(); i++) {
//            export.append(getValidOrdersCSVExportline(equipments.getJsonObject(i)));
//        }
//
//        request.response()
//                .putHeader("Content-Type", "text/csv; charset=utf-8")
//                .putHeader("Content-Disposition", "attachment; filename=orders.csv")
//                .end(export.toString());
//    }

    private String getValidOrdersCSVExportline(JsonObject equipment) {
        return equipment.getString("uai")
                + ";"
                + equipment.getString("structure_name")
                + ";"
                + equipment.getString("city")
                + ";"
                + equipment.getString("phone")
                + ";"
                + equipment.getString("name")
                + ";"
                + equipment.getString("amount")
                + "\n";
    }


    private void retrieveContract(final HttpServerRequest request, JsonArray validationNumbers,
                                  final Handler<JsonObject> handler) {
        contractService.getContract(validationNumbers, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight() && event.right().getValue().size() == 1) {
                    handler.handle(event.right().getValue().getJsonObject(0));
                } else {
                    log.error("An error occured when collecting contract data");
                    badRequest(request);
                }
            }
        });
    }

    private void retrieveStructures(final HttpServerRequest request, JsonArray ids,
                                    final Handler<JsonObject> handler) {
        orderService.getStructuresId(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray structures = event.right().getValue();
                    JsonArray structuresList = new fr.wseduc.webutils.collections.JsonArray();
                    final JsonObject structureMapping = new JsonObject();
                    JsonObject structure;
                    JsonObject structureInfo;
                    JsonArray orderIds;
                    for (int i = 0; i < structures.size(); i++) {
                        structure = structures.getJsonObject(i);
                        if (!structuresList.contains(structure.getString("id_structure"))) {
                            structuresList.add(structure.getString("id_structure"));
                            structureInfo = new JsonObject();
                            structureInfo.put("orderIds", new fr.wseduc.webutils.collections.JsonArray());
                        } else {
                            structureInfo = structureMapping.getJsonObject(structure.getString("id_structure"));
                        }
                        orderIds = structureInfo.getJsonArray("orderIds");
                        orderIds.add(structure.getInteger("id"));
                        structureMapping.put(structure.getString("id_structure"), structureInfo);
                    }
                    structureService.getStructureById(structuresList, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray structures = event.right().getValue();
                                JsonObject structure;
                                for (int i = 0; i < structures.size(); i++) {
                                    structure = structures.getJsonObject(i);
                                    JsonObject structureObject = structureMapping.getJsonObject(structure.getString("id"));
                                    structureObject.put("structureInfo", structure);
                                }
                                handler.handle(structureMapping);
                            } else {
                                log.error("An error occurred when collecting structures based on ids");
                                badRequest(request);
                            }
                        }
                    });
                } else {
                    log.error("An error occurred when getting structures id based on order ids.");
                    renderError(request);
                }
            }
        });
    }

    private void retrieveOrderData(final HttpServerRequest request, JsonArray validationNumbers,
                                   final Handler<JsonObject> handler) {
        orderService.getOrderByValidatioNumber(validationNumbers,  new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonObject order = new JsonObject();
                    JsonArray orders = OrderHelper.formatOrders(event.right().getValue());
                    order.put("orders", orders);
                    Double sumWithoutTaxes = getSumWithoutTaxes(orders);
                    Double totalTTC = OrderHelper.getSumTTC(orders);
                    order.put("sumLocale",
                            OrderController.getReadableNumber(roundWith2Decimals(sumWithoutTaxes)));
                    order.put("totalTaxesLocale",
                            OrderController.getReadableNumber(roundWith2Decimals(totalTTC - sumWithoutTaxes)));
                    order.put("totalPriceTaxeIncludedLocal",
                            OrderController.getReadableNumber(roundWith2Decimals(totalTTC )));
                    handler.handle(order);
                } else {
                    log.error("An error occurred when retrieving order data");
                    badRequest(request);
                }
            }
        });
    }

    public static String getReadableNumber(Double number) {
        DecimalFormat instance = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.FRENCH);
        DecimalFormatSymbols symbols = instance.getDecimalFormatSymbols();
        symbols.setCurrencySymbol("");
        instance.setDecimalFormatSymbols(symbols);
        return instance.format(number);
    }

    // A DDEPLACER DANS LE SERVICE
    private Double getTotalOrder(JsonArray orders) {
        Double sum = 0D;
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            sum += (Double.parseDouble(order.getString("price")) * Integer.parseInt(order.getString("amount"))
                    * (Double.parseDouble(order.getString("tax_amount")) / 100 + 1));
        }

        return sum;
    }



    @Get("/orders/preview")
    @ApiDoc("Get orders preview data")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOrdersPreviewData(final HttpServerRequest request) {
        MultiMap params = request.params();
        if (!params.contains("ids") && !params.contains("bc_number")
                && !params.contains("engagement_number") && !params.contains("dateGeneration")
                && !params.contains("supplierId")) {
            badRequest(request);
        } else {
            final List<String> validationNumbers = params.getAll("ids");
            final List<Integer> integerIds = new ArrayList<>();
            final String nbrBc = params.get("bc_number");
            final String nbrEngagement = params.get("engagement_number");
            final String dateGeneration = params.get("dateGeneration");
            Number supplierId = Integer.parseInt(params.get("supplierId"));
            getOrdersData(request, nbrBc, nbrEngagement, dateGeneration, supplierId,
                    new fr.wseduc.webutils.collections.JsonArray(validationNumbers), new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject data) {
                            renderJson(request, data);
                        }
                    });
        }
    }

    private void getOrdersData(final HttpServerRequest request, final String nbrBc,
                               final String nbrEngagement, final String dateGeneration,
                               final Number supplierId, final JsonArray validationNumbers,
                               final Handler<JsonObject> handler) {
        final JsonObject data = new JsonObject();
        retrieveManagementInfo(request, validationNumbers, supplierId, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject managmentInfo) {
                retrieveStructures(request, validationNumbers, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject structures) {
                        retrieveOrderData(request, validationNumbers, new Handler<JsonObject>() {
                            @Override
                            public void handle(final JsonObject order) {
                                retrieveOrderDataForCertificate(request, validationNumbers,  structures, new Handler<JsonArray>() {
                                    @Override
                                    public void handle(final JsonArray certificates) {
                                        retrieveContract(request, validationNumbers, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject contract) {
                                                retrieveOrderParam(validationNumbers, new Handler<JsonObject>() {
                                                    @Override
                                                    public void handle(JsonObject event) {
                                                        JsonObject certificate;
                                                        for (int i = 0; i < certificates.size(); i++) {
                                                            certificate = certificates.getJsonObject(i);
                                                            certificate.put(LystoreBDD.AGENT, managmentInfo.getJsonObject("userInfo"));
                                                            certificate.put(LystoreBDD.SUPPLIER,
                                                                    managmentInfo.getJsonObject("supplierInfo"));
                                                            addStructureToOrders(certificate.getJsonArray(LystoreBDD.ORDERS),
                                                                    certificate.getJsonObject(CommonConstants.STRUCTURE));
                                                        }
                                                        data.put(LystoreBDD.SUPPLIER, managmentInfo.getJsonObject("supplierInfo"))
                                                                .put(LystoreBDD.AGENT, managmentInfo.getJsonObject("userInfo"))
                                                                .put(LystoreBDD.ORDER, order)
                                                                .put(LystoreBDD.CERTIFICATES, certificates)
                                                                .put(LystoreBDD.CONTRACT, contract)
                                                                .put(LystoreBDD.NBR_BC, nbrBc)
                                                                .put(LystoreBDD.NBR_ENGAGEMENT, nbrEngagement)
                                                                .put(LystoreBDD.DATE_GENERATION, dateGeneration)
                                                                .put(REGION_TYPE_NAME, I18n.getInstance().translate(config.getString(REGION_TYPE_NAME), getHost(request), I18n.acceptLanguage(request)));
                                                        if( nbrBc == null || nbrBc.equals("")){
                                                            data.put(LystoreBDD.NBR_BC,  event.getString("order_number"))
                                                                    .put(LystoreBDD.NBR_ENGAGEMENT, event.getString("engagement_number"));

                                                        }
                                                        handler.handle(data);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void retrieveOrderParam(JsonArray validationNumbers, Handler<JsonObject> jsonObjectHandler) {
        orderService.getOrderBCParams(validationNumbers, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()) {
                    jsonObjectHandler.handle(event.right().getValue());
                }

            }
        });
    }

    private void addStructureToOrders(JsonArray orders, JsonObject structure) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            order.put("structure", structure);
        }
    }

    private void retrieveOrderDataForCertificate(final HttpServerRequest request, final JsonArray numberValidation, final JsonObject structures,
                                                 final Handler<JsonArray> handler) {
        JsonObject structure;
        String structureId;
        Iterator<String> structureIds = structures.fieldNames().iterator();
        final JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
        while (structureIds.hasNext()) {
            structureId = structureIds.next();
            structure = structures.getJsonObject(structureId);
            JsonObject finalStructure = structure;
            String finalStructureId = structureId;
            orderService.getOrderByValidatioNumber(numberValidation,
                    new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight() && event.right().getValue().size() > 0) {
                                JsonArray allorders = event.right().getValue();
                                JsonArray orders = new JsonArray();
                                for (int i = 0 ; i< allorders.size();i++){
                                    JsonObject order =allorders.getJsonObject(i);
                                    if(order.getString("id_structure").equals(finalStructureId)){
                                        orders.add(order);
                                    }
                                }
                                result.add(new JsonObject()
                                        .put("id_structure", finalStructureId)
                                        .put("structure", finalStructure.getJsonObject("structureInfo"))
                                        .put("orders",orders)
                                );
                                if (result.size() == structures.size()) {
                                    handler.handle(result);
                                }
                            } else {
                                log.error("An error occurred when collecting orders for certificates");
                                badRequest(request);
                                return;
                            }
                        }
                    });
        }
    }

    private void retrieveManagementInfo(final HttpServerRequest request, JsonArray validationNumbers,
                                        final Number supplierId, final Handler<JsonObject> handler) {
        agentService.getAgentByOrderIds(validationNumbers, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> user) {
                if (user.isRight()) {
                    final JsonObject userObject = user.right().getValue();
                    supplierService.getSupplier(supplierId.toString(), new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> supplier) {
                            if (supplier.isRight()) {
                                JsonObject supplierObject = supplier.right().getValue();
                                handler.handle(
                                        new JsonObject()
                                                .put("userInfo", userObject)
                                                .put("supplierInfo", supplierObject)
                                );
                            } else {
                                log.error("An error occurred when collecting supplier data");
                                badRequest(request);
                                return;
                            }
                        }
                    });
                } else {
                    log.error("An error occured when collecting user information");
                    badRequest(request);
                    return;
                }
            }
        });
    }

    @Put("/orders/done")
    @ApiDoc("Wind up orders ")
    @ResourceFilter(ManagerRight.class)
    public void windUpOrders(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                try {
                    List<String> params = new ArrayList<>();
                    for (Object id : orders.getJsonArray("ids")) {
                        params.add(id.toString());
                    }
                    List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                    JsonArray override_region = orders.getJsonArray("override_region");
                    orderService.windUpOrders(ids,
                            override_region,
                            Logging.defaultResponsesHandler(eb,
                                    request,
                                    Contexts.ORDER.toString(),
                                    Actions.UPDATE.toString(),
                                    params,
                                    null)
                    );
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting order id", e);
                }
            }
        });

    }

    @Get("/orders/export")
    @ApiDoc("Export list of waiting orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void exportCSVordersSelected(final HttpServerRequest request) {
        List<String> params = request.params().getAll("id");
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
        if (!idsOrders.isEmpty()) {
            orderService.getExportCsvOrdersAdmin(idsOrders, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> ordersWithIdStructure) {
                    if (ordersWithIdStructure.isRight()) {
                        final JsonArray orders = ordersWithIdStructure.right().getValue();
                        JsonArray idsStructures = new fr.wseduc.webutils.collections.JsonArray();
                        for (int i = 0; i < orders.size(); i++) {
                            JsonObject order = orders.getJsonObject(i);
                            idsStructures.add(order.getString("idstructure"));
                        }
                        structureService.getStructureById(idsStructures, new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> repStructures) {
                                if (repStructures.isRight()) {
                                    JsonArray structures = repStructures.right().getValue();

                                    Map<String, String> structuresMap = retrieveUaiNameStructure(structures);
                                    for (int i = 0; i < orders.size(); i++) {
                                        JsonObject order = orders.getJsonObject(i);
                                        order.put("uaiNameStructure", structuresMap.get(order.getString("idstructure")));
                                    }

                                    request.response()
                                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                            .end(generateExport(request, orders));

                                } else {
                                    log.error("An error occured when collecting StructureById");
                                    renderError(request);
                                }
                            }
                        });
                    } else {
                        log.error("An error occurred when collecting ordersSqlwithIdStructure");
                        renderError(request);
                    }
                }
            });
        } else {
            badRequest(request);
        }


    }

    private Map<String, String> retrieveUaiNameStructure(JsonArray structures) {
        final Map<String, String> structureMap = new HashMap<String, String>();

        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.getJsonObject(i);
            String uaiNameStructure = structure.getString("uai") + " - " + structure.getString("name");
            structureMap.put(structure.getString("id"), uaiNameStructure);
        }

        return structureMap;
    }

    @Get("/order/:id/file/:fileId")
    @ApiDoc("Download specific file")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getFile(HttpServerRequest request) {
        Integer orderId = Integer.parseInt(request.getParam("id"));
        String fileId = request.getParam("fileId");
        orderService.getFile(orderId, fileId, event -> {
            if (event.isRight()) {
                storage.sendFile(fileId, event.right().getValue().getString("filename"), request, false, new JsonObject());
            } else {
                notFound(request);
            }
        });
    }

    @Get("/order/download/:id/file/:fileId")
    @ApiDoc("Download specific file")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getFileOrderRegion(HttpServerRequest request) {
        String fileId = request.getParam("fileId");
        orderService.getFileOrderRegion(fileId, event -> {
            if (event.isRight()) {
                storage.sendFile(fileId, event.right().getValue().getString("filename"), request, false, new JsonObject());
            } else {
                notFound(request);
            }
        });
    }

    @Delete("/order/update")
    @ApiDoc("delete files unaffiliated with an order-region-equipment id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteOrderRegionFile(final HttpServerRequest request) {
        orderService.deleteOrderRegionFile(event-> {
            if(event.isRight()){
                request.response().setStatusCode(204).end();
            } else {
                renderError(request);
            }
        });
    }

    @Delete("/order/update/file/:fileId")
    @ApiDoc("Delete file from basket")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteFileFromOrder(HttpServerRequest request) {
        String fileId = request.getParam("fileId");

        orderService.deleteFileFromOrder(fileId, event -> {
            if (event.isRight()) {
                request.response().setStatusCode(204).end();
                deleteFile(fileId);
            } else {
                renderError(request);
            }
        });
    }

    /**
     * Delete file from storage based on identifier
     *
     * @param fileId File identifier to delete
     */
    private void deleteFile(String fileId) {
        storage.removeFile(fileId, e -> {
            if (!"ok".equals(e.getString("status"))) {
                log.error("[Lystore@uploadFile] An error occurred while removing " + fileId + " file.");
            }
        });
    }

    @Put("/orders/operation/in-progress/:idOperation")
    @ApiDoc("update operation in orders with status in progress")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperationInProgress(final HttpServerRequest request) {
        final Integer idOperation = Integer.parseInt(request.params().get("idOperation"));
        RequestUtils.bodyToJsonArray(request, idOrders -> orderService.updateOperationInProgress(idOperation, idOrders, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDER.toString(),
                Actions.UPDATE.toString(),
                idOperation.toString(),
                new JsonObject().put("ids", idOrders))));
    }

    @Put("/orders/operation/:idOperation")
    @ApiDoc("update operation in orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperation(final HttpServerRequest request) {
        final Integer idOperation = Integer.parseInt(request.params().get("idOperation"));
        RequestUtils.bodyToJsonArray(request, idOrders -> orderService.updateOperation(idOperation, idOrders, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDER.toString(),
                Actions.UPDATE.toString(),
                idOperation.toString(),
                new JsonObject().put("ids", idOrders))));
    }

    @Put("/order/:idOrder")
    @ApiDoc("update status in orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateStatusOrder(final HttpServerRequest request) {
        final Integer idOrder = Integer.parseInt(request.params().get("idOrder"));
        RequestUtils.bodyToJson(request, statusEdit -> orderService.updateStatusOrder(idOrder, statusEdit, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDER.toString(),
                Actions.UPDATE.toString(),
                idOrder.toString(),
                statusEdit)));
    }

    @Get("/order/:idOrder")
    @ApiDoc("get an order")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getOrder(HttpServerRequest request) {
        try {
            Integer orderId = Integer.parseInt(request.getParam("idOrder"));
            orderService.getOrder(orderId, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error(" An error occurred when casting order id", e);
        }

    }

    @Get("/orderClient/:id/order/")
    @ApiDoc("get order by id order client ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOneOrderProgress(HttpServerRequest request) {
        Integer idOrder = Integer.parseInt(request.getParam("id"));
        orderService.getOneOrderClient(idOrder,defaultResponseHandler(request));
    }

    @Put("/orderClient/reject")
    @ApiDoc("reject orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createRejectOrders(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, rejectOrder -> orderService.createRejectOrders(rejectOrder, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDER.toString(),
                Actions.REJECT.toString(),
                null,
                rejectOrder)));
    }

    @Get("/orderClient/rejectComment/:idCampaign")
    @ApiDoc("Returns comments")
    @SecuredAction(value="", type = ActionType.AUTHENTICATED)
    public void getRejectOrderComment(HttpServerRequest request) {
        Integer idCampaign = Integer.parseInt(request.getParam("idCampaign"));
        orderService.getRejectOrderComment(idCampaign, arrayResponseHandler(request));
    }
    @Post("/orderClient/send/mail/notification/etab")
    @ApiDoc("Get the pdf of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void sendNotificationEtab (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, order ->{
            final String orderNumber = order.getString("bc_number");
            try{
                String domainMail =  config.getJsonObject("mail").getString("domainMail");
                EmailSender emailSender = notificationEmailFactory.getSender();
                orderService.sendNotification(orderNumber,domainMail,request,emailSender);
            }catch (Exception e){
                badRequest(request,e.getMessage());
            }

        });

    }

    @Post("/orderClient/send/mail/notification/region")
    @ApiDoc("Get the pdf of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void sendNotificationREgion (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, order ->{
            final String orderNumber = order.getString("bc_number");
            try{
                String domainMail =  config.getJsonObject("mail").getString("domainMail","lystore.monlycee.net");
                String emailNotificationHelpDeskReceiver =
                        config.getJsonObject("mail",new JsonObject()).getString("notificationHelpDeskReceiver","collecteur.lystore@monlycee.net");

                EmailSender emailSender = notificationHelpDeskEmailFactory.getSender();
                orderService.sendNotificationHelpDesk(orderNumber,domainMail,request,emailSender,emailNotificationHelpDeskReceiver);
            }catch (Exception e){
                badRequest(request,e.getMessage());
            }

        });

    }
}
