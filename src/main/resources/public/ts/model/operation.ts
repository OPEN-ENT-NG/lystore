import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';
import {_, moment, notify, toasts} from "entcore";
// @ts-ignore
import {Utils} from "./Utils";
import {Instruction} from "./instruction";
import {IOrderClientResponse, OrderClient, OrdersClient} from "./OrderClient";
import {Structure} from "./Structure";
import {Label} from "./LabelOperation";
import {Contract} from "./Contract";
import {Notification} from "./Notification";
import {Project} from "./project";
import {Order, Orders} from "./Order";
import {OrderRegion} from "./OrderRegion";


export class Operation implements Selectable {
    id?: number;
    id_label: number;
    label: Label;
    status: boolean = false;
    Operations: any;
    bc_numbers: Array<any>;
    programs: Array<any>;
    contracts: Array<any>;
    nb_orders: number;
    amount: number;
    selected: boolean;
    id_instruction: number;
    instruction: Instruction;
    date_cp: Date;
    date_operation: Date;
    nbOrberSub: Number;
    number_sub: Number;
    cp_adopted = false;

    constructor() {

    }

    async save() {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create() {
        try {
            await http.post(`/lystore/operation`, this.toJson());
        } catch (e) {
            notify.error('lystore.operation.create.err');
            throw e;
        }
    }

    async deleteOrders(orders) {
        let ordersClientId = [];
        let ordersRegionId = [];

        orders.forEach(order => {
            if (order.typeOrder === "region") {
                ordersRegionId.push(order.id);
                if (order.id_order_client_equipment) {
                    ordersClientId.push(order.id_order_client_equipment);
                }
            } else {
                ordersClientId.push(order.id);
            }
        });
        try {
            return await http.put(`lystore/operation/delete/orders`, [{
                "ordersClientId": ordersClientId,
                "ordersRegionId": ordersRegionId
            }])
        } catch (e) {
            notify.error('lystore.operation.order.delete.err');
            throw e;
        }
    }

    async update() {
        try {
            await http.put(`/lystore/operation/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }

    async getOrders(structures: Structure[] = []) {
        try {
            const {data} = await http.get(`/lystore/operations/${this.id}/orders`);
            let OrderClientsResponse: IOrderClientResponse[] = data;
              new Orders().build(OrderClientsResponse);
            let orders =  new Orders().build(OrderClientsResponse);
            if (structures.length > 0) {
                orders.all.forEach((order:Order) => {
                    if(order instanceof  OrderClient){
                        //à enlever dès que le prix sera mieux gérer
                        order.priceUnitedTTC = order.price_proposal ?
                            parseFloat(( order.price_proposal).toString()) :
                            order.priceTTC;
                        order.total = order.priceUnitedTTC * order.amount
                        order.structure = structures.find(structureFilter => structureFilter.id === order.id_structure)
                    }else if(order instanceof  OrderRegion){
                        //à enlever dès que le prix sera mieux gérer
                        order.priceUnitedTTC = order.price_proposal ?
                            parseFloat(( order.price_proposal).toString()) :
                            order.price;
                        order.total = order.price * order.amount
                        order.structure = structures.find(structureFilter => structureFilter.id === order.id_structure)
                    }
                });
            }
            return orders;
        } catch (e) {
            notify.error("lystore.operation.orders.sync.err");
            throw e;
        }
    }

    toJson() {
        return {
            id_label: this.id_label,
            status: this.status,
            id_instruction: this.id_instruction,
            date_operation: this.date_operation ? Utils.formatDatePost(this.date_operation) : null,
        };
    }

    displayOperation = () => {
        return this.label.label + " - " + (this.date_cp && this.date_cp === null) ? "Pas de date de CP" : this.date_cp.toDateString();
    }

}

export class Operations extends Selection<Operation> {

    filters: Array<string>;

    constructor() {
        super([]);
        this.filters = [];
    }

    async sync(onlylist?: boolean) {
        try {
            let url: string;
            const queriesFilter = Utils.formatGetParameters({q: this.filters});
            if (onlylist) {
                url = `/lystore/operations/list/?${queriesFilter}`
            } else {
                url = `/lystore/operations/?${queriesFilter}`

            }

            let {data} = await http.get(url);
            this.all = Mix.castArrayAs(Operation, data);
            this.all.map(operation => {
                operation.instruction
                    ? operation.instruction = JSON.parse(operation.instruction.toString())
                    : operation.instruction = null;
                operation.date_cp = operation.date_cp !== null && !_.isEmpty(operation.instruction) ? moment(operation.instruction.date_cp) : null;
                operation.date_operation = operation.date_operation !== null ? moment(operation.date_operation) : null;
                operation.label.toString() !== 'null' && operation.label !== null ?
                    operation.label = Mix.castAs(Label, JSON.parse(operation.label.toString()))
                    : operation.label = new Label();
                operation.nb_orders = operation.nb_orders || 0;
                operation.nbOrberSub = operation.number_sub || 0;
            })
        } catch (e) {
            notify.error('lystore.operation.sync.err');
            throw e;
        }
    }

    async delete() {
        let operationsIds = this.selected.map(operation => operation.id);
        try {
            await http.delete('/lystore/operations', {data: operationsIds});
        } catch (err) {
            notify.error('lystore.operation.delete.err');
        }
    }

    async updateOperations(id_instruction: number, operationIds: Array<number>) {
        try {
            await http.put(`/lystore/operations/instructionAttribute/${id_instruction}`, operationIds);
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }

    async updateRemoveOperations(operationIds: Array<number>) {
        try {
            await http.put('/lystore/operations/instructionRemove', operationIds);
        } catch (e) {
            notify.error('lystore.operation.update.err');
            throw e;
        }
    }
}