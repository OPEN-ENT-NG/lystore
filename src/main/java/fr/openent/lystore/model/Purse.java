package fr.openent.lystore.model;

import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.utils.OrderUtils;
import io.vertx.core.json.JsonObject;

public class Purse {
    private double amount;
    private double initialAmount;
    private Campaign campaign;
    private Structure structure;
    private double totalOrder;
    private Integer id;
    public Purse(JsonObject purseJO) {
        this.build(purseJO);
    }

    private void build(JsonObject purseJO) {
        this.id = purseJO.getInteger(CommonConstants.ID);
        this.amount = OrderUtils.safeGetDouble(purseJO, LystoreBDD.AMOUNT);
        this.initialAmount = OrderUtils.safeGetDouble(purseJO, LystoreBDD.INITIAL_AMOUNT);
        this.campaign = new Campaign();
        campaign.setId(purseJO.getValue(LystoreBDD.ID_CAMPAIGN).toString());
        this.structure = new Structure();
        this.structure.setId(purseJO.getString(LystoreBDD.ID_STRUCTURE));
        this.totalOrder = OrderUtils.safeGetDouble(purseJO, LystoreBDD.TOTAL_ORDER);
    }

    public double getTotalOrder() {
        return totalOrder;
    }

    public void setTotalOrder(double totalOrder) {
        this.totalOrder = totalOrder;
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


    public JsonObject toJsonObject() {
        return new JsonObject()
                .put(CommonConstants.ID,id)
                .put(LystoreBDD.AMOUNT,amount)
                .put(LystoreBDD.INITIAL_AMOUNT,initialAmount)
                .put(LystoreBDD.ID_CAMPAIGN, Integer.parseInt(campaign.getId()))
                .put(LystoreBDD.ID_STRUCTURE, structure.getId())
                .put(LystoreBDD.TOTAL_ORDER , totalOrder);
    }
}
