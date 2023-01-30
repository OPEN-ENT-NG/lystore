import {ng} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {ExportChoices} from "../../model/parameter/export-choices.model";

interface IViewModel {
}


interface IDirectiveProperties {
    exportChoices: ExportChoices;

    onChange(): void;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {

    constructor(private $scope: IDirectiveScope) {
    }

    $onInit() {

    }

    $onDestroy() {
    }

}

function directive(): IDirective {
    return {
        replace: true,
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/parameter-valid-command/parameter-valid-command.html`,
        scope: {
            hasOperationsAndInstructions: '=',
            onChange: '&'
        },
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', Controller],
        /* interaction DOM/element */
        link: function (scope: ng.IScope,
                        element: ng.IAugmentedJQuery,
                        attrs: ng.IAttributes,
                        vm: ng.IController) {
        }
    }
}

export const lystoreParameterValidCommand = ng.directive('lystoreParameterValidCommand', directive);