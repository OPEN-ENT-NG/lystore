import {ng} from "entcore";
import {IStructureTitles, Structure, Structures, StructureTitles} from "../../model";
import http from "axios";

export interface Title {
    name: string;
    id: string;
    selected: boolean;
}


export interface TitleService {
    syncStructuresTitle(idCampaign: number): Promise<Structures>
}

export const titleService: TitleService = {

    syncStructuresTitle(idCampaign: number): Promise<Structures> {
        return http.get(`/lystore/titles/campaigns/${idCampaign}`).then(
            (res) =>            {
                let StructureTitlesResponse: IStructureTitles[] = res.data;
                console.log(res.data)
                return new StructureTitles().build(StructureTitlesResponse);
                //    return Promise.resolve(undefined);
            })
    }

}

export const TitleService = ng.service('TitleService', (): TitleService => titleService);