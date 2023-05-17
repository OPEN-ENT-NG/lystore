import http from "axios";
import {_, moment, notify} from "entcore";
import {
    Campaign,
    Contract,
    ContractType,
    Grade,
    Order,
    Program,
    Structure,
    Structures,
    Supplier,
    TechnicalSpec,
    Title,
    Utils,
    OrderClient,
    Equipment,
    Project,
    IOrderResponse,
    IProjectResponse,
    ITitleResponse,
    OrderOptionClient,
    Operation,
    Label, Instruction, ICampaignResponse,
} from "./index";
import {Selection} from "entcore-toolkit";
import {IOrderClientOptionResponse} from "./OrderOptionClient";
import {BCOrder} from "./BCOrder";

export interface IOrderRegionResponse extends IOrderResponse {
    priceTTC: number,
    id: number,
    comment: string,
    preference: number,
    price: number,
    amount: number,
    creation_date: string,
    id_campaign: number,
    id_structure: string,
    name: string,
    summary?: string,
    image?: string,
    status: string,
    id_contract: number,
    rank?: number,
    project?: IProjectResponse,
    title?: ITitleResponse,
    name_supplier?: string,
    cp_number: string,
    operation_label: string,
    order_creation_date: string,
    done_date?: string,
    instruction_object: string,
    date_operation?: string,
    date_cp?: string,
    campaign: ICampaignResponse,
    //à adapater dans des refactos ultérieures
    files: string,
    total?:number
}


export class OrderRegion implements Order {
    amount: number;
    campaign: Campaign;
    comment: string;
    contract: Contract;
    contract_type: ContractType;
    creation_date: Date;
    equipment: Equipment;
    equipment_key: number;
    id?: number;
    id_operation: Number;
    id_structure: string;
    inheritedClass: Order | OrderClient | OrderRegion;
    options;
    order_parent?: any;
    price: number;
    price_proposal: number;
    price_single_ttc: number;
    program: Program;
    project: Project;
    rank: number;
    rankOrder: Number;
    selected: boolean;
    structure: Structure;
    tax_amount: number;
    title: Title;
    typeOrder: string;

    contract_name?: string;
    description: string;
    files?: any;
    id_campaign: number;
    id_contract: number;
    id_orderClient: number;
    id_project: number;
    id_supplier: string;
    grade?: Grade;
    name: string;
    name_structure: string;
    number_validation: string;
    label_program: string;
    order_client: OrderClient;
    order_number?: string;
    preference: number;
    priceUnitedTTC: number;
    structure_groups: any;
    supplier: Supplier;
    supplier_name?: string;
    summary: string;
    image: string;
    status: string;
    technical_spec: TechnicalSpec;
    title_id ?: number;
    id_type: number;
    override_region: boolean;
    filesMetadata?: any;
    done_date: Date;
    bCOrder?: BCOrder;
    operation: Operation;
    total:number


    constructor() {
        this.typeOrder = "region";
    }

    toJson(): any {
        return {
            amount: this.amount,
            name: this.equipment ? (this.equipment.name ? this.equipment.name : "") : "",
            price: this.price,
            summary: this.summary ? this.summary : "",
            description: (this.description) ? this.description : "",
            ...(this.id_orderClient && {id_order_client_equipment: this.id_orderClient}),
            image: this.image,
            creation_date: moment().format('YYYY-MM-DD'),
            status: this.status,
            ...(this.number_validation && {number_validation: this.number_validation}),
            ...(this.title_id && {title_id: this.title_id}),

            id_contract: this.id_contract,
            files: this.files,
            name_structure: this.name_structure,
            id_campaign: this.id_campaign,
            id_structure: this.id_structure,
            id_project: this.id_project,
            equipment_key: this.equipment ? (this.equipment.id ? this.equipment_key : "") : "",
            comment: this.comment ? this.comment : "",
            ...(this.rank && {rank: this.rank}),
            technical_specs: (Utils.parsePostgreSQLJson(this.technical_spec) === null || Utils.parsePostgreSQLJson(this.technical_spec).length === 0) ?
                [] :
                Utils.parsePostgreSQLJson(this.technical_spec).map(spec => {
                    return {
                        name: spec.name,
                        value: spec.value
                    }
                }),
            id_operation: this.id_operation,
            rank: this.rank - 1,
            id_type: this.id_type ? this.id_type : "",
        }
    }

    createFromOrderClient(order: OrderClient): void {
        this.order_client = order;
        this.id_orderClient = order.id;
        this.amount = order.amount;
        this.name = order.name;
        this.summary = order.summary;
        this.description = order.description;
        this.image = order.image;
        this.creation_date = order.creation_date;
        this.status = order.status;
        this.number_validation = order.number_validation;
        this.technical_spec = order.technical_spec;
        this.contract = order.contract;
        this.campaign = order.campaign;
        this.structure_groups = order.structure_groups;
        this.contract_name = order.contract_name;
        this.project = order.project;
        this.files = order.files;
        this.contract_type = order.contract_type;
        this.name_structure = order.name_structure;
        this.id_contract = order.id_contract;
        this.id_campaign = order.id_campaign;
        this.id_structure = order.id_structure;
        this.id_project = order.id_project;
        this.comment = order.comment;
        this.price = order.price_single_ttc;
        this.rank = order.rank;
        this.structure = order.structure;
        this.id_operation = order.id_operation;
        this.equipment = order.equipment;
        this.id_type = order.equipment.id_type;
    }


    async create(): Promise<any> {
        try {
            return await http.post(`/lystore/region/order`, this.toJson());
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    async update(id: number): Promise<any> {
        try {
            return await http.put(`/lystore/region/order/${id}`, this.toJson());
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    initDataFromEquipment(): void {
        if (this.equipment) {
            this.summary = this.equipment.name;
            this.image = this.equipment.image;

        }
    }

    async delete(id: number): Promise<any> {
        try {
            return await http.delete(`/lystore/region/${id}/order`);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    async getOneOrderRegion(id: number, structures: Structures): Promise<Order> {
        try {
            const {data} = await http.get(`/lystore/orderRegion/${id}/order`);
            return new Order(Object.assign(data, {typeOrder: "region"}), structures);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }


    async getFilesMetadata(idOrder) {
        try {
            const {data} = await http.get(`/lystore/orderRegion/${idOrder}/files`);
            return data
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    async deleteDocument(file) {
        try {
            await http.delete(`/lystore/order/update/file/${file.id}`);
        } catch (err) {
            throw err;
        }
    }

    downloadFile(file, id): void {
        window.open(`/lystore/order/${id}/file/${file.id}`);
    }

    build(orderRegionResponse: IOrderRegionResponse): OrderRegion {
        this.amount = orderRegionResponse.amount;
        this.comment = orderRegionResponse.comment;
        this.creation_date = new Date(orderRegionResponse.creation_date);
        if (orderRegionResponse.done_date)
            this.done_date = new Date(orderRegionResponse.done_date);
        if (orderRegionResponse.files && orderRegionResponse.files.length > 0 && orderRegionResponse.files[0] !== null)
            this.files = orderRegionResponse.files;
        this.id = orderRegionResponse.id;
        this.id_campaign = orderRegionResponse.id_campaign;
        this.image = orderRegionResponse.image;
        this.name = orderRegionResponse.name;
        this.preference = orderRegionResponse.preference;
        this.price = orderRegionResponse.priceTTC;
        this.price_proposal = orderRegionResponse.price_proposal;
        this.status = orderRegionResponse.status;
        this.summary = orderRegionResponse.summary;
        this.tax_amount = orderRegionResponse.tax_amount;
        //supplier
        this.supplier_name = orderRegionResponse.name_supplier;
        if (orderRegionResponse.options && orderRegionResponse.options.length > 0 && orderRegionResponse.options[0] !== null)
            this.options = orderRegionResponse.options.map((options: IOrderClientOptionResponse) => new OrderOptionClient().build(options));
        else
            this.options = [];

        if (orderRegionResponse.campaign) {
            this.campaign = new Campaign().build(orderRegionResponse.campaign)
        }
        //à transformer en contact
        this.id_contract = orderRegionResponse.id_contract;
        //project
        this.id_project = orderRegionResponse.id_project;
        orderRegionResponse.project.title = orderRegionResponse.title;
        this.project = new Project().build(orderRegionResponse.project);
        //structure
        this.id_structure = orderRegionResponse.id_structure
        if (orderRegionResponse.order_creation_date) {
            this.bCOrder = new BCOrder();
            this.bCOrder.dateCreation = new Date(orderRegionResponse.order_creation_date);
        }
        //
        if (orderRegionResponse.date_operation) {
            this.operation = new Operation();
            this.operation.date_operation = new Date(orderRegionResponse.date_operation);
            let label = new Label();
            label.label = orderRegionResponse.operation_label
            this.operation.label = label
            if (orderRegionResponse.instruction_object) {
                let instruction = new Instruction();
                if (orderRegionResponse.cp_number)
                    instruction.cp_number = orderRegionResponse.cp_number
                instruction.object = orderRegionResponse.instruction_object;
                if (orderRegionResponse.date_cp)
                    instruction.date_cp = moment(orderRegionResponse.date_cp);

                this.operation.instruction = instruction;
            }
        }
        return this;
    }
}

export class OrdersRegion extends Selection<OrderRegion> {
    constructor() {
        super([]);
    }

    async create(): Promise<any> {
        let orders = [];
        this.all.map(order => {
            order.initDataFromEquipment();
            orders.push(order.toJson());
        });
        try {
            return await http.post(`/lystore/region/orders/`, {orders: orders});
        } catch (e) {
            notify.error('lystore.order.create.err');
            throw e;
        }
    }

    async updateOperation(idOperation: number, idsRegions: Array<number>): Promise<any> {
        try {
            await http.put(`/lystore/order/region/${idOperation}/operation`, idsRegions);
        } catch (e) {
            notify.error('lystore.admin.order.update.err');
            throw e;
        }
    }

    build(ordersRegionsResponse: IOrderRegionResponse[]) {
        this.all = ordersRegionsResponse.map((orderRegionResponse: IOrderRegionResponse) => {
            return new OrderRegion().build(orderRegionResponse);
        });
        return this;
    }
}