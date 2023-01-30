import {ng} from 'entcore'
import {BcOptions} from "../../model/parameter/bc-options.model";
import {ExportChoices} from "../../model/parameter/export-choices.model";
import {LystoreOptions} from "../../model/parameter/lystore-options.model";


export interface ParameterSettingService {
    getOptions(): Promise<LystoreOptions>;

    getBCoptions(): Promise<BcOptions>;

    getExportChoices(): Promise<ExportChoices>;

    getHasOperationsAndInstructions(): Promise<boolean>;

    saveExportChoices(exportChoices: ExportChoices): Promise<void>;

    saveBcForm(bcOptions: BcOptions): Promise<void>

    saveHasOperationsAndInstructions(hasOperationsAndInstructions: boolean): Promise<void>;
}


export const parameterSettingService: ParameterSettingService = {
    getBCoptions(): Promise<BcOptions> {
        let bcOptions: BcOptions = new BcOptions();
        return Promise.resolve(bcOptions);
    },
    getExportChoices(): Promise<ExportChoices> {
        return Promise.resolve(new ExportChoices());
    },
    getHasOperationsAndInstructions(): Promise<boolean> {
        return Promise.resolve(true);
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
    saveBcForm(bcOptions: BcOptions): Promise<void> {
        return Promise.resolve();
    },
    saveHasOperationsAndInstructions(hasOperationsAndInstructions: boolean): Promise<void> {
        return Promise.resolve();
    }

}
export const ParameterSettingService = ng.service('ParameterSettingService', (): ParameterSettingService => parameterSettingService);