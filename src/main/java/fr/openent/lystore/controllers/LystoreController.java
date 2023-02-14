package fr.openent.lystore.controllers;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserUtils;

import static fr.openent.lystore.constants.ParametersConstants.*;


public class LystoreController extends ControllerHelper {

    public LystoreController() {
        super();
    }

    @Get("")
    @ApiDoc("Display the home view")
    @SecuredAction("lystore.access")
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            new SuperAdminFilter().authorize(null, null, user, isAuthorized -> {
                JsonObject params = new JsonObject();
                params.put(ISSUPERADMIN, isAuthorized)
                        .put(REGIONTYPENAME, config.getString(REGION_TYPE_NAME));
                renderView(request, params);
            });
        });
    }

}
