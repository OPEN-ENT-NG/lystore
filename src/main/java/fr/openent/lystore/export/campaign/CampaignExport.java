package fr.openent.lystore.export.campaign;

import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.campaign.extractionOrder.ExtractionOrder;
import fr.openent.lystore.export.campaign.extractionOrder.RecapStructOrder;
import fr.openent.lystore.export.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.List;
import java.util.Map;

public class CampaignExport extends ExportObject {
    private final Logger log = LoggerFactory.getLogger(CampaignExport.class);
    private final Integer id;
    private final List<Integer> ids;
    private final String regionTypeName;


    public CampaignExport(ExportService exportService, String idNewFile, Integer id, List<Integer> ids, String regionTypeName){
        super(exportService,idNewFile);
        this.id = id;
        this.ids = ids;
        this.regionTypeName = regionTypeName;
        log.info("id CAMPAGNE "+ ids);
    }


    public void exportOrders(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExportHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }else{
            Workbook workbook = new XSSFWorkbook();
            getStructures().onSuccess( structures -> {
                Map<String, JsonObject> structuresMap = getStructureMap(structures);
                new ExtractionOrder(workbook, this.ids,structuresMap, regionTypeName).create()
                        .compose(LB -> new RecapStructOrder(workbook, this.ids, structuresMap, regionTypeName).create())
                        .onSuccess(getFinalHandler(handler, workbook)
                        ).onFailure(failure ->{
                    handler.handle(new Either.Left<>("Error when resolving futures : " + failure.getMessage()));
                });

            }).onFailure( f->{
                handler.handle(new Either.Left<>(f.getMessage()+ " getting neo"));
            });
        }
    }

}
