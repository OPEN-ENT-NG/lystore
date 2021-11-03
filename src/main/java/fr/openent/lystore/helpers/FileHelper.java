package fr.openent.lystore.helpers;

import fr.openent.lystore.model.file.Attachment;
import fr.openent.lystore.model.file.Metadata;
import fr.wseduc.swift.utils.FileUtils;
import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class FileHelper {
    private static final Logger log = LoggerFactory.getLogger(FileHelper.class);

    private FileHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * This method will fetch all uploaded files from your {@link HttpServerRequest} request and upload them into your
     * storage and return each of them an object {@link Attachment}
     * <p>
     *  <b>WARNING</b><br/>
     *  Must SPECIFY a custom header where you can define a number to decide whether or not your upload should finish
     *  and complete (e.g adding "Files" as custom header as key and its value the number of file to loop/fetch will
     *  allow your uploadHandler to trigger n callback with different upload object)
     * </p>
     *
     * @param headerCount   the name of your header used to fetch your total number file
     * @param request       request HttpServerRequest
     * @param storage       Storage vertx
     * @param fileSystem    FileSystem vertx
     *
     * @return list of {@link Attachment} (and all your files will be uploaded)
     */
    public static Future<List<Attachment>> uploadMultipleFiles(String headerCount, HttpServerRequest request, Storage storage,
                                                               FileSystem fileSystem) {
        Promise<List<Attachment>> promise = Promise.promise();
        String totalFilesToUpload = request.getHeader(headerCount);
        AtomicInteger incrementFile = new AtomicInteger(0);
        List<Attachment> listMetadata = new ArrayList<>();

        request.setExpectMultipart(true);
        request.exceptionHandler(event -> {
            String message = String.format("[Lystore@%s::uploadMultipleFiles] An error has occurred during http request process: %s",
                    FileHelper.class.getSimpleName(), event.getMessage());
            log.error(message, event);
            promise.fail(event.getMessage());
        });

        request.uploadHandler(upload -> {
            final String id = UUID.randomUUID().toString();
            final JsonObject metadata = FileUtils.metadata(upload);
            final String path;
            listMetadata.add(new Attachment(id, new Metadata(metadata)));
            try {
                path = getFilePath(id, storage.getBucket());
                mkdirsIfNotExists(fileSystem, path, directory -> {
                    if (directory.failed()) {
                        String message = String.format("[Lystore@%s::uploadMultipleFiles] Failed to create directory in storage: %s",
                                FileHelper.class.getSimpleName(), directory.cause().getMessage());
                        log.error(message, directory.cause());
                        promise.fail(message);
                        return;
                    }
                    upload.streamToFileSystem(path);
                    incrementFile.set(incrementFile.get() + 1);
                });
            } catch (FileNotFoundException e) {
                log.error(e.getMessage(), e);
                promise.fail(e.getMessage());
                return;
            }

            upload.exceptionHandler(err -> {
                String message = String.format("[Lystore@%s::uploadMultipleFiles] An exception has occurred during http upload process: %s",
                        FileHelper.class.getSimpleName(), err.getMessage());
                log.error(message, err);
                promise.fail(message);
            });
            upload.endHandler(aVoid -> {
                if (incrementFile.get() == Integer.parseInt(totalFilesToUpload)) {
                    promise.complete(listMetadata);
                }
            });

        });

        return promise.future();
    }

    private static void mkdirsIfNotExists(FileSystem fileSystem, String path, final Handler<AsyncResult<Void>> handler) {
        final String dir = org.entcore.common.utils.FileUtils.getParentPath(path);
        fileSystem.exists(dir, event -> {
            if (event.succeeded()) {
                if (Boolean.FALSE.equals(event.result())) {
                    fileSystem.mkdirs(dir, handler);
                } else {
                    handler.handle(new DefaultAsyncResult<>((Void) null));
                }
            } else {
                handler.handle(new DefaultAsyncResult<>(event.cause()));
            }
        });
    }

    private static String getFilePath(String file, final String bucket) throws FileNotFoundException {
        if (isNotEmpty(file)) {
            final int startIdx = file.lastIndexOf(File.separatorChar) + 1;
            final int extIdx = file.lastIndexOf('.');
            String filename = (extIdx > 0) ? file.substring(startIdx, extIdx) : file.substring(startIdx);
            if (isNotEmpty(filename)) {
                final int l = filename.length();
                if (l < 4) {
                    filename = "0000".substring(0, 4 - l) + filename;
                }
                return bucket + filename.substring(l - 2) + File.separator + filename.substring(l - 4, l - 2) +
                        File.separator + filename;
            }

        }
        throw new FileNotFoundException("Invalid file : " + file);
    }

}
