import {http} from 'entcore-toolkit';
import {mockHttpResponse} from "../../test-utils/httpMock";
import {purseService} from "../app/admin/purse.service";
import {Purse, PurseImporter, Purses} from "../../model";

jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

describe('purse service test', () => {
    let data :  [{"amount": number, "id": number, "id_campaign": number, "initial_amount": number, "selected": boolean,
        "structure": {"id": string, "name": string, "selected": boolean, "titles": {"arr": [], "selectedElements": []}, "uai": string}}];
    const dataDefault = undefined;
    const dataImporter = undefined;

    it('calling sync data when retrieve request is correctly called', done => {
        let idCampaign = 81;
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url: `/lystore/campaign/${idCampaign}/purses/list`}));
        purseService.sync(idCampaign).then(response => {
            expect(http.get).toHaveBeenCalledWith(`/lystore/campaign/${idCampaign}/purses/list`);
            expect(response).toBeInstanceOf(Purses);
            done();
        });
    });

    it('calling check purse', done => {
        let idCampaign = 81;
        let purses:Purses = new Purses(idCampaign);
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(dataDefault, {url: `/lystore/campaign/${idCampaign}/purse/check`}));
        purseService.check(idCampaign,purses).then(response => {
            expect(http.get).toHaveBeenCalledWith(`/lystore/campaign/${idCampaign}/purse/check`);
            expect(response).toBeUndefined();
            done();
        });
    });

});
