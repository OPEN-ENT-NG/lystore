package fr.openent.lystore.factory;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.constants.LystoreBDD;
import fr.openent.lystore.service.*;
import fr.openent.lystore.service.impl.*;
import fr.openent.lystore.service.parameter.ActiveStructureService;
import fr.openent.lystore.service.parameter.ParameterService;
import fr.openent.lystore.service.parameter.impl.DefaultActiveStructureService;
import fr.openent.lystore.service.parameter.impl.DefaultParameterService;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;

public class ServiceFactory {

    private final Vertx vertx;
    private final Storage storage;
    private final Neo4j neo4j;
    private final Sql sql;
    private final MongoDb mongoDb;
    private final JsonObject config;
    private final JsonObject slackConfig;
    private final EmailSender emailSender;
    private final EventBus eb;

    public ServiceFactory(Vertx vertx, Storage storage, Neo4j neo4j, Sql sql, MongoDb mongoDb, JsonObject config, EventBus eb) {
        this.vertx = vertx;
        this.storage = storage;
        this.neo4j = neo4j;
        this.sql = sql;
        this.mongoDb = mongoDb;
        this.config = config;
        this.slackConfig = config.getJsonObject("slack", new JsonObject());
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        this.emailSender = emailFactory.getSender();
        this.eb = eb;

    }

    public JsonObject getConfig() {
        return config;
    }

    public Storage getStorage() {
        return storage;
    }

    public Vertx getVertx() {
        return vertx;
    }

    public EventBus getEb() {
        return eb;
    }

    public ActiveStructureService activeStructureService() {
        return new DefaultActiveStructureService(eb);
    }

    public AgentService agentService() {
        return new DefaultAgentService(Lystore.lystoreSchema, LystoreBDD.AGENT);
    }

    public BasketService basketService() {
        return new DefaultBasketService(Lystore.lystoreSchema, LystoreBDD.BASKET,config.getJsonObject("mail", new JsonObject()));
    }

    public CampaignService campaignService() {
        return new DefaultCampaignService(Lystore.lystoreSchema, LystoreBDD.CAMPAIGN);
    }

    public ContractService contractService() {
        return new DefaultContractService(Lystore.lystoreSchema, LystoreBDD.CONTRACT);
    }

    public ContractTypeService contractTypeService() {
        return new DefaultContractTypeService(Lystore.lystoreSchema, LystoreBDD.CONTRACT_TYPE);
    }

    public EquipmentService equipmentService() {
        return new DefaultEquipmentService(Lystore.lystoreSchema, LystoreBDD.EQUIPMENT);
    }

    public EquipmentTypeService equipmentTypeService() {
        return new DefaultEquipmentType(Lystore.lystoreSchema, LystoreBDD.EQUIPMENT_TYPE);
    }

    public ExportPDFService exportPDFService() {
        return new DefaultExportPDFService(vertx, config);
    }

    public ExportService exportService() {
        return new DefaultExportServiceService(storage);
    }

    public GradeService gradeService() {
        return new DefaultGradeService(Lystore.lystoreSchema, LystoreBDD.GRADE);
    }

    public InstructionService instructionService() {
        return new DefaultInstructionService(Lystore.lystoreSchema, LystoreBDD.INSTRUCTION);
    }

    public LabelOperationService labelOperationService() {
        return new DefaultLabelOperationService(LystoreBDD.LABEL_OPERATION);
    }

    public LogService logService() {
        return new DefaultLogService();
    }

    public NotificationService notificationService() {
        return new SlackService(
                vertx,
                slackConfig.getString("api-uri"),
                slackConfig.getString("token"),
                slackConfig.getString("bot-username"),
                slackConfig.getString("channel")
        );
    }

    public OperationService operationService() {
        return new DefaultOperationService(Lystore.lystoreSchema, LystoreBDD.OPERATION);
    }

    public OrderRegionService orderRegionService() {
        return new DefaultOrderRegionService(LystoreBDD.ORDER_REGION_EQUIPMENT);
    }

    public OrderService orderService() {
        return new DefaultOrderService(Lystore.lystoreSchema, LystoreBDD.ORDER_CLIENT_EQUIPMENT, emailSender);
    }

    public ParameterService parameterService() {
        return new DefaultParameterService(Lystore.lystoreSchema, LystoreBDD.PARAMETER_BC_OPTIONS);
    }

    public ProgramService programService() {
        return new DefaultProgramService(Lystore.lystoreSchema, LystoreBDD.PROGRAM);
    }

    public ProjectService projectService() {
        return new DefaultProjectService(Lystore.lystoreSchema, LystoreBDD.PROJECT);
    }

    public PurseService purseService() {
        return new DefaultPurseService();
    }

    public StructureGroupService structureGroupService() {
        return new DefaultStructureGroupService(Lystore.lystoreSchema, LystoreBDD.STRUCTURE_GROUP);
    }

    public StructureService structureService() {
        return new DefaultStructureService(Lystore.lystoreSchema);
    }

    public SupplierService supplierService() {
        return new DefaultSupplierService(Lystore.lystoreSchema, LystoreBDD.SUPPLIER);
    }

    public TagService tagService() {
        return new DefaultTagService(Lystore.lystoreSchema, LystoreBDD.TAG);
    }

    public TaxService taxService() {
        return new DefaultTaxService(Lystore.lystoreSchema, LystoreBDD.TAX);
    }

    public TitleService titleService() {
        return new DefaultTitleService(Lystore.lystoreSchema, LystoreBDD.TITLE);
    }

    public UserInfoService userInfoService() {
        return new DefaultUserInfoService();
    }

    public UserService userService() {
        return new DefaultUserService();
    }
}