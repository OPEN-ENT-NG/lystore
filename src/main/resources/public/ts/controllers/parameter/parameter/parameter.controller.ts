import {ng, template} from "entcore";
import {IScope, IWindowService} from "angular";
import {ParameterSettingService} from "../../../services";
import {LystoreOptions} from "../../../services";
import {Utils} from "../../../model";

interface IViewModel extends ng.IController {
    lystoreOptions: LystoreOptions
    saveForm():void;
    cancelForm():void
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
                Utils.safeApply(this.$scope)
            })

    }

    $onDestroy() {

    }

    lystoreOptions: LystoreOptions;

    cancelForm(): void {
        console.log("cancel")
    }

    saveForm(): void {
        console.log(" save")
    }

}

export const parameterController = ng.controller('ParameterController', ['$scope', 'ParameterSettingService',Controller]);