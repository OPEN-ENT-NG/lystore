package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class Operation extends Model {

    String label ;
    String status;
    Instruction instruction;
    String date_operation ;// mettre DAte plus tard

    public Operation(){

    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
