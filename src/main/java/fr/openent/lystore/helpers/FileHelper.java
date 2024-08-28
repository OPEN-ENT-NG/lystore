package fr.openent.lystore.helpers;

import fr.openent.lystore.model.file.Attachment;
import fr.openent.lystore.model.file.Metadata;
import fr.wseduc.webutils.DefaultAsyncResult;
import io.vertx.core.*;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.storage.Storage;
import org.entcore.common.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
     * @param vertx    Vertx vertx
     *
     * @return list of {@link Attachment} (and all your files will be uploaded)
     * (process will continue in background to stream all these files in your storage)
     */
    public static Future<List<Attachment>> uploadMultipleFiles(String headerCount, HttpServerRequest request, Storage storage,
                                                               Vertx vertx, JsonObject config) {
        request.response().setChunked(true);
        request.setExpectMultipart(true);
        Promise<List<Attachment>> promise = Promise.promise();
        String totalFilesToUpload = request.getHeader(headerCount);
        AtomicBoolean responseSent= new AtomicBoolean();
        responseSent.set(false);

        List<String> fileIds = new ArrayList<>();
        AtomicReference<List<String>> pathIds= new AtomicReference<>();
        for(int i = 0 ; i< Integer.parseInt(totalFilesToUpload);i++){
            fileIds.add(UUID.randomUUID().toString());
        }
        String path  = " ";

        // return empty arrayList if no header is sent (meaning no files to upload)
        if (totalFilesToUpload == null || totalFilesToUpload.isEmpty() || Integer.parseInt(totalFilesToUpload) == 0) {
            promise.complete(new ArrayList<>());
            return promise.future();
        }
        AtomicInteger incrementFile = new AtomicInteger(0);
        List<Attachment> listMetadata = new ArrayList<>();
        request.exceptionHandler(event -> {
            String message = String.format("[Lystore@%s::uploadMultipleFiles] An error has occurred during http request process: %s",
                    FileHelper.class.getSimpleName(), event.getMessage());
            log.error(message, event);
            promise.fail(event.getMessage());
        });

        request.pause();
        request.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                promise.fail(event.getMessage());
            }
        });

        request.uploadHandler(upload -> {
            String finalPath = pathIds.get().get(incrementFile.get());
            final JsonObject metadata = FileUtils.metadata(upload);
            listMetadata.add(new Attachment(fileIds.get(incrementFile.get()), new Metadata(metadata)));
            upload.streamToFileSystem(finalPath);
            incrementFile.set(incrementFile.get() + 1);




            upload.exceptionHandler(err -> {
                String message = String.format("[Lystore@%s::uploadMultipleFiles] An exception has occurred during http upload process: %s",
                        FileHelper.class.getSimpleName(), err.getMessage());
                log.error(message, err);
                promise.fail(message);
            });
            upload.endHandler(aVoid -> {
                if (incrementFile.get() == Integer.parseInt(totalFilesToUpload) && !responseSent.get()) {
                    responseSent.set(true);
                    for(Attachment at : listMetadata){
                        log.info(at.id());
                    }
                    promise.complete(listMetadata);
                }
            });

        });
        request.endHandler(end ->{
            log.info(request.formAttributes().entries());
        });

        List<Future<String>> makeFolders = new ArrayList<>();

        for(int i=0;i< fileIds.size();i++) {
            makeFolders.add(makeFolder( storage, vertx,fileIds, path, i));
        }

        FutureHelper.all(makeFolders).onSuccess(success ->{
            pathIds.set(success.list());
            request.resume();
        }).onFailure(failure -> promise.fail(failure.getMessage()));

        return promise.future();
    }

    private static Future<String> makeFolder( Storage storage, Vertx vertx, List<String> fileIds, String path, int i) {
        Promise<String> promise = Promise.promise();
        try {
            path = getFilePath(fileIds.get(i), storage.getBucket());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String finalPath = path;
        mkdirsIfNotExists(vertx.fileSystem(), path, event -> {
            if (event.succeeded()) {
                promise.complete(finalPath);
            } else {
                promise.fail("mkdir.error: ");
            }
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

    public static Future<ArrayList<String>> getCopyOldFiles(Map<String, String> namesAndFiles, Storage storage) {
        Promise<ArrayList<String>> promise = Promise.promise();

        List<Future<String>> copyIds = new ArrayList<>();

        for (Map.Entry<String, String> entry : namesAndFiles.entrySet()) {
            copyIds.add(copyFile(entry.getKey(),entry.getValue(),storage));
        }
        FutureHelper.all(copyIds).onSuccess(success ->{
            ArrayList<String> ids = new ArrayList<>();
            for(Object idObject: success.list()){
                ids.add(idObject.toString());
            }
            promise.complete(ids);
        }).onFailure(failure -> promise.fail(failure.getMessage()));

        return promise.future();
    }

    private static Future<String> deleteFile(String id, Storage storage) {
        Promise<String> promise = Promise.promise();
        storage.removeFile(id,  event -> {
            if (event.getString("status").equals("ok")) {
                promise.complete("ok");
            } else {
                promise.fail("error when copying file");
            }
        });
        return promise.future();
    }

    private static Future<String> copyFile( String filename,String id,Storage storage) {
        log.info(id);
        Promise<String> promise = Promise.promise();
        storage.copyFile(id,  event -> {
            if (event.getString("status").equals("ok")) {
                log.info(event.getString("_id"));
                promise.complete(filename + "/" +event.getString("_id"));
            } else {
                promise.fail("error when copying file");
            }
        });
        return promise.future();
    }

    public static Future<String> deleteFiles(List<String> idsFiles, Storage storage) {
        Promise<String> promise = Promise.promise();
        List<Future<String>> removeIdsFutures = new ArrayList<>();

        for (String id : idsFiles) {
            removeIdsFutures.add(deleteFile(id,storage));
        }
        FutureHelper.all(removeIdsFutures).onSuccess(success ->{
            promise.complete("ok");
        }).onFailure(failure -> {
            promise.fail(failure.getMessage());
        });

        return promise.future();
    }
}
