import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface StructureLystore {
    deployed: boolean;
    uai: string;
    name: string;
    structureId: string;
    id: string;
}

export interface ParameterService {
    getStructuresLystore():Promise<Array<StructureLystore>>;
    createGroupLystoreToStructure(name: string, structureId: string): Promise<any>;
    undeployStructure(id: string);
}



export const parameterService: ParameterService =  {

    getStructuresLystore: async (): Promise<Array<StructureLystore>> => {
        try {
            const {data}: AxiosResponse = await http.get(`structures/lystore`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    createGroupLystoreToStructure: async (name: string, structureId: string) => {
        try {
            return await http.post(`structure/lystore/group`, {name: name, structureId: structureId});
        } catch (err) {
            throw err;
        }
    },


    undeployStructure: async (id: string) => await http.delete(`/lystore/structures/${id}`)
}
export const ParameterService = ng.service('ParameterService', (): ParameterService => parameterService);