package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.PurseService;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private static final Logger log = LoggerFactory.getLogger (DefaultOrderService.class);
    private PurseService purseService ;
    private EmailSendService emailSender ;
    private StructureService structureService;

    public DefaultOrderService(
            String schema, String table, EmailSender emailSender){
        super(schema,table);
        this.purseService = new DefaultPurseService();
        this.emailSender = new EmailSendService(emailSender);
        this.structureService = new DefaultStructureService(Lystore.lystoreSchema);
    }

    @Override
    public void listOrder(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT " +
                "   oe.id as id, " +
                "   oe.comment, " +
                "   oe.price_proposal, " +
                "   prj.preference as preference, " +
                "   prj.id as id_project, " +
                "   oe.id_project, " +
                "   oe.price, " +
                "   oe.tax_amount, " +
                "   oe.amount, " +
                "   oe.creation_date, " +
                "   oe.id_campaign, " +
                "   oe.id_structure, " +
                "   oe.name, " +
                "   oe.summary, " +
                "   oe.image, " +
                "   oe.status, " +
                "   oe.id_contract, " +
                "   oe.rank, " +
                "   array_to_json(array_agg(order_opts)) as options, " +
                "   to_json(prj.*) as project, " +
                "   to_json(tt.*) as title, " +
                "   c.name as name_supplier, " +
                "   operation_instruction.cp_number, " +
                "   array_to_json(array_agg(DISTINCT order_file.*)) as files  " +
                "FROM " +
                "    " + Lystore.lystoreSchema + ".order_client_equipment oe  " +
                "   LEFT JOIN " +
                "       " + Lystore.lystoreSchema + ".order_client_options order_opts  " +
                "      ON oe.id = order_opts.id_order_client_equipment  " +
                "   INNER JOIN " +
                "       " + Lystore.lystoreSchema + ".project as prj  " +
                "      ON oe.id_project = prj.id  " +
                "   INNER JOIN " +
                "       " + Lystore.lystoreSchema + ".title as tt  " +
                "      ON tt.id = prj.id_title  " +
                "   LEFT JOIN " +
                "       " + Lystore.lystoreSchema + ".order_file  " +
                "      ON oe.id = order_file.id_order_client_equipment  " +
                "   LEFT JOIN " +
                "       " + Lystore.lystoreSchema + ".campaign  " +
                "      ON oe.id_campaign = campaign.id  " +
                "  LEFT JOIN  " +
                "      (  " +
                "         SELECT  " +
                "            operation.id,  " +
                "            cp_number   " +
                "         FROM  " +
                "             " + Lystore.lystoreSchema + ".operation   " +
                "            INNER JOIN  " +
                "                " + Lystore.lystoreSchema + ".instruction   " +
                "               on operation.id_instruction = instruction.id  " +
                "      )  " +
                "      as operation_instruction   " +
                "      on oe.id_operation = operation_instruction.id  " +
                "   INNER JOIN " +
                "      ( " +
                "         SELECT " +
                "            supplier.name, " +
                "            contract.id  " +
                "         FROM " +
                "             " + Lystore.lystoreSchema + ".supplier  " +
                "            INNER JOIN " +
                "                " + Lystore.lystoreSchema + ".contract  " +
                "               ON contract.id_supplier = supplier.id " +
                "      ) " +
                "      c  " +
                "      ON oe.id_contract = c.id  " +
                "WHERE " +
                "   id_campaign = ? " +
                "   AND id_structure =  ? " +
                "GROUP BY " +
                "(prj.id , oe.id, tt.id, c.name, prj.preference, operation_instruction.cp_number,campaign.priority_enabled )  " +
                "ORDER BY " +
                "   CASE " +
                "      WHEN " +
                "         campaign.priority_enabled = false  " +
                "      THEN " +
                "         oe.creation_date  " +
                "   END " +
                "   ASC,  " +
                "   CASE " +
                "      WHEN " +
                "         campaign.priority_enabled = true  " +
                "      THEN " +
                "         preference  " +
                "   END " +
                "   ASC";

        values.add(idCampaign).add(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }

    @Override
    public  void listOrder(String status, List<String> filters, Handler<Either<String, JsonArray>> handler){

        String query = "SELECT oce.id, oce.price, oce.tax_amount, oce.amount, oce.creation_date, oce.id_campaign, oce.id_structure, oce.name, oce.summary, oce.description," +
                " oce.image, oce.technical_spec, oce.status, oce.id_contract, oce.equipment_key,title.name as project_name," +
                " project.description as project_description,project.room as project_room, project.building as project_building ,contract_type.name as contract_type_name, " +
                " " +
                " array_to_json(array_agg(DISTINCT order_file.*)) as files , " +
                " array_to_json(array_agg( DISTINCT oco.*)) as options," +
                " oce.cause_status, oce.number_validation, oce.id_order, oce.comment, oce.price_proposal, oce.id_project, oce.rank, oce.program," +
                " oce.action, array_to_json(array_agg( distinct structure_group.name)) as structure_groups, " +
                " oce.id_operation, oce.override_region, oce.id_type,  " +
                 Lystore.lystoreSchema + ".order_total(oce.id) AS Total"+
                " FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "INNER JOIN " + Lystore.lystoreSchema + ".project ON (oce.id_project = project.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".title ON (project.id_title = title.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                " LEFT JOIN " + Lystore.lystoreSchema + ".order_file ON oce.id = order_file.id_order_client_equipment " +
                " WHERE oce.status = ?  " +

                "GROUP  BY oce.id, " +
                "    oce.id_project, " +
                "    oce.id_structure, " +
                "    oce.id_contract," +
                "   project_description, " +
                "project_room , " +
                "project_building," +
                "   contract_type_name, " +
                "   title.name" +
                " ORDER by id DESC" +
//                " LIMIT 50 OFFSET 50" +
                " ;";
        JsonArray params = new JsonArray().add(status);

        if (!filters.isEmpty()) {
            sql.prepared(query, params, SqlResult.validResultHandler(filterOrders(filters,  status , handler)));
        }else{
            sql.prepared(query, params, SqlResult.validResultHandler(handler));
        }
    }


    @Override
    public void listOrders(List<Integer> ids, List<String> filters, Handler<Either<String, JsonArray>> handler) {

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        String query = "SELECT oce.* , prj.id as id_project ,prj.preference as preference , oce.price * oce.amount as total_price , " +
                "to_json(contract.*) contract ,to_json(supplier.*) supplier, " +
                "to_json(campaign.* ) campaign, to_json( prj.*) as project, to_json( tt.*) as title," +
                "to_json( gr.*) as grade, array_to_json(array_agg(  oco.*)) as options " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".contract ON oce.id_contract = contract.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".supplier ON contract.id_supplier = supplier.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".project as prj ON oce.id_project = prj.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".title as tt ON tt.id = prj.id_title " +
                "INNER JOIN " + Lystore.lystoreSchema + ".grade as gr ON gr.id = prj.id_grade " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "WHERE oce.id in "+ Sql.listPrepared(ids.toArray()) +
                " GROUP BY (prj.preference, prj.id , oce.id, tt.id, gr.id, contract.id, supplier.id, campaign.id) ORDER BY prj.preference, oce.id_project DESC; ";

        for (Integer id : ids) {
            params.add( id);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getStructuresId(JsonArray validationNumbers, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, id_structure " +
                "FROM " + Lystore.lystoreSchema + ".allOrders " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.getList()) + ";";

        Sql.getInstance().prepared(query, validationNumbers, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrders(JsonArray ids, String structureId, Boolean isNumberValidation, Boolean groupByStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT price, tax_amount, name, id_contract, " +
                "SUM(amount) as amount " + (groupByStructure ? ", id_structure " : "") +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE " + (isNumberValidation ? "number_validation" : "id") + " IN " + Sql.listPrepared(ids.getList());
        if (structureId != null) {
            query += "AND id_structure = ?";
        }
        query += " GROUP BY equipment_key, price, tax_amount, name, id_contract " + (groupByStructure ? ", id_structure " : "") +
                "UNION " +
                "SELECT opt.price, opt.tax_amount, opt.name, opt.id_contract, SUM(opt.amount) as amount " +
                (structureId != null || groupByStructure ? ", equipment.id_structure " : "") +
                "FROM (" +
                "SELECT options.price, options.tax_amount," +
                "options.name, equipment.id_contract," +
                "equipment.amount, options.id_order_client_equipment " + (groupByStructure ? ", equipment.id_structure " : "") +
                "FROM " + Lystore.lystoreSchema + ".order_client_options options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment equipment " +
                "ON (options.id_order_client_equipment = equipment.id) " +
                "WHERE " + (isNumberValidation ? "number_validation" : "id_order_client_equipment") + " IN " + Sql.listPrepared(ids.getList()) +
                (structureId != null ? " AND equipment.id_structure = ?" : "") +
                ") as opt";
        query += (groupByStructure || structureId != null ? " INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment equipment ON (opt.id_order_client_equipment = equipment.id)" : "");
        query += " GROUP BY opt.name, opt.price, opt.tax_amount, opt.id_contract" + (groupByStructure ? ", equipment.id_structure" : "");

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < ids.size(); j++) {
                if (isNumberValidation) {
                    params.add(ids.getString(j));
                } else {
                    params.add(ids.getInteger(j));
                }
            }
            if (structureId != null) {
                params.add(structureId);
            }
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }


    @Override
    public void getOrderByValidatioNumber(JsonArray validationNumbers, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT \"price TTC\" as pricettc,tax_amount,  name, id_contract,id_structure, " +
                "SUM(amount) as amount " +
                "FROM " + Lystore.lystoreSchema + ".allOrders  " +
                "WHERE number_validation  IN " + Sql.listPrepared(validationNumbers.getList());
        query += " GROUP BY equipment_key, \"price TTC\",  name, id_contract,id_structure,tax_amount " +
                "UNION " +
                "SELECT " +
                "  opt.price + ( opt.tax_amount * opt.price) as pricettc,tax_amount, " +
                "  opt.name, " +
                "  opt.id_contract," +
                "  id_structure, " +
                "  SUM(opt.amount) as amount " +
                "FROM (" +
                "SELECT options.price, options.tax_amount," +
                "options.name, equipment.id_contract," +
                "equipment.amount, options.id_order_client_equipment, equipment.id_structure " +
                "FROM " + Lystore.lystoreSchema + ".order_client_options options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment equipment " +
                "ON (options.id_order_client_equipment = equipment.id) " +
                "WHERE  number_validation IN " + Sql.listPrepared(validationNumbers.getList()) +
                ") as opt";
        query += " GROUP BY opt.name, opt.price, opt.tax_amount, opt.id_contract,id_structure" ;

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < validationNumbers.size(); j++) {
                params.add(validationNumbers.getString(j));
            }
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrdersGroupByValidationNumber(JsonArray status, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT row.number_validation, row.status, contract.name as contract_name, contract.id as id_contract," +
                "CASE WHEN id_operation is null THEN false else true END as has_operation ,  " +
                "supplier.name as supplier_name, " +
                "array_to_json(array_agg(structure_group.name)) as structure_groups, count(distinct row.id_structure) as structure_count," +
                " supplier.id as supplierId, " +
                Lystore.lystoreSchema + ".order.label_program, " + Lystore.lystoreSchema + ".order.order_number " +
                "FROM " + Lystore.lystoreSchema + ".allOrders row " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (row.id_contract = contract.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".supplier ON (contract.id_supplier = supplier.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (row.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id) " +
                "LEFT OUTER JOIN " + Lystore.lystoreSchema + ".order ON (row.id_order = " + Lystore.lystoreSchema + ".order.id)  " +
                "WHERE row.status IN " + Sql.listPrepared(status.getList()) +
                " GROUP BY  row.number_validation, contract.name, supplier.name, contract.id, supplierId, row.status,has_operation, " + Lystore.lystoreSchema +
                ".order.label_program, " + Lystore.lystoreSchema + ".order.order_number;";

        this.sql.prepared(query, status, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrdersDetailsIndexedByValidationNumber(JsonArray status, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT \"price TTC\" as price, 0 as tax_amount, amount::text, number_validation " +
                "FROM " + Lystore.lystoreSchema + ".allorders " +
                "WHERE status IN " + Sql.listPrepared(status.getList()) +
                " UNION ALL " +
                "SELECT order_client_options.price, order_client_options.tax_amount, order_client_equipment.amount::text, order_client_equipment.number_validation " +
                "FROM " + Lystore.lystoreSchema + ".order_client_options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment ON (order_client_equipment.id = order_client_options.id_order_client_equipment) " +
                "WHERE order_client_equipment.status IN " + Sql.listPrepared(status.getList()) +
                "";

        JsonArray statusList = new fr.wseduc.webutils.collections.JsonArray();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < status.size(); j++) {
                statusList.add(status.getString(j));
            }
        }

        this.sql.prepared(query, statusList, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrdersForCSVExportByValidationNumbers(JsonArray validationNumbers, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT name, SUM(amount) as amount, id_structure, equipment_key " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.getList()) +
                "GROUP BY equipment_key, id_structure, name, id_structure " +
                "UNION ALL " +
                "SELECT order_client_options.name, SUM(order_client_options.amount) as amount, order_client_equipment.id_structure, order_client_equipment.equipment_key " +
                "FROM " + Lystore.lystoreSchema + ".order_client_options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment ON (order_client_options.id_order_client_equipment = order_client_equipment.id) " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.getList()) +
                "GROUP BY equipment_key, id_structure, order_client_options.name, id_structure " +
                "ORDER BY id_structure";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < validationNumbers.size(); j++) {
                params.add(validationNumbers.getString(j));
            }
        }

        this.sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void cancelValidation(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment SET number_validation = '', status = 'WAITING' ,id_order = NULL " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.getList());

        this.sql.prepared(query, validationNumbers, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getOrderFileId(String orderNumber, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT id_mongo FROM " + Lystore.lystoreSchema + ".file " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order ON (" + Lystore.lystoreSchema + ".order.id = file.id_order) " +
                "WHERE order_number = ? " +
                "ORDER BY date DESC " +
                "LIMIT 1;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(orderNumber);

        this.sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateComment(Integer id, String comment, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                " SET comment = ? " +
                " WHERE id = ?; ";
        values.add(comment).add(id);


        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));

    }

    @Override
    public void updatePriceProposal(Integer id, Double price_proposal, Handler<Either<String, JsonObject>> handler) {
        JsonArray values;
        String query = "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment" +
                "SET price_proposal = " + (price_proposal == null ? " null " : " ? ") +
                "WHERE id = ?;";
        values = price_proposal == null ? new JsonArray().add(id) : new JsonArray().add(price_proposal).add(id);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getFile(Integer orderId, String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema + ".order_file WHERE id = ? AND id_order_client_equipment = ?";
        JsonArray params = new JsonArray()
                .add(fileId)
                .add(orderId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight() && event.right().getValue().size() > 0) {
                handler.handle(new Either.Right<>(event.right().getValue().getJsonObject(0)));
            } else {
                handler.handle(new Either.Left<>("Not found"));
            }
        }));
    }

    @Override
    public void getFileOrderRegion(String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema + ".order_region_file WHERE id = ?";
        JsonArray params = new JsonArray()
                .add(fileId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isRight() && event.right().getValue().size() > 0) {
                handler.handle(new Either.Right<>(event.right().getValue().getJsonObject(0)));
            } else {
                handler.handle(new Either.Left<>("Not found"));
            }
        }));
    }


    @Override
    public void listExport(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();

        String price_proposal= "CASE WHEN oe.price_proposal IS NOT NULL THEN " +
                "ROUND (oe.price_proposal*oe.amount,2) " +
                "ELSE ";

        String closeCaseForPriceProposal="END ";

        String query = "SELECT oe.name as equipment_name, oe.amount as equipment_quantity, " +
                "oe.creation_date as equipment_creation_date,title.name as project_name, oe.summary as equipment_summary, " +
                "oe.status as equipment_status,cause_status, price_all_options, " +
                "CASE count(price_all_options) " +
                "WHEN 0 THEN "+price_proposal+"ROUND ((oe.price+( oe.tax_amount*oe.price)/100)*oe.amount,2) "+closeCaseForPriceProposal+
                "ELSE "+price_proposal+"ROUND((price_all_options +( oe.price + ROUND((oe.tax_amount*oe.price)/100,2)))*oe.amount,2) " +closeCaseForPriceProposal+
                "END as price_total_equipment "+
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT ROUND (SUM(( price +( tax_amount*price)/100)*amount),2) as price_all_options," +
                " id_order_client_equipment FROM "+ Lystore.lystoreSchema + ".order_client_options " +

                "GROUP BY id_order_client_equipment)" +
                " opts ON oe.id = opts.id_order_client_equipment " +
                " INNER JOIN " + Lystore.lystoreSchema + ".project on project.id = oe.id_project " +
                "INNER JOIN " + Lystore.lystoreSchema + ".title ON (project.id_title = title.id) " +
                " WHERE id_campaign = ? AND id_structure = ?" +
                " GROUP BY oe.id, price_all_options, title.name ORDER BY creation_date";

        values.add(idCampaign).add(idStructure);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void orderForDelete(Integer idOrder, Handler<Either<String, JsonObject>> handler) {

        String query = "SELECT  oe.id, oe.name,date_trunc('day',oe.creation_date)as creation_date, " +
                " id_campaign, id_structure," +
                " CASE count(opts) " +
                "WHEN 0 THEN ROUND((oe.price + (oe.tax_amount * oe.price)/100), 2) * oe.amount "+
                "ELSE (price_all_options +(ROUND(oe.price + (oe.tax_amount * oe.price)/100, 2))) * oe.amount " +
                "END as price_total_equipment "+
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT SUM((ROUND(price +(tax_amount * price)/100,2))) as price_all_options," +
                " id_order_client_equipment FROM "+ Lystore.lystoreSchema + ".order_client_options " +
                "GROUP BY id_order_client_equipment)" +
                " opts ON oe.id = opts.id_order_client_equipment WHERE id= ? " +
                " GROUP BY oe.id, price_all_options";

        sql.prepared(query,new fr.wseduc.webutils.collections.JsonArray().add(idOrder),SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deletableOrder(Integer idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT count(oce.id) as count" +
                " FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "WHERE oce.status != 'WAITING' AND oce.id = ? ;";

        JsonArray params = new JsonArray().add(idOrder);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deleteFileFromOrder(String fileId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".order_region_file WHERE id = ?";

        JsonArray params = new JsonArray()
                .add(fileId);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void deleteOrderRegionFile(Handler<Either<String, JsonObject>> handler){
        String query =
                "DELETE FROM " + Lystore.lystoreSchema + ".order_region_file " +
                        "WHERE id_order_region_equipment IS NULL;";

        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }



    @Override
    public void deleteOrder(final Integer idOrder, JsonObject order,
                            final String idStructure, final Handler<Either<String, JsonObject>> handler) {
        Integer idCampaign = order.getInteger("id_campaign");
        String getCampaignPurseEnabledQuery = "SELECT purse_enabled FROM " + Lystore.lystoreSchema + ".campaign WHERE id = ?";
        JsonArray params = new JsonArray().add(idCampaign);
        Sql.getInstance().prepared(getCampaignPurseEnabledQuery, params, SqlResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray results = event.right().getValue();
                Boolean purseEnabled = (results.size() > 0 && results.getJsonObject(0).getBoolean("purse_enabled"));
                Double price = Double.valueOf(order.getString("price_total_equipment"));
                try {
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
                    if (purseEnabled) {
                        statements.add(purseService.updatePurseAmountStatement(price, idCampaign, idStructure, "+"));
                    }
                    statements.add(getOptionsOrderDeletion(idOrder));
                    statements.add(getEquipmentOrderDeletion(idOrder));
                    if (purseEnabled) {
                        statements.add(getNewPurse(idCampaign, idStructure));
                    }
                    statements.add(getNewNbOrder(idCampaign, idStructure));

                    sql.transaction(statements, new Handler<Message<JsonObject>>() {
                        @Override
                        public void handle(Message<JsonObject> event) {
                            JsonArray results = event.body().getJsonArray("results");
                            JsonObject res = new JsonObject();
                            JsonObject newPurse = purseEnabled ? results.getJsonObject(3) : new JsonObject();
                            JsonObject newOrderNumber = results.getJsonObject(purseEnabled ? 4 : 2);
                            JsonArray newPurseArray = purseEnabled ? newPurse.getJsonArray("results").getJsonArray(0) : new JsonArray();
                            JsonArray newOrderNumberArray = newOrderNumber.getJsonArray("results").getJsonArray(0);
                            res.put("f1", newPurseArray.size() > 0
                                    ? Double.parseDouble(newPurseArray.getString(0))
                                    : 0);
                            res.put("f2", newOrderNumberArray.size() > 0
                                    ? Double.parseDouble(newOrderNumberArray.getLong(0).toString())
                                    : 0);

                            getTransactionHandler(event, res, handler);

                        }
                    });
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting order elements", e);
                    handler.handle(new Either.Left<>(""));
                }
            } else {
                handler.handle(new Either.Left<>("An error occurred when getting campaign"));
            }
        }));
    }

    @Override
    public  void windUpOrders(List<Integer> ids, JsonArray override_region, Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new JsonArray();
        for(int i = 0 ; i < ids.size() ; i++){
            Integer id = ids.get(i);
            Boolean override = override_region.getBoolean(i);
            statements.add(getUpdateORderDoneStatus(id, override));
        }
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event,0));
            }
        });
    }

    private JsonObject getUpdateORderDoneStatus(Integer id, Boolean override) {
        String query = "";
        JsonArray params = new JsonArray().add(id);
        if(override != null){
            query =  "UPDATE lystore.order_client_equipment " +
                    " SET  status = 'DONE' " +
                    " WHERE id = ?;";
        }else{
            query =  "UPDATE lystore.\"order-region-equipment\"" +
                    " SET  status = 'DONE' " +
                    " WHERE id = ?;";
        }
        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getOptionsOrderDeletion (Integer idOrder){
        String queryDeleteOptionsOrder = "DELETE FROM " + Lystore.lystoreSchema + ".order_client_options"
                + " WHERE id_order_client_equipment = ? ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idOrder);
        return new JsonObject()
                .put("statement", queryDeleteOptionsOrder)
                .put("values", params)
                .put("action", "prepared");
    }

    @Override
    public void sendOrders(List<Integer> ids, List<String> filters, final Handler<Either<String, JsonObject>> handler){

        this.listOrders(ids, filters, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray res = event.right().getValue();
                    final JsonObject ordersObject = formatSendOrdersResult(res);
                    structureService.getStructureById(ordersObject.getJsonArray("id_structures"),
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> structureArray) {
                                    if(structureArray.isRight()){
                                        Either<String, JsonObject> either;
                                        JsonObject returns = new JsonObject()
                                                .put("ordersCSF",
                                                        getOrdersFormatedCSF(ordersObject.getJsonArray("order"),
                                                                (JsonArray) structureArray.right().getValue()))
                                                .put("ordersBC",
                                                        getOrdersFormatedBC(ordersObject.getJsonArray("order"),
                                                                (JsonArray) structureArray.right().getValue()))
                                                .put("total",
                                                        getTotalsOrdersPrices(ordersObject.getJsonArray("order")));
                                        either = new Either.Right<>(returns);
                                        handler.handle(either);
                                    }
                                }
                            });
                } else {
                    handler.handle(new Either.Left<String, JsonObject>("An error occurred when collecting orders"));
                }
            }
        });
    }

    @Override
    public void updateStatusToSent(final List<String> validationNumbers, String status, final String engagementNumber, final String labelProgram, final String dateCreation,
                                   final String orderNumber, final Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT distinct id_order " +
                "FROM " + Lystore.lystoreSchema + ".allOrders orders " +
                "WHERE orders.number_validation IN " + Sql.listPrepared(validationNumbers.toArray());
        Sql.getInstance().prepared(query, new fr.wseduc.webutils.collections.JsonArray(validationNumbers), SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> updateOrCreateEvent) {
                if (updateOrCreateEvent.isRight()) {
                    JsonArray orderIds =  updateOrCreateEvent.right().getValue();
                    JsonObject orderObject = orderIds.getJsonObject(0);
                    if (null == orderObject.getInteger("id_order")) {
                        String nextValQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".order_id_seq') as id";
                        Sql.getInstance().raw(nextValQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
                            @Override
                            public void handle(Either<String, JsonObject> eventId) {
                                if (eventId.isRight()) {
                                    Number orderId = eventId.right().getValue().getInteger("id");
                                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                            .add(getOrderCreateStatement(orderId, engagementNumber, labelProgram, dateCreation, orderNumber))
                                            .add(getAddOrderClientRef(orderId, validationNumbers))
                                            .add(getAddOrderRegionRef(orderId, validationNumbers))
                                            .add(getUpdateClientOrderStatement(new JsonArray(validationNumbers), "SENT"))
                                            .add(getUpdateRegionOrderStatement(new JsonArray(validationNumbers), "SENT"));

                                    Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
                                } else {
                                    handler.handle(new Either.Left<String, JsonObject>(eventId.left().getValue()));
                                }
                            }
                        }));
                    } else {
                        Number orderId = (orderIds.getJsonObject(0)).getInteger("id_order");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getUpdateOrderStatement(engagementNumber, labelProgram, dateCreation, orderNumber, orderId))
                                .add(getAddOrderRegionRef(orderId, validationNumbers))
                                .add(getUpdateClientOrderStatement(new JsonArray(validationNumbers), "SENT"))
                                .add(getUpdateRegionOrderStatement(new JsonArray(validationNumbers), "SENT"));

                        Sql.getInstance().transaction(statements, SqlResult.validRowsResultHandler(handler));
                    }
                } else {
                    handler.handle(new Either.Left<String, JsonObject>(updateOrCreateEvent.left().getValue()));
                }
            }
        }));
    }

    private JsonObject getAddOrderClientRef(Number orderId, List<String> validationNumbers) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET id_order = ? " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.toArray());

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(orderId);
        for (String number : validationNumbers)  {
            params.add(number);
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }


    private JsonObject getAddOrderRegionRef(Number orderId, List<String> validationNumbers) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                "SET id_order = ? " +
                "WHERE number_validation IN " + Sql.listPrepared(validationNumbers.toArray());

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(orderId);
        for (String number : validationNumbers)  {
            params.add(number);
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }


    private JsonObject getUpdateOrderStatement (String engagementNumber, String labelProgram, String dateCreation, String orderNumber, Number orderId) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".order " +
                "SET engagement_number = ?, label_program = ?, date_creation = to_date(?, 'DD/MM/YYYY'), order_number = ? " +
                "WHERE id = ?;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(engagementNumber).add(labelProgram).add(dateCreation)
                .add(orderNumber).add(orderId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getOrderCreateStatement(Number id, String engagementNumber, String labelProgram, String dateCreation,
                                               String orderNumber) {

        String query = "INSERT INTO " + Lystore.lystoreSchema + ".order(id, engagement_number, label_program, date_creation, order_number) " +
                "VALUES (?, ?, ?, to_date(?, 'DD/MM/YYYY'), ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(engagementNumber)
                .add(labelProgram)
                .add(dateCreation)
                .add(orderNumber);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getUpdateClientOrderStatement (JsonArray validationNumbers, String status) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                " SET  status = ? " +
                " WHERE number_validation in " + Sql.listPrepared(validationNumbers.getList()) +";";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(status);

        for (int i = 0; i < validationNumbers.size(); i++) {
            params.add(validationNumbers.getString(i));
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }


    private JsonObject getUpdateRegionOrderStatement (JsonArray validationNumbers, String status) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                " SET  status = ? " +
                " WHERE number_validation in " + Sql.listPrepared(validationNumbers.getList()) +";";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(status);

        for (int i = 0; i < validationNumbers.size(); i++) {
            params.add(validationNumbers.getString(i));
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }


    private JsonObject getAddFileStatement(String mongoId, String owner, Number orderId) {
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".file(id_mongo, owner, id_order) " +
                "VALUES (?, ?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(mongoId)
                .add(owner)
                .add(orderId);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonArray getOrdersFormatedCSF (JsonArray ordersArray, JsonArray structures) {
        JsonArray orders = new fr.wseduc.webutils.collections.JsonArray();
        JsonObject orderOld;
        for (int i = 0 ; i< ordersArray.size(); i++){
            orderOld = ordersArray.getJsonObject(i);
            JsonObject order = orderOld
                    .put("structure",
                            (getStructureObject( structures, orderOld.getString("id_structure"))));
            if (orderOld.getJsonArray("options").size() == 0) {
                order.put("hasOptions", false);
            } else {
                order.put("hasOptions", true);
            }
            orders.add(order);
        }

        return orders;
    }

    private JsonArray getOrdersFormatedBC (JsonArray ordersArray, JsonArray structures) {
        JsonArray orders = new fr.wseduc.webutils.collections.JsonArray();
        boolean isIn;
        JsonObject orderOld;
        JsonObject orderNew;
        for (int i = 0 ; i< ordersArray.size(); i++){
            isIn = false;
            orderOld = ordersArray.getJsonObject(i);
            for(int j = 0; j<orders.size(); j++){
                orderNew = orders.getJsonObject(j);
                if(orderOld.getInteger("equipment_key").equals(orderNew.getInteger("equipment_key"))){
                    isIn = true;
                    JsonArray structure;
                    structure = orderNew.getJsonArray("structures");
                    structure.add(getStructureObject( structures,
                            orderOld.getString("id_structure"),
                            orderOld.getInteger("amount").toString(),
                            orderOld.getString("number_validation")));
                    orderNew.put("structures", structure);
                    Integer amount = (Integer.parseInt(orderOld.getInteger("amount").toString()) +
                            Integer.parseInt( orderNew.getInteger("amount").toString())) ;
                    orderNew.put("amount",amount.toString());
                }
            }
            if(! isIn) {
                JsonObject order = new JsonObject()
                        .put("price", orderOld.getString("price"))
                        .put("tax_amount", orderOld.getString("tax_amount"))
                        .put("amount", orderOld.getInteger("amount"))
                        .put("id_campaign", orderOld.getInteger("id_campaign").toString())
                        .put("name", orderOld.getString("name"))
                        .put("summary", orderOld.getString("summary"))
                        .put("description", orderOld.getString("description"))
                        .put("image", orderOld.getString("image"))
                        .put("technical_spec", orderOld.getString("technical_spec"))
                        .put("id_contract", orderOld.getInteger("id_contract").toString())
                        .put("equipment_key", orderOld.getInteger("equipment_key"))
                        .put("contract", new JsonObject( orderOld.getString("contract")))
                        .put("supplier",new JsonObject( orderOld.getString("supplier")) )
                        .put("campaign", new JsonObject( orderOld.getString("campaign")))
                        .put("options", orderOld.getJsonArray("options"))
                        .put("structures", new fr.wseduc.webutils.collections.JsonArray()
                                .add(getStructureObject( structures,
                                        orderOld.getString("id_structure"),
                                        orderOld.getInteger("amount").toString(),
                                        orderOld.getString("number_validation"))));
                orders.add(order);
            }
        }
        return orders;
    }

    private JsonObject getStructureObject(JsonArray structures, String structureId ){
        JsonObject structure = new JsonObject();
        for (int i = 0; i < structures.size() ; i++) {
            if((structures.getJsonObject(i)).getString("id").equals(structureId)){
                structure =  structures.getJsonObject(i);
            }
        }
        return structure;
    }

    private JsonObject getStructureObject(JsonArray structures, String structureId,
                                          String amount, String numberValidation ){
        JsonObject structure = new JsonObject();
        for (int i = 0; i < structures.size() ; i++) {
            if((structures.getJsonObject(i)).getString("id").equals(structureId)){
                structure = (structures.getJsonObject(i)).copy();
                structure.put("amount", amount)
                        .put("number_validation", numberValidation);
            }
        }
        return structure;
    }

    private JsonObject getTotalsOrdersPrices(JsonArray orders){

        Double tva = new Double(0);
        Double total = new Double(0);
        final Integer Const = 100;
        Double totalTTC ;
        try {
            tva = Double.parseDouble((orders.getJsonObject(0)).getString("tax_amount"));
        }catch (ClassCastException e) {
            log.error("An error occurred when casting tax amount", e);
        }
        for (int i = 0; i < orders.size(); i++) {
            try {
                total += Double.parseDouble((orders.getJsonObject(0)).getString("price")) *
                        Double.parseDouble((orders.getJsonObject(0)).getInstant("amount").toString());
            }catch (ClassCastException e) {
                log.error("An error occurred when casting order price", e);
            }
        }
        totalTTC = (total * tva)/Const + total;
        return new JsonObject()
                .put("totalPrice", total)
                .put("tva", tva)
                .put("totalTTC", totalTTC)
                ;
    }

    private JsonObject formatSendOrdersResult(JsonArray orders){
        JsonObject orderObject = new JsonObject();
        JsonArray structures = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray ordersList = new fr.wseduc.webutils.collections.JsonArray();
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            structures.add(order.getString("id_structure"));
            order.put("options",
                    !order.getString("options").contains("null")
                            ? new fr.wseduc.webutils.collections.JsonArray(order.getString("options"))
                            : new fr.wseduc.webutils.collections.JsonArray());
            ordersList.add(order);
        }
        orderObject.put("order", ordersList)
                .put ("id_structures", structures);
        return orderObject;
    }

    private JsonObject getEquipmentOrderDeletion (Integer idOrder){
        String queryDeleteEquipmentOrder = "DELETE FROM " + Lystore.lystoreSchema + ".order_client_equipment"
                + " WHERE id = ? ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idOrder);

        return new JsonObject()
                .put("statement", queryDeleteEquipmentOrder)
                .put("values", params)
                .put("action", "prepared");
    }

    private  JsonObject getNewPurse(Integer idCampaign, String idStructure){
        String query = "SELECT amount FROM " + Lystore.lystoreSchema + ".purse " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure);

        return  new JsonObject()
                .put("statement",query)
                .put("values",params)
                .put("action", "prepared");
    }
    private JsonObject getNewNbOrder(Integer idCampaign, String idStructure) {

        String query = "SELECT count(id) FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? AND status != 'VALID';";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idCampaign).add(idStructure);

        return  new JsonObject()
                .put("statement",query)
                .put("values",params)
                .put("action", "prepared");
    }

    public static String getNextValidationNumber() {
        return "Select "+ Lystore.lystoreSchema + ".get_validation_number() as numberOrder ";
    }
    @Override
    public void validateOrders(final HttpServerRequest request, final UserInfos user, final List<Integer> ids,
                               final String url, final Handler<Either<String, JsonObject>> handler){
        String getIdQuery = getNextValidationNumber();
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final String numberOrder = event.right().getValue().getString("numberorder");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getValidateStatusStatement(ids, numberOrder, "VALID"))
                                .add(getAgentInformation( ids));
                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> jsonObjectMessage) {


                                final JsonArray rows = ((jsonObjectMessage).body()
                                        .getJsonArray("results").getJsonObject(1)).getJsonArray("results");
                                JsonArray names = new fr.wseduc.webutils.collections.JsonArray();
                                final int agentNameIndex = 2;
                                final int structureIdIndex = 4;
                                JsonArray structureIds = new fr.wseduc.webutils.collections.JsonArray();
                                for (int j = 0; j < rows.size(); j++) {


                                    names.add((rows.getJsonArray(j)).getString(agentNameIndex));
                                    structureIds.add((rows.getJsonArray(j)).getString(structureIdIndex));
                                }
                                final JsonArray agentNames = names;
                                emailSender.getPersonnelMailStructure(structureIds,
                                        new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> stringJsonArrayEither) {

                                                try {
                                                    final JsonObject result = new JsonObject()
                                                            .put("number_validation", numberOrder)
                                                            .put("agent", agentNames);
                                                    handler.handle(new Either.Right<String, JsonObject>(result));
                                                    emailSender.sendMails(request, result, rows, user, url,
                                                            (JsonArray) stringJsonArrayEither.right().getValue());
                                                }catch (NullPointerException e){
                                                    log.error("no mail to send");
                                                }
                                            }
                                        });
                            }
                        });
                    } catch (ClassCastException e) {
                        log.error("An error occurred when casting numberOrder", e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                } else {
                    log.error("An error occurred when selecting number of the order");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }


    private static JsonObject getAgentInformation(List<Integer> ids){
        String query = "SELECT oce.id, contract.name, agent.name, agent.email, oce.id_structure , oce.id_campaign" +
                " FROM lystore.order_client_equipment oce " +
                " INNER JOIN lystore.contract ON contract.id = oce.id_contract " +
                " INNER JOIN lystore.agent ON contract.id_agent= agent.id " +
                " WHERE oce.id in "+ Sql.listPrepared(ids.toArray()) +"" +
                " ORDER BY id_structure,id_campaign ;  ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add( id);
        }
        return new JsonObject().put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getValidateStatusStatement(List<Integer>  ids, String numberOrder, String status){

        String query = "UPDATE lystore.order_client_equipment " +
                " SET  status = ?, number_validation = ?  " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(status).add(numberOrder);

        for (Integer id : ids) {
            params.add( id);
        }

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getUpdateStatusStatement(List<Integer>  ids, String status){

        String query = "UPDATE lystore.order_client_equipment " +
                " SET  status = ? " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +";";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray().add(status);

        for (Integer id : ids) {
            params.add( id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }


    private static void getTransactionHandler(Message<JsonObject> event, JsonObject amountPurseNbOrder,
                                              Handler<Either<String, JsonObject>> handler) {
        JsonObject result = event.body();
        if (result.containsKey("status")&& "ok".equals(result.getString("status"))){
            JsonObject returns = new JsonObject();

            returns.put("amount", amountPurseNbOrder.getDouble("f1"));
            returns.put("nb_order",amountPurseNbOrder.getDouble("f2"));
            handler.handle(new Either.Right<String, JsonObject>(returns));
        }  else {
            log.error("An error occurred when launching 'order' transaction");
            handler.handle(new Either.Left<String, JsonObject>(""));
        }

    }

    @Override
    public void getExportCsvOrdersAdmin(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT oce.id, oce.id_structure as idStructure, contract.name as namecontract," +
                " supplier.name as namesupplier, campaign.name as namecampaign, oce.amount as qty," +
                " oce.creation_date as date, CASE count(priceOptions)"+
                "WHEN 0 THEN ROUND ((oce.price+( oce.tax_amount*oce.price)/100)*oce.amount,2)"+
                "ELSE ROUND((priceOptions +( oce.price + ROUND((oce.tax_amount*oce.price)/100,2)))*oce.amount,2) "+
                "END as priceTotal "+
                "FROM "+ Lystore.lystoreSchema +".order_client_equipment  oce "+
                "LEFT JOIN (SELECT ROUND (SUM(( price +( tax_amount*price)/100)*amount),2) as priceOptions, "+
                "id_order_client_equipment FROM "+ Lystore.lystoreSchema +".order_client_options  GROUP BY id_order_client_equipment) opts "+
                "ON oce.id = opts.id_order_client_equipment "+
                "LEFT JOIN "+ Lystore.lystoreSchema +".contract ON contract.id=oce.id_contract "+
                "INNER JOIN "+ Lystore.lystoreSchema +".campaign ON campaign.id = oce.id_campaign "+
                "INNER JOIN "+ Lystore.lystoreSchema +".supplier ON contract.id_supplier = supplier.id "+
                "WHERE oce.id in "+ Sql.listPrepared(idsOrders.toArray()) +
                " GROUP BY oce.id, idStructure, qty,date,oce.price, oce.tax_amount, oce.id_campaign, priceOptions," +
                " namecampaign, namecontract, namesupplier ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for(Integer id : idsOrders){
            params.add(id);
        }

        sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }
    @Override
    public void updateRank( JsonArray orders, Handler<Either<String, JsonObject>> handler) {
        String query= "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment SET "+
                "rank = ? " +
                "WHERE id = ? RETURNING order_client_equipment.id  ;  " +
                "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment SET "+
                "rank = ? " +
                "WHERE id = ? RETURNING order_client_equipment.id ; ";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for(Object object : orders){
            values.add(((JsonObject) object).getInteger("rank"));
            values.add(((JsonObject) object).getInteger("id"));
        }
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        statements.add(new JsonObject()
                .put("statement",query)
                .put("values",values)
                .put("action","prepared"));
        sql.transaction(statements, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void updateOperation(Integer idOperation, JsonArray idOrders, Handler<Either<String, JsonObject>> handler) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                " SET id_operation = " +
                idOperation +
                " WHERE id IN " +
                Sql.listPrepared(idOrders.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < idOrders.size(); i++) {
            values.add(idOrders.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void updateOperationInProgress(Integer idOperation, JsonArray idOrders, Handler<Either<String, JsonObject>> handler) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                " SET status = 'IN PROGRESS', " +
                "id_operation = " +
                idOperation +
                " WHERE id IN " +
                Sql.listPrepared(idOrders.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < idOrders.size(); i++) {
            values.add(idOrders.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void updateStatusOrder(Integer idOrder, JsonObject orderStatus, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET id_operation = null, " +
                "status = ? " +
                "WHERE id = " +
                idOrder +
                " RETURNING id";

        values.add(orderStatus.getString("status"));
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOrder(Integer idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT oec.* , array_to_json( array_agg( project.* )) as project , array_to_json ( array_agg ( campaign.*)) as campaign " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment oec " +
                "INNER JOIN " + Lystore.lystoreSchema + ".project on oec.id_project = project.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".campaign on oec.id_campaign =  campaign.id " +
                "where oec.id = ? " +
                "group by oec.id";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void setInProgress(JsonArray ids, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET  " +
                "status = ? " +
                "WHERE id IN " +
                Sql.listPrepared(ids.getList()) +
                " RETURNING id";

        values.add("IN PROGRESS");
        for (int i = 0; i < ids.size(); i++) {
            values.add(ids.getInteger(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));

    }
    public void getOneOrderClient(int idOrder, Handler<Either<String, JsonObject>> handler){
        String query = "" +
                "SELECT oce.*, " +
                "       (  " +
                "               (SELECT " +
                "                  CASE " +
                "                  WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount)  IS NULL THEN 0 " +
                "                  WHEN oce.price_proposal IS NOT NULL THEN 0 " +
                "                  ELSE  SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) " +
                "                  END " +
                "               FROM " + Lystore.lystoreSchema +".order_client_options oco " +
                "               WHERE id_order_client_equipment = oce.id) + " +
                "                                                         (CASE  " +
                "                                                             WHEN oce.price_proposal IS NOT NULL THEN (oce.price_proposal)  " +
                "                                                             ELSE (oce.price + ((oce.price * oce.tax_amount) /100))  " +
                "                                                         END)) AS price_single_ttc,  " +
                "       to_json(contract.*) contract, " +
                "       to_json(ct.*) contract_type, " +
                "       to_json(campaign.*) campaign, " +
                "       to_json(prj.*) AS project, " +
                "       to_json(tt.*) AS title " +
                "FROM  " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "LEFT JOIN  " + Lystore.lystoreSchema + ".contract ON oce.id_contract = contract.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ct ON ct.id = contract.id_contract_type " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".project AS prj ON oce.id_project = prj.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".title AS tt ON tt.id = prj.id_title " +
                "WHERE oce.id = ? " +
                "GROUP BY (prj.id, " +
                "          oce.id, " +
                "          contract.id, " +
                "          ct.id, " +
                "          campaign.id, " +
                "          tt.id)";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getOrderBCParams(JsonArray validationNumbers, Handler<Either<String, JsonObject>> handler) {
        //MDOIFIER
        String query = "SELECT DISTINCT engagement_number, label_program , order_number " +
                " FROM " + Lystore.lystoreSchema + ".order od " +
                " INNER JOIN " + Lystore.lystoreSchema + ".allOrders orders on orders.id_order = od.id " +
                " WHERE orders.number_validation = ?";

        JsonArray params = new JsonArray().add(validationNumbers.getString(0));

        Sql.getInstance().prepared(query,params,SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void listOrderSent(String status, List<String> filters, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT orders.id, " +
                "       orders.amount, " +
                "       orders.id_campaign, " +
                "       orders.id_structure, " +
                "       orders.NAME, " +
                "       orders.status, " +
                "       orders.id_contract, " +
                "       orders.equipment_key, " +
                "       contract.NAME                                           AS contract_name, " +
                "       supplier.NAME                                           AS supplier_name, " +
                "       Array_to_json(Array_agg(DISTINCT order_file.*))         AS files, " +
                "       Array_to_json(Array_agg(DISTINCT oco.*))                AS options," +
                "       orders.\"price TTC\" as price, " +
                "       orders.tax_amount, " +
                "       orders.cause_status, " +
                "       orders.number_validation, " +
                "       orders.id_project, " +
                "       orders.action, " +
                "       Array_to_json(Array_agg(DISTINCT structure_group.NAME)) AS " +
                "       structure_groups, " +
                "       ord.order_number, " +
                "       orders.id_operation, " +
                "       orders.override_region, " +
                "       orders.id_type, " +
                Lystore.lystoreSchema + ".order_total(oce.id) AS Total " +
                "FROM   " + Lystore.lystoreSchema + ".allorders orders " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign " +
                "               ON ( orders.id_campaign = rel_group_campaign.id_campaign ) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure " +
                "               ON ( orders.id_structure = rel_group_structure.id_structure ) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".structure_group " +
                "               ON ( rel_group_structure.id_structure_group = structure_group.id " +
                "                    AND rel_group_campaign.id_structure_group = " +
                "                        structure_group.id ) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".contract " +
                "               ON contract.id = orders.id_contract " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".supplier " +
                "               ON supplier.id = contract.id_supplier " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".order ord " +
                "               ON orders.id_order = ord.id " +
                "       LEFT JOIN " + Lystore.lystoreSchema + ".order_client_options oco " +
                "              ON oco.id_order_client_equipment = orders.id " +
                "       LEFT JOIN " + Lystore.lystoreSchema + ".order_file " +
                "              ON orders.id = order_file.id_order_client_equipment " +
                "WHERE  orders.status = ? and override_region is not true " +
                "GROUP  BY orders.id, " +
                "          orders.id_project, " +
                "          orders.id_structure, " +
                "          orders.id_contract, " +
                "    orders.amount, " +
                "          ord.order_number, " +
                "    orders.id_campaign, " +
                "    orders.name, " +
                "          supplier.NAME, " +
                "    orders.status, " +
                "        orders.id_contract, " +
                "        orders.equipment_key, " +
                "   orders.cause_status, " +
                "   orders.action, " +
                "   orders.override_region, " +
                "   orders.id_type, " +
                "   orders.price_proposal, " +
                "   orders.id_operation, " +
                "   orders.\"price TTC\" , " +
                "       orders.tax_amount, " +
                "   orders.number_validation, " +
                "          contract.NAME " +
                "ORDER  BY id DESC; " +
                " ;";
        JsonArray params = new JsonArray().add(status);
        if (!filters.isEmpty()) {
            sql.prepared(query, params, SqlResult.validResultHandler(filterOrders(filters, status, handler)));
        }else{
            sql.prepared(query, params, SqlResult.validResultHandler(handler));
        }

    }

    @Override
    public void createRejectOrders(JsonObject rejectOrders, Handler<Either<String, JsonObject>> handler) {
        /*
        Ici recup les rejectorders puis faire statementes de création des rejectOrders ET d update des commandes
         */
        log.info(rejectOrders);
        JsonArray statements = new JsonArray();
        JsonArray ordersArray = rejectOrders.getJsonArray("ordersToReject");
        for(int i = 0; i < ordersArray.size(); i++) {
            JsonObject order = ordersArray.getJsonObject(i);
            statements.add(createRejectOrder(order.getInteger("id_order"), order.getString("comment")));
            statements.add(updateStatusRejectOrder(order.getInteger("id_order")));
            statements.add(updatePurse(order.getInteger("id_order")));
            //get info + update (coir si select update)
        }

       handleRejectOrders(ordersArray.getJsonObject(0).getInteger("id_order"), statements, handler);

    }

    private JsonObject updatePurse(Integer id_order) {
        String statement = " " +
                "UPDATE " +
                "    lystore.purse " +
                "SET " +
                "    amount = purse.amount +         Round(( (SELECT CASE  " +
                "                                        WHEN oce.price_proposal IS NOT NULL THEN 0  " +
                "                                        WHEN oce.override_region IS NULL THEN 0  " +
                "                                        WHEN Sum(oco.price + ( ( oco.price * oco.tax_amount ) / " +
                "                                                                100 ) " +
                "                                                              *  " +
                "                                                              oco.amount) IS  " +
                "                                              NULL THEN 0  " +
                "                                         ELSE Sum(oco.price + ( ( oco.price * oco.tax_amount )/ " +
                "                                                                100 )  " +
                "                                                              *  " +
                "                                                              oco.amount)  " +
                "                                      END  " +
                "                                FROM   lystore.order_client_options oco " +
                "                                WHERE  oco.id_order_client_equipment = oce.id) " +
                "                               + (CASE   WHEN oce.price_proposal IS NOT NULL THEN oce.price_proposal  " +
                "                               ELSE oce.price + oce.price * oce.tax_amount / 100 END " +
                "                ) * oce.amount ), 2) " +
                "                         " +
                "FROM " +
                "    lystore.purse as purse_tmp " +
                "    INNER JOIN lystore.order_client_equipment AS oce " +
                "        ON purse_tmp.id_campaign = oce.id_campaign AND purse_tmp.id_structure = oce.id_structure " +
                "WHERE " +
                "    oce.id = ? AND purse_tmp.id = purse.id; " +
                "\t";

        JsonArray  params = new JsonArray().add(id_order);
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    private void handleRejectOrders(Number id, JsonArray statements, Handler<Either<String, JsonObject>> handler) {
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
            }
        });
    }

    private JsonObject createRejectOrder(Integer id_order, String comment) {
        String statement = "INSERT INTO " +
                Lystore.lystoreSchema + " .order_reject(id_order, comment) " +
                "VALUES (?, ?) RETURNING id;";

        JsonArray params = new JsonArray().add(id_order).add(comment);
        return new JsonObject()
                .put("statement", statement)
                .put("values",  params)
                .put("action", "prepared");
    }

    private JsonObject updateStatusRejectOrder(Integer id_order) {
        String statement = "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                " SET status = 'REJECTED' " +
                " WHERE id = ? " +
                " RETURNING id;";

        JsonArray  params = new JsonArray().add(id_order);
        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    public void getRejectOrderComment(int idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT order_reject.id_order, order_reject.comment FROM " + Lystore.lystoreSchema + ".order_reject " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment ON order_reject.id_order = order_client_equipment.id " +
                "WHERE order_client_equipment.id_campaign = ?;";
        sql.prepared(query, new JsonArray().add(idCampaign), SqlResult.validResultHandler(handler));
    }
    private Handler<Either<String, JsonArray>> filterOrders(List<String> filters, String status, Handler<Either<String, JsonArray>> handler) {
        return new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                structureService.getStructures(new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> eventStructure) {
                        JsonArray orders = event.right().getValue();
                        JsonArray structures = eventStructure.right().getValue();
                        for(int i = 0 ; i < orders.size();i++ ){
                            JsonObject order = orders.getJsonObject(i);
                            List<JsonObject> setStruct =    structures.stream()
                                    .map(JsonObject.class::cast)
                                    .filter(structure -> structure.getString("id").equals(order.getString("id_structure")))
                                    .collect(Collectors.toList());

                            order.put("uai",setStruct.get(0).getString("uai"));
                            order.put("department",setStruct.get(0).getString("department"));
                            order.put("name_etab",setStruct.get(0).getString("name"));
                            order.put("academy",setStruct.get(0).getString("academy"));
                            order.put("city",setStruct.get(0).getString("city"));
                            order.put("type_etab",setStruct.get(0).getString("type_etab"));
                            while(order.getString("name_etab").contains("  ")){
                                order.put("name_etab",order.getString("name_etab").replaceAll(" {2}", " "));
                            }
                        }
                        for(int i = 0 ; i < orders.size();i++ ) {
                            JsonObject order = orders.getJsonObject(i);
                            List<String> keys = new ArrayList<>(order.fieldNames());
                            boolean thisFilter = false, isFiltered = true;

                            for(String filter : filters){
                                thisFilter = false;
                                if(status.equals("WAITING")) {
                                    for (String key : keys) {
                                        if (order.getValue(key) != null) {
                                            thisFilter = thisFilter || order.getValue(key).toString().toLowerCase().contains(filter.toLowerCase());
                                        }
                                    }
                                }
                                else{
                                    try{
                                        thisFilter = thisFilter || order.getValue("name_etab").toString().toLowerCase().contains(filter.toLowerCase());
                                        thisFilter = thisFilter || order.getValue("order_number").toString().toLowerCase().contains(filter.toLowerCase());
                                        thisFilter = thisFilter || order.getValue("uai").toString().toLowerCase().contains(filter.toLowerCase());
                                        thisFilter = thisFilter || order.getValue("contract_name").toString().toLowerCase().contains(filter.toLowerCase());
                                        thisFilter = thisFilter || order.getValue("supplier_name").toString().toLowerCase().contains(filter.toLowerCase());
                                        thisFilter = thisFilter || order.getValue("amount").toString().toLowerCase().contains(filter.toLowerCase());
                                        thisFilter = thisFilter || order.getValue("total").toString().toLowerCase().contains(filter.toLowerCase());
                                    }catch (NullPointerException e){
                                        log.info("LYSTORE " + e.getMessage());
                                        log.info(order);
                                    }
                                }
                                isFiltered = isFiltered && thisFilter;
                            }
                            order.put("isFiltered",isFiltered);
                        }
                        orders = new JsonArray(orders.stream()
                                .map(JsonObject.class::cast)
                                .filter(order -> order.getBoolean("isFiltered"))
                                .collect(Collectors.toList()));
                        handler.handle(new Either.Right<>(orders));
                    }
                });
            }
        };
    }

    @Override
    public JsonArray filterValidOrders(JsonArray orders, List<String> filters) {
        for(int i = 0 ; i < orders.size();i++ ) {
            JsonObject order = orders.getJsonObject(i);
            boolean isFiltered = true;

            for(String filter : filters){
                boolean thisFilter = false;
                thisFilter = thisFilter || order.getValue("number_validation").toString().toLowerCase().contains(filter.toLowerCase());
                thisFilter = thisFilter || order.getValue("contract_name").toString().toLowerCase().contains(filter.toLowerCase());
                thisFilter = thisFilter || order.getValue("supplier_name").toString().toLowerCase().contains(filter.toLowerCase());
                if(order.containsKey("label_program") && order.getValue("label_program") != null)
                    thisFilter = thisFilter || order.getValue("label_program").toString().toLowerCase().contains(filter.toLowerCase());
                if(order.containsKey("order_number") && order.getValue("order_number") != null)
                    thisFilter = thisFilter || order.getValue("order_number").toString().toLowerCase().contains(filter.toLowerCase());
                thisFilter = thisFilter || order.getValue("price").toString().toLowerCase().contains(filter.toLowerCase());
                thisFilter = thisFilter || order.getValue("structure_count").toString().toLowerCase().contains(filter.toLowerCase());
                isFiltered = isFiltered && thisFilter;
            }
            order.put("isFiltered",isFiltered);
        }
        orders = new JsonArray(orders.stream()
                .map(JsonObject.class::cast)
                .filter(order -> order.getBoolean("isFiltered"))
                .collect(Collectors.toList()));
        return orders;
    }

    @Override
    public void listOrderWaiting(List<String> idCampaigns, List<String> filters, Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT oce.id, oce.price, oce.tax_amount, oce.amount, oce.creation_date, oce.id_campaign, oce.id_structure, oce.name, oce.summary, oce.description," +
                " oce.image, oce.technical_spec, oce.status, oce.id_contract, oce.equipment_key,title.name as project_name," +
                " project.description as project_description,project.room as project_room, project.building as project_building ,contract_type.name as contract_type_name, " +
                " " +
                " array_to_json(array_agg(DISTINCT order_file.*)) as files , " +
                " array_to_json(array_agg( DISTINCT oco.*)) as options," +
                " oce.cause_status, oce.number_validation, oce.id_order, oce.comment, oce.price_proposal, oce.id_project, oce.rank, oce.program," +
                " oce.action, array_to_json(array_agg( distinct structure_group.name)) as structure_groups, " +
                " oce.id_operation, oce.override_region, oce.id_type,  " +
                Lystore.lystoreSchema + ".order_total(oce.id) AS Total " +
                " FROM lystore.order_client_equipment oce " +
                "INNER JOIN lystore.project ON (oce.id_project = project.id) " +
                "INNER JOIN lystore.title ON (project.id_title = title.id) " +
                "INNER JOIN lystore.contract ON (oce.id_contract = contract.id) " +
                "INNER JOIN lystore.contract_type ON (contract.id_contract_type = contract_type.id) " +
                "INNER JOIN lystore.rel_group_campaign ON (oce.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN lystore.rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN lystore.structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "AND rel_group_campaign.id_structure_group = structure_group.id) " +
                "LEFT JOIN lystore.order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                " LEFT JOIN " + Lystore.lystoreSchema + ".order_file ON oce.id = order_file.id_order_client_equipment " +
                " WHERE oce.status = 'WAITING' "
                + ((idCampaigns.isEmpty()) ? " " : " AND oce.id_campaign IN  " + Sql.listPrepared(idCampaigns) )+

                "GROUP  BY oce.id, " +
                "    oce.id_project, " +
                "    oce.id_structure, " +
                "    oce.id_contract," +
                "   project_description, " +
                "project_room , " +
                "project_building," +
                "   contract_type_name, " +
                "   title.name" +
                " ORDER by id DESC" +
                " ;";
        JsonArray params = new JsonArray();

        for (String idC : idCampaigns) {
            params.add(Integer.parseInt(idC));
        }
        if (!filters.isEmpty()) {
            sql.prepared(query, params, SqlResult.validResultHandler(filterOrders(filters,"WAITING",   handler)));
        }else{
            sql.prepared(query, params, SqlResult.validResultHandler(handler));
        }
    }
}

