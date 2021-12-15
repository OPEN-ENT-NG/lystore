package fr.openent.lystore.service;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.impl.DefaultCampaignService;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.sql.Sql;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultCampaignServiceTest {

    private Vertx vertx;
    private final Sql sql = mock(Sql.class);
    private CampaignService campaignService;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.lystore");
        this.campaignService = new DefaultCampaignService(Lystore.lystoreSchema, "campaign");
    }

    @Test
    public void getCampaignsPursesWithSql_Should_Get_Correct_Data_Into_SQLPrepare(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "SELECT SUM(amount) as purse, purse.id_campaign " +
                "FROM " + Lystore.lystoreSchema + ".purse " +
                "GROUP BY id_campaign;";

        JsonArray expectedParams = new JsonArray();

        vertx.eventBus().consumer("fr.openent.lystore", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        try {
            Whitebox.invokeMethod(this.campaignService, "getCampaignsPurses",(Handler) e -> {

            });
        } catch (Exception e) {
            ctx.assertNotNull(e);
        }
    }

    @Test
    public void getCampaignStructures_Should_Get_Correct_Data_Into_SQLPrepare(TestContext ctx) {
        String expectedQuery = "SELECT distinct id_structure FROM lystore.campaign " +
                "INNER JOIN lystore.rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN lystore.rel_group_structure " +
                "ON (rel_group_structure.id_structure_group = rel_group_campaign.id_structure_group) " +
                "WHERE campaign.id = ?;";

        JsonArray expectedParams = new JsonArray().add(5);
        Mockito.doAnswer((Answer<Void>) invocation -> {
            String queryResult = invocation.getArgument(0);
            JsonArray paramsResult = invocation.getArgument(1);
            ctx.assertEquals(queryResult, expectedQuery);
            ctx.assertEquals(paramsResult.toString(), expectedParams.toString());
            return null;
        }).when(sql).prepared(Mockito.anyString(), Mockito.any(JsonArray.class), Mockito.any(Handler.class));
        this.campaignService.getCampaignStructures(5, e -> {});
    }
}