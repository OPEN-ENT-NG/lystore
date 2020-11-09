package fr.openent.lystore.export.campaign;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.campaign.extractionOrder.ExtractionOrder;
import fr.openent.lystore.export.campaign.extractionOrder.RecapStructOrder;
import fr.openent.lystore.export.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.DefaultSupplierService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

public class CampaignExport extends ExportObject {
    private JsonObject params;
    private ExportService exportService;
    private Logger log = LoggerFactory.getLogger(fr.openent.lystore.export.validOrders.ValidOrders.class);
    private String idFile;
    private Integer id;
    private List<Integer> ids;
    private SupplierService supplierService;

    public CampaignExport(ExportService exportService, String idNewFile, Integer id, List<Integer> ids){
        super(exportService,idNewFile);
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
        this.id = id;
        this.ids = ids;
        log.info("id CAMPAGNE "+ ids);
    }


    public void exportOrders(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExportHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }else{
            Workbook workbook = new XSSFWorkbook();
            List<Future> futures = new ArrayList<>();
            Future<Boolean> ExtractionFuture = Future.future();
            Future<Boolean> RecapListFuture = Future.future();

            futures.add(ExtractionFuture);
            futures.add(RecapListFuture);
            futureHandler(handler, workbook, futures);
            new ExtractionOrder(workbook,this.ids).create(getHandler(ExtractionFuture));
            new RecapStructOrder(workbook,this.ids).create(getHandler(RecapListFuture));

        }
    }

}
