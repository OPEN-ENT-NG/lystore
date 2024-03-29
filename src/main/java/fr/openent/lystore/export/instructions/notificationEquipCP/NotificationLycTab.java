package fr.openent.lystore.export.instructions.notificationEquipCP;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.CommonConstants;
import fr.openent.lystore.constants.ExportConstants;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class NotificationLycTab extends TabHelper {
    private int lineNumber = 0;


    final String Subvention = "236";
    private final String SubventionLabel = "GESTION DIRECTE\n" +
            "Les matériels seront fournis au lycée par l'intermédiaire des marchés publics Région.";
    private final String NotSubventionLabel = "SUBVENTIONS\n" +
            "Ce document constitue une information et sert également de notification comptable.";

    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     * @param structuresMap
     */
    public NotificationLycTab(Workbook wb, JsonObject instruction, Map<String, JsonObject> structuresMap) {
        super(wb, instruction, "NOTIFICATION POUR LES LYCEES");
        this.structures = structuresMap;
    }

    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {
        ArrayList structuresId = new ArrayList<>();
        for (int i = 0; i < datas.size(); i++) {
            JsonObject data = datas.getJsonObject(i);
            JsonArray actions = new JsonArray(data.getString("actions"));
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                if (!structuresId.contains(action.getString(LystoreBDD.ID_STRUCTURE)))
                    structuresId.add(structuresId.size(), action.getString(LystoreBDD.ID_STRUCTURE));
            }
        }

        fillPage(structures);
        HandleCatchResult(false, "", new JsonArray(structuresId), handler);
    }

    @Override
    protected void fillPage(Map<String, JsonObject> structures) {
        setStructuresFromDatas(structures);
        datas = sortByUai(datas);
        writeArray();
    }

    private void writeArray() {

        log.info(ExportConstants.NOTIFICATION_LYC_TAB + " writeArray");
        for (int i = 0; i < datas.size(); i++) {
            if (i != 0)
                excel.setRowBreak(lineNumber + 1);
            try {

                lineNumber += 3;
                excel.insertBlackTitleHeaderBorderless(0, lineNumber, datas.getJsonObject(i).getString(LystoreBDD.CITY));
                excel.insertBlackTitleHeaderBorderless(2, lineNumber, datas.getJsonObject(i).getString(LystoreBDD.NAME_ETAB));
                excel.insertBlackTitleHeaderBorderless(4, lineNumber, datas.getJsonObject(i).getString(LystoreBDD.UAI));
                JsonObject structure = datas.getJsonObject(i);
                JsonArray orders = structure.getJsonArray(ExportConstants.ACTIONS_JO);
                orders = sortByType(orders);
                String previousCode = "";

                if (orders.isEmpty()) {
                    return;
                } else {
                    for (int j = 0; j < orders.size(); j++) {
                        try {
                            JsonObject order = orders.getJsonObject(j);
                            String market = order.getString(LystoreBDD.MARKET);
                            String campaign = order.getString(LystoreBDD.CAMPAIGN);
                            String code = order.getString(LystoreBDD.CODE);

                            String room = getStr(order, LystoreBDD.ROOM);
                            String stair = getStr(order, LystoreBDD.STAIR);
                            String building = getStr(order, LystoreBDD.BUILDING);
                            String date = getFormatDate(instruction.getString(LystoreBDD.DATE_CP));
                            String equipmentNameComment = ExportConstants.NOTIFICATION_LIBELLE_REGION + " : "+
                                    formatStrToCell(order.getString(LystoreBDD.NAME_EQUIPMENT), 10);
                            String idFormatted = "";
                            if (order.getBoolean(LystoreBDD.ISREGION)) {
                                equipmentNameComment += "\n " + ExportConstants.NOTIFICATION_COMMENT_REGION + " : "
                                        + formatStrToCell(makeCellWithoutNull(order.getString(LystoreBDD.COMMENT)), 10);
                                idFormatted += ExportConstants.NOTIFICATION_REGION_COMMAND_PREFIX + order.getInteger(CommonConstants.ID).toString();
                            } else {
                                idFormatted += ExportConstants.NOTIFICATION_CLIENT_COMMAND_PREFIX + order.getInteger(CommonConstants.ID).toString();
                            }

                            if (!previousCode.equals(code)) {
                                if (code.equals(Subvention)) {
                                    lineNumber += 2;
                                    sizeMergeRegion(lineNumber, 0, 7);
                                    excel.insertWithStyle(0, lineNumber, NotSubventionLabel, excel.labelOnYellow);
                                    previousCode = Subvention;
                                    lineNumber += 2;
                                    setLabels();
                                } else if (!previousCode.equals(ExportConstants.NOT_SUBV)) {
                                    lineNumber += 2;
                                    excel.insertWithStyle(0, lineNumber, SubventionLabel, excel.labelOnLimeGreen);
                                    sizeMergeRegion(lineNumber, 0, 7);
                                    previousCode = ExportConstants.NOT_SUBV;
                                    lineNumber += 2;
                                    setLabels();
                                }
                            }
                            excel.insertCellTab(0, lineNumber,
                                    ExportConstants.ROOM_LABEL + ": " + room + "\n"
                                            + ExportConstants.STAIR_LABEL + ": " + stair + "\n"
                                            + ExportConstants.BUILDING_LABEL + ": " + building
                            );


                            excel.insertCellTabCenterBold(1, lineNumber, market + " " + campaign);
                            excel.insertCellTabCenterBold(2, lineNumber, market + " " + campaign);

                            excel.insertCellTabBlue(3, lineNumber, equipmentNameComment);
                            excel.insertCellTabCenterBold(4, lineNumber,
                                    makeCellWithoutNull(instruction.getString(LystoreBDD.CP_NUMBER)) + "\n" + date);
                            excel.insertCellTabCenter(5, lineNumber, idFormatted);
                            excel.insertCellTabCenterBold(6, lineNumber, order.getInteger(LystoreBDD.AMOUNT).toString());
                            excel.insertCellTabDoubleWithPrice(7, lineNumber, safeGetDouble(order, LystoreBDD.TOTAL,
                                    ExportConstants.NOTIFICATION_LYC_TAB));

                            lineNumber++;

                        } catch (NullPointerException e) {
                            log.error("@LystoreWorker[" + this.getClass() + "] error in second for loop data : \n" + orders.getJsonObject(j));
                            throw e;
                        }
                    }
                }
            } catch (NullPointerException e) {
                log.error("@LystoreWorker[" + this.getClass() + "] error in first loop: \n");
                throw e;
            }
            if (i <= 1) {
                excel.autoSize(8);
            }
        }
    }


    @Override
    protected String getFormatDate(String dateCp) {
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat formatterDateExcel = new SimpleDateFormat("dd/MM/yyyy");
        Date orderDate = null;
        try {
            orderDate = formatterDate.parse(dateCp);
        } catch (ParseException e) {
            log.error("Incorrect date format");
        }
        return formatterDateExcel.format(orderDate);
    }

    private String getStr(JsonObject order, String key) {
        try {
            return (order.getString(key).equals("null")) ? "" : order.getString(key);
        } catch (ClassCastException ee) {
            try {
                return order.getInteger(key).toString();
            } catch (NullPointerException e) {
                return "";
            }
        } catch (NullPointerException e) {
            return "";
        }
    }


    private JsonArray sortByType(JsonArray orders) {
        JsonArray sortedJsonArray = new JsonArray();

        List<JsonObject> jsonValues = new ArrayList<JsonObject>();
        for (int i = 0; i < orders.size(); i++) {
            jsonValues.add(orders.getJsonObject(i));
        }

        Collections.sort(jsonValues, new Comparator<JsonObject>() {
            private static final String KEY_NAME = "code";

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
                    log.error("error when sorting orders NotificationLycTab during export");
                }

                if (valA.equals(Subvention) && !valB.equals(valA)) {
                    return -6000000;
                } else if (valB.equals(Subvention) && !valB.equals(valA)) {
                    return 6000000;
                } else {
                    return valA.compareTo(valB);
                }
            }
        });

        for (int i = 0; i < orders.size(); i++) {
            sortedJsonArray.add(jsonValues.get(i));
        }
        return sortedJsonArray;
    }

    @Override
    protected void setLabels() {
        excel.insertHeader(0, lineNumber, ExportConstants.DESTINATION_LABEL);
        excel.insertHeader(1, lineNumber, ExportConstants.MARKET_CODE_LABEL);
        excel.insertHeader(2, lineNumber, ExportConstants.CAMPAIGN_LABEL);
        excel.insertHeader(3, lineNumber, ExportConstants.REGION_LABEL);
        excel.insertHeader(4, lineNumber, ExportConstants.DATE_LABEL);
        excel.insertHeader(5, lineNumber, ExportConstants.NUMBER_ORDER_LABEL);
        excel.insertHeader(6, lineNumber, ExportConstants.AMOUNT_LABEL);
        lineNumber++;
    }


    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "       With values as  (             " +
                "     SELECT  orders.id ,orders.\"price TTC\",  " +
                getTotalPriceTTCWithOptions() +
                "             as Total, contract.name as market, contract_type.code as code,    " +
                "             program.name as program,         CASE WHEN orders.id_order_client_equipment is not null  " +
                "             THEN  (select oce.name FROM " + Lystore.lystoreSchema + ".order_client_equipment oce    " +
                "              where oce.id = orders.id_order_client_equipment limit 1)     " +
                "             ELSE ''      " +
                "             END as old_name,     " +
                "             orders.id_structure,orders.id_operation as id_operation, label.label as operation ,     " +
                "             orders.equipment_key as key, orders.name as name_equipment, true as region,  orders.id as id,  " +
                "             program_action.id_program, orders.amount ,contract.id as market_id,   campaign.name as campaign, orders.comment, project.room, orders.isregion, " +
                "             project.stair,project.building,    " +
                "             case when specific_structures.type is null      " +
                "             then '" + LYCEE + "'          " +
                "             ELSE specific_structures.type     " +
                "             END as cite_mixte     " +
                "             FROM (      " +
                "             (select ore.id,  true as isregion, ore.price as \"price TTC\",  ore.amount,  ore.creation_date,  ore.modification_date,  ore.name,  ore.summary, " +
                "             ore.description,  ore.image,    ore.status,  ore.id_contract,  ore.equipment_key,  ore.id_campaign,  ore.id_structure, " +
                "             ore.cause_status,  ore.number_validation,  ore.id_order,  ore.comment,  ore.rank as \"prio\", null as price_proposal,  " +
                "             ore.id_project,  ore.id_order_client_equipment, null as program, null as action,  ore.id_operation ," +
                "             null as override_region          from " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore )      " +
                "             UNION      " +
                "             (select oce.id ,  false as isregion," +
                "             CASE WHEN price_proposal is null then  price + (price*tax_amount/100)  else price_proposal end as \"price TTC\", " +
                "             amount, creation_date, null as modification_date, name,  " +
                "             summary, description, image,  status, id_contract, equipment_key, id_campaign, id_structure, cause_status, number_validation, " +
                "             id_order, comment, rank as \"prio\", price_proposal, id_project, null as id_order_client_equipment,  program, action,  " +
                "             id_operation, override_region           from " + Lystore.lystoreSchema + ".order_client_equipment  oce) " +
                "             ) as orders       " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id   and (orders.override_region != true OR orders.override_region is NULL))               " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".label_operation as label ON (operation.id_label = label.id)      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id  AND instruction.id = ?)    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id)                  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id )      " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON orders.id_campaign = campaign.id  " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".project ON orders.id_project = project.id  " +
                "             LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id    " +
                "             INNER JOIN  " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id)         " +
                "   AND ((spa.structure_type = '" + CMD + "' AND specific_structures.type ='" + CMD + "') " +
                "  OR (spa.structure_type = '" + CMR + "' AND specific_structures.type ='" + CMR + "') " +
                "     OR                     (spa.structure_type = '" + LYCEE + "' AND" +
                " ( specific_structures.type is null OR  specific_structures.type ='" + LYCEE + "') ))    " +
                "     INNER JOIN  " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id)    " +
                "     INNER JOIN " + Lystore.lystoreSchema + ".program on program_action.id_program = program.id           " +


                "             Group by program.name,code,specific_structures.type , orders.amount , orders.name, orders.equipment_key , " +
                "             orders.id_operation,orders.id_structure  ,orders.id, contract.id ,label.label  ,program_action.id_program ,  " +
                "             orders.id_order_client_equipment,orders.\"price TTC\",orders.price_proposal,orders.override_region , orders.comment,campaign.name , orders.id," +
                "               orders.isregion, " +
                "              project.room,project.stair, project.building " +
                "             order by code,campaign,market_id, id_structure,program,code " +
                "  )    SELECT  values.id_structure as id_structure,    array_to_json(array_agg(values))as actions  " +
                "  from  values      " +
                "  Group by values.id_structure   " +
                "  Order by values.id_structure   ;";

        sqlHandler(handler);
    }
}
