package fr.openent.lystore.export.campaign.extractionOrder;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.apache.poi.ss.usermodel.Workbook;

public class ExtractionOrder extends TabHelper {
    Integer id_campaign;
    public ExtractionOrder(Workbook workbook, Integer id) {
        super(workbook,"Extraction");
        id_campaign = id;
    }

    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }


    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {

    }
    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT campaign.name as campaign_name " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "WHERE campaign.id = ? " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure " +
                "ON rel_group_structure.id_campaign = campaign.id ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(id_campaign);
        sqlHandler(handler,params);
    }
}
