package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class Instruction extends Model {
    String exercise;
    String object;
    String service_number;
    Boolean submittedToCp;
    String date_cp;
    String cp_number;
    String comment;

    public String getExercise() {
        return exercise;
    }

    public void setExercise(String exercise) {
        this.exercise = exercise;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getService_number() {
        return service_number;
    }

    public void setService_number(String service_number) {
        this.service_number = service_number;
    }

    public Boolean getSubmittedToCp() {
        return submittedToCp;
    }

    public void setSubmittedToCp(Boolean submittedToCp) {
        this.submittedToCp = submittedToCp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCp_number() {
        return cp_number;
    }

    public void setCp_number(String cp_number) {
        this.cp_number = cp_number;
    }

    public String getDate_cp() {
        return date_cp;
    }

    public void setDate_cp(String date_cp) {
        this.date_cp = date_cp;
    }



    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
