package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class LabelOperation extends Model{
    private String label;
    private String start_date;
    private String end_date;

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject().put("id", this.id).put("label", this.label).put("start_date", this.start_date).put("end_date", this.end_date);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }
}
