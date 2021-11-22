package fr.openent.lystore.utils;

import fr.wseduc.webutils.I18n;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.http.Renders.getHost;

public class OrderUtils {
    public static Double safeGetDouble(JsonObject jo, String key) {
        Double result;
        try {
            result = jo.getDouble(key);
        }catch (Exception e){
            result = Double.parseDouble(jo.getString(key).replaceAll(",", "."));
        }
        return  result;
    }

    public static String getValidOrdersCSVExportHeader(HttpServerRequest request) {
        return I18n.getInstance().
                translate("UAI", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("lystore.structure.name", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("city", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("phone", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("EQUIPMENT", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("lystore.amount", getHost(request), I18n.acceptLanguage(request)) +
                "\n";
    }

}
