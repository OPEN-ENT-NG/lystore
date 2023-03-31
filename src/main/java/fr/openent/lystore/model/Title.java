package fr.openent.lystore.model;

import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.LystoreBDD;
import io.vertx.core.json.JsonObject;

public class Title {
    private int id;
    private String name ;

    public Title() {
    }

    public Title(JsonObject params) {
        this.set(params);
    }

    public Title set (JsonObject params){
        this.setId(params.getInteger(CommonConstants.ID));
        this.setName(params.getString(LystoreBDD.NAME));
        return this;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
