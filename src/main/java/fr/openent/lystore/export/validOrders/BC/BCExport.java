package fr.openent.lystore.export.validOrders.BC;

//import fr.openent.lystore.helpers.RendersHelper;
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

        import java.util.*;

import static fr.openent.lystore.constants.ParametersConstants.BC_OPTIONS;
import static fr.wseduc.webutils.http.Renders.badRequest;


public class BCExport extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);


    public BCExport(EventBus eb, Vertx vertx, JsonObject config, Storage storage) {
        super(eb, vertx, config, storage);

    }


    public void create(JsonArray validationNumbersArray, Handler<Either<String, Buffer>> exportHandler) {
        parameterService.getBcOptions()
                .onSuccess(bcOptions -> {
                    List<String> validationNumbers = validationNumbersArray.getList();
                    supplierService.getSupplierByValidationNumbers(new fr.wseduc.webutils.collections.JsonArray(validationNumbers), new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> event) {
                            if (event.isRight()) {
                                JsonObject supplier = event.right().getValue();
                                getOrdersData(exportHandler, "", "", "", supplier.getInteger("id"),
                                        new JsonArray(validationNumbers), false,
                                        data -> {
                                            log.info(bcOptions.toJson());
                                            data.put("print_order", true)
                                                    .put("print_certificates", false)
                                                    .put(BC_OPTIONS, bcOptions.toJson());
                                            generatePDF(exportHandler, data,
                                                    "BC.xhtml",
                                                    pdf -> exportHandler.handle(new Either.Right(pdf))
                                            );
                                        });
                            } else {
                                log.error("error when getting supplier");
                            }
                        }
                    });
                })
                .onFailure(fail -> exportHandler.handle(new Either.Left<>(
                        LystoreUtils.generateErrorMessage(BCExportDuringValidation.class, "create", "Error when calling getBcOptions", fail))));
    }
}
