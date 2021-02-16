import {Mix, Selectable, Selection} from "entcore-toolkit";
import http from "axios";
import {moment, toasts} from "entcore";
import {Utils} from "./Utils";

export class label implements Selectable {
    id: number;
    label: string;
    start_date: Date;
    end_date: Date;
    selected: boolean;
    is_used: number;
    max_creation_date: string;

    constructor() {
        this.label = '';
    }

    async save() {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create() {
        try {
            await http.post(`/lystore/labelOperation/manage`, this.toJson());
        } catch (e) {
            toasts.warning('lystore.operation.label.create.err');
            throw e;
        }
    }

    async update() {
        try {
            await http.put(`/lystore/labelOperation/manage/${this.id}`, this.toJson());
        } catch (e) {
            toasts.warning('lystore.operation.label.update.err');
            throw e;
        }
    }

    async delete() {
        try {
            let labelIds = [this.id];
            let {status} = await http.delete('/lystore/labelOperations/manage', {data: labelIds});
            if (status === 202) {
                toasts.warning('lystore.operation.label.delete.err');
            }
        } catch (e) {
            toasts.warning('lystore.operation.label.delete.err');
            throw e;
        }

    }

    toJson() {
        return {
            id: this.id,
            label: this.label,
            start_date: moment(this.start_date).format('YYYY-MM-DD'),
            end_date: moment(this.end_date).format('YYYY-MM-DD'),

        }
    }
}

export class labels extends Selection<label> {
    filters: Array<string>;

    constructor() {
        super([]);
        this.filters = [];
    }

    async delete() {
        let labelIds = this.selected.map(label => label.id);
        try {
            await http.delete('/lystore/labelOperations/manage', {data: labelIds});
        } catch (err) {
            toasts.warning('lystore.operation.label.delete.err');
        }
    }

    async sync() {
        try {
            const queriesFilter = Utils.formatGetParameters({q: this.filters});
            let {data} = await http.get(`/lystore/labels/?${queriesFilter}`);
            this.all = Mix.castArrayAs(label, data);
            this.all.map(label => {
                label.label = label.label.trim();
                if (label.start_date && label.end_date) {
                    label.start_date = moment(label.start_date).format('YYYY-MM-DD');
                    label.end_date = moment(label.end_date).format('YYYY-MM-DD');
                }
            })
        } catch (err) {
            toasts.warning('lystore.operation.label.get.err')
        }
    }
}