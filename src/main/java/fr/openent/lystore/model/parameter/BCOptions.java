package fr.openent.lystore.model.parameter;

import io.vertx.core.json.JsonObject;

import static fr.openent.lystore.constants.ParametersConstants.*;

public class BCOptions {
    BCOptionsAddress address;
    BCOptionsName name;
    BCOptionsSignature signature;
    String img;

    public BCOptions() {

    }

    public BCOptionsAddress getAddress() {
        return address;
    }

    public void setAddress(BCOptionsAddress address) {
        this.address = address;
    }

    public BCOptionsName getName() {
        return name;
    }

    public void setName(BCOptionsName name) {
        this.name = name;
    }

    public BCOptionsSignature getSignature() {
        return signature;
    }

    public void setSignature(BCOptionsSignature signature) {
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
