package fr.openent.lystore.model.file;

import io.vertx.core.json.JsonObject;

public class Attachment {
    private final String id;
    private final Metadata metadata;

    public Attachment(String id, Metadata metadata) {
        this.id = id;
        this.metadata = metadata;
    }

    public String id() {
        return id;
    }

    public Metadata metadata() {
        return metadata;
    }
    public JsonObject toJson() {
        return new JsonObject().put("id",id).put("metadata",metadata.toJSON());
    }
}