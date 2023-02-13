import {ng} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {BcOptions} from "../../model/parameter/bc-options.model";
import {OrdersClient} from "../../model";

interface IViewModel {
}


interface IDirectiveProperties {
    bcOptions: BcOptions;
    orderToSend: OrdersClient;
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
        templateUrl: `${RootsConst.directive}/bc-preview/bc-preview.html`,
        scope: {
            bcOptions: '=',
            orderToSend:'='
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

export const lystoreBcPreview = ng.directive('lystoreBcPreview', directive);