import {ng} from "entcore";
import {IStructureTitlesResponse, Structures, Titles} from "../../model";
import http, {AxiosPromise, AxiosResponse} from "axios";



export interface TitleService {
    syncStructuresTitle(idCampaign: number): Promise<Structures>;
    delete(idCampaign: number, titles: Titles): AxiosPromise;
}

export const titleService: TitleService = {

    syncStructuresTitle(idCampaign: number): Promise<Structures> {
        return http.get(`/lystore/titles/campaigns/${idCampaign}`).then((res: AxiosResponse) => {
            let StructureTitlesResponse: IStructureTitlesResponse[] = res.data;
            return new Structures().buildWithTitle(StructureTitlesResponse);
        });
    },

    delete(idCampaign: number, titles: Titles): AxiosPromise {
        return http.post(`/lystore/delete/titles/${idCampaign}`, titles.toJson());
    }

}

export const TitleService = ng.service('TitleService', (): TitleService => titleService);