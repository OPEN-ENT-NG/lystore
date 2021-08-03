import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface FileLystore {
    file_id: number;
    id_order_region_equipment: number;
    file_name: string;
}

interface IStatementFileBody {
    id_file: string,
    id_orderRegion_equipment: string,
    filename: string;
}

export interface IStatementsFilesService {
    create(statementsFiles: IStatementFileBody): Promise<AxiosResponse>;
}

export const statementsFilesService: IStatementsFilesService = {
    create: async (statementsFiles: IStatementFileBody): Promise<AxiosResponse> => {
        const formData: FormData = new FormData();
        const headers = {'headers': {'Content-type': 'multipart/form-data'}};

        formData.append('id', statementsFiles.id_file);
        formData.append('id_orderRegion_equipment', statementsFiles.id_orderRegion_equipment);
        formData.append('filename', statementsFiles.filename);

        return http.post(`/order/upload/file`, formData, headers);
    }
}