import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from "axios";
import {Structure, Structures} from "./Structure";

export interface Title {
    id: number;
    name: string;
}

export class Title implements Selectable {
    selected: boolean;

    constructor(id?: number, name?: string) {
        if (id) {
            this.id = id;
        }
        if (name) {
            this.name = name;
        }
    }
}

export interface IStructureTitles {
    id_structure:string;
    name:string;
    titles:[{
        id:number,
        name:string
    }]

}

export class StructureTitles {

    build(StructureTitlesResponse: IStructureTitles[]): Promise<Structures> {
        let structures: Structures = new Structures();
        StructureTitlesResponse.forEach(structureResponse => {
            let structure: Structure = new Structure();
            structure.id = structureResponse.id_structure;
            structure.name = structureResponse.name;
            structureResponse.titles.forEach(titleResponse => {
                let title: Title = new Title();
                title.id = titleResponse.id
                title.name = titleResponse.name
                title.selected = false
                structure.titles.all.push(title)
            })
            structures.all.push(structure)
        })
        return Promise.resolve(structures);
    }
}

export class Titles extends Selection<Title> {
    constructor() {
        super([]);
    }

    async sync(idCampaign?: number, idStructure?: string): Promise<void> {
        {

            const uri = idCampaign
                ? idStructure
                    ? `/lystore/titles/campaigns/${idCampaign}/structures/${idStructure}`
                    : `/lystore/titles/campaigns/${idCampaign}`
                : `/lystore/titles/`;
            let titles = await http.get(uri);
            this.all = Mix.castArrayAs(Title, titles.data);
        }
    }

    async syncAdmin(idCampaign: number): Promise<void> {
        {
            const uri = `/lystore/titles/campaigns/admin/${idCampaign}`;
            let titles = await http.get(uri);
            this.all = Mix.castArrayAs(Title, titles.data);
        }
    }

    toJson() {
        return {};
    }
}


export class TitleImporter {
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

    async validate(): Promise<any> {
        try {
            await this.postFile();
        } catch (err) {
            throw err;
        }
    }

    private async postFile(): Promise<any> {
        let formData = new FormData();
        formData.append('file', this.files[0], this.files[0].name);
        let response;
        try {
            response = await http.post(`/lystore/titles/campaigns/${this.id_campaign}/import`,
                formData, {'headers': {'Content-Type': 'multipart/form-data'}});
        }
        catch (err) {
            throw err.response.data;
        }
        return response;
    }
}