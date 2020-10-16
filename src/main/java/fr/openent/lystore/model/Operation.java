package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class Operation extends Model {

    String label ;
    boolean status;
    Instruction instruction;
    boolean hasInstruction = false;
    String date_operation ;// mettre DAte plus tard

    public Operation(){

    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public void setInstruction(Instruction instruction) {
        hasInstruction = true;
        this.instruction = instruction;
    }

    public String getDate_operation() {
        return date_operation;
    }

    public void setDate_operation(String date_operation) {
        this.date_operation = date_operation;
    }


    public boolean hasInstruction() {
        return hasInstruction;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
