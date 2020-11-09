package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class Market extends Model{
    String name;
    String market_number;
    String agent;
    String region_supplier;
    String accoutingNature;
    String accoutingCode;
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMarket_number() {
        return market_number;
    }

    public void setMarket_number(String market_number) {
        this.market_number = market_number;
    }

    public String getAccoutingNature() {
        return accoutingNature;
    }

    public void setAccoutingNature(String accoutingNature) {
        this.accoutingNature = accoutingNature;
    }

    public String getAccoutingCode() {
        return accoutingCode;
    }

    public void setAccoutingCode(String accoutingCode) {
        this.accoutingCode = accoutingCode;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public String getRegion_supplier() {
        return region_supplier;
    }

    public void setRegion_supplier(String region_supplier) {
        this.region_supplier = region_supplier;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
