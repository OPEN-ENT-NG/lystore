import {ng, toasts} from "entcore";
import {IScope} from "angular";
import {ParameterSettingService} from "../../../services";
import {Utils} from "../../../model";
import {BcOptions} from "../../../model/parameter/bc-options.model";
import {LystoreOptions} from "../../../model/parameter/lystore-options.model";

interface IViewModel extends ng.IController {
    lystoreOptions: LystoreOptions;
    oldBcForm: BcOptions;

    saveForm(): void;

    cancelForm(): void;

    updateExportChoices(): void;

    saveValidCommand(): void;
}

interface IMainScope extends IScope {
    vm: IViewModel;
}

class Controller implements IViewModel {
    constructor(private $scope: IMainScope,
                private parameterSettingService: ParameterSettingService

                /*  inject service etc..just as we do in controller */) {
        this.$scope.vm = this;
    }


    $onInit() {
        this.parameterSettingService.getOptions().then(options => {
            this.lystoreOptions = options;
            this.oldBcForm = options.bcOptions.copy(options.bcOptions);
            Utils.safeApply(this.$scope)
        })

    }

    $onDestroy() {

    }

    lystoreOptions: LystoreOptions;
    oldBcForm: BcOptions;

    cancelForm(): void {
        this.lystoreOptions.bcOptions = this.oldBcForm;
    }

    saveForm(): void {
        this.parameterSettingService.saveBcForm(this.lystoreOptions.bcOptions).then(
            () => toasts.confirm("lystore.parameter.save")
        );
    }

    updateExportChoices(): void {
        this.parameterSettingService.saveExportChoices(this.lystoreOptions.exportChoices).then(
            () => toasts.confirm("lystore.parameter.save")
        );
    }

    saveValidCommand(): void {
        this.parameterSettingService.saveHasOperationsAndInstructions(this.lystoreOptions.hasOperationsAndInstructions).then(
            () => toasts.confirm("lystore.parameter.save")
        );
    }

}

export const parameterController = ng.controller('ParameterController', ['$scope', 'ParameterSettingService', Controller]);