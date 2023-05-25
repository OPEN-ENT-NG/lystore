import {ng, idiom as lang} from 'entcore';
import {RootsConst} from "../../core/constants/roots.const";
import {IDirective, IScope} from "angular";
import {tableFields} from "../display-order-list/TabFields";


interface IViewModel {
    lang: typeof lang;
    hideArticle: boolean;
    showArticle: boolean;
}


interface IDirectiveProperties {
    ngModel: typeof tableFields;

    ngChange(): void;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {
    lang: typeof lang;
    hideArticle: boolean;
    showArticle: boolean;

    $onInit() {
        this.hideArticle = false;
        this.showArticle = false;
        this.lang = lang
    }

    $onDestroy() {
    }
}

function directive(): IDirective {
    return {
        restrict: 'E',
        scope: {
            ngModel: '=',
            ngChange: '&'
        },
        templateUrl: `${RootsConst.directive}/parameter-table/parameter-table.html`,
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$location', '$window', Controller],
    };
};

export const parameterTableDirective = ng.directive('parameterTable', directive);