package fr.openent.lystore.export.validOrders.listLycee;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

public class ListLycee extends TabHelper {
    private String numberValidation;
    public ListLycee(Workbook workbook, String numberValidation, Map<String, JsonObject> structuresMap) {
        super(workbook,"Sommaire Commande",structuresMap);
        this.numberValidation = numberValidation;
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }


    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        setStructuresFromDatas(structures);
        writeArray(handler);
        handler.handle(new Either.Right<>(true));

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {

        excel.insertWithStyle(0,0,"Numéro de validation",excel.labelOnLightYellow);
        excel.insertWithStyle(1,0,"Numéro du marché",excel.labelOnLightYellow);
        excel.insertWithStyle(2,0,"Libellé du marché",excel.labelOnLightYellow);
        excel.insertWithStyle(3,0,"Date du BC",excel.labelOnLightYellow);

        excel.insertWithStyle(4,0,"UAI",excel.labelOnLightYellow);
        excel.insertWithStyle(5,0,"Nom de l'établissement",excel.labelOnLightYellow);
        excel.insertWithStyle(6,0,"Commune",excel.labelOnLightYellow);
        excel.insertWithStyle(7,0,"Tel",excel.labelOnLightYellow);
        excel.insertWithStyle(8,0,"Equipment",excel.labelOnLightYellow);
        excel.insertWithStyle(9,0,"Qté",excel.labelOnLightYellow);
        for(int i=0;i<datas.size();i++){
            JsonObject data = datas.getJsonObject(i);
            excel.insertCellTab(0,i+1,makeCellWithoutNull(numberValidation));
            excel.insertCellTab(1,i+1,makeCellWithoutNull(data.getString("market_reference")));
            excel.insertCellTab(2,i+1,makeCellWithoutNull(data.getString("market_name")));
            try {
                excel.insertCellTab(3, i + 1, makeCellWithoutNull(getFormatDate(data.getString("creation_bc"))));
            }catch (NullPointerException e){
                excel.insertCellTab(3, i + 1, "Bon de commande en attente de validation");
            }
            excel.insertCellTab(4,i+1,makeCellWithoutNull(data.getString("uai")));
            excel.insertCellTab(5,i+1,makeCellWithoutNull(data.getString("nameEtab")));
            excel.insertCellTab(6,i+1,makeCellWithoutNull(data.getString("city")));
            excel.insertCellTab(7,i+1,makeCellWithoutNull(data.getString("phone")));
            excel.insertCellTab(8,i+1,makeCellWithoutNull(data.getString("name")));
            excel.insertCellTab(9,i+1,makeCellWithoutNull(data.getString("amount")));
            if(i == 10){
                excel.autoSize(10);
            }
        }
        if(datas.size() < 10){
            excel.autoSize(10);
        }
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "SELECT " +
                " " +
                " oce.name as name, " +
                "oce.id_contract as id_contract," +
                "SUM(oce.amount) as amount ," +
                " oce.id_structure , " +
                "       market.name as market_name," +
                "       market.reference as market_reference," +
                "       od.date_creation as creation_bc " +
                "FROM " + Lystore.lystoreSchema + ".allorders oce" +
                "       INNER JOIN "+ Lystore.lystoreSchema +".contract market  " +
                "               ON oce.id_contract = market.id  " +
                "           LEFT JOIN " + Lystore.lystoreSchema + ".order od " +
                "               ON oce.id_order = od.id " +
                "WHERE number_validation = ? ";
        query += " GROUP BY equipment_key, oce.name, oce.id_contract,  oce.id_structure ,market_name, market_reference, creation_bc" +
                " UNION " +
                " SELECT  opt.name, opt.id_contract," +
                " SUM(opt.amount) as amount " +
                ", equipment.id_structure, " +
                "       market_name as market_name," +
                "       market_reference as market_reference," +
                "       creation_bc as creation_bc " +
                "FROM (" +
                "  SELECT options.price, options.tax_amount ," +
                "       market.name as market_name," +
                "       market.reference as market_reference," +
                "       od.date_creation as creation_bc, " +
                "   options.name, equipment.id_contract," +
                "   equipment.amount," +
                "    options.id_order_client_equipment , " +
                "   equipment.id_structure "+
                " FROM " + Lystore.lystoreSchema + ".order_client_options options " +
                " INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment equipment " +
                " ON (options.id_order_client_equipment = equipment.id) " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".equipment_type et  " +
                "               ON equipment.id_type = et.id  " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".contract market  " +
                "               ON equipment.id_contract = market.id  " +
                "           LEFT JOIN " + Lystore.lystoreSchema + ".order od " +
                "               ON equipment.id_order = od.id " +

                "WHERE number_validation = ? "+
                ") as opt";
        query += " INNER JOIN lystore.order_client_equipment equipment ON (opt.id_order_client_equipment = equipment.id)" ;
        query += " GROUP BY opt.name, opt.id_contract , equipment.id_structure,market_name, market_reference, creation_bc";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(this.numberValidation).add(this.numberValidation);
        sqlHandler(handler,params);
    }
}
