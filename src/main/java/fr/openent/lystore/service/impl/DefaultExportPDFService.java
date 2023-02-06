package fr.openent.lystore.service.impl;

import fr.openent.lystore.constants.ExportConstants;
import fr.openent.lystore.service.ExportPDFService;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import fr.wseduc.webutils.data.FileResolver;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import static fr.wseduc.webutils.http.Renders.badRequest;
import static fr.wseduc.webutils.http.Renders.getScheme;



public class DefaultExportPDFService  implements ExportPDFService {
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);
    private String node;
    private JsonObject config;
    private Vertx vertx;
    private EventBus eb;
    private Renders renders ;

    public DefaultExportPDFService(EventBus eb, Vertx vertx, JsonObject config) {
        super();
        this.eb = eb;
        this.config = config;
        this.vertx = vertx;
        this.renders = new Renders(this.vertx, config);
    }

    public void generatePDF(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                            final String prefixPdfName,  final Handler<Buffer> handler) {

        final JsonObject exportConfig = config.getJsonObject("exports");
        final String templatePath = exportConfig.getString("template-path");
        final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) +
                config.getString("app-address") + "/public/";

        node = (String) vertx.sharedData().getLocalMap("server").get("node");
        if (node == null) {
            node = "";
        }

        final String path = FileResolver.absolutePath(templatePath + templateName);

        vertx.fileSystem().readFile(path, result -> {
            if (!result.succeeded()) {
                badRequest(request);
                return;
            }

            StringReader reader = new StringReader(result.result().toString(ExportConstants.UTF_8));
            renders.processTemplate(request, templateProps, templateName, reader, writer -> {
                String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                if (processedTemplate == null) {
                    badRequest(request);
                    return;
                }
                JsonObject actionObject = new JsonObject();
                byte[] bytes;
                try {
                    bytes = processedTemplate.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    bytes = processedTemplate.getBytes();
                    LOGGER.error(e.getMessage(), e);
                }

                actionObject
                        .put("content", bytes)
                        .put("baseUrl", baseUrl);
                eb.send(node + "entcore.pdf.generator", actionObject, (Handler<AsyncResult<Message<JsonObject>>>) reply -> {
                    JsonObject pdfResponse = reply.result().body();
                    if (!"ok".equals(pdfResponse.getString("status"))) {
                        badRequest(request, pdfResponse.getString("message"));
                        return;
                    }
                    byte[] pdf = pdfResponse.getBinary("content");
                    Buffer either = Buffer.buffer(pdf);
                    handler.handle(either);
                });
            });

        });

    }
}
