import {ng} from "entcore";
import {IStructureTitlesResponse, Structures} from "../../../model";
import http, {AxiosPromise, AxiosResponse} from "axios";



export interface TitleService {
    syncStructuresTitle(idCampaign: number): Promise<Structures>;
    delete(idCampaign: number,  structures: Structures): AxiosPromise;
}

export const titleService: TitleService = {

    syncStructuresTitle(idCampaign: number): Promise<Structures> {
        return http.get(`/lystore/titles/campaigns/${idCampaign}`).then((res: AxiosResponse) => {
            let StructureTitlesResponse: IStructureTitlesResponse[] = res.data;
            return new Structures().buildWithTitle(StructureTitlesResponse);
        });
    },

    delete(idCampaign: number, structures: Structures): AxiosPromise {
        return http.post(`/lystore/delete/titles/${idCampaign}`, structures.getTitlesJson());
    }

}

export const TitleService = ng.service('TitleService', (): TitleService => titleService);