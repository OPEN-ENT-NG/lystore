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


public class BCExportDuringValidation extends PDF_OrderHElper {

    private Logger log = LoggerFactory.getLogger(BCExport.class);

    public BCExportDuringValidation(EventBus eb, Vertx vertx, JsonObject config, Storage storage) {
        super(eb, vertx, config, storage);

    }


    public void create(JsonObject params, Handler<Either<String, Buffer>> exportHandler) {
        final JsonArray ids = params.getJsonArray(CommonConstants.IDS);
        final String nbrBc = params.getString(LystoreBDD.NBRBC);
        final String nbrEngagement = params.getString(LystoreBDD.NBRENGAGEMENT);
        final String dateGeneration = params.getString(LystoreBDD.DATEGENERATION);
        Number supplierId = params.getInteger(LystoreBDD.SUPPLIERID);
        parameterService.getBcOptions()
                .onSuccess(bcOptions -> getOrdersData(exportHandler, nbrBc, nbrEngagement, dateGeneration, supplierId, ids, false,
                        data -> {
                            data.put(ExportConstants.PRINT_ORDER, true)
                                    .put(ExportConstants.PRINT_CERTIFICATES, false)
                                    .put(BC_OPTIONS, bcOptions.toJson());
                            generatePDF(exportHandler, data,
                                    ExportConstants.BC_TEMPLATE
                            );
                        }))
                .onFailure(fail -> exportHandler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(BCExportDuringValidation.class, "create", "Error when calling getBcOptions", fail))));

    }
}

