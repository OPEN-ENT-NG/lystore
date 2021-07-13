package fr.openent.lystore.export.instructions.publipostage;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Map;

public class Publipostage extends TabHelper {
    private StructureService structureService;
    private int lengthGlobalCols = 7;
    private int lengthGlobalRows = 1;
    public Publipostage(Workbook workbook, JsonObject instruction, Map<String, JsonObject> structuresMap) {
        super(workbook, instruction, "Publipostage",structuresMap);
        structureService = new DefaultStructureService(Lystore.lystoreSchema);
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        getDatas(dataFromRequest -> {
            try{
                if (dataFromRequest.isLeft()) {
                    log.error("Failed to retrieve datas");
                    handler.handle(new Either.Left<>("Failed to retrieve datas"));
                } else {
                    if (checkEmpty()) {
                        handler.handle(new Either.Right<>(true));
                    } else {
                        makeHeader();
                        initDatas(handler);
                    }
                }
            }catch(Exception e){
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("error when creating excel"));
            }
        });
    }

    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            getElemsStructure(structures,data);
        }
        datas = sortByCity(datas,true);
        fillPage(datas);
        HandleCatchResult(false, "", new JsonArray(), handler);
    }
    @Override
    protected void fillPage(JsonArray structures){
        makeBody(structures);
    }

    private void makeHeader(){
        JsonArray contentsHeader = new JsonArray()
                .add("Instruction")
                .add("UAI")
                .add("Nom courant")
                .add("Commune")
                .add("Adresse")
                .add("CP")
                .add("Contact");

        lengthGlobalCols = contentsHeader.size();
        for (int colNumber = 0 ; colNumber<contentsHeader.size() ; colNumber++){
            if(contentsHeader.getValue(colNumber) instanceof String ){
                excel.insertBlackTitleHeader(colNumber, 0, contentsHeader.getString(colNumber));
            } else {
                excel.insertBlackTitleHeader(colNumber, 0, "");
            }
        }
    }

    protected void makeBody(JsonArray structures) {
        for (int rowNumber = 0; rowNumber < structures.size(); rowNumber++) {
            JsonObject structure = structures.getJsonObject(rowNumber);
            JsonArray contentsBody = new JsonArray()
                    .add(makeCellWithoutNull(this.instruction.getString("cp_number"))) //Instruction cp number
                    .add(makeCellWithoutNull(structure.getString("uai"))) //structure UAI
                    .add(makeCellWithoutNull(structure.getString("nameEtab")))  //structure nom courant
                    .add(makeCellWithoutNull(structure.getString("city"))) //structure commune
                    .add(makeCellWithoutNull(structure.getString("address"))) //structure adresse
                    .add(makeCellWithoutNull(structure.getString("zipCode"))) //structure CP
                    .add("M. Mme Le Proviseur.e"); //Contact

            for (int colNumber = 0 ; colNumber<contentsBody.size() ; colNumber++){
                if(contentsBody.getValue(colNumber) instanceof String ){
                    excel.insertCellTab(colNumber, lengthGlobalRows, contentsBody.getString(colNumber));
                } else {
                    excel.insertCellTab(colNumber, lengthGlobalRows, "");
                }
            }
            lengthGlobalRows++;
            if(rowNumber == 10){
                excel.autoSize(lengthGlobalCols);
            }
        }
    }



    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "" +
                "SELECT DISTINCT " +
                "orders.id_structure,  " +
                "orders.id_operation  " +
                "FROM "+ Lystore.lystoreSchema +".allorders AS orders  " +
                "INNER JOIN   " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id  " +
                "                                 AND (orders.override_region != TRUE  " +
                "                                      OR orders.override_region IS NULL))  " +
                "INNER JOIN   " + Lystore.lystoreSchema + ".label_operation AS label ON (operation.id_label = label.id)  " +
                "INNER JOIN   " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  " +
                "                                   AND instruction.id = ? )  " +
                "LEFT JOIN   " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id";

        sqlHandler(handler, new JsonArray().add(instruction.getInteger("id")));
    }
}
