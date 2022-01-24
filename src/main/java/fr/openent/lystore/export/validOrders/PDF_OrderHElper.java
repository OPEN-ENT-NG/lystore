package fr.openent.lystore.export.validOrders;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.controllers.OrderController;
import fr.openent.lystore.export.validOrders.BC.BCExport;
import fr.openent.lystore.helpers.OrderHelper;
import fr.openent.lystore.helpers.RendersHelper;
import fr.openent.lystore.service.*;
import fr.openent.lystore.service.impl.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static fr.openent.lystore.helpers.OrderHelper.getSumWithoutTaxes;
import static fr.openent.lystore.helpers.OrderHelper.roundWith2Decimals;

public class PDF_OrderHElper {

    protected SupplierService supplierService;
    protected JsonObject config;
    protected Vertx vertx;
    protected EventBus eb;
    protected String node;
    protected Logger log = LoggerFactory.getLogger(BCExport.class);
    protected OrderService orderService;
    protected ProgramService programService;

    protected DefaultContractService contractService;
    protected StructureService structureService;
    protected AgentService agentService;
    protected RendersHelper renders ;

    public PDF_OrderHElper(EventBus eb, Vertx vertx, JsonObject config){
        this.vertx = vertx;
        this.config = config;
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.eb = eb;
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
        this.orderService = new DefaultOrderService(Lystore.lystoreSchema, "order_client_equipment", emailSender);
        this.structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
        this.contractService = new DefaultContractService(Lystore.lystoreSchema, "contract");
        this.agentService = new DefaultAgentService(Lystore.lystoreSchema, "agent");
        this.renders = new RendersHelper(this.vertx, config);
        programService = new DefaultProgramService(Lystore.lystoreSchema,"program");


    }


    protected void addStructureToOrders(JsonArray orders, JsonObject structure) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            order.put("structure", structure);
        }
    }

    private void setOrdersToArray(JsonArray ordersArray, ArrayList<String> listStruct, JsonObject order) {
        for (String idStruct : listStruct) {
            JsonObject ordersJsonObject = order.getJsonObject(idStruct);
            ordersArray.add(ordersJsonObject);
            order.remove(idStruct);
        }
        order.put("orderArray",ordersArray);
    }

    protected JsonArray sortByUai(JsonArray values) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<JsonObject>();
        for (int i = 0; i < values.size(); i++) {
            jsonValues.add(values.getJsonObject(i));
        }

        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "id_structure";

            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    if (a.containsKey(KEY_NAME)) {
                        valA = a.getString(KEY_NAME);
                    }
                    if (b.containsKey(KEY_NAME)) {
                        valB = b.getString(KEY_NAME);
                    }
                } catch (NullPointerException e) {
                    log.error("error when sorting structures during export");
                }

                return valA.compareTo(valB);
            }
        });

        for (int i = 0; i < values.size(); i++) {
            sortedJsonArray.add(jsonValues.get(i));
        }
        return sortedJsonArray;
    }

    protected void getStructureById(JsonObject order, ArrayList<String> listStruct, Handler<JsonObject> handler, Handler<Either<String, Buffer>> exportHandler) {
        structureService.getStructureById(new JsonArray(listStruct), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray structures = event.right().getValue();
                    JsonObject structure ;
                    for (int i = 0; i < structures.size(); i++) {
                        structure = structures.getJsonObject(i);
                        JsonObject ordersByStructure = order.getJsonObject(structure.getString("id"));
                        ordersByStructure.put("name",structure.getString("name"));
                        ordersByStructure.put("uai",structure.getString("uai"));
                        ordersByStructure.put("address",structure.getString("address"));
                        ordersByStructure.put("phone",structure.getString("phone"));
                        order.put(structure.getString("id"),ordersByStructure);

                    }
                    JsonArray ordersArray = new JsonArray();
                    setOrdersToArray(ordersArray, listStruct, order);
                    handler.handle(order);
                } else {
                    log.error("An error occurred when collecting structures based on validationNumbers");
                    exportHandler.handle(new Either.Left<>("An error occurred when collecting structures based on validationNumbers"));

                }
            }
        });
    }
    protected  void sortOrdersByStructure(JsonObject order, ArrayList<String> listStruct, JsonArray orders) {
        for(int i=0;i<orders.size();i++){
            JsonObject orderSorted = orders.getJsonObject(i);
            String idStruct = orderSorted.getString("id_structure");
            if(order.containsKey(idStruct)){
                JsonArray tempOrders = order.getJsonObject(idStruct).getJsonArray("orders").add(orderSorted);
                order.put(orderSorted.getString("id_structure"),new JsonObject().put("orders",tempOrders));
            }else{
                listStruct.add(idStruct);
                order.put(orderSorted.getString("id_structure"),new JsonObject().put("orders", new JsonArray().add(orderSorted)));
            }
        }
    }


    protected void getSubTotalByStructure(JsonObject order, ArrayList<String> listStruct) {
        for (String s : listStruct) {
            JsonObject ordersByStructure = order.getJsonObject(s);
            Double sumWithoutTaxes = getSumWithoutTaxes(ordersByStructure.getJsonArray("orders"));
            Double taxTotal = OrderHelper.getSumTTC(ordersByStructure.getJsonArray("orders"));

            ordersByStructure.put("sumLocale",
                    OrderController.getReadableNumber(roundWith2Decimals(sumWithoutTaxes)));
            ordersByStructure.put("totalTaxesLocale",
                    OrderController.getReadableNumber(roundWith2Decimals(taxTotal - sumWithoutTaxes)));
            ordersByStructure.put("totalPriceTaxeIncludedLocal",
                    OrderController.getReadableNumber(roundWith2Decimals(taxTotal )));
            order.put(s, ordersByStructure);
        }
    }
    protected void retrieveOrderDataForCertificate(final Handler<Either<String, Buffer>> exportHandler,
                                                   final JsonArray numberValidation, final JsonObject structures,
                                                   final Handler<JsonArray> handler) {
        Iterator<String> structureIds = structures.fieldNames().iterator();
        final JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
        if(!structureIds.hasNext()){
            exportHandler.handle(new Either.Left<>("no structure get"));
        }
        while (structureIds.hasNext()) {
            structureIds.next();
            orderService.getOrderByValidatioNumber(numberValidation,
                    new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight() && event.right().getValue().size() > 0) {
                                JsonObject order = event.right().getValue().getJsonObject(0);
                                result.add(new JsonObject()
                                        .put("id_structure", order.getString("id_structure"))
                                        .put("structure", structures.getJsonObject(order.getString("id_structure"))
                                                .getJsonObject("structureInfo"))
                                        .put("orders",event.right().getValue())
                                );
                                if (result.size() == structures.size()) {
                                    handler.handle(result);
                                }
                            } else {
                                log.error("An error occurred when collecting orders for certificates");
                                exportHandler.handle(new Either.Left<>("An error occured when collecting orders for certificates"));
                                return;
                            }
                        }
                    });
        }
    }

    protected Map<String, String> retrieveUaiNameStructure(JsonArray structures) {
        final Map<String, String> structureMap = new HashMap<String, String>();

        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.getJsonObject(i);
            String uaiNameStructure = structure.getString("uai") + " - " + structure.getString("name");
            structureMap.put(structure.getString("id"), uaiNameStructure);
        }

        return structureMap;
    }

    protected void retrieveContract(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
                                    final Handler<JsonObject> handler) {
        contractService.getContract(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight() && event.right().getValue().size() == 1) {
                    handler.handle(event.right().getValue().getJsonObject(0));
                } else {
                    exportHandler.handle(new Either.Left<>("An error occured when collecting contract data"));
                    log.error("An error occured when collecting contract data");
                }
            }
        });
    }

    protected void retrieveStructures(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
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
                                exportHandler.handle(new Either.Left<>("An error occurred when collecting structures based on ids"));

                            }
                        }
                    });
                } else {
                    log.error("An error occurred when getting structures id based on order ids.");
                    exportHandler.handle(new Either.Left<>("An error occurred when getting structures id based on order ids."));

                }
            }
        });
    }



    protected void retrieveOrderData(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,boolean groupByStructure,
                                     final Handler<JsonObject> handler) {
        orderService.getOrderByValidatioNumber(ids,  new Handler<Either<String, JsonArray>>() {
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
                    exportHandler.handle(new Either.Left<>("An error occurred when retrieving order data"));
                }
            }
        });
    }
    protected void retrieveManagementInfo(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
                                          final Number supplierId, final Handler<JsonObject> handler) {
        agentService.getAgentByOrderIds(ids, new Handler<Either<String, JsonObject>>() {
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
                                exportHandler.handle(new Either.Left<>("An error occurred when collecting supplier data"));

                                return;
                            }
                        }
                    });
                } else {
                    log.error("An error occured when collecting user information");
                    exportHandler.handle(new Either.Left<>("An error occured when collecting user information"));

                    return;
                }
            }
        });
    }
    protected void getOrdersData(final Handler<Either<String, Buffer>> exportHandler, final String nbrBc,
                                 final String nbrEngagement, final String dateGeneration,
                                 final Number supplierId, final JsonArray validationNumbers, boolean groupByStructure,
                                 final Handler<JsonObject> handler) {
        SimpleDateFormat formatterDatePDF = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        final JsonObject data = new JsonObject();

        retrieveManagementInfo(exportHandler, validationNumbers, supplierId, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject managmentInfo) {
                retrieveStructures(exportHandler, validationNumbers, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject structures) {
                        retrieveOrderData(exportHandler, validationNumbers,groupByStructure, new Handler<JsonObject>() {
                            @Override
                            public void handle(final JsonObject order) {
                                retrieveOrderDataForCertificate(exportHandler, validationNumbers, structures, new Handler<JsonArray>() {
                                    @Override
                                    public void handle(final JsonArray certificates) {
                                        retrieveContract(exportHandler, validationNumbers, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject contract) {
                                                JsonObject certificate;
                                                for (int i = 0; i < certificates.size(); i++) {
                                                    certificate = certificates.getJsonObject(i);
                                                    certificate.put("agent", managmentInfo.getJsonObject("userInfo"));
                                                    certificate.put("supplier",
                                                            managmentInfo.getJsonObject("supplierInfo"));
                                                    addStructureToOrders(certificate.getJsonArray("orders"),
                                                            certificate.getJsonObject("structure"));
                                                }
                                                Date orderDate = null;

                                                try {
                                                    orderDate = formatterDate.parse(dateGeneration);
                                                } catch (ParseException e) {
                                                    log.error("Incorrect date format");
                                                }
                                                String date ;
                                                try{
                                                    date =  formatterDatePDF.format(orderDate);
                                                }catch (java.lang.NullPointerException e){
                                                    date = dateGeneration;
                                                }
                                                data.put("supplier", managmentInfo.getJsonObject("supplierInfo"))
                                                        .put("agent", managmentInfo.getJsonObject("userInfo"))
                                                        .put("order", order)
                                                        .put("certificates", certificates)
                                                        .put("contract", contract)
                                                        .put("nbr_bc", nbrBc)
                                                        .put("nbr_engagement", nbrEngagement)
                                                        .put("date_generation",date);
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





    public void generatePDF( Handler<Either<String, Buffer>> exportHandler,final JsonObject templateProps, final String templateName,
                             final String prefixPdfName, final Handler<Buffer> handler) {

        final JsonObject exportConfig = config.getJsonObject("exports");
        final String templatePath = exportConfig.getString("template-path");
        final String baseUrl = config.getString("host") +
                config.getString("app-address") + "/public/";
        final String logo = exportConfig.getString("logo-path");

        node = (String) vertx.sharedData().getLocalMap("server").get("node");
        if (node == null) {
            node = "";
        }

        final String path = FileResolver.absolutePath(templatePath + templateName);
        final String logoPath = FileResolver.absolutePath(logo);

        vertx.fileSystem().readFile(path, new Handler<AsyncResult<Buffer>>() {

            @Override
            public void handle(AsyncResult<Buffer> result) {
                if (!result.succeeded()) {
                    return;
                }

                Buffer logoBuffer = vertx.fileSystem().readFileBlocking(logoPath);
                String encodedLogo = "";
                try {
                    encodedLogo = new String(Base64.getMimeEncoder().encode(logoBuffer.getBytes()), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    log.error("[DefaultExportPDFService@generatePDF] An error occurred while encoding logo to base 64");
                }
                templateProps.put("logo-data", encodedLogo);

                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                renders.processTemplate(exportHandler, templateProps, templateName, reader, new Handler<Writer>() {
                    //
                    @Override
                    public void handle(Writer writer) {
                        String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                        if (processedTemplate == null) {
                            exportHandler.handle(new Either.Left<>("processed template is null"));
                            return;
                        }
                        JsonObject actionObject = new JsonObject();
                        byte[] bytes;
                        try {
                            bytes = processedTemplate.getBytes("UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            bytes = processedTemplate.getBytes();
                            log.error(e.getMessage(), e);
                        }

                        actionObject
                                .put("content", bytes)
                                .put("baseUrl", baseUrl);
                        eb.send(node + "entcore.pdf.generator", actionObject, new Handler<AsyncResult<Message<JsonObject>>>() {
                            @Override
                            public void handle(AsyncResult<Message<JsonObject>> reply) {
                                JsonObject pdfResponse = reply.result().body();
                                if (!"ok".equals(pdfResponse.getString("status"))) {
                                    exportHandler.handle(new Either.Left<>("wrong status when calling bus (pdf) "));
                                    return;
                                }
                                byte[] pdf = pdfResponse.getBinary("content");
                                Buffer either = Buffer.buffer(pdf);
                                handler.handle(either);
                            }
                        });
                    }
                });
            }
        });

    }
}
