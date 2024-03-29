package fr.openent.lystore.export.instructions.equipmentRapp;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.openent.lystore.service.StructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.*;

public class ComptaTab extends TabHelper {
    private String type;
    private int yProgramLabel = 0;
    private StructureService structureService;

    public ComptaTab(Workbook workbook, JsonObject instruction, String type, Map<String,JsonObject> structuresMap) {
        super(workbook, instruction, "COMPTA du rapport  " + type,structuresMap);
        this.type = type;
        excel.setDefaultFont();
    }


    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        fillPage(structures);
        HandleCatchResult(false, "", new JsonArray(), handler);
    }

    @Override
    protected void fillPage(Map<String, JsonObject> structures){
        setStructures(structures);
        setLabels();
    }


    @Override
    protected void setLabels() {
        log.info("setLabels");
        int initYProgramLabel = 2;
        for (int i = 0; i < datas.size(); i++) {
            log.info("first looop");

            JsonObject programLabel = new JsonObject();
            //creating label
            int columnTotal = 4;
            JsonObject operation = datas.getJsonObject(i);
            int currentY = yProgramLabel;
            yProgramLabel += 2;
            setTitle(currentY, operation);
            JsonArray actions = operation.getJsonArray("actionsJO");
            JsonObject idPassed = new JsonObject();
            initYProgramLabel = yProgramLabel;
            yProgramLabel += 2;
            String campaign = "", key = "", oldkey = "";
            Double oldTotal = 0.d;

//            //Insert datas
//
            actions = sort(actions);
            for (int j = 0; j < actions.size(); j++) {
                log.info("second looop");
                try{
                    JsonObject action = actions.getJsonObject(j);
                    if (!action.getString("campaign").equals(campaign)) {
                        if (j != 0) {
                            setTotal(programLabel.size() + 4, initYProgramLabel);
                        }
                        campaign = action.getString("campaign");
                        yProgramLabel += 2;
                        setCampaign(campaign, yProgramLabel);
                        yProgramLabel += 2;
                        initYProgramLabel = yProgramLabel;
                        yProgramLabel += 2;
                        if (arrayLength - 4 < columnTotal) {
                            arrayLength += columnTotal;
                        }
                        columnTotal = 4;
                        idPassed = new JsonObject();
                        programLabel = new JsonObject();
                    }
                    key = action.getString("program") + " - " + action.getString("code");
                    if (!programLabel.containsKey(key)) {
                        programLabel.put(key, programLabel.size());
                        excel.insertHeader(4 + programLabel.getInteger(key), initYProgramLabel,
                                action.getString("program"));
                        excel.insertHeader(4 + programLabel.getInteger(key), initYProgramLabel + 1,
                                action.getString("code"));
                    }


                    if (!checkIdPassed(idPassed, action.getString("id_structure"))) {
                        columnTotal = 4;
                        idPassed.put(action.getString("id_structure"), true);

                        try {
                            excel.insertLabel(0, yProgramLabel, action.getString("zipCode").substring(0, 2));

                        } catch (NullPointerException e) {
                            excel.insertLabel(0, yProgramLabel, action.getString("zipCode"));
                        }
                        excel.insertLabel(1, yProgramLabel, action.getString("city"));
                        excel.insertLabel(2, yProgramLabel, action.getString("nameEtab"));
                        excel.insertLabel(3, yProgramLabel, action.getString("uai"));

                        oldTotal = 0.d;
                        oldkey = key;

                        oldTotal += safeGetDouble(action, "total", "ComptaTab");
                        excel.insertCellTabDouble(4 + programLabel.getInteger(key),
                                yProgramLabel, oldTotal);
                    } else {
                        yProgramLabel--;
                        if (!oldkey.equals(key)) {
                            oldTotal = 0.d;
                        }
                        oldkey = key;
                        oldTotal += safeGetDouble(action, "total", "ComptaTab");
                        excel.insertCellTabDouble(4 + programLabel.getInteger(action.getString("program") + " - " + action.getString("code")), yProgramLabel
                                , oldTotal);
                    }

                    columnTotal++;
                    yProgramLabel++;
                }catch (NullPointerException e){
                    log.error("@LystoreWorker["+ this.getClass() +"] error in second for loop data : \n" + actions.getJsonObject(j));
                    throw e;
                }
            }
            if (arrayLength - 4 < columnTotal) {
                arrayLength += columnTotal;
            }
            setTotal(programLabel.size() + 4, initYProgramLabel);
            yProgramLabel += 2;
            if(i < 1) {
                log.info("autosize");
                excel.autoSize(arrayLength);
            }
        }
    }

    private JsonArray sort(JsonArray values) {
        JsonArray sortedJsonArray = new JsonArray();

        values = sortByCity(values, false);
        List<JsonObject> jsonValues = new ArrayList<JsonObject>();
        for (int i = 0; i < values.size(); i++) {
            jsonValues.add(values.getJsonObject(i));
        }

        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "campaign";
            @Override
            public int compare(JsonObject a, JsonObject b) {
                String valA = "";
                String valB = "";
                try {
                    if (a.containsKey(KEY_NAME)) {
                        valA = a.getString(KEY_NAME);
                    }
                    if (b.containsKey(KEY_NAME)) {
                        valB = b.getString(KEY_NAME);
                    }
                } catch (NullPointerException e) {
                    log.error("error when sorting values by id_campaign during export");
                }
                return valA.compareTo(valB);
            }
        });

        for (int i = 0; i < values.size(); i++) {
            sortedJsonArray.add(jsonValues.get(i));
        }
        return sortedJsonArray;
    }

    private void setCampaign(String campaign, int y) {
        log.info("setCampaign");
        CellRangeAddress merge = new CellRangeAddress(y, y, 0, 6);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        excel.insertYellowHeader(0, y, campaign);
    }

    private void setTitle(int currentY, JsonObject operation) {
        log.info("setTitle");
        CellRangeAddress merge = new CellRangeAddress(currentY, currentY, 0, 6);
        sheet.addMergedRegion(merge);
        excel.setRegionHeader(merge, sheet);
        excel.insertTitleHeader(0, currentY, "Lycées concernés par: " + operation.getString("label"));
    }

    private void setTotal(int nbTotaux, int initYProgramLabel) {
        log.info("setTotal");
        excel.fillTab(4, nbTotaux, initYProgramLabel + 2, yProgramLabel);
        excel.insertHeader(3, yProgramLabel, excel.totalLabel);
        for (int nbTotal = 4; nbTotal < nbTotaux; nbTotal++) {
            excel.setTotalX(initYProgramLabel + 1, yProgramLabel - 1, nbTotal, yProgramLabel);
        }
        excel.insertHeader(nbTotaux, initYProgramLabel + 1, excel.totalLabel);
        for (int y = initYProgramLabel + 2; y <= yProgramLabel; y++) {
            excel.setTotalY(4, nbTotaux - 1, y, nbTotaux);
        }
    }

    private boolean checkIdPassed(JsonObject idPassed, String id) {
        return idPassed.containsKey(id);
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "       With values as  (             " +
                "     SELECT  orders.id ,orders.\"price TTC\",  " +
                getTotalPriceTTCWithOptions() +
                "             as Total, contract.name as market, contract_type.code as code, campaign.name as campaign,   " +
                "             program.name as program,         CASE WHEN orders.id_order_client_equipment is not null  " +
                "             THEN  (select oce.name FROM " + Lystore.lystoreSchema + ".order_client_equipment oce    " +
                "              where oce.id = orders.id_order_client_equipment limit 1)     " +
                "             ELSE ''      " +
                "             END as old_name,     " +
                "             orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,    " +
                "             program_action.id_program, orders.amount ,contract.id as market_id,       " +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation , " +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             union      " +
                "             (select oce.id," +
                "             CASE WHEN price_proposal is null then  price + (price*tax_amount/100)  else price_proposal end as \"price TTC\", " +
                "             amount, creation_date, null as modification_date, name,  " +
                "             summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, " +
                "             id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action,  " +
                "             id_operation, override_region           from " + Lystore.lystoreSchema + ".order_client_equipment  oce) " +
                "             ) as orders       " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id   and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id )                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id)      " +
                "             INNER JOIN " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         ";
        if (type.equals(CMR))
            query +=
                    "   AND (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "')  ";
        else {
            query +=
                    "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "')  " +
                            "     OR                    " +
                            " (spa.structure_type = '" + LYCEE + "' AND " +
                            "   ( specific_structures.type is null OR  specific_structures.type ='" + LYCEE + "') ))    ";
        }

        query +=
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                        "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +
                        "     WHERE    ";


        if (type.equals(CMR))
            query += "   specific_structures.type =  '" + CMR + "'   ";
        else {
            query += "  specific_structures.type !=  '" + CMR + "'   " +
                    "  OR specific_structures.type is null " +
                    "  OR specific_structures.type !=  '" + LYCEE + "'   " ;
        }
        query +=
                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                        "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                        "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region ,campaign" +
                        "             order by  campaign,program,code ,orders.id_structure    )        " +
                        " SELECT values.operation as label , array_to_json(array_agg(values)) as actions   " +
                        " from values  " +
                        " Group by label ; ";


        sqlHandler(handler);

    }
}

