import {BcOptions} from "./bc-options.model";
import {ExportChoices} from "./export-choices.model";

export interface ILystoreOptions {
    hasOperationsAndInstructions: boolean;
    bcOptions: BcOptions;
    exportChoices: ExportChoices;
}

export class LystoreOptions implements ILystoreOptions {
    hasOperationsAndInstructions: boolean;
    bcOptions: BcOptions;
    exportChoices: ExportChoices;

    constructor() {
        this.hasOperationsAndInstructions = false;
        this.bcOptions = new BcOptions();
        this.exportChoices = new ExportChoices();
    }
}