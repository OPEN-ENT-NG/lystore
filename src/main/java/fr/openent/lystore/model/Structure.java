package fr.openent.lystore.model;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.LystoreBDD;
import io.vertx.core.json.JsonObject;

import static fr.openent.lystore.constants.CommonConstants.ID;

public class Structure  extends  Model{


    private String UAI;
    private String name;
    private String academy;
    private String address;
    private String zipCode;
    private String city;
    private String phone;
    private String citeMixte;
    private String type;
    public Structure(){
        super();
    }
    
    public Structure (JsonObject data){
        super();
        this.build(data);
    }

    private void build(JsonObject data) {
        this.setId(data.getString(ID));
        this.setAcademy(data.getString(LystoreBDD.ACADEMY));
        this.setUAI(data.getString(LystoreBDD.UAI));
        this.setType(data.getString(LystoreBDD.TYPE));
        this.setName(data.getString(LystoreBDD.NAME));
        this.setZipCode(data.getString(LystoreBDD.ZIPCODE));
        this.setCity(data.getString(LystoreBDD.CITY));
    }

    public String getUAI() {
        return UAI;
    }

    public void setUAI(String UAI) {
        this.UAI = UAI;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAcademy() {
        return academy;
    }

    public String getCiteMixte() {
        return citeMixte;
    }

    public void setCiteMixte(String citeMixte) {
        this.citeMixte = citeMixte;
    }

    public void setAcademy(String academy) {
        this.academy = academy;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public JsonObject toJsonObject() {
        return new JsonObject()
                .put(ID,id)
                .put(LystoreBDD.UAI,UAI)
                .put(LystoreBDD.NAME,name)
                .put(LystoreBDD.ACADEMY, academy)
                .put(LystoreBDD.ZIPCODE,zipCode)
                .put(LystoreBDD.ADDRESS,address)
                .put(LystoreBDD.TYPE,type);
    }

}
