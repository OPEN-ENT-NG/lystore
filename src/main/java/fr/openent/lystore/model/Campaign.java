package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

/**
 * Model Class for campaign
 */
public class Campaign extends Model {

    private boolean open;
    private boolean hasPurse = false;
    private Double purse;
    private String startDate;
    private String endDate;
    private String name;

    @Override
    public JsonObject toJsonObject() {
        return null;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Double getPurse() {
        return purse;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPurse(Double purse) {
        this.setHasPurse(true);
        this.purse = purse;
    }

    public void setHasPurse(boolean hasPurse) {
        this.hasPurse = hasPurse;
    }

    public boolean hasPurse() {
        return hasPurse;
    }

    public String getStartDate() {
        if(startDate != null) {
            return startDate;
        }else {
            return "";
        }
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        if(endDate != null) {
            return endDate;
        }else {
            return "";
        }
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
