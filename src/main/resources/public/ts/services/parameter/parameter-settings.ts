import {ng} from 'entcore';
import http, {AxiosPromise} from 'axios';
import {BcOptions, IBCOptions} from "../../model/parameter/bc-options.model";
import {ExportChoices} from "../../model/parameter/export-choices.model";
import {LystoreOptions} from "../../model/parameter/lystore-options.model";


export interface ParameterSettingService {
    getOptions(): Promise<LystoreOptions>;

    getBCoptions(): Promise<BcOptions>;

    getExportChoices(): Promise<ExportChoices>;

    getHasOperationsAndInstructions(): Promise<boolean>;

    saveExportChoices(exportChoices: ExportChoices): Promise<void>;

    saveBcForm(bcOptions: BcOptions):   Promise<AxiosPromise>;

    saveHasOperationsAndInstructions(hasOperationsAndInstructions: boolean): Promise<void>;
}


export const parameterSettingService: ParameterSettingService = {
    getBCoptions(): Promise<BcOptions> {
       return http.get(`/lystore/parameter/bc/options`).then(
             (res) =>            {
                 let BCOptionsResponse: IBCOptions = res.data;
                 return new BcOptions().build(BCOptionsResponse);
             })
    },
    getExportChoices(): Promise<ExportChoices> {
        return Promise.resolve(new ExportChoices());
    },
    getHasOperationsAndInstructions(): Promise<boolean> {
        return Promise.resolve(false);
    },
    getOptions(): Promise<LystoreOptions> {
        let lystoreOptions = new LystoreOptions();
        return Promise.all([this.getBCoptions(), this.getExportChoices(), this.getHasOperationsAndInstructions()])
            .then((results: [BcOptions, ExportChoices, boolean]) => {
                lystoreOptions.bcOptions = results[0];
                lystoreOptions.exportChoices = results [1];
                lystoreOptions.hasOperationsAndInstructions = results [2];
                return lystoreOptions;
            });
    },
    saveExportChoices(exportChoices: ExportChoices): Promise<void> {
        return Promise.resolve();
    },
    saveBcForm : async (bcOptions: BcOptions):  Promise<AxiosPromise> =>
         http.put(`/lystore/parameter/bc/options`,bcOptions)
    ,
    saveHasOperationsAndInstructions(hasOperationsAndInstructions: boolean): Promise<void> {
        return Promise.resolve();
    },
}
export const ParameterSettingService = ng.service('ParameterSettingService', (): ParameterSettingService => parameterSettingService);