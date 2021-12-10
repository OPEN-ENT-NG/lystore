package fr.openent.lystore.helpers;

import fr.openent.lystore.model.file.Attachment;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class AttachmentHelper {
    public static List<JsonObject> attachmentsToJsonArray(List<Attachment> attachments) {
        return attachments.stream().map(Attachment::toJson).collect(Collectors.toList());
    }
}
