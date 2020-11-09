package fr.openent.lystore.model;

import fr.openent.lystore.controllers.ProjectController;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Model Class for orders
 */
public class Order extends Model {
    JsonObject oldJo;
    private boolean hasOperation = false;
    private Long idOrderClientEquipment;
    private Operation operation;
    private Structure structure;
    private Campaign campaign;
    private Project project;
    private double priceHT;
    private double priceTTC;
    private double priceProposal;
    private double tax_amount;
    private double totalTTC;
    private boolean hasPriceProposal = false;
    private boolean hasRank = false;
    private boolean overrideRegion;
    private int rank;
    private int amount;
    private int equipment_key;
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
    private String numberValidation;
    private boolean hasidOrderClientEquipment = false;
    private boolean atLeastValid = false;
    private boolean hasBC = false;
    private Market market;
    private AccountingProgram accountingProgram;
    private AccountingProgramAction programAction;
    private BCOrder bcOrder;
    private List<String> filenames = new ArrayList<>();
    private List<String> optionsNames = new ArrayList<>();
    private Double optionAmount;
    private  boolean hasOptions;
    public Order() {
        super();

    }

    public boolean hasOperation() {
        return hasOperation;
    }

    public List<String> getFilenames() {
        return filenames;
    }


    public void addFilenames(String filename) {
        filenames.add(filename);
    }

    public boolean hasFilename(){
        return filenames.size() > 0;
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

    public List<String> getOptionsNames() {
        return optionsNames;
    }

    public void setOptionsNames(List<String> optionsNames) {
        this.optionsNames = optionsNames;
    }
    public void addOption(String option){
        this.optionsNames.add(option);
    };
    public Double getOptionAmount() {
        return optionAmount;
    }

    public void setOptionAmount(Double optionAmount) {
        this.optionAmount = optionAmount;
    }

    public boolean hasOptions() {
        return hasOptions;
    }

    public void setHasOptions(boolean hasOptions) {
        this.hasOptions = hasOptions;
    }

    public Market getMarket() {
        return market;
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public String getNumberValidation() {
        return numberValidation;
    }

    public void setNumberValidation(String numberValidation) {
        this.numberValidation = numberValidation;
    }


    public void setHasBC(boolean hasBC) {
        this.hasBC = hasBC;
    }

    public BCOrder getBcOrder() {
        return bcOrder;
    }

    public void setBcOrder(BCOrder bcOrder) {
        this.bcOrder = bcOrder;
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

    public double getTotalTTC() {
        return totalTTC;
    }

    public void setTotalTTC(double totalTTC) {
        this.totalTTC = totalTTC;
    }

    public double getPriceHT() {
        return priceHT;
    }

    public boolean isAtLeastValid() {
        return atLeastValid;
    }

    public void setAtLeastValid(boolean atLeastValid) {
        this.atLeastValid = atLeastValid;
    }

    public boolean hasBC() {
        return hasBC;
    }

    public void hasBC(boolean hasBC) {
        this.hasBC = hasBC;
    }

    public void setPriceHT(double priceHT) {
        this.priceHT = priceHT;
    }

    public double getPriceTTC() {
        return priceTTC;
    }

    public void setPriceTTC(double priceTTC) {
        this.priceTTC = priceTTC;
    }

    public double getPriceProposal() {
        return priceProposal;
    }

    public void setPriceProposal(double priceProposal) {
        hasPriceProposal = true;
        this.priceProposal = priceProposal;
    }

    public double getTax_amount() {
        return tax_amount;
    }

    public void setTax_amount(double tax_amount) {
        this.tax_amount = tax_amount;
    }

    public boolean getOverrideRegion() {
        return overrideRegion;
    }

    public void setOverrideRegion(boolean overrideRegion) {
        this.overrideRegion = overrideRegion;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        hasRank = true;
        this.rank = rank;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getEquipment_key() {
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

    public void setEquipment_key(int equipment_key) {
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

    public Long getIdOrderClientEquipment() {
        return idOrderClientEquipment;
    }

    public void setIdOrderClientEquipment(Long idOrderClientEquipment) {
        hasidOrderClientEquipment = true;
        this.idOrderClientEquipment = idOrderClientEquipment;
    }

    public boolean hasidOrderClientEquipment() {
        return hasidOrderClientEquipment;
    }

    @Override
    public JsonObject toJsonObject() {
        return null;
    }
}
