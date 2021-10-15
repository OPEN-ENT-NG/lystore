import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface FileLystore {
    file_id: number;
    id_order_region_equipment: number;
    file_name: string;
}

interface IStatementOrderBody {
    id_campaign: string,
    id_structure: string,
    title_id: string,
    id_operation: string,
    equipment_key: string,
    equipment: string,
    comment: string,
    amount: string,
    price: string,
    equipment_name: string,
    technical_spec: string,
    id_contract: string,
    name_structure: string,
    files: Array<File>;
}

export interface IStatementsOrdersService {
    create(statementsOrders: IStatementOrderBody): Promise<AxiosResponse>;
}

export const statementsOrdersService: IStatementsOrdersService = {
    create: async (statementsOrders: IStatementOrderBody): Promise<AxiosResponse> => {
        const formData: FormData = new FormData();
        const headers = {'headers':
                {'Content-type': 'multipart/form-data', 'Files': statementsOrders.files.length}
        };

        formData.append('id_campaign', statementsOrders.id_campaign);
        formData.append('id_structure', statementsOrders.id_structure);
        formData.append('title_id', statementsOrders.title_id);
        formData.append('id_operation', statementsOrders.id_operation);
        formData.append('equipment_key', statementsOrders.equipment_key);
        formData.append('equipment', statementsOrders.equipment);
        formData.append('comment', statementsOrders.comment);
        formData.append('amount', statementsOrders.amount);
        formData.append('price', statementsOrders.price);
        formData.append('equipment_name', statementsOrders.equipment_name);
        formData.append('technical_spec', statementsOrders.technical_spec);
        formData.append('id_contract', statementsOrders.id_contract);
        formData.append('name_structure', statementsOrders.name_structure);
        statementsOrders.files.forEach(file => {
            formData.append('fileToUpload[]', file);
        });


        return http.post(`/lystore/region/orders/`, formData, headers);
    }
}
export const StatementsOrdersService = ng.service('StatementsOrdersService', (): IStatementsOrdersService => statementsOrdersService);