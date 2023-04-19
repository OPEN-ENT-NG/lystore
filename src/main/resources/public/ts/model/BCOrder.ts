export interface IBCOrder {
    id:number;
    dateCreation :string;
    dateCp:string;
    engagementNumber?:string;
    number:string;
}

export class BCOrder implements IBCOrder{
    dateCp: string;
    dateCreation: string;
    engagementNumber?: string;
    id: number;
    number: string;

}