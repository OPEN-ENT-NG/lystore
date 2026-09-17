import {activeStructureService} from "../parameter/active-structure";
import {http} from 'entcore-toolkit';
import {mockHttpResponse} from "../../test-utils/httpMock";

jest.mock('entcore-toolkit', () => ({
    ...jest.requireActual('entcore-toolkit'),
    http: {get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn()}
}));

describe('parameter service test', () => {
    const data = {response: true};

    it('calling getStructuresLystore data when retrieve request is correctly called', done => {
        (http.get as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url: `structures/lystore`}));
        activeStructureService.getStructuresLystore().then(response => {
            expect(http.get).toHaveBeenCalledWith(`structures/lystore`);
            expect(response).toEqual(data);
            done();
        });
    });

    it('calling undeployStructure should return query correctly', done => {
        (http.delete as jest.Mock).mockResolvedValueOnce(mockHttpResponse(data, {url: `/lystore/structures/5`}));
        activeStructureService.undeployStructure("5").then(response => {
            expect(http.delete).toHaveBeenCalledWith(`/lystore/structures/5`);
            expect(response.data).toEqual(data);
            done();
        });
    });
});
