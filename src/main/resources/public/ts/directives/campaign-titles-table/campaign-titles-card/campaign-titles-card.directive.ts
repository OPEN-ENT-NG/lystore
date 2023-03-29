import {ng} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../../core/constants/roots.const";
import {Titles} from "../../../model";

interface IViewModel {
}


interface IDirectiveProperties {
    titles : Titles;

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
        replace: false,
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/campaign-titles-table/campaign-titles-card/campaign-titles-card.html`,
        scope: {
            titles: '='
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

export const lystoreCampaignTitleCard = ng.directive('lystoreCampaignTitleCard', directive);