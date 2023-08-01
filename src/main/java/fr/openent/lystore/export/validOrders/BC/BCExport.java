package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.ExportConstants;
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

import java.util.List;

import static fr.openent.lystore.constants.ParametersConstants.BC_OPTIONS;


public class BCExport extends PDF_OrderHelper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);


    public BCExport(ServiceFactory serviceFactory) {
        super(serviceFactory);

    }


    public void create(JsonArray validationNumbersArray, Handler<Either<String, Buffer>> exportHandler) {
        parameterService.getBcOptions()
                .onSuccess(bcOptions -> {
                    List<String> validationNumbers = validationNumbersArray.getList();
                    supplierService.getSupplierByValidationNumbers(new fr.wseduc.webutils.collections.JsonArray(validationNumbers), event -> {
                        if (event.isRight()) {
                            JsonObject supplier = event.right().getValue();
                            getOrdersData(exportHandler, "", "", "", supplier.getInteger(CommonConstants.ID),
                                    new JsonArray(validationNumbers), false,
                                    data -> {
                                        data.put(ExportConstants.PRINT_ORDER, true)
                                                .put(ExportConstants.PRINT_CERTIFICATES, false)
                                                .put(BC_OPTIONS, bcOptions.toJson());
                                        generatePDF(exportHandler, data,
                                                ExportConstants.BC_TEMPLATE
                                        );
                                    });
                        } else {
                           log.error(LystoreUtils.generateErrorMessage(BCExport.class, "create",
                                   "error when getting supplier", event.left().getValue()
                                    ));
                        }
                    });
                })
                .onFailure(fail -> exportHandler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(BCExport.class, "create", "Error when calling getBcOptions",
                                fail))));
    }
}
