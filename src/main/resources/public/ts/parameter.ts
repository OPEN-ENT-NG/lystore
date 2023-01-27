import {ng, routes} from "entcore";
import {ActiveStructureService,ParameterSettingService} from "./services";
import {activeStructureController,parameterMainController, parameterController} from "./controllers/index"
import * as directives from "./directives";

ng.services.push(ActiveStructureService);
ng.services.push(ParameterSettingService);
ng.controllers.push(activeStructureController);
ng.controllers.push(parameterMainController);
ng.controllers.push(parameterController)

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