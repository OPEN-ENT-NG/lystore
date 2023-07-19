import {ng, notify} from "entcore";
import {IPurseStructureResponse, Purse, PurseImporter, Purses} from "../../../model";
import http, {AxiosPromise, AxiosResponse} from "axios";
import {parse} from "ts-jest/dist/utils/json";


export interface PurseService {
    save(purse: Purse): AxiosPromise;

    sync(idCampaign: number) :Promise<Purses>;

    check(idCampaign: number, purses: Purses): Promise<void>;

    validateImport(importer: PurseImporter): Promise<any>;
}

export const purseService: PurseService = {

    save(purse: Purse): AxiosPromise {
        return http.put(`/lystore/purse/${purse.id}`, purse.toJson());
    },

    sync(idCampaign: number): Promise<Purses> {
        return http.get(`/lystore/campaign/${idCampaign}/purses/list`).then((res: AxiosResponse) => {
            let PurseStructureResponses: IPurseStructureResponse[] = res.data;
            return new Purses(idCampaign).build(PurseStructureResponses);
        }).catch(e => {
            console.error(e);
            notify.error("lystore.purse.get.err");
            return new Purses(idCampaign);
        });
    },
    check(idCampaign: number, purses: Purses): Promise<void> {
       return http.get(`/lystore/campaign/${idCampaign}/purse/check`).then((res: AxiosResponse) => {
            if (res.status === 201) {
                purses.all.map(purse => {
                    purse.substraction = 0.00;
                    res.data.map(back_data => {
                        if (back_data.id_structure && back_data.id_structure === purse.structure.id) {
                            purse.substraction = parseFloat(back_data.difference);
                            if (purse.substraction !== 0) {
                                purse.bigDifference = Math.abs(purse.substraction) >= 2;
                            }
                        }
                    });
                });
            }
        });
    },

    validateImport(importer: PurseImporter): Promise<any> {
        let formData = new FormData();
        formData.append('file', importer.files[0], importer.files[0].name);

        return  http.post(`/lystore/campaign/${importer.id_campaign}/purses/import`,
            formData, {'headers': {'Content-Type': 'multipart/form-data'}});
    }
}

export const PurseService = ng.service('PurseService', (): PurseService => purseService);