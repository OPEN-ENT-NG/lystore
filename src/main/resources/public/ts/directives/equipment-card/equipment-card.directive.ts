import {ng, idiom as lang} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {EquipmentStatusConst} from "../../core/constants/equipment.status.const";
import {Equipment} from "../../model";

interface IViewModel {
    equipment: Equipment;
    equipmentStatusConst: any;
    displayPrice(price: number, typePrice: string) :string;
}


interface IDirectiveProperties {
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

    displayPrice(price: number, typePrice: string) :string {
        return price.toFixed(2) +  lang.translate('money.symbol') + ' ' + lang.translate(typePrice);
    }

    equipment: Equipment;
    equipmentStatusConst = EquipmentStatusConst;

}

function directive(): IDirective {
    return {
        replace: true,
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/equipment-card/equipment-card.html`,
        scope: {
            equipment: '='
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

export const lystoreEquipmentCard = ng.directive('lystoreEquipmentCard', directive);