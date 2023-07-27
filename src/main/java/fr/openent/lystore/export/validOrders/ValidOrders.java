package fr.openent.lystore.export.validOrders;

import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.helpers.ExportHelper;
import fr.openent.lystore.export.validOrders.BC.*;
import fr.openent.lystore.export.validOrders.listLycee.ListLycWithPrice;
import fr.openent.lystore.export.validOrders.listLycee.ListLycee;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.ServiceFactory;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.entcore.common.storage.Storage;

import java.util.Map;

public class ValidOrders extends ExportObject {
    private String bcNumber;
    private String numberValidation = "";
    private JsonObject params;
    private Logger log = LoggerFactory.getLogger(ValidOrders.class);
    private JsonObject config;
    private Vertx vertx;
    private EventBus eb;
    private Storage storage;
    ServiceFactory serviceFactory;

    public ValidOrders(ExportService exportService, String idNewFile, EventBus eb, Vertx vertx, JsonObject config, Storage storage,
                       ServiceFactory serviceFactory) {
        super(exportService, idNewFile);
        this.vertx = vertx;
        this.config = config;
        this.eb = eb;
        this.storage = storage;
        this.serviceFactory = serviceFactory;
    }

    public ValidOrders(ExportService exportService, String param, String idNewFile, EventBus eb, Vertx vertx,
                       JsonObject config, boolean HasNumberValidation, Storage storage, ServiceFactory serviceFactory) {
        this(exportService, idNewFile, eb, vertx, config, storage,serviceFactory);
        if (HasNumberValidation)
            this.numberValidation = param;
        else
            this.bcNumber = param;

    }

    public ValidOrders(ExportService exportService, JsonObject params, String idNewFile, EventBus eb,
                       Vertx vertx, JsonObject config, Storage storage , ServiceFactory serviceFactory) {
        this(exportService, idNewFile, eb, vertx, config, storage,serviceFactory);
        this.params = params;
    }

    public void exportListLycee(Handler<Either<String, Buffer>> handler) {
        if (this.numberValidation == null || this.numberValidation.equals("")) {
            ExportHelper.catchError(exportService, idFile, "number validation is not nullable");
            handler.handle(new Either.Left<>("number validation is not nullable"));
        }
        getStructures().onSuccess(structures -> {
            Map<String, JsonObject> structuresMap = getStructureMap(structures);
            Workbook workbook = new XSSFWorkbook();
            new ListLycee(workbook, this.numberValidation, structuresMap).create()
                    .compose(c -> new ListLycWithPrice(workbook, this.numberValidation, structuresMap).create())
                    .onSuccess(getFinalHandler(handler, workbook)
                    ).onFailure(failure -> {
                handler.handle(new Either.Left<>("Error when resolving futures : " + failure.getMessage()));
            }).onFailure(f -> {
                handler.handle(new Either.Left<>(f.getMessage() + " getting neo"));
            });

        });
    }


    public void exportBC(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        } else {
            new BCExport(eb, vertx, config, storage, serviceFactory).create(params.getJsonArray("numberValidations"), handler);
        }
    }


    public void exportBCDuringValidation(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        } else {
            new BCExportDuringValidation(eb, vertx, config, storage, serviceFactory).create(params, handler);
        }

    }

    public void exportBCAfterValidationByStructures(Handler<Either<String, Buffer>> handler) {

        if (this.bcNumber == null || this.bcNumber.isEmpty()) {

            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        } else {
            new BCExportAfterValidationStructure(eb, vertx, config, storage, serviceFactory).create(bcNumber, handler);
        }
    }

    public void exportBCBeforeValidationByStructures(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        } else {
            new BCExportBeforeValidationStructure(eb, vertx, config, storage, serviceFactory)
                    .create(params.getJsonArray("numberValidations"), handler);
        }
    }

    public void exportBCAfterValidation(Handler<Either<String, Buffer>> handler) {
        if (this.bcNumber == null || this.bcNumber.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        } else {
            new BCExportAfterValidation(eb, vertx, config, storage, serviceFactory).create(bcNumber, handler);
        }
    }
}
