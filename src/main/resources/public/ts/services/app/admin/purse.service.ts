import {ng} from "entcore";
import {Purse} from "../../../model";
import http, {AxiosPromise} from "axios";



export interface PurseService {
    save(purse: Purse): AxiosPromise;
}

export const purseService: PurseService = {

    save(purse: Purse): AxiosPromise {
        return http.put(`/lystore/purse/${purse.id}`, purse.toJson());
    },



}

export const PurseService = ng.service('PurseService', (): PurseService => purseService);