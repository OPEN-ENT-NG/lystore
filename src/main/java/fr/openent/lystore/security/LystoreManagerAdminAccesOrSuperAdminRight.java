package fr.openent.lystore.security;

import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserInfos;

public class LystoreManagerAdminAccesOrSuperAdminRight extends SuperAdminFilter implements ResourcesProvider {

    @Override
    public void authorize(HttpServerRequest resourceRequest, Binding binding,
                          UserInfos user, Handler<Boolean> handler) {
        super.authorize(resourceRequest, binding, user, event -> handler.handle(
                event
                        || WorkflowActionUtils.hasRight(user, WorkflowActions.MANAGER_RIGHT.toString())
                        || WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString())));
    }
}