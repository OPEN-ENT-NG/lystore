import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import {purseService} from "../app/admin";
import {Purse, PurseImporter, Purses} from "../../model";


describe('parameter service test', () => {
    const mock = new MockAdapter(axios);
    const data = {response: true};

    it('calling sync data when retrieve request is correctly called', done => {
        let idCampaign = 1;
        mock.onGet(`/lystore/campaign/${idCampaign}/purses/list`).reply(200, data);
        purseService.sync(idCampaign).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('calling save should return query correctly', done => {
        let purse:Purse = new Purse();
        purse.id = 1;
        purse.total_order = 2497.5;
        purse.initial_amount = 2500.5;
        purse.amount = 3;
        purse.id_campaign = 4;
        purse.id_structure = "test-structure";
        mock.onPut(`/lystore/purse/${purse.id}`, purse.toJson()).reply(200, data);
        purseService.save(purse).then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });

    it('calling sync data when retrieve request is correctly called', done => {
        let idCampaign = 1;
        let purses:Purses = new Purses(idCampaign);
        mock.onGet(`/lystore/campaign/${idCampaign}/purse/check`).reply(200, data);
        purseService.check(idCampaign,purses).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('calling sync data when retrieve request is correctly called', done => {
        let importer:PurseImporter = new PurseImporter(2);

        mock.onPost(`/lystore/campaign/${importer.id_campaign}/purses/import`).reply(200, data);
        purseService.validateImport(importer).then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

});
