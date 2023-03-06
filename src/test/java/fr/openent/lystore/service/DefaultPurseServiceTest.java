package fr.openent.lystore.service;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.impl.DefaultPurseService;
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
import org.powermock.reflect.Whitebox;

import static org.mockito.Mockito.mock;

@RunWith(VertxUnitRunner.class)
public class DefaultPurseServiceTest {
    private Vertx vertx;
    private final Sql sql = mock(Sql.class);
    private PurseService purseService;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        Sql.getInstance().init(vertx.eventBus(), "fr.openent.lystore");
        this.purseService = new DefaultPurseService();
    }

    @Test
    public void getPursesByCampaignIdWithSql_Should_Get_Correct_Data_With_ID_Into_SQLPrepare(TestContext ctx) {
        Async async = ctx.async();

        String expectedQuery = "   " +
                " WITH orders as (SELECT oce.id_structure, " +
                "               oce.id_campaign, " +
                "               Sum(( (SELECT CASE " +
                "                               WHEN oce.price_proposal IS NOT NULL THEN 0 " +
                "                               WHEN Sum(oco.price + ( oco.price * oco.tax_amount " +
                "                                                      / 100 " +
                "                                                    ) * " +
                "                                                    oco.amount " +
                "                                    ) IS " +
                "                                    NULL THEN 0 " +
                "                               ELSE Sum(Round(oco.price + ( " +
                "                                              oco.price * oco.tax_amount " +
                "                                              / 100 ) " +
                "                                                          * " +
                "                                        oco.amount, 2)) " +
                "                             END " +
                "                      FROM     " + Lystore.lystoreSchema + ".order_client_options oco " +
                "                      WHERE  oco.id_order_client_equipment = oce.id) " +
                "                     + Round((oce.price + oce.price * oce.tax_amount /100), 2) ) " +
                "                   * " +
                "                   oce.amount) " +
                "               AS total_order " +
                "        FROM     " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "               INNER JOIN   " + Lystore.lystoreSchema + ".purse " +
                "                       ON purse.id_campaign = oce.id_campaign " +
                "                          AND oce.id_structure = purse.id_structure " +
                "        GROUP  BY oce.id_structure, " +
                "                  oce.id_campaign " +
                "        ORDER  BY id_structure) " +
                "SELECT purse.*, " +
                "       orders.total_order " +
                "FROM      " + Lystore.lystoreSchema + ".purse " +
                "left join orders " +
                "               ON orders.id_structure = purse.id_structure AND  orders.id_campaign = purse.id_campaign " +
                "WHERE  " +
                "        purse.id_campaign = ? ;  " ;

        JsonArray expectedParams = new JsonArray();

        vertx.eventBus().consumer("fr.openent.lystore", message -> {
            JsonObject body = (JsonObject) message.body();
            ctx.assertEquals("prepared", body.getString("action"));
            ctx.assertEquals(expectedQuery, body.getString("statement"));
            ctx.assertEquals(expectedParams.toString(), body.getJsonArray("values").toString());
            async.complete();
        });

        try {
            Whitebox.invokeMethod(this.purseService, "getPursesByCampaignId",(Handler) e -> {
            });
        } catch (Exception e) {
            ctx.assertNotNull(e);
        }
    }

}
