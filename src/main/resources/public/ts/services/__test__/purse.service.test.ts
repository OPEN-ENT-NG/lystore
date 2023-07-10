import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {purseService} from "../app/admin/purse.service";
import {Purse, PurseImporter, Purses} from "../../model";


describe('purse service test', () => {
    const mock = new MockAdapter(axios);
    let data :  [{"amount": number, "id": number, "id_campaign": number, "initial_amount": number, "selected": boolean,
        "structure": {"id": string, "name": string, "selected": boolean, "titles": {"arr": [], "selectedElements": []}, "uai": string}}];
    const dataDefault = undefined;
    const dataImporter = undefined;

    it('calling sync data when retrieve request is correctly called', done => {
        let idCampaign = 81;
        mock.onGet(`/lystore/campaign/${idCampaign}/purses/list`).reply(200, data);
        purseService.sync(idCampaign).then(response => {
            expect(response).toBeInstanceOf(Purses);
            done();
        });
    });

    it('calling save should return query correctly', done => {
        let purse:Purse = new Purse();
        purse.id = 1;
        purse.initial_amount = 2500.5;
        purse.amount = 2500.5;
        purse.id_campaign = 4;
        mock.onPut(`/lystore/purse/1`, purse.toJson()).reply(200, data);
        purseService.save(purse).then(response => {
            expect(response.data).toBeDefined();
            done();
        });
    });

    it('calling check purse', done => {
        let idCampaign = 81;
        let purses:Purses = new Purses(idCampaign);
        mock.onGet(`/lystore/campaign/${idCampaign}/purse/check`).reply(200, dataDefault);
        purseService.check(idCampaign,purses).then(response => {
            expect(response).toBeUndefined();
            done();
        });
    });

});
