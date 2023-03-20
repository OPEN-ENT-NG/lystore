import {ng} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";
import {OrderClient, OrderRegion} from "../../../model";

interface IViewModel {
}


interface IDirectiveProperties {
    orderParent: OrderClient;
    orderToUpdate: OrderRegion;
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
        templateUrl: `${RootsConst.directive}/order-client-update-form/order-client-update-form-resume/order-client-update-form-resume.html`,
        scope: {
            orderParent: '=',
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

export const lystoreOrderClientUpdateFormResume = ng.directive('lystoreOrderClientUpdateFormResume', directive);