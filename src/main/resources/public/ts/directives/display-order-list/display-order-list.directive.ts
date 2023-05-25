import {ng, idiom as lang} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {tableFields} from './TabFields'
import {Userbook, Utils} from "../../model";


interface IViewModel {
    lang: typeof lang;
    tableFields: typeof tableFields;
    savePreference():void;
}


interface IDirectiveProperties {
    displayedOrders: any;//à changer obviously
    isManager: boolean;
    prefName:string;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {
    lang: typeof lang;
    tableFields: typeof tableFields;
    ub:Userbook;

    scrollDisplay: {
        limitTo: number
    }

    constructor(private $scope: IDirectiveScope) {
        this.lang = lang;
        this.tableFields = tableFields;
        this.ub = new Userbook();
    }

    savePreference():void{
        this.ub.putPreferences(this.$scope.vm.prefName, this.jsonPref(this.tableFields));
    };

    jsonPref (prefs: typeof tableFields) : any{
        let json = {};
        prefs.forEach(pref =>{
            json[pref.fieldName]= pref.display;
        });
        return json;
    };

    async $onInit() {
        let preferences
        try {
            preferences = await this.ub.getPreferences();
        } catch (e) {
            preferences = {}
        }
        if (preferences && preferences.preference) {
            let loadedPreferences = JSON.parse(preferences.preference);
            if (loadedPreferences[this.$scope.vm.prefName])
                this.tableFields.forEach(table => {
                    table.display = loadedPreferences[this.$scope.vm.prefName][table.fieldName]
                });
        }
        Utils.safeApply(this.$scope)
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
            prefName:'@'
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