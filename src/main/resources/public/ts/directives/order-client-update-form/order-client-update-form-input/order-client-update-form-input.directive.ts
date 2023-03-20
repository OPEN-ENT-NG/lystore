import {ng} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";
import {OrderRegion} from "../../../model";

interface IViewModel {
}


interface IDirectiveProperties {
    orderToUpdate : OrderRegion
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
        templateUrl: `${RootsConst.directive}/order-client-update-form/order-client-update-form-input/order-client-update-form-input.html`,
        scope: {
            orderToUpdate:'='
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

export const lystoreOrderClientUpdateFormInput = ng.directive('lystoreOrderClientUpdateFormInput', directive);