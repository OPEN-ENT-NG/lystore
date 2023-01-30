export interface IExportChoices {
    iris: boolean;
    rme: boolean;
    equipmentRapport: boolean;
    equipmentNotification: boolean;
    subvention: boolean;
    publipostage: boolean;
    listLycee: boolean;
    BC: boolean;
    BCStructure: boolean;
    CSF: boolean;
}

export class ExportChoices implements IExportChoices {
    iris: boolean;
    rme: boolean;
    equipmentRapport: boolean;
    equipmentNotification: boolean;
    subvention: boolean;
    publipostage: boolean;
    listLycee: boolean;
    BC: boolean;
    BCStructure: boolean;
    CSF: boolean;

    constructor() {
        this.BC = false;
        this.BCStructure = false;
        this.CSF = false;
        this.equipmentNotification = false;
        this.equipmentRapport = false;
        this.iris = false;
        this.listLycee = false;
        this.publipostage = false;
        this.rme = false;
        this.subvention = false;
    }
}