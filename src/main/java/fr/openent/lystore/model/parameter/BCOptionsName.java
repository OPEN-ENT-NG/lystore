package fr.openent.lystore.model.parameter;

import io.vertx.core.json.JsonObject;

import static fr.openent.lystore.constants.ParametersConstants.LINE3;
import static fr.openent.lystore.constants.ParametersConstants.LINE4;

public class BCOptionsName extends BCOptionsFormData {
    String line3;
    String line4;

    public String getLine3() {
        return line3;
    }

    public void setLine3(String line3) {
        this.line3 = line3;
    }

    public String getLine4() {
        return line4;
    }

    public void setLine4(String line4) {
        this.line4 = line4;
    }

    @Override
    public JsonObject toJson() {
        return super.toJson().put(LINE3, line3).put(LINE4, line4);
    }
}