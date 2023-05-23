import http from 'axios';
import { Selectable, Selection, Mix } from 'entcore-toolkit';

export interface IContractTypeResponse {
    id: number,
    code: string,
    name: string,
    description: string
}
export class ContractType implements Selectable {
    id?: number;
    code: string;
    name: string;
    displayName: string;
    description: string
    selected: boolean;

    constructor (code?: string, name?: string) {
        if (code) this.code = code;
        if (name) this.name = name;

        this.selected = false;
    }

    build(contractTypeResponse: IContractTypeResponse):ContractType{
        this.id = contractTypeResponse.id;
        this.code = contractTypeResponse.code;
        this.name = contractTypeResponse.name;
        this.description = contractTypeResponse.description;
        return  this;
    }
}

export class ContractTypes extends Selection<ContractType> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let types = await http.get(`/lystore/contract/types`);
        this.all = Mix.castArrayAs(ContractType, types.data);
        this.all.map((type) => type.displayName = type.code + ' - ' + type.name);
    }
}