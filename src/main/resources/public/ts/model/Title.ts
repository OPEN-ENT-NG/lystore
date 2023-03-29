import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from "axios";
import {Structure, Structures} from "./Structure";

export interface Title {
    id: number;
    name: string;
    selected: boolean;
}

interface ITitleResponse {
    id: number;
    name: string;
}


export class Title implements Selectable {
    constructor(id?: number, name?: string) {
        if (id) {
            this.id = id;
        }
        if (name) {
            this.name = name;
        }
    }

    toJson() :ITitleResponse{
        return {
            id: this.id,
            name: this.name
        }
    }
}
export interface IStructureTitlesResponse {
    id_structure:string;
    name:string;
    titles: Array<ITitleResponse>;

}



export class Titles extends Selection<Title> {
    constructor() {
        super([]);
    }

    build(titlesResponse: Array<ITitleResponse>): Titles {
        this.all = titlesResponse.map((titleResponse: ITitleResponse) => {
            let title: Title = new Title(titleResponse.id, titleResponse.name);
            title.selected = false;
            return title;
        });
        return this;
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

    toJson() :TitlesPayload {
        return {
            titles: this.all.map((title: Title) => title.toJson())
        }
    }
}

export interface TitlesPayload {
    titles : ITitleResponse[];
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