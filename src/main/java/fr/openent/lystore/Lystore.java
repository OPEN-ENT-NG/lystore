package fr.openent.lystore;

import fr.openent.lystore.controllers.*;
import fr.openent.lystore.controllers.parameter.ActiveStructureController;
import fr.openent.lystore.controllers.parameter.ParameterController;
import fr.openent.lystore.export.ExportLystoreWorker;
import fr.openent.lystore.service.ServiceFactory;
import fr.wseduc.mongodb.MongoDb;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BaseServer;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.sql.Sql;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Lystore extends BaseServer {

    public static final String ORDERSSENT ="ORDERSSENT" ;
    public static String lystoreSchema;
    public static Integer iterationWorker;
    public static JsonObject CONFIG;
    public static Storage STORAGE;
    public static Integer PAGE_SIZE = 50;
    public static final String LYSTORE_COLLECTION = "lystore_export";
    public static final String ADMINISTRATOR_RIGHT = "lystore.administrator";
    public static final String MANAGER_RIGHT = "lystore.manager";
    public static long timeout = 99999999999L;
    public static final String ORDERS = "ORDERS";
    public static final String INSTRUCTIONS = "INSTRUCTION";
    public static final String CAMPAIGN = "CAMPAIGN";
    public static final String XLSX = "xlsx";
    public static final String PDF = "pdf";

    @Override
    public void start() throws Exception {
        super.start();
        lystoreSchema = config.getString("db-schema");
       if(config.containsKey("iteration-worker")){
           iterationWorker = config.getInteger("iteration-worker");
       }else{
           log.info("no iteration worker in config");
           iterationWorker = 10 ;
        }
        EventBus eb = getEventBus(vertx);
        Storage storage = new StorageFactory(vertx, config).getStorage();
        STORAGE = storage;

        ServiceFactory serviceFactory = new ServiceFactory(vertx, storage, Neo4j.getInstance(), Sql.getInstance(),
                MongoDb.getInstance(), config ,eb);

        EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Lystore.class.getSimpleName());

        addController(new ActiveStructureController(serviceFactory));
        addController(new AgentController(serviceFactory));
        addController(new BasketController(storage, serviceFactory));
        addController(new CampaignController(serviceFactory));
        addController(new ContractController(serviceFactory));
        addController(new ContractTypeController(serviceFactory));
        addController(new EquipmentController(vertx, serviceFactory));
        addController(new EquipmentTypeController(serviceFactory));
        addController(new ExportController(serviceFactory));
        addController(new GradeController(serviceFactory));
        addController(new InstructionController(serviceFactory));
        addController(new LabelOperationController(serviceFactory));
        addController(new LogController());
        addController(new LystoreController(eventStore));
        addController(new OrderController(storage, vertx, config, eb, serviceFactory));
        addController(new OrderRegionController(storage, serviceFactory));
        addController(new ParameterController(serviceFactory));
        addController(new ProgramController(serviceFactory));
        addController(new ProjectController(serviceFactory));
        addController(new PurseController(storage, serviceFactory));
        addController(new StructureController(serviceFactory));
        addController(new StructureGroupController(vertx, serviceFactory));
        addController(new SupplierController(serviceFactory));
        addController(new TagController(serviceFactory));
        addController(new TaxController(serviceFactory));
        addController(new TitleController(vertx, eb, serviceFactory));
        addController(new UserController(serviceFactory));

        CONFIG = config;
        vertx.deployVerticle(ExportLystoreWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        launchWorker(eb);

    }

    public static void launchWorker(EventBus eb) {
        eb.send(ExportLystoreWorker.class.getSimpleName(), new JsonObject(), new DeliveryOptions().setSendTimeout(1000 * 1000L), handlerToAsyncHandler(eventExport ->{
                    if(!eventExport.body().getString("status").equals("ok"))
                        launchWorker(eb);
                    log.info("Ok calling worker " + eventExport.body().toString());
                }
        ));
    }
}
