import {Selectable} from "entcore-toolkit";
import {OrderClient} from "./OrderClient";


export interface IOrderClientOptionResponse {
    id: number;
    tax_amount: number;
    price: number;
    id_order_client_equipment: number;
    name: string;
    amount: number;
    required: boolean;
    id_type: number;
}

export class OrderOptionClient implements Selectable {
    id?: number;
    tax_amount: number;
    price: number;
    name: string;
    amount: number;
    required: boolean;
    id_order_client_equipment: number;
    selected: boolean;
    id_type: number;
    orderClient: OrderClient;

    constructor() {
    }

    build(orderClientOptionResponse: IOrderClientOptionResponse): OrderOptionClient {
        this.id = orderClientOptionResponse.id;
        this.tax_amount = orderClientOptionResponse.tax_amount;
        this.price = orderClientOptionResponse.price;
        this.name = orderClientOptionResponse.name;
        this.amount = orderClientOptionResponse.amount;
        this.required = orderClientOptionResponse.required;
        this.id_type = orderClientOptionResponse.id_type;
        return this;
    }


    calculatePriceTTC():number{
         return Number.parseFloat((this.price + this.price * this.tax_amount / 100).toFixed(2)) ;
    }
}