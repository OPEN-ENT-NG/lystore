package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class BCOrder extends  Model{

    String engagementNumber;
    String labelProgram;
    String number;
    String dateCreation;
    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public String getEngagementNumber() {
        return engagementNumber;
    }

    public void setEngagementNumber(String engagementNumber) {
        this.engagementNumber = engagementNumber;
    }

    public String getLabelProgram() {
        return labelProgram;
    }

    public void setLabelProgram(String labelProgram) {
        this.labelProgram = labelProgram;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }
}
