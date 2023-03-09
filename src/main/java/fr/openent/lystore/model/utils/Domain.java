package fr.openent.lystore.model.utils;

public class Domain {
    private final String host;
    private final String lang;

    public Domain(String host, String lang) {
        this.host = host;
        this.lang = lang;
    }

    public String getHost() {
        return host;
    }

    public String getLang() {
        return lang;
    }

}
