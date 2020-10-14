package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class AccountingProgramAction extends Model {
    String description;
    String number;
    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
