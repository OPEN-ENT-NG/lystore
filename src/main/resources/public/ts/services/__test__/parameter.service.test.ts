import axios from 'axios';
import {parameterService} from "../parameter";
import MockAdapter from 'axios-mock-adapter';

describe('parameter service test', () => {
    const mock = new MockAdapter(axios);
    const data = {response: true};

    it('calling getStructuresLystore data when retrieve request is correctly called', done => {
        mock.onGet(`structures/lystore`).reply(200, data);
        parameterService.getStructuresLystore().then(response => {
            expect(response).toEqual(data);
            done();
        });
    });

    it('calling undeployStructure should return query correctly', done => {
        mock.onDelete(`/lystore/structures/5`).reply(200, data);
        parameterService.undeployStructure("5").then(response => {
            expect(response.data).toEqual(data);
            done();
        });
    });
});
