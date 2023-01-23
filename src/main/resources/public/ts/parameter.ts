import {ng, routes} from "entcore";
import {ParameterService} from "./services";
import {activeStructureController,parameterMainController} from "./controllers/index"

ng.services.push(ParameterService);
ng.controllers.push(activeStructureController);
ng.controllers.push(parameterMainController);

routes.define(($routeProvider) => {
    $routeProvider
        .when('/', {
            action: 'main'
        });

    $routeProvider
        .otherwise({
        redirectTo: '/'
    });
});