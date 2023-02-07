package fr.openent.lystore.export.validOrders.BC;


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


public class BCExportDuringValidation extends PDF_OrderHElper {

    private Logger log = LoggerFactory.getLogger(BCExport.class);

    public BCExportDuringValidation(EventBus eb, Vertx vertx, JsonObject config, Storage storage) {
        super(eb, vertx, config, storage);

    }


    public void create(JsonObject params, Handler<Either<String, Buffer>> exportHandler) {
        final JsonArray ids = params.getJsonArray("ids");
        final String nbrBc = params.getString("nbrBc");
        final String nbrEngagement = params.getString("nbrEngagement");
        final String dateGeneration = params.getString("dateGeneration");
        Number supplierId = params.getInteger("supplierId");
        parameterService.getBcOptions()
                .onSuccess(bcOptions -> getOrdersData(exportHandler, nbrBc, nbrEngagement, dateGeneration, supplierId, ids, false,
                        data -> {
                            log.info(bcOptions.toJson());
                            data.put("print_order", true)
                                    .put("print_certificates", false)
                                    .put(BC_OPTIONS, bcOptions.toJson());
                            generatePDF(exportHandler, data,
                                    "BC.xhtml",
                                    pdf -> exportHandler.handle(new Either.Right<>(pdf))
                            );
                        }))
                .onFailure(fail -> exportHandler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(BCExportDuringValidation.class, "create", "Error when calling getBcOptions", fail))));

    }
}

