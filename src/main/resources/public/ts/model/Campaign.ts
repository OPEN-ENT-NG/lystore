import http from 'axios';
import {_, notify,moment,toasts} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {Purses, StructureGroup, Tags, Titles} from './index';

export interface ICampaignResponse {
    id: number,
    name: string,
    description: string,
    image: string,
    purse_enabled: boolean,
    priority_enabled: boolean,
    start_date: string,
    end_date: string,
    automatic_close: boolean

}
export class Campaign implements Selectable  {
    id?: number;
    name: string;
    description: string;
    image: string;
    groups: StructureGroup[];
    selected: boolean;
    purse_amount?: number;
    initial_purse_amount?: number;
    nb_structures: number;
    nb_equipments: number;
    nb_orders: number;
    purses?: Purses;
    titles: Titles;
    nb_panier?: number;
    nb_baskets?: number;
    purse_enabled: boolean;
    priority_enabled: boolean;
    priority_field: null|PRIORITY_FIELD;
    end_date :Date;
    start_date : Date;
    automatic_close : boolean;
    isOpen: boolean ;

    constructor (name?: string, description?: string) {
        if (name) this.name = name;
        if (description) this.description = description;
        this.groups = [];
        this.image = '';
        this.purse_enabled = false;
        this.priority_enabled = true;
        this.automatic_close = true;
        this.priority_field = PRIORITY_FIELD.ORDER
        //default value = 1 an
        this.start_date = moment().format('YYYY-MM-DD')
        this.end_date = moment().add(1,'year').format('YYYY-MM-DD')
        this.isOpen = this.checkIsOpen();
    }

    toJson () {
        return {
            name: this.name,
            description: this.description || null,
            image: this.image || null,
            groups: this.groups.map((group) => {
                return group.toJson();
            }),
            purse_enabled: this.purse_enabled,
            priority_enabled: this.priority_enabled,
            priority_field: this.priority_field,
            end_date: this.end_date,
            start_date: this.start_date,
            automatic_close: this.automatic_close
        };
    }
    toString() {
        return this.name;
    }
    async save () {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create () {
        try {
            await http.post(`/lystore/campaign`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/lystore/campaign/${this.id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.campaign.update.err');
        }
    }

    async delete () {
        try {
            await http.delete(`/lystore/campaign/${this.id}`);
        } catch (e) {
            notify.error('lystore.campaign.delete.err');
        }
    }

    projectPriorityEnable(){
        return (this.priority_field == PRIORITY_FIELD.PROJECT || !this.priority_field )  && this.priority_enabled ;
    }
    orderPriorityEnable(){
        return this.priority_field == PRIORITY_FIELD.ORDER  && this.priority_enabled ;
    }
    async sync (id, tags?: Tags) {
        try {
            let { data } = await http.get(`/lystore/campaigns/${id}`);
            Mix.extend(this, Mix.castAs(Campaign, data));
            this.start_date =  moment(this.start_date);
            this.end_date = moment(this.end_date);
            this.isOpen = this.checkIsOpen();
            if (this.groups[0] !== null ) {
                this.groups = Mix.castArrayAs(StructureGroup, JSON.parse(this.groups.toString())) ;
                if (tags) {
                    this.groups.map((group) => {
                        group.tags =  group.tags.map( (tag) => {
                            return _.findWhere(tags, {id: tag});
                        });
                    });
                }
            } else this.groups = [];
        } catch (e) {
            notify.error('lystore.campaign.sync.err');
        }
    }

    checkIsOpen(): boolean {
        return (this.automatic_close)
            ? moment().isBetween(moment(this.start_date), moment(this.end_date), "days", "[]")
            : (this.end_date)
                ? moment().isBetween(moment(this.start_date), moment(this.end_date), "days", "[)")
                : moment().isSameOrAfter(moment(this.start_date));
    }


    build(campaign: ICampaignResponse): Campaign {
        this.id = campaign.id;
        this.name = campaign.name;
        this.description = campaign.description;
        this.image = campaign.image;
        this.purse_enabled = campaign.purse_enabled;
        this.priority_enabled = campaign.priority_enabled;
        this.start_date = new Date(campaign.start_date);
        this.automatic_close = campaign.automatic_close;
        this.end_date = new Date(campaign.start_date);
        return this;
    }
}


export class Campaigns extends Selection<Campaign> {

    constructor () {
        super([]);
    }

    async delete (campaigns: Campaign[]): Promise<void> {
        try {
            let filter = '';
            campaigns.map((campaign) => filter += `id=${campaign.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/lystore/campaign?${filter}`);
        } catch (e) {
            notify.error('lystore.campaign.delete.err');
        }
    }

    async sync (Structure?: string) {
        try {
            let { data } = await http.get( Structure ? `/lystore/campaigns?idStructure=${Structure}`  : `/lystore/campaigns`  );
            this.all = Mix.castArrayAs(Campaign, data);
            this.all.forEach(c =>{
                    c.isOpen = c.checkIsOpen();
            });
        } catch (e) {
            notify.error('lystore.campaigns.sync.err');
        }
    }

    get (idCampaign: number): Campaign {
        return _.findWhere(this.all, { id: idCampaign });
    }

    isEmpty (): boolean {
        return this.all.length === 0;
    }

    async exportOrders(campaigns: Campaign[]) {
        let filter = '';
        campaigns.map((campaign) => filter += `id=${campaign.id}&`);
        filter = filter.slice(0, -1);
        let { status } = await http.get(`/lystore/campaign/export/order?${filter}`);
        if(status === 201)
            toasts.info("lystore.export.notif")
    }
}

export enum PRIORITY_FIELD {
    PROJECT = 'PROJECT',
    ORDER = 'ORDER'
}