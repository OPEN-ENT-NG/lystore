import {ng, routes} from "entcore";
import {ParameterService} from "./services";
import {activeStructureController,parameterMainController} from "./controllers/index"
import * as directives from "./directives";

ng.services.push(ParameterService);
ng.controllers.push(activeStructureController);
ng.controllers.push(parameterMainController);

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

routes.define(($routeProvider) => {
    $routeProvider
        .when('/', {
            action: 'main'
        })
        .when('/parameter',{
            action: 'parameter'
        });

    $routeProvider
        .otherwise({
        redirectTo: '/'
    });
});