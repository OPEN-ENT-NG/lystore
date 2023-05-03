import {ng, idiom as lang} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";
import {tableFields} from '../TabFields'
import {AxiosPromise} from "axios";

const NBDISPLAYEDORDERS = 25;

interface IViewModel {
    lang: typeof lang

    countColSpan(field: string): number

    scrollDisplay: {
        limitTo: number
    }
}


interface IDirectiveProperties {
    displayedOrders: any;//à changer obviously
    isManager: boolean;
    tableFields: typeof tableFields;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {
    lang: typeof lang;

    scrollDisplay: {
        limitTo: number
    }

    constructor(private $scope: IDirectiveScope) {
        this.lang = lang
        this.scrollDisplay = {
            limitTo: NBDISPLAYEDORDERS
        }
    }

    $onInit() {
    }

    $onDestroy() {
    }


    //TODO A RETRAVAILLER
    countColSpan(field: string): number {
        let totalCol = this.$scope.vm.isManager ? 1 : 0;
        let priceCol: number;
        let amount_field = 13;
        for (let _i = 0; _i < this.$scope.vm.tableFields.length; _i++) {
            if (_i < amount_field && this.$scope.vm.tableFields[_i].display) {
                totalCol++;
            }
        }
        if (this.$scope.vm.tableFields[14].display) {
            priceCol = 3;
            if (!this.$scope.vm.tableFields[13].display) {
                totalCol++;
            }
        } else {
            priceCol = 1;
        }
        
        return field === this.lang.translate('totals') ? totalCol : priceCol;
    };
}

function directive(): IDirective {
    return {
        replace: true,
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/display-order-list/display-order-list-table/display-order-list-table.html`,
        scope: {
            displayedOrders: '=',
            isManager: '=',
            tableFields:'='
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

export const displayOrderListTable = ng.directive('displayOrderListTable', directive);