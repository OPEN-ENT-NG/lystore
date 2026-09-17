import {IOrderClientResponse, OrdersClient} from "../../../model";
import { http, HttpResponse } from 'entcore-toolkit';
import {ng} from "entcore";

export interface OrderService {
    sync(idCampaign: number, idStructure:string): Promise<OrdersClient>;
}

export const orderService: OrderService = {

    sync(idCampaign: number, idStructure:string): Promise<OrdersClient>{
        return http.get(`/lystore/orders/${idCampaign}/${idStructure}`).then((res: HttpResponse) => {
            let OrderClientsResponse: IOrderClientResponse[] = res.data;
            return new OrdersClient().build(OrderClientsResponse);
        });
    },

}

export const OrderService = ng.service('OrderService', (): OrderService => orderService);