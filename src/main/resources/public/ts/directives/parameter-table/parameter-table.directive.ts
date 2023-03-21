import {ng, idiom as lang} from 'entcore';
import {RootsConst} from "../../core/constants/roots.const";
import {IAttributes, IDirective, IScope} from "angular";
import {IDisplayModel} from "../../model/utils/displayModel";


interface IViewModel {
    valueChange(): void;

    showArticle: boolean;
    hideArticle: boolean;
    lang: typeof lang;
}


interface IDirectiveProperties {
    ngModel: IDisplayModel[];

    ngChange(): void;

    ngDisabled: boolean;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {
    lang: typeof lang;

    hideArticle: boolean;
    showArticle: boolean;

    constructor(private $scope: IDirectiveScope, $attrs: IAttributes) {
        this.lang = lang;
        this.hideArticle = false;
        this.showArticle = false;
    }

    $onInit() {
    }

    $onDestroy() {
    }


    valueChange(): void {
        {
            setTimeout(function () {
                if (this.$attrs.ngChange) this.$scope.$parent.$eval(this.$attrs.ngChange);
            }, 0);
        }

    }
}

function directive(): IDirective {
    return {
        replace: false,
        restrict: 'E',
        scope: {
            ngModel: '=',
            ngChange: '&',
            ngDisabled: '@'
        },
        templateUrl: `${RootsConst.directive}/parameter-table/parameter-table.html`,
        controllerAs: 'vm',
        bindToController: true,
        controller: ['$scope', '$attrs', Controller],
    };
};

export const parameterTable = ng.directive('parameterTable', directive);
