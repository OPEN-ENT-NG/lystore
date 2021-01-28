package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.ParameterService;
import io.vertx.core.eventbus.EventBus;

public class DefaultParameterService  implements ParameterService {

    EventBus eb;
    public DefaultParameterService(EventBus eb) {
        this.eb = eb;
    }
}
