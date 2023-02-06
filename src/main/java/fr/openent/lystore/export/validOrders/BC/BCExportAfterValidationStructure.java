package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.validOrders.PDF_OrderHElper;
import fr.openent.lystore.helpers.OrderHelper;
import fr.openent.lystore.utils.LystoreUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;

import java.util.ArrayList;

import static fr.openent.lystore.constants.ParametersConstants.BC_OPTIONS;

public class BCExportAfterValidationStructure extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);

    public BCExportAfterValidationStructure(EventBus eb, Vertx vertx, JsonObject config, Storage storage) {
        super(eb, vertx, config, storage);
    }


    public void create(String nbrBc, Handler<Either<String, Buffer>> exportHandler) {
        parameterService.getBcOptions()
                .onSuccess(bcOptions -> getOrdersDataSql(nbrBc, event -> {
                    if (event.isRight()) {
                        log.info(bcOptions.toJson());
                        JsonArray paramstemp = event.right().getValue();
                        JsonObject params = paramstemp.getJsonObject(0);
                        final JsonArray ids = new JsonArray();
                        JsonArray idsArray = new JsonArray(params.getString("ids"));
                        for (int i = 0; i < idsArray.size(); i++) {
                            ids.add(idsArray.getValue(i).toString());
                        }
                        final String nbrEngagement = params.getString("nbr_engagement");
                        final String dateGeneration = params.getString("date_generation");
                        Number supplierId = params.getInteger("supplier_id");
                        getOrdersData(exportHandler, nbrBc, nbrEngagement, dateGeneration, supplierId, ids, true,
                                data -> {
                                    data.put("print_order", true)
                                    .put("print_certificates", false)
                                    .put(BC_OPTIONS , bcOptions.toJson());
                                    generatePDF(exportHandler, data,
                                            "BC_Struct.xhtml",
                                            pdf -> exportHandler.handle(new Either.Right<>(pdf))
                                    );
                                });
                    } else {
                        exportHandler.handle(new Either.Left<>("sql failed"));
                    }
                }))
                .onFailure(fail -> exportHandler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(BCExportDuringValidation.class, "create", "Error when calling getBcOptions", fail))));


    }

    @Override
    protected void retrieveOrderData(final Handler<Either<String, Buffer>> exportHandler, JsonArray validationNumbers, boolean groupByStructure,
                                     final Handler<JsonObject> handler) {
        orderService.getOrderByValidatioNumber(validationNumbers, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonObject order = new JsonObject();
                    ArrayList<String> listStruct = new ArrayList<>();
                    JsonArray orders = OrderHelper.formatOrders(event.right().getValue());
                    orders = sortByUai(orders);

                    sortOrdersByStructure(order, listStruct, orders);
                    getSubTotalByStructure(order, listStruct);

                    getStructureById(order, listStruct, handler, exportHandler);

                } else {
                    log.error("An error occurred when retrieving order data");
                    exportHandler.handle(new Either.Left<>("An error occurred when retrieving order data"));
                }
            }
        });
    }


    private void getOrdersDataSql(String nbrbc, Handler<Either<String, JsonArray>> handler) {
        getOrdersDataQueryByStructure(nbrbc, handler);
    }

    static void getOrdersDataQueryByStructure(String nbrbc, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT ord.engagement_number AS nbr_engagement, " +
                "       ord.date_creation     AS date_generation, " +
                "       supplier.id           AS supplier_id, " +
                "       array_to_json(Array_agg(DISTINCT orders.number_validation)) as ids " +
                "FROM   lystore.order ord " +
                "       INNER JOIN lystore.allOrders orders " +
                "               ON orders.id_order = ord.id " +
                "       LEFT JOIN lystore.contract " +
                "              ON contract.id = orders.id_contract " +
                "       INNER JOIN lystore.supplier " +
                "               ON contract.id_supplier = supplier.id " +
                "WHERE  ord.order_number = ? " +
                "GROUP  BY ord.engagement_number, " +
                "          ord.date_creation, " +
                "          supplier_id ";

        Sql.getInstance().prepared(query, new JsonArray().add(nbrbc), new DeliveryOptions().setSendTimeout(Lystore.timeout * 1000000000L), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));
    }
}
