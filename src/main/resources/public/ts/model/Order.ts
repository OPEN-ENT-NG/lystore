import {
    Campaign,
    Contract,
    ContractType,
    Equipment,
    IContractResponse,
    IContractTypeResponse,
    IOrderClientResponse,
    IOrderRegionResponse,
    IProjectResponse,
    ITitleResponse,
    OrderClient,
    OrderOptionClient,
    OrderRegion,
    OrdersClient,
    OrdersRegion,
    Program,
    Project,
    Structure,
    Structures,
    Supplier,
    Title,
    Utils
} from './index';
import {Mix, Selectable,Selection} from "entcore-toolkit";
import {_} from "entcore";
import {IOrderClientOptionResponse} from "./OrderOptionClient";

export interface IOrderResponse{
    id: number,
    comment: string,
    price_proposal?: number,
    preference: number,
    id_project?: number,
    price: number,
    tax_amount?: number,
    amount: number,
    creation_date: string,
    id_campaign: number,
    id_structure: string,
    name: string,
    summary?: string,
    image?: string,
    status: string,
    id_contract: number,
    rank?:number,
    options: IOrderClientOptionResponse[],
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
    //à adapater dans des refactos ultérieures
    files: string,
    typeOrder: string
    contract?:IContractResponse,
    contract_type?:IContractTypeResponse
}

export interface OrderImp extends Selectable{
    amount: number;
    campaign: Campaign;
    comment: string;
    contract: Contract;
    contract_type: ContractType;
    creation_date: Date;
    equipment_key:number;
    id_structure: string;
    price: number;
    price_proposal: number;
    price_single_ttc: number;
    project: Project;
    rank: number;
    rankOrder: Number;
    selected:boolean;
    structure: Structure;
    tax_amount: number;
    title:Title;
}

export class Order implements OrderImp{
    amount: number;
    campaign: Campaign;
    comment: string;
    contract: Contract;
    contract_type: ContractType;
    creation_date: Date;
    equipment: Equipment;
    equipment_key:number;
    id?: number;
    id_operation:Number;
    id_structure: string;
    inheritedClass:Order|OrderClient|OrderRegion;
    options;
    order_parent?:any;
    price: number;
    price_proposal: number;
    price_single_ttc: number;
    program: Program;
    project: Project;
    files?: any;
    rank: number;
    rankOrder: Number;
    selected:boolean;
    structure: Structure;
    tax_amount: number;
    title:Title;
    typeOrder:string;
    override_region: boolean;
    total?:number;

    constructor(order: Order, structures:Structures){
        this.amount  = order.amount? parseInt(order.amount.toString()) : null;
        this.campaign = order.campaign? Mix.castAs(Campaign, JSON.parse(order.campaign.toString())) : null;
        this.comment = order.comment;
        this.contract = order.contract? JSON.parse(order.contract.toString()) : null;
        this.contract_type = order.contract_type? JSON.parse(order.contract_type.toString()) : null;
        this.equipment_key = order.equipment_key;
        this.inheritedClass = order;
        this.project = order.project? Mix.castAs(Project, JSON.parse(order.project.toString())) : null;
        this.price = order.price? parseFloat(order.price.toString()) : null;
        this.price_proposal = order.price_proposal? parseFloat(order.price_proposal.toString()) : null;
        this.price_single_ttc  = order.price_single_ttc? parseFloat(order.price_single_ttc.toString()) : null;
        this.rank  = order.rank? parseInt(order.rank.toString()) + 1: null;
        this.structure = order.id_structure? OrderUtils.initStructure( order.id_structure, structures) : new Structure();
        this.tax_amount  = order.tax_amount? parseFloat(order.tax_amount.toString()) : null;
        this.title = order.title?JSON.parse(order.title.toString()) : null;
        this.typeOrder = order.typeOrder;
        if(order.id)this.id = order.id;
        if(order.id_operation)this.id_operation = order.id_operation;
        if(order.order_parent){
            this.order_parent = order.order_parent;
        }
        if(order.options){
            this.options = order.options.toString() !== '[null]' && order.options !== null ?
                Mix.castArrayAs(OrderOptionClient, JSON.parse(order.options.toString()))  :
                [];
        }
    }

}


export class Orders extends Selection<Order> {
    constructor() {
        super([]);
    }

    build(ordersResponse: IOrderResponse[]): Orders {
        let ordersClient: OrdersClient;
        let ordersRegion: OrdersRegion;

        ordersClient = new OrdersClient().build(ordersResponse.filter((orderResponse: IOrderResponse) => {
            return orderResponse.typeOrder === "client"
        }).map((order: IOrderResponse) => {
            return order as IOrderClientResponse;
        }))

        ordersRegion = new OrdersRegion().build(ordersResponse.filter((orderResponse: IOrderResponse) => {
            return orderResponse.typeOrder === "region"
        }).map((orderResponse: IOrderResponse) => {
            return orderResponse as IOrderRegionResponse;
        }))

        this.all =  this.all.concat(ordersClient.all)
        this.all =  this.all.concat(ordersRegion.all)
        return this;
    }
}
export class OrderUtils {
    static initStructure(idStructure:string, structures:Structures):Structure{
        const structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure : new Structure() ;
    }

    static initNameStructure (idStructure: string, structures: Structures):string {
        let structure = _.findWhere(structures, { id : idStructure});
        return  structure ? structure.uai + '-' + structure.name : '' ;
    }

    static calculatePriceTTC( roundNumber?: number, order?:Order|OrderClient|OrderRegion):number|any {
        let price = parseFloat(Utils.calculatePriceTTC(order.price , order.tax_amount).toString());
        if (order.options !== undefined) {
            order.options.map((option) => {
                price += parseFloat(Utils.calculatePriceTTC(option.price , option.tax_amount).toString() );
            });
        }
        return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price ) : price ;
    }
    static initParentOrder( order:Order):Object{
        if(!order)return;
        if(order.equipment) {
            return {
                amount: order.amount || 0,
                comment: order.comment || "",
                equipment: {
                    contract_type_name: order.equipment.contract_type_name || "",
                    name: order.equipment.name || "",
                },
                price_single_ttc: OrderUtils.findGoodPrice(order) || 0,
                rank: order.rank || 0,
            };
        } else {
            return  {
                amount : order.amount || 0,
                comment : order.comment || "",
                equipment : {
                    contract_type_name: "",
                    name: "",
                },
                price_single_ttc : OrderUtils.findGoodPrice(order) || 0,
                rank : order.rank || 0,
            };
        }
    }
    //DEPRECATED
    static findGoodPrice(order:Order):Number{
        if(order.price_single_ttc) return Number(order.price_single_ttc.toFixed(2));
        if(order.price_proposal) return Number(order.price_proposal.toFixed(2));
        if(order.price) return OrderUtils.calculatePriceTTC(2,order);
        return 0;
    }
}