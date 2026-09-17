import {ng} from "entcore";
import {IStructureTitlesResponse, Structures} from "../../../model";
import { http, HttpPromise, HttpResponse } from 'entcore-toolkit';



export interface TitleService {
    syncStructuresTitle(idCampaign: number): Promise<Structures>;
    delete(idCampaign: number,  structures: Structures): HttpPromise;
}

export const titleService: TitleService = {

    syncStructuresTitle(idCampaign: number): Promise<Structures> {
        return http.get(`/lystore/titles/campaigns/${idCampaign}`).then((res: HttpResponse) => {
            let StructureTitlesResponse: IStructureTitlesResponse[] = res.data;
            return new Structures().buildWithTitle(StructureTitlesResponse);
        });
    },

    delete(idCampaign: number, structures: Structures): HttpPromise {
        return http.post(`/lystore/delete/titles/${idCampaign}`, structures.getTitlesJson());
    }

}

export const TitleService = ng.service('TitleService', (): TitleService => titleService);