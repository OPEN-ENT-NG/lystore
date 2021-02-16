import {ng} from "entcore";
import {ParameterService} from "./services";
import {parameterController} from "./controllers/parameter"

ng.services.push(ParameterService);
ng.controllers.push(parameterController);