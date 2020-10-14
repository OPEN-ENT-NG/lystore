package fr.openent.lystore.model;

import fr.openent.lystore.controllers.ProjectController;
import io.vertx.core.json.JsonObject;

/**
 * Model Class for orders
 */
public class Order extends Model {
    JsonObject oldJo;
    private boolean hasOperation = false;
    private Operation operation;
    private Structure structure;
    private Campaign campaign;
    private Project project;
    private Double priceHT;
    private Double priceTTC;
    private Double priceProposal;
    private Double tax_amount;
    private Double totalTTC;
    private boolean hasPriceProposal = false;
    private boolean hasRank = false;
    private Boolean overrideRegion;
    private Integer rank;
    private Integer amount;
    private Integer equipment_key;
    private String image;
    private String name;
    private String description;
    private String origin ;
    private String summary;
    private String comment;
    private String program;
    private String status;
    private String action;
    private String creationDate;

    private Market market;
    private AccountingProgram accountingProgram;
    private AccountingProgramAction programAction;

    public Order() {
        super();

    }

    public boolean isHasOperation() {
        return hasOperation;
    }

    public AccountingProgram getAccountingProgram() {
        return accountingProgram;
    }

    public void setAccountingProgram(AccountingProgram accountingProgram) {
        this.accountingProgram = accountingProgram;
    }

    public AccountingProgramAction getProgramAction() {
        return programAction;
    }

    public void setProgramAction(AccountingProgramAction programAction) {
        this.programAction = programAction;
    }

    public JsonObject getOldJo() {
        return oldJo;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public void setOldJo(JsonObject oldJo) {
        this.oldJo = oldJo;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        hasOperation = true;
        this.operation = operation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Structure getStructure() {
        return structure;
    }

    public void setStructure(Structure structure) {
        this.structure = structure;
    }

    public Double getTotalTTC() {
        return totalTTC;
    }

    public void setTotalTTC(Double totalTTC) {
        this.totalTTC = totalTTC;
    }

    public Double getPriceHT() {
        return priceHT;
    }

    public void setPriceHT(Double priceHT) {
        this.priceHT = priceHT;
    }

    public Double getPriceTTC() {
        return priceTTC;
    }

    public void setPriceTTC(Double priceTTC) {
        this.priceTTC = priceTTC;
    }

    public Double getPriceProposal() {
        return priceProposal;
    }

    public void setPriceProposal(Double priceProposal) {
        hasPriceProposal = true;
        this.priceProposal = priceProposal;
    }

    public Double getTax_amount() {
        return tax_amount;
    }

    public void setTax_amount(Double tax_amount) {
        this.tax_amount = tax_amount;
    }

    public Boolean getOverrideRegion() {
        return overrideRegion;
    }

    public void setOverrideRegion(Boolean overrideRegion) {
        this.overrideRegion = overrideRegion;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        hasRank = true;
        this.rank = rank;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getEquipment_key() {
        return equipment_key;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setEquipment_key(Integer equipment_key) {
        this.equipment_key = equipment_key;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public boolean hasPriceProposal() {
        return hasPriceProposal;
    }

    public boolean hasRank() {
        return hasRank;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
