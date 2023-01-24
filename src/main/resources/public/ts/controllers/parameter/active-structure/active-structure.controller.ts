import {ng, toasts} from "entcore";
import {ParameterService, StructureLystore} from "../../../services"
import {Utils} from "../../../model";
import {IScope} from "angular";

declare const window: any;

/**
 Parameter controller
 ------------------.
 **/

interface IViewModel {
    match(): void;

    counter: {
        value: number
    }
    loadingArray: boolean;
    filter: {
        property: string,
        desc: boolean,
        value: string
    };
    createButton: boolean;
    addButton: boolean;
    structureLystoreLists :Array<StructureLystore>;

    createLystoreGroup({structureId, deployed}): void;

    showRespAffecLystoreGroup({structureId, id}): void;

}

interface IActiveStructureScope extends IScope {
    vm: IViewModel;
}

class Controller implements ng.IController, IViewModel {

    counter: {
        value: number
    }
    loadingArray: boolean;
    filter: {
        property: string,
        desc: boolean,
        value: string
    };
    createButton: boolean;
    addButton: boolean;
    structureLystoreLists :Array<StructureLystore>;
    private GROUP_Lystore_NAME : string;

    constructor(private $scope: IActiveStructureScope,
                private parameterService: ParameterService) {
        this.$scope.vm = this;
        this.GROUP_Lystore_NAME = "Lystore";
        this.loadingArray = true;
        this.counter = {
            value: 0
        };
        this.structureLystoreLists = [];
        this.filter = {
            property: 'uai',
            desc: false,
            value: ''
        };
        this.createButton = false;
        this.addButton = false;
    }

    $onInit() {
        this.parameterService.getStructuresLystore().then(structures => {
            this.structureLystoreLists = structures;
            this.loadingArray = false;
            Utils.safeApply(this.$scope)
        });

        this.$scope.$watch(() => this.structureLystoreLists, this.getDeployedCounter);
    }

    match = function () {

        const viewModel: IViewModel = this;
        return function (item) {
            if (viewModel.filter.value.trim() === '') return true;
            return item.name.toLowerCase().includes(viewModel.filter.value.toLowerCase())
                || item.uai.toLowerCase().includes(viewModel.filter.value.toLowerCase());
        }
    };
     getDeployedCounter(): void {
         if (this && this.counter) {
             this.counter.value = this.structureLystoreLists.filter(({deployed}) => deployed).length;
         }
    }

    createLystoreGroup = async ({structureId, deployed}) => {
        let response;
        this.createButton = true;
        Utils.safeApply(this.$scope)
        if (!deployed) {
            response = await this.parameterService.createGroupLystoreToStructure(this.GROUP_Lystore_NAME, structureId);
            toasts.info("lystore.deploy.structure.valid")
        } else {
            response = await this.parameterService.undeployStructure(structureId);
            toasts.info("lystore.undeploy.structure.valid")
        }
        if (response.status === 200) {
            this.loadingArray = true;
            Utils.safeApply(this.$scope);
            this.structureLystoreLists = await this.parameterService.getStructuresLystore();
            this.loadingArray = false;
            Utils.safeApply(this.$scope);
        } else {
            toasts.warning("lystore.parameter.update.error")
        }
        this.createButton = false;
        Utils.safeApply(this.$scope);

    };

    showRespAffecLystoreGroup({structureId, id}): void {
        window.open(`/admin/${structureId}/groups/manual/${id}/details`);
    }
}

export const activeStructureController = ng.controller("ActiveStructureController",  ["$scope", "ParameterService", Controller]);

