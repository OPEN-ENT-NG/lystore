import {ng} from 'entcore'
import http, {AxiosResponse} from 'axios';

export interface BCAdress {
    line1: string;
    line2: string;
}

export interface BCName {
    line1: string;
    line2: string;
    line3: string;
    line4: string;
}

export interface BCSignature {
    line1: string;
    line2: string;
}

export interface BCOptions {
    img: any;
    name: BCName;
    address: BCAdress;
    signature: BCSignature;
}

export interface ExportChoices {
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

export interface LystoreOptions {
    hasOperationsAndInstructions: boolean;
    bcOptions: BCOptions;
    exportChoices: ExportChoices
}

export interface ParameterSettingService {
    getOptions(): Promise<LystoreOptions>;

    getBCoptions(): Promise<BCOptions>;

    getExportChoices(): Promise<ExportChoices>;

    getHasOperationsAndInstructions(): Promise<boolean>;
}


export const parameterSettingService: ParameterSettingService = {
    getBCoptions(): Promise<BCOptions> {
        let address:BCAdress = {line1: "", line2: ""}
        let name:BCName = {line3: "", line4: "", line1: "", line2: ""}
        let signature:BCSignature = {line1: "", line2: ""}
        let bcOptions: BCOptions = {
            address: address,
            img: undefined,
            name: name,
            signature: signature}
        return Promise.resolve(bcOptions);
    },
    getExportChoices(): Promise<ExportChoices> {
        let exportChoices: ExportChoices = {
            BC: false,
            BCStructure: false,
            CSF: false,
            equipmentNotification: false,
            equipmentRapport: false,
            iris: false,
            listLycee: false,
            publipostage: false,
            rme: false,
            subvention: false
        };
        return Promise.resolve(exportChoices);
    },
    getHasOperationsAndInstructions(): Promise<boolean> {
        return Promise.resolve(true);
    },
    getOptions (): Promise<LystoreOptions> {
        let lystoreOptions: LystoreOptions = {
            bcOptions: undefined,
            exportChoices: undefined,
            hasOperationsAndInstructions: false
        };
        return  Promise.all([this.getBCoptions(), this.getExportChoices(), this.getHasOperationsAndInstructions()])
            .then((results: [BCOptions, ExportChoices, boolean]) => {
                lystoreOptions.bcOptions = results[0];
                lystoreOptions.exportChoices = results [1];
                lystoreOptions.hasOperationsAndInstructions = results [2];
                return lystoreOptions;
            });
    }

}
export const ParameterSettingService = ng.service('ParameterSettingService', (): ParameterSettingService => parameterSettingService);