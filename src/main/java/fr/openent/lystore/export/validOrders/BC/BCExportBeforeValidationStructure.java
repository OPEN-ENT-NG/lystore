package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.controllers.OrderController;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static fr.openent.lystore.constants.ParametersConstants.BC_OPTIONS;
import static fr.openent.lystore.helpers.OrderHelper.*;

public class BCExportBeforeValidationStructure extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExportBeforeValidationStructure.class);

    public BCExportBeforeValidationStructure(EventBus eb, Vertx vertx, JsonObject config) {
        super(eb, vertx, config);
    }


    public void create(JsonArray validationNumbersArray, Handler<Either<String, Buffer>> exportHandler) {
        parameterService.getBcOptions()
                .onSuccess(bcOptions -> {
                    log.info(bcOptions.toJson());
                    List<String> validationNumbers = validationNumbersArray.getList();
                    supplierService.getSupplierByValidationNumbers(new fr.wseduc.webutils.collections.JsonArray(validationNumbers), event -> {
                        if (event.isRight()) {
                            JsonObject supplier = event.right().getValue();
                            getOrdersData(exportHandler, "", "", "", supplier.getInteger("id"),
                                    new JsonArray(validationNumbers), false,
                                    data -> {
                                        data.put(BC_OPTIONS, bcOptions.toJson())
                                                .put("print_order", true)
                                                .put("print_certificates", false);
                                        generatePDF(exportHandler, data,
                                                "BC_Struct.xhtml", "CSF_",
                                                pdf -> exportHandler.handle(new Either.Right(pdf))
                                        );
                                    });
                        } else {
                            log.error("error when getting supplier");
                        }
                    });
                })
                .onFailure(fail -> exportHandler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(BCExportDuringValidation.class, "create", "Error when calling getBcOptions", fail))));
    }


    @Override
    protected void retrieveOrderData(final Handler<Either<String, Buffer>> exportHandler, JsonArray validationNumbers, boolean groupByStructure,
                                     final Handler<JsonObject> handler) {
        orderService.getOrderByValidatioNumber(validationNumbers, event -> {
            if (event.isRight()) {
                JsonObject order = new JsonObject();
                ArrayList<String> listStruct = new ArrayList<>();
                JsonArray orders = formatOrders(event.right().getValue());
                orders = sortByUai(orders);

                sortOrdersByStructure(order, listStruct, orders);
                getSubTotalByStructure(order, listStruct);

                getStructureById(order, listStruct, handler, exportHandler);

            } else {
                log.error("An error occurred when retrieving order data");
                exportHandler.handle(new Either.Left<>("An error occurred when retrieving order data"));
            }
        });
    }

    private void getOrdersDataSql(String nbrbc, Handler<Either<String, io.vertx.core.json.JsonArray>> handler) {
        String query = "SELECT ord.engagement_number AS nbr_engagement, " +
                "       ord.date_creation     AS date_generation, " +
                "       supplier.id           AS supplier_id, " +
                "       array_to_json(Array_agg(DISTINCT oce.number_validation)) as ids " +
                "FROM   lystore.order ord " +
                "       INNER JOIN lystore.order_client_equipment oce " +
                "               ON oce.id_order = ord.id " +
                "       LEFT JOIN lystore.contract " +
                "              ON contract.id = oce.id_contract " +
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

