package fr.openent.lystore.model.parameter;

import io.vertx.core.json.JsonObject;

import static fr.openent.lystore.constants.ParametersConstants.*;

public class BCOptions {
    BCOptionsFormData address;
    BCOptionsName name;
    BCOptionsFormData signature;
    String img;

    public BCOptions() {

    }

    public BCOptions(JsonObject params) {
        this.set(params);

    }


    public BCOptions set(JsonObject params) {
        this.setImg(params.getString(IMG));
        this.name = new BCOptionsName();
        this.address = new BCOptionsFormData();
        this.signature = new BCOptionsFormData();
        setBCOptionsName(params);
        setBCOptionsAddress(params);
        setBCOptionsSignature( params);
        return this;
    }

    private void setBCOptionsSignature(JsonObject result) {
        setBCOptionsFormData(this.signature, result, SIGNATURE);
    }

    private void setBCOptionsAddress(JsonObject result) {
        setBCOptionsFormData(this.address, result, ADDRESS);
    }

    private String[] setBCOptionsFormData(BCOptionsFormData optionsFormData, JsonObject params, String key) {
        String[] split = params.getString(key).split("\n");
        try {
            optionsFormData.setLine1(split[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            optionsFormData.setLine1("");
        }
        try {
            optionsFormData.setLine2(split[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            optionsFormData.setLine2("");
        }
        return split;
    }

    private void setBCOptionsName(JsonObject params) {
        String[] split = setBCOptionsFormData(this.name, params, NAME);
        try {
            this.name.setLine3(split[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
            this.name.setLine3("");
        }
        try {
            this.name.setLine4(split[3]);
        } catch (ArrayIndexOutOfBoundsException e) {
            this.name.setLine4("");
        }
    }

    public BCOptionsFormData getAddress() {
        return address;
    }

    public void setAddress(BCOptionsFormData address) {
        this.address = address;
    }

    public BCOptionsName getName() {
        return name;
    }

    public void setName(BCOptionsName name) {
        this.name = name;
    }

    public BCOptionsFormData getSignature() {
        return signature;
    }

    public void setSignature(BCOptionsFormData signature) {
        this.signature = signature;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put(NAME, this.name.toJson())
                .put(SIGNATURE, this.signature.toJson())
                .put(ADDRESS, this.address.toJson())
                .put(IMG, this.img);
    }
}
