package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.ExportConstants;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.export.validOrders.PDF_OrderHElper;
import fr.openent.lystore.utils.LystoreUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;

import static fr.openent.lystore.constants.ParametersConstants.BC_OPTIONS;

public class BCExportAfterValidation extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);

    public BCExportAfterValidation(EventBus eb, Vertx vertx, JsonObject config, Storage storage) {
        super(eb, vertx, config, storage);

    }


    public void create(String nbrBc, Handler<Either<String, Buffer>> exportHandler) {
        parameterService.getBcOptions()
                .onSuccess(bcOptions -> getOrdersDataSql(nbrBc, event -> {
                            if (event.isRight()) {
                                JsonArray paramstemp = event.right().getValue();
                                JsonObject params = paramstemp.getJsonObject(0);
                                final JsonArray ids = new JsonArray();
                                JsonArray idsArray = new JsonArray(params.getString(CommonConstants.IDS));
                                for (int i = 0; i < idsArray.size(); i++) {
                                    ids.add(idsArray.getValue(i).toString());
                                }
                                final String nbrEngagement = params.getString(LystoreBDD.NBR_ENGAGEMENT);
                                final String dateGeneration = params.getString(LystoreBDD.DATE_GENERATION);
                                Integer supplierId = params.getInteger(LystoreBDD.SUPPLIER_ID);
                                getOrdersData(exportHandler, nbrBc, nbrEngagement, dateGeneration, supplierId, ids, false,
                                        data -> {
                                            data.put(ExportConstants.PRINT_ORDER, true)
                                                    .put(ExportConstants.PRINT_CERTIFICATES, false)
                                                    .put(BC_OPTIONS, bcOptions.toJson());
                                            generatePDF(exportHandler, data,
                                                    ExportConstants.BC_TEMPLATE
                                            );
                                        });
                            } else {
                                exportHandler.handle(new Either.Left<>(
                                        LystoreUtils.generateErrorMessage(BCExportAfterValidation.class,
                                                "create", "sql failed", event.left().getValue())));
                            }
                        }

                ))
                .onFailure(fail -> exportHandler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(BCExportDuringValidation.class, "create",
                                "Error when calling getBcOptions", fail))));


    }

    private void getOrdersDataSql(String nbrbc, Handler<Either<String, JsonArray>> handler) {
        BCExportAfterValidationStructure.getOrdersDataQueryByStructure(nbrbc, handler);
    }
}
