import { Selectable, Mix, Selection } from 'entcore-toolkit';
import http from 'axios';

export class Structure implements Selectable {
    id: string;
    name: string;
    uai: string;
    city: string;
    academy: string;
    type:string;

    selected: boolean;

    constructor (name?: string, uai?: string, city?: string) {
       if (name) this.name = name;
       if (uai) this.uai = uai;
       if (city) this.city = city;
       this.selected = false;
    }

    toJson () {
        return {
            id: this.id,
            name: this.name,
            uai: this.uai,
            city: this.city
        };
    }

}

export class Structures  extends Selection<Structure> {

    constructor () {
        super([]);
    }

    async sync (): Promise<void> {
        let {data} = await http.get(`/lystore/structures`);
        this.all = Mix.castArrayAs(Structure, data);
    }

    async syncUserStructures (): Promise<void> {
        let { data } = await http.get('/lystore/user/structures');
        this.all = Mix.castArrayAs(Structure, data);
    }

}