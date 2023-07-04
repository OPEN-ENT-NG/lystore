import {notify, toasts} from 'entcore';
import http from 'axios';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {Structure} from "./Structure";


export interface IPurseStructureResponse {
    id: number,
    amount: number,
    id_campaign: number,
    initial_amount: number,
    total_order: number,
    uai: string,//à changer en Istruct surement
    id_structure: string,
    name: string
}

export class Purse implements Selectable {
    id?: number;
    id_structure: string;
    amount: number;
    id_campaign: number;
    selected: boolean;
    substraction?: any;
    bigDifference: boolean;
    initial_amount: any;
    total_order: number;
    structure: Structure;

    constructor(id_structure?: string, amount?: number, id_campaign?: number, initial_amount?: number) {
        if (id_structure) this.id_structure = id_structure;
        if (amount) this.amount = amount;
        if (initial_amount) this.initial_amount = initial_amount;
        if (id_campaign) this.id_campaign = id_campaign;
        this.structure = new Structure();
        this.selected = false;
    }

    toJson() {
        return {
            id_structure: this.id_structure,
            amount: this.amount,
            id_campaign: this.id_campaign
        };
    }

    copy(): Purse {
        let copyPurse = new Purse()
        copyPurse.id = this.id;
        copyPurse.id_campaign = this.id_campaign;
        copyPurse.amount = this.amount;
        copyPurse.initial_amount = this.initial_amount;
        copyPurse.total_order = this.total_order;
        copyPurse.structure = this.structure;
        copyPurse.selected = this.selected;
        return copyPurse;
    }

    build(purseData: IPurseStructureResponse): Purse {
        this.id = purseData.id;
        this.id_campaign = purseData.id_campaign;
        this.amount = purseData.amount
        this.initial_amount = purseData.initial_amount;

        (!isNaN(purseData.total_order)) ? this.total_order = purseData.total_order : 0;
        this.structure.name = purseData.name;
        this.structure.uai = purseData.name;
        this.structure.id = purseData.id_structure;
        this.selected = false;
        return this;
    }
}

export class Purses extends Selection<Purse> {

    id_campaign: number;

    constructor(id_campaign: number) {
        super([]);
        this.id_campaign = id_campaign;
    }

    build(pursesData: IPurseStructureResponse[]): Purses {
        this.all = pursesData.map(purseData => {
            return new Purse().build(purseData);
        })
        return this
    }
}

export class PurseImporter {
    files: File[];
    id_campaign: number;
    message: string;

    constructor(id_campaign: number) {
        this.files = [];
        this.id_campaign = id_campaign;
    }

    isValid(): boolean {
        return this.files.length > 0
            ? this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== ''
            : false;
    }
}