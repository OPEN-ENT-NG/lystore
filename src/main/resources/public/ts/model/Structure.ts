import {_} from "entcore";
import { Selectable, Mix, Selection } from 'entcore-toolkit';
import http from 'axios';
import {IStructuresTitlesResponse, IStructureTitlesResponse, Titles} from "./Title";

export class Structure implements Selectable {
    id: string;
    name: string;
    uai: string;
    city: string;
    academy: string;
    type:string;
    department:number;
    selected: boolean;
    titles:Titles;
    constructor (name?: string, uai?: string, city?: string, department?: number) {
       if (name) this.name = name;
       if (uai) this.uai = uai;
       if (city) this.city = city;
       if(department) this.department = department;
       this.selected = false;
       this.titles = new Titles();
    }

    toJson () {
        return {
            id: this.id,
            name: this.name,
            uai: this.uai,
            city: this.city
        };
    }

    getTitleJson() :IStructureTitlesResponse{
        return {
            id_structure : this.id,
            titles: this.titles.selected.map(titles => titles.toJson())
        }
    }
}

export class Structures  extends Selection<Structure> {

    constructor () {
        super([]);
    }

    buildWithTitle(StructureTitlesResponse: IStructureTitlesResponse[]): Structures {

        this.all = StructureTitlesResponse.map((structureResponse: IStructureTitlesResponse) => {
            let structure: Structure = new Structure(structureResponse.name);
            structure.id = structureResponse.id_structure;
            structure.titles = new Titles().build(structureResponse.titles);
            return structure;
        });
        return this;
    }

    async sync (): Promise<void> {
        let {data} = await http.get(`/lystore/structures`);
        this.all = Mix.castArrayAs(Structure, data);
    }
    async getStructureType() : Promise<void> {
        let {data} = await http.get(`/lystore/structures/type`);
        this.all.map((structure)=>{
            let type = _.findWhere(data, {id: structure.id});
            structure.type = type ? type.type : 'LYC';
        })
    }

    async syncUserStructures (): Promise<void> {
        let { data } = await http.get('/lystore/user/structures');
        this.all = Mix.castArrayAs(Structure, data);
    }

    getTitlesJson():IStructuresTitlesResponse {
        return {
            structures : this.all.map((structure:Structure) => structure.getTitleJson())
        };
    }
}