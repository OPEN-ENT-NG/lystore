package fr.openent.lystore.model.file;

public class Attachment {
    private final String id;
    private final Metadata metadata;

    public Attachment(String id, Metadata metadata) {
        this.id = id;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public Metadata getMetadata() {
        return metadata;
    }
}
