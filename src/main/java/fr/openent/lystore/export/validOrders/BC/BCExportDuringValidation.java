package fr.openent.lystore.export.validOrders.BC;


import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.ExportConstants;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.export.validOrders.PDF_OrderHelper;
import fr.openent.lystore.factory.ServiceFactory;
import fr.openent.lystore.utils.LystoreUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.openent.lystore.constants.ParametersConstants.BC_OPTIONS;


public class BCExportDuringValidation extends PDF_OrderHelper {

    private Logger log = LoggerFactory.getLogger(BCExport.class);

    public BCExportDuringValidation(ServiceFactory serviceFactory) {
        super(serviceFactory);

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

