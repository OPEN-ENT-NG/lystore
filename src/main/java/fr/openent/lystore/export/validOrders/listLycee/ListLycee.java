package fr.openent.lystore.export.validOrders.listLycee;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;

public class ListLycee extends TabHelper {
    private String numberValidation;
    public ListLycee(Workbook workbook, String numberValidation) {
        super(workbook,"Sommaire Commande");
        this.numberValidation = numberValidation;
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }


    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {

        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            if(!structuresId.contains(data.getString("id_structure")))
                structuresId.add(structuresId.size(), data.getString("id_structure"));

        }
        getStructures(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> repStructures) {
//
                boolean errorCatch= false;
                if (repStructures.isRight()) {
                    try {
                        JsonArray structures = repStructures.right().getValue();
                        setStructuresFromDatas(structures);
                        if (datas.isEmpty()) {
                            handler.handle(new Either.Left<>("No data in database"));
                        } else {
                            datas = sortByCity(datas, false);
                            writeArray(handler);
                        }
                    }catch (Exception e){
                        errorCatch = true;
                    }
                    if(errorCatch)
                        handler.handle(new Either.Left<>("Error when writting files"));
                    else
                        handler.handle(new Either.Right<>(true));
                } else {
                    handler.handle(new Either.Left<>("Error when casting neo"));
//

                }
            }
//
//
        });

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {

        excel.insertWithStyle(0,0,"Numéro de validation",excel.labelOnYellow);
        excel.insertWithStyle(1,0,"Numéro du marché",excel.labelOnYellow);
        excel.insertWithStyle(2,0,"Libellé du marché",excel.labelOnYellow);
        excel.insertWithStyle(3,0,"Date du BC",excel.labelOnYellow);

        excel.insertWithStyle(4,0,"UAI",excel.labelOnYellow);
        excel.insertWithStyle(5,0,"Nom de l'établissement",excel.labelOnYellow);
        excel.insertWithStyle(6,0,"Commune",excel.labelOnYellow);
        excel.insertWithStyle(7,0,"Tel",excel.labelOnYellow);
        excel.insertWithStyle(8,0,"Equipment",excel.labelOnYellow);
        excel.insertWithStyle(9,0,"Qté",excel.labelOnYellow);
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
        query = "SELECT oce.price as price," +
                " oce.tax_amount," +
                " oce.name as name, " +
                "oce.id_contract as id_contract," +
                "SUM(oce.amount) as amount ," +
                " oce.id_structure , " +
                "       market.name as market_name," +
                "       market.reference as market_reference," +
                "       od.date_creation as creation_bc " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment oce" +
                "       INNER JOIN "+ Lystore.lystoreSchema +".equipment_type et  " +
                "               ON oce.id_type = et.id  " +
                "       INNER JOIN "+ Lystore.lystoreSchema +".contract market  " +
                "               ON oce.id_contract = market.id  " +
                "           LEFT JOIN " + Lystore.lystoreSchema + ".order od " +
                "               ON oce.id_order = od.id " +
                "WHERE number_validation = ? ";
        query += " GROUP BY equipment_key, price, tax_amount, oce.name, oce.id_contract,  oce.id_structure ,market_name, market_reference, creation_bc" +
                " UNION " +
                " SELECT opt.price," +
                " opt.tax_amount," +
                " opt.name, opt.id_contract," +
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
        query += " GROUP BY opt.name, opt.price, opt.tax_amount, opt.id_contract , equipment.id_structure,market_name, market_reference, creation_bc";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(this.numberValidation).add(this.numberValidation);
        sqlHandler(handler,params);
    }
}
