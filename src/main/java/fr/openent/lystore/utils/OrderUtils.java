package fr.openent.lystore.utils;

import fr.wseduc.webutils.I18n;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import static fr.wseduc.webutils.http.Renders.getHost;

public class OrderUtils {
    public static double safeGetDouble(JsonObject jo, String key) {
        double result;
        try {
            result = jo.getDouble(key);
        }catch (ClassCastException e){
            result = Double.parseDouble(jo.getString(key).replaceAll(",", "."));
        }catch (Exception e){
            result = 0.d;
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
