import {ng, template} from "entcore";
import {IScope} from "angular";

declare let window: any;

interface IViewModel extends ng.IController {
}

interface IMainScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {

    constructor(private $scope: IMainScope,
                private route: any,
                /*  inject service etc..just as we do in controller */) {
        this.$scope.vm = this;
    }

    $onInit() {
        template.open("main", "parameter/parameter-main");
        this.route({
            main: () => {
                template.open('parameter', 'parameter/parameter');
                console.log("main");
            },
        })
    }

    $onDestroy() {

    }
}

export const parameterMainController = ng.controller('ParameterMainController', ['$scope', 'route', Controller]);