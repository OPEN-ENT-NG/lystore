package fr.openent.lystore.model;

import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.utils.OrderUtils;
import io.vertx.core.json.JsonObject;

public class Purse extends Model{
    private double amount;
    private double initialAmount;
    private Campaign campaign;
    private Structure structure;
    private double totalOrder;

    public Purse(JsonObject purseJO) {
        this.build(purseJO);
    }

    private void build(JsonObject purseJO) {
        this.id = purseJO.getValue(CommonConstants.ID).toString();
        this.amount = OrderUtils.safeGetDouble(purseJO, "amount");
        this.initialAmount = OrderUtils.safeGetDouble(purseJO, "initial_amount");
        this.campaign = new Campaign();
        campaign.setId(purseJO.getValue("id_campaign").toString());
        this.structure = new Structure();
        this.structure.setId(purseJO.getString("id_structure"));
        this.totalOrder = OrderUtils.safeGetDouble(purseJO, "total_order");
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

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
