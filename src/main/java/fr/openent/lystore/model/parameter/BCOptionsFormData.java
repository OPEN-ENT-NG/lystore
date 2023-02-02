package fr.openent.lystore.model.parameter;

import io.vertx.core.json.JsonObject;

import javax.swing.*;

import static fr.openent.lystore.constants.ParametersConstants.LINE1;
import static fr.openent.lystore.constants.ParametersConstants.LINE2;

public class BCOptionsFormData {
    String line1;
    String line2;


    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public JsonObject toJson(){
        return new JsonObject().put(LINE1, line1)
                .put(LINE2, line2);
    }
}
