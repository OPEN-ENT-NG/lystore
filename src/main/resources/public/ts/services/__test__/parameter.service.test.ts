import axios from 'axios';
import {activeStructureService} from "../parameter/active-structure";
import MockAdapter from 'axios-mock-adapter';

describe('parameter service test', () => {
    const mock = new MockAdapter(axios);
    const data = {response: true};

    it('calling getStructuresLystore data when retrieve request is correctly called', done => {
        mock.onGet(`structures/lystore`).reply(200, data);
        activeStructureService.getStructuresLystore().then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('calling undeployStructure should return query correctly', done => {
        mock.onDelete(`/lystore/structures/5`).reply(200, data);
        activeStructureService.undeployStructure("5").then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });
});
