package fr.openent.lystore.service.impl;

import fr.openent.lystore.constants.ExportConstants;
import fr.openent.lystore.service.ExportPDFService;
import fr.openent.lystore.utils.LystoreUtils;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.pdf.PdfFactory;
import org.entcore.common.pdf.PdfGenerator;

import java.io.StringReader;
import java.io.StringWriter;

import static fr.openent.lystore.constants.ExportConstants.NODE_PDF_GENERATOR;
import static fr.wseduc.webutils.http.Renders.badRequest;


public class DefaultExportPDFService implements ExportPDFService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderService.class);
    private JsonObject config;
    private Vertx vertx;
    private Renders renders;
    private final PdfFactory pdfFactory;

    public DefaultExportPDFService(Vertx vertx, JsonObject config) {
        super();
        this.config = config;
        this.vertx = vertx;
        this.renders = new Renders(this.vertx, config);
        pdfFactory = new PdfFactory(vertx, new JsonObject().put(NODE_PDF_GENERATOR,
                config.getJsonObject(NODE_PDF_GENERATOR, new JsonObject())));

    }

    public void generatePDF(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                            final String prefixPdfName, final Handler<Buffer> handler) {

        final JsonObject exportConfig = config.getJsonObject("exports");
        final String templatePath = exportConfig.getString("template-path");

        final String path = FileResolver.absolutePath(templatePath + templateName);

        vertx.fileSystem().readFile(path, result -> {
            if (!result.succeeded()) {
                badRequest(request);
                return;
            }
            StringReader reader = new StringReader(result.result().toString(ExportConstants.UTF_8));
            renders.processTemplate(request, templateProps, templateName, reader, writer -> {
                String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                if (processedTemplate.isEmpty()) {
                    badRequest(request);
                    LOGGER.error(LystoreUtils.generateErrorMessage(DefaultExportPDFService.class, "generatePDF", "Processed Template is empty", ""));
                    return;
                }
                PdfGenerator pdfGenerator;
                try {
                    pdfGenerator = pdfFactory.getPdfGenerator();
                    pdfGenerator.generatePdfFromTemplate("", processedTemplate)
                            .onSuccess(pdf -> handler.handle(pdf.getContent()))
                            .onFailure(error -> {
                                badRequest(request, error.getMessage());
                                LOGGER.error(LystoreUtils.generateErrorMessage(DefaultExportPDFService.class, "generatePDF", error.getMessage(), "error when generatePdfFromTemplate"));
                            });
                } catch (Exception exception) {
                    LOGGER.error(LystoreUtils.generateErrorMessage(DefaultExportPDFService.class, "generatePDF", exception.getMessage(), "PdfGenerator is null"));
                    badRequest(request, exception.getMessage());
                }
            });

        });

    }
}
