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

    calculateTotal(orderClient: OrderClient, roundNumber: number): string;

    calculatePriceOfOrderClient(orderClient: OrderClient, selectedOptions: boolean, roundNumber: number): number;

    hasAProposalPrice(): boolean;

    getTooltip(orderClient: OrderClient): string;

    displayOptions: boolean;

    lang: typeof lang;

    displayEquipmentOptions(): void;
}


interface IDirectiveProperties {
    orderClient: OrderClient;
}

interface IDirectiveScope extends IScope {
    vm: IDirectiveProperties;
}

class Controller implements ng.IController, IViewModel {

    orderClient: OrderClient;
    displayOptions: boolean;
    lang: typeof lang;

    constructor(private $scope: IDirectiveScope) {
    }

    $onInit() {
        this.orderClient = this.$scope.vm.orderClient
        this.displayOptions = false;
        this.lang = lang;
    }

    $onDestroy() {
    }

    displayEquipmentOptions() {
        this.displayOptions = !this.displayOptions;
        Utils.safeApply(this.$scope);
    }

    calculatePriceOfOrderClient(orderClient: OrderClient, selectedOptions: boolean, roundNumber: number = 2): number {
        return (!isNaN(orderClient.calculatePriceTTC(selectedOptions)))
            ? (roundNumber
                ? Number.parseFloat(orderClient.calculatePriceTTC(selectedOptions).toFixed(roundNumber))
                : orderClient.calculatePriceTTC(selectedOptions))
            : orderClient.calculatePriceTTC(selectedOptions);
    }

    calculateTotal(orderClient: OrderClient, roundNumber: number): string {
        let totalPrice:number = this.calculatePriceOfOrderClient(orderClient, true, roundNumber) * orderClient.amount;
        return totalPrice.toFixed(roundNumber);
    }


    hasAProposalPrice(): boolean {
        return this.orderClient.price_proposal !== undefined && this.orderClient.price_proposal !== null && this.orderClient.price_proposal !== 0;
    }

    getTooltip(orderClient: OrderClient): string {
        if (orderClient.operation && orderClient.operation.instruction)
            return orderClient.operation.instruction.object;
        if (orderClient.operation)
            return orderClient.operation.label.label;
        return lang.translate(orderClient.status);
    }

    displayStatus(): string {
        return lang.translate(this.orderClient.status);
    }

    displayInstruction(): string {
        return lang.translate("INSTRUCTION") + " : " + this.orderClient.operation.instruction.object;
    }

    getDate(): Date {
        if (this.orderClient.rejectOrder && this.orderClient.rejectOrder.reject_date)
            return this.orderClient.rejectOrder.reject_date
        if (this.orderClient.done_date)
            return this.orderClient.done_date
        if (this.orderClient.operation && this.orderClient.operation.instruction && this.orderClient.operation.instruction.date_cp)
            return this.orderClient.operation.instruction.date_cp;
        if (this.orderClient.bCOrder && this.orderClient.bCOrder.dateCreation)
            return this.orderClient.bCOrder.dateCreation;
        if (this.orderClient.operation && this.orderClient.operation.date_operation)
            return this.orderClient.operation.date_operation;
        return this.orderClient.creation_date;
    }

    formatDate(date: Date): string {
        return Utils.formatDate(date);
    }
}

function directive(): IDirective {
    return {
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