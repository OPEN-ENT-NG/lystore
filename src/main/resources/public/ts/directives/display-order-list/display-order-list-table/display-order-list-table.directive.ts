import {ng, idiom as lang} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";
import {tableFields} from '../TabFields'
import {Utils} from "../../../model";

const NBDISPLAYEDORDERS = 25;

interface IViewModel {
    lang: typeof lang

    countColSpan(field: string): number
    allOrdersSelected:boolean
    scrollDisplay: {
        limitTo: number
    }
    formatDate(date: Date):string
    switchAllOrders():void
    getStructureGroupsList  (structureGroups : any): string
}


interface IDirectiveProperties {
    displayedOrders: any;
    isManager: boolean;
    tableFields: typeof tableFields;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {
    lang: typeof lang;
    allOrdersSelected:boolean
    scrollDisplay: {
        limitTo: number
    }

    constructor(private $scope: IDirectiveScope) {
        this.lang = lang;
        this.scrollDisplay = {
            limitTo: NBDISPLAYEDORDERS
        };
        this.allOrdersSelected = false;
    }

    $onInit() {
    }

    $onDestroy() {
    }

    formatDate = Utils.formatDate;

    switchAllOrders(): void {
        this.$scope.vm.displayedOrders.all.map((order) => order.selected = this.allOrdersSelected);
    };

    getStructureGroupsList (structureGroups : any): string  {
        try{
            return structureGroups.join(', ');
        }catch (e) {
            let result = "-";
            if(structureGroups)
               result = structureGroups.replaceAll("\"","").replace("[","").replace("]","")
            return result
        }
    };

    calculTotalPriceTTC(limitTo): number {
        let total = 0;
        this.$scope.vm.displayedOrders.all.slice(0, limitTo).map((order) => {
                total += parseFloat(order.total.toString());//obliger pour gérer l'écran initiale à changer si on a le temps
        });
        return total;
    }
    calculTotalAmount (limitTo):number {
        let total = 0;
        this.$scope.vm.displayedOrders.all.slice(0, limitTo).map((order) => {
            total += order.amount;
        });
        return total;
    }

    countColSpan(field: string): number {
        let totalCol = this.$scope.vm.isManager ? 1 : 0;
        let priceCol: number;
        let amount_field = 13;
        for (let i = 0; i < this.$scope.vm.tableFields.length; i++) {
            if (i < amount_field && this.$scope.vm.tableFields[i].display) {
                totalCol++;
            }
        }
        if (this.$scope.vm.tableFields[14].display) {
            priceCol = 2;
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