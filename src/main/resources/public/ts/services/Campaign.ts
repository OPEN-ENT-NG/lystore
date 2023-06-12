import {moment, ng} from "entcore";
import http, {AxiosPromise} from "axios";
import {Campaign} from "../model";

export interface CampaignService {
    updateAccessibility(campaign: Campaign): Promise<AxiosPromise>;
}


export const campaignService: CampaignService = {
    updateAccessibility: async (campaign: Campaign): Promise<AxiosPromise> => {
        if (campaign.automatic_close) {
            if (campaign.checkIsOpen()) {
                campaign.end_date = moment().toDate();
            } else {
                campaign.end_date = null;
                if (moment().isBefore(moment(campaign.start_date))) {
                    campaign.start_date = moment().toDate();
                }
            }
            campaign.automatic_close = false;

        } else {
            if (campaign.start_date) {
                if (campaign.end_date) {
                    campaign.end_date = null;
                } else {
                    campaign.end_date = moment().toDate();
                }
            } else
                campaign.start_date = moment().toDate();
        }
        return http.put(`/lystore/campaign/accessibility/${campaign.id}`, campaign.toJson())
    }
}
export const CampaignService = ng.service('CampaignService', (): CampaignService => campaignService);