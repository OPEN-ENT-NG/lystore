import {ng, template} from "entcore";
import {IScope, IWindowService} from "angular";

interface IViewModel extends ng.IController {
    redirectToHref(path: string): void;
    redirectTo (path: string):void;
}

interface IMainScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {
    constructor(private $scope: IMainScope,
                private route: any,
                private $window: any,
                private $location: any
                /*  inject service etc..just as we do in controller */) {
        this.$scope.vm = this;
    }
    redirectToHref = (path: string): void => {
        this.$window.location.href = this.$window.location.origin  + path;
    };
    redirectTo = (path: string): void => {
        this.$location.path(path);
    };

    $onInit() {
        template.open("main", "parameter/parameter-main");
        this.route({
            main: () => {
                template.open('parameter', 'parameter/active-structure/active-structure');
            },
            parameter: () => {
                template.open('parameter', 'parameter/parameter/parameter');
            },
        });
    }

    $onDestroy() {

    }
}

export const parameterMainController = ng.controller('ParameterMainController', ['$scope', 'route','$window','$location', Controller]);