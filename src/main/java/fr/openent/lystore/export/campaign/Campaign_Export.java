package fr.openent.lystore.export.campaign;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.validOrders.BC.*;
import fr.openent.lystore.export.validOrders.ValidOrders;
import fr.openent.lystore.export.validOrders.listLycee.ListLycWithPrice;
import fr.openent.lystore.export.validOrders.listLycee.ListLycee;
import fr.openent.lystore.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.DefaultSupplierService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

public class Campaign_Export extends ExportObject {
    private JsonObject params;
    private ExportService exportService;
    private Logger log = LoggerFactory.getLogger(fr.openent.lystore.export.validOrders.ValidOrders.class);
    private String idFile;
    private Integer id;
    private SupplierService supplierService;
    private JsonObject config;
    private Vertx vertx;
    private EventBus eb;

    public Campaign_Export(ExportService exportService, String idNewFile,EventBus eb, Vertx vertx, JsonObject config){
        super(exportService,idNewFile);
        this.vertx = vertx;
        this.config = config;
        this.eb = eb;
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");

    }
    public Campaign_Export(ExportService exportService, Integer param, String idNewFile,EventBus eb, Vertx vertx, JsonObject config) {
        this(exportService,idNewFile,eb,vertx,config);
        this.id = param;
    }




    public void exportOrders(Handler<Either<String, Buffer>> handler) {
        ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
        handler.handle(new Either.Left<>("number validations is not nullable"));
    }
}
