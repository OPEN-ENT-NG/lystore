import {ng} from "entcore";
import {IStructureTitles, Structures, StructureTitles, Titles} from "../../model";
import http, {AxiosPromise, AxiosResponse} from "axios";

export interface Title {
    name: string;
    id: string;
    selected: boolean;
}


export interface TitleService {
    syncStructuresTitle(idCampaign: number): Promise<Structures>

    delete(idCampaign: number, titles: Titles): AxiosPromise
}

export const titleService: TitleService = {

    syncStructuresTitle(idCampaign: number): Promise<Structures> {
        return http.get(`/lystore/titles/campaigns/${idCampaign}`).then(
            (res: AxiosResponse) => {
                let StructureTitlesResponse: IStructureTitles[] = res.data;
                return new StructureTitles().build(StructureTitlesResponse);
            })
    },

    delete(idCampaign: number, titles: Titles): AxiosPromise {
        return http.post(`/lystore/delete/titles/${idCampaign}`, titles.toJson());
    }

}

export const TitleService = ng.service('TitleService', (): TitleService => titleService);