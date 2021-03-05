package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.validOrders.PDF_OrderHElper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class BCExportAfterValidation  extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);

    public BCExportAfterValidation(EventBus eb, Vertx vertx, JsonObject config) {
        super(eb, vertx, config);
    }


    public void create(String nbrBc, Handler<Either<String, Buffer>> exportHandler) {
        getOrdersDataSql(nbrBc,new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            JsonArray paramstemp = event.right().getValue();
                            log.info(paramstemp);
                            JsonObject params = paramstemp.getJsonObject(0);
                            final JsonArray ids = new JsonArray();
                            JsonArray idsArray =  new JsonArray(params.getString("ids"));
                            for(int i = 0 ; i < idsArray.size();i++){
                                ids.add(idsArray.getValue(i).toString());
                            }
                            final String nbrEngagement = params.getString("nbr_engagement");
                            final String dateGeneration = params.getString("date_generation");
                            Number supplierId = params.getInteger("supplier_id");
                            getOrdersData(exportHandler, nbrBc, nbrEngagement, dateGeneration, supplierId, ids,false,
                                    new Handler<JsonObject>() {
                                        @Override
                                        public void handle(JsonObject data) {
                                            data.put("print_order", true);
                                            data.put("print_certificates", false);
                                            generatePDF(exportHandler, data,
                                                    "BC.xhtml", "Bon_Commande_",
                                                    new Handler<Buffer>() {
                                                        @Override
                                                        public void handle(final Buffer pdf) {
                                                            exportHandler.handle(new Either.Right<>(pdf));
                                                        }
                                                    }
                                            );
                                        }
                                    });
                        }else{
                            exportHandler.handle(new Either.Left<>("sql failed"));
                        }
                    }
                }

        );

    }

    private void getOrdersDataSql(String nbrbc, Handler<Either<String,JsonArray>> handler) {
        BCExportAfterValidationStructure.getOrdersDataQueryByStructure(nbrbc, handler);
    }
}
