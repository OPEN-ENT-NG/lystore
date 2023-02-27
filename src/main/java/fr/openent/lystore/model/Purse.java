package fr.openent.lystore.model;

import io.vertx.core.json.JsonObject;

public class Purse extends Model{
    private double amount;
    private double initialAmount;
    private Campaign campaign;
    private Structure structure;

    public Purse() {
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getInitialAmount() {
        return initialAmount;
    }

    public void setInitialAmount(double initialAmount) {
        this.initialAmount = initialAmount;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
