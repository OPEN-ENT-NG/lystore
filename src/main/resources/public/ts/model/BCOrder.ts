export interface IBCOrder {
    id:number;
    dateCreation :Date;
    dateCp:string;
    engagementNumber?:string;
    number:string;
}

export class BCOrder implements IBCOrder{
    dateCp: string;
    dateCreation: Date;
    engagementNumber?: string;
    id: number;
    number: string;

}