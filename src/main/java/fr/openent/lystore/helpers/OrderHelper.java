package fr.openent.lystore.helpers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;


/**
 * Class pour les fonctions utiles liées au traitement des commandes
 */
public class OrderHelper {
    public static Double getSumTTC(JsonArray orders) {
        Double sum = 0D;
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            sum += Double.parseDouble(order.getString("pricettc")) * Integer.parseInt(order.getString("amount"))
                  ;
        }
        return sum;
    }

    public static JsonArray formatOrders(JsonArray orders) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            order.put("priceLocale",
                    roundWith2Decimals(getTaxExcludedPrice(Double.parseDouble(order.getString("pricettc")),
                            Double.parseDouble(order.getString("tax_amount")))));
            order.put("totalPriceLocale",
                    roundWith2Decimals(getTotalPriceHT(Double.parseDouble(order.getString("pricettc")),
                            Double.parseDouble(order.getString("amount")),
                            Double.parseDouble(order.getString("tax_amount")))));
//            order.put("totalPriceLocale",
//                    getReadableNumber(roundWith2Decimals(getTaxExcludedPrice(order.getDouble("totalPrice"),
//                            Double.parseDouble(order.getString("tax_amount"))))));
        }
        return orders;
    }

    public static Double getTotalPrice(Double price, Double amount) {
        return price * amount;
    }

    public static Double getTaxIncludedPrice(Double price, Double taxAmount) {
        Double multiplier = taxAmount / 100 + 1;
        return roundWith2Decimals(price) * multiplier;
    }

    public static Double roundWith2Decimals(Double numberToRound) {
        BigDecimal bd = new BigDecimal(numberToRound);
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static Double getTotalPriceHT(double pricettc, double amount, double tax_amount) {
        return getTaxExcludedPrice(pricettc,tax_amount) * amount ;
    }

    public static Double getTaxExcludedPrice(double price, double tax_amount) {
        if(tax_amount == -1.d){
            tax_amount = 20.d;
        }
        return price / ((100 + tax_amount)/ 100 );

    }
    public static Double getSumWithoutTaxes(JsonArray orders) {
        JsonObject order;
        Double sum = 0D;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            sum += order.getDouble("priceLocale") * Integer.parseInt(order.getString("amount"));
        }

        return sum;
    }
}
