import {ng, idiom as lang} from "entcore";
import {IDirective, IScope,} from "angular";
import {RootsConst} from "../../core/constants/roots.const";
import {OrderClient, Utils} from "../../model";

interface IViewModel {
    hasAProposalPrice(): boolean;

    getDate(): string | Date;

    formatDate(date: any): string;

    displayStatus(): string;

    displayInstruction(): string;

    calculateTotal(orderClient: OrderClient, roundNumber: number): string

    calculatePriceOfOrderClient(orderClient: OrderClient, selectedOptions: boolean, roundNumber: number): number
    hasAProposalPrice():boolean
}


interface IDirectiveProperties {
    orderClient: OrderClient;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {
    constructor(private $scope: IDirectiveScope) {
    }

    orderClient: OrderClient;

    $onInit() {
        this.orderClient = this.$scope.vm.orderClient
    }

    $onDestroy() {
    }

    calculatePriceOfOrderClient(orderClient: OrderClient, selectedOptions: boolean, roundNumber: number = 2): number {
        return (!isNaN(orderClient.calculatePriceTTC(selectedOptions)))
            ? (roundNumber
                ? Number.parseFloat(orderClient.calculatePriceTTC(selectedOptions).toFixed(roundNumber))
                : orderClient.calculatePriceTTC(selectedOptions))
            : orderClient.calculatePriceTTC(selectedOptions);
    }

    calculateTotal(orderClient: OrderClient, roundNumber: number): string {
        let totalPrice = this.calculatePriceOfOrderClient(orderClient, true, roundNumber) * orderClient.amount;
        return totalPrice.toFixed(roundNumber);
    }


    hasAProposalPrice(): boolean {
        return this.orderClient.price_proposal !== undefined && this.orderClient.price_proposal !== null ;
    }

    displayStatus(): string {
        return lang.translate(this.orderClient.status)
    }

    displayInstruction(): string {
        return " : Rapport " + this.orderClient.cp_number;
    }

    getDate(): string | Date {
        console.log("getDate");
        console.log(this.orderClient);
        if (this.orderClient.rejectOrder && this.orderClient.rejectOrder.reject_date)
            return this.orderClient.rejectOrder.reject_date
        if (this.orderClient.done_date)
            return this.orderClient.done_date
        if (this.orderClient.date_cp)
            return this.orderClient.date_cp;
        if (this.orderClient.order_creation_date)
            return this.orderClient.order_creation_date;
        if (this.orderClient.date_operation)
            return this.orderClient.date_operation;
        return this.orderClient.creation_date;
    }

    //any car il n y a pas que des dates pour l instant
    formatDate(date: any) :string {
        return Utils.formatDate(date);
    }
}

function directive(): IDirective {
    return {
        replace: true,
        restrict: 'E',
        templateUrl: `${RootsConst.directive}/order-client-display-viewer/order-client-display-viewer.html`,
        scope: {
            orderClient: '='
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

export const orderClientDisplayViewer = ng.directive('orderClientDisplayViewer', directive);