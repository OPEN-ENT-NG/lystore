package fr.openent.lystore;

import fr.openent.lystore.controllers.*;
import fr.openent.lystore.controllers.parameter.ActiveStructureController;
import fr.openent.lystore.controllers.parameter.ParameterController;
import fr.openent.lystore.export.ExportLystoreWorker;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.BaseServer;
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
    public void start(Promise<Void> startPromise) throws Exception {
      final Promise<Void> promise = Promise.promise();
      super.start(promise);
      promise.future()
        .compose(e -> this.initLystore())
        .onComplete(startPromise);
    }

    public Future<Void> initLystore() {
        lystoreSchema = config.getString("db-schema");
        if(config.containsKey("iteration-worker")){
           iterationWorker = config.getInteger("iteration-worker");
        } else {
           log.info("no iteration worker in config");
           iterationWorker = 10 ;
        }
        EventBus eb = getEventBus(vertx);
        return StorageFactory.build(vertx, config)
          .compose(storageFactory -> {
              STORAGE = storageFactory.getStorage();
              JsonObject mail = config.getJsonObject("mail", new JsonObject());


              EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Lystore.class.getSimpleName());

              addController(new LystoreController(eventStore));
              addController(new AgentController());
              addController(new SupplierController());
              addController(new ProgramController());
              addController(new ContractTypeController());
              addController(new ContractController());
              addController(new TagController());
              addController(new EquipmentController(vertx, STORAGE));
              addController(new TaxController());
              addController(new LogController());
              addController(new CampaignController(STORAGE));
              addController(new PurseController(vertx, STORAGE));
              addController(new StructureGroupController(vertx, STORAGE));
              addController(new StructureController());
              addController(new BasketController(vertx, STORAGE, config.getJsonObject("slack", new JsonObject()), mail));
              addController(new OrderController(STORAGE, vertx, config, eb));
              addController(new UserController());
              addController(new EquipmentTypeController());
              addController(new TitleController(vertx, eb, STORAGE));
              addController(new GradeController());
              addController(new ProjectController());
              addController(new OperationController());
              addController(new InstructionController(STORAGE));
              addController(new OrderRegionController(STORAGE));
              addController(new ExportController(STORAGE));
              addController(new ActiveStructureController(eb));
              addController(new LabelOperationController());
              addController(new ParameterController());
              CONFIG = config;
              return vertx.deployVerticle(ExportLystoreWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
            })
          .map(e -> {
            launchWorker(eb);
            return null;
          });
    }

    public static void launchWorker(EventBus eb) {
        eb.request(ExportLystoreWorker.class.getSimpleName(), new JsonObject(), new DeliveryOptions().setSendTimeout(1000 * 1000L), handlerToAsyncHandler(eventExport ->{
                    if(!eventExport.body().getString("status").equals("ok"))
                        launchWorker(eb);
                    log.info("Ok calling worker " + eventExport.body().toString());
                }
        ));

    }
}
