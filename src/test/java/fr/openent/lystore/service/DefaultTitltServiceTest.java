package fr.openent.lystore.service;

import com.redis.S;
import fr.openent.lystore.Lystore;
import fr.openent.lystore.model.Structure;
import fr.openent.lystore.model.Title;
import fr.openent.lystore.service.impl.DefaultPurseService;
import fr.openent.lystore.service.impl.DefaultTitleService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.poi.ss.formula.functions.T;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultTitltServiceTest {
    private Vertx vertx;
    private final Sql sql = mock(Sql.class);
    private TitleService titleService;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.lystore");
        this.titleService = new DefaultTitleService(Lystore.lystoreSchema, "title");
    }

    @Test
    public void getDeletionStatement_Should_Delete_Correct_Data_Into_SQLStatement(TestContext ctx) throws Exception {
        // expected data
        String expectedQuery = "DELETE FROM " + Lystore.lystoreSchema + ".rel_title_campaign_structure " +
                "WHERE id_campaign = ? " +
                "AND id_title = ? " +
                "AND id_structure = ?;";

        JsonArray expectedParams = new JsonArray("[1,2,\"5\"]");
        JsonObject body =  Whitebox.invokeMethod(titleService, "getDeletionStatement",1,2,"5");
        ctx.assertEquals(body.getString("statement"),expectedQuery);
        ctx.assertEquals(body.getJsonArray("params"),expectedParams);
        ctx.assertEquals(body.getString("action"),"prepared");
    }
}
