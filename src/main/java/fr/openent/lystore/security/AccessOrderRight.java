package fr.openent.lystore.security;

import fr.openent.lystore.service.UserService;
import fr.openent.lystore.service.impl.DefaultUserService;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

/**
 * Created by agnes.lapeyronnie on 27/02/2018.
 */
public class AccessOrderRight implements ResourcesProvider {
    UserService userService = new DefaultUserService();
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user,
                          Handler<Boolean> handler) {

        if (WorkflowActionUtils.hasRight(user, WorkflowActions.MANAGER_RIGHT.toString())) {
            handler.handle(true);
        } else {
                userService.getStructures(user.getUserId(), event -> {
                if(event.isRight()) {
                   JsonArray authorizedStructures =  event.right().getValue();
                   boolean authorized = false;
                    String idStructure = request.params().get("idStructure");
                    for (int i = 0 ; i < authorizedStructures.size(); i++){
                        JsonObject structure = authorizedStructures.getJsonObject(i);
                        authorized = authorized  || structure.getString("id").equals(idStructure);
                    }
                    handler.handle( authorized);
                }else {
                    handler.handle(false);
                }
            });
        }
    }
}
