import {ng, idiom as lang} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {tableFields} from './TabFields'


interface IViewModel {
    lang: typeof lang
    tableFields: typeof tableFields;
}


interface IDirectiveProperties {
    displayedOrders: any;//à changer obviously
    isManager: boolean;
    preferences: any;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {
    lang: typeof lang;
    tableFields: typeof tableFields;


    scrollDisplay: {
        limitTo: number
    }

    constructor(private $scope: IDirectiveScope) {
        this.lang = lang
        this.tableFields = tableFields;
    }

    $onInit() {
        this.$scope.vm.preferences
        if (this.$scope.vm.preferences && this.$scope.vm.preferences.preference) {
            let loadedPreferences = JSON.parse(this.$scope.vm.preferences.preference);
            if (loadedPreferences.ordersWaitingDisplay)
                this.tableFields.map(table => {
                    table.display = loadedPreferences.ordersWaitingDisplay[table.fieldName]
                });
            // if(loadedPreferences.searchFields){
            //     $scope.search.filterWords = loadedPreferences.searchFields;
            //     $scope.filterDisplayedOrders();
            // }
            // $scope.ub.putPreferences("searchFields", []);
        }
    }

    $onDestroy() {
    }

}

function directive(): IDirective {
    return {
        replace: true,
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/display-order-list/display-order-list.html`,
        scope: {
            displayedOrders: '=',
            isManager: '=',
            preferences: '='
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

export const displayOrderList = ng.directive('displayOrderList', directive);