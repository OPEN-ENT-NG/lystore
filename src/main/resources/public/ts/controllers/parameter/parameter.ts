import {appPrefix, ng, template, toasts} from "entcore";
import {ParameterService} from  "../../services"
import {Utils} from "../../model";
declare const window: any;

/**
 Parameter controller
 ------------------.
 **/
export const parameterController = ng.controller("ParameterController", [
    "$scope", "ParameterService", async ($scope, parameterService: ParameterService) => {
        template.open("main", "parameter/parameter");
        $scope.counter = {
            value: 0
        };
        $scope.loadingArray = true;

        $scope.filter = {
            property: 'uai',
            desc: false,
            value: ''
        };

        const GROUP_Lystore_NAME = "Lystore";
        $scope.structureLystoreLists = [];
        parameterService.getStructuresLystore().then(structures => {
            $scope.structureLystoreLists = structures;
            $scope.structureLystoreLists.map((structure) => structure.number_deployed = structure.deployed ? 1 : 0);
            $scope.loadingArray = false;
            Utils.safeApply($scope)
        });

        $scope.match = function () {
            return function (item) {
                if ($scope.filter.value.trim() === '') return true;
                return item.name.toLowerCase().includes($scope.filter.value.toLowerCase())
                    || item.uai.toLowerCase().includes($scope.filter.value.toLowerCase());
            }
        };

        /* button handler */
        $scope.createButton = false;
        $scope.addButton = false;
        Utils.safeApply($scope)



        function getDeployedCounter(): void {
            let counter = 0;
            $scope.structureLystoreLists.map(({deployed}) => counter += deployed);
            $scope.counter.value = counter;
        }

        $scope.$watch(() => $scope.structureLystoreLists, getDeployedCounter);

        $scope.createLystoreGroup = async ({structureId, deployed}) => {
            let response;
            $scope.createButton = true;
            Utils.safeApply($scope)
            if (!deployed) {
                response = await parameterService.createGroupLystoreToStructure(GROUP_Lystore_NAME, structureId);
                toasts.info("lystore.deploy.structure.valid")
            } else {
                response = await parameterService.undeployStructure(structureId);
                toasts.info("lystore.undeploy.structure.valid")
            }
            if (response.status === 200) {
                $scope.loadingArray = true;
                Utils.safeApply($scope);
                $scope.structureLystoreLists = await parameterService.getStructuresLystore();
                $scope.structureLystoreLists.map((structure) => structure.number_deployed = structure.deployed ? 1 : 0);
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            }else{
                toasts.warning("lystore.parameter.update.error")
            }
            $scope.createButton = false;
            Utils.safeApply($scope)
        };

        $scope.showRespAffecLystoreGroup = function ({structureId, id}) {
            window.open(`/admin/${structureId}/groups/manual/${id}/details`);
        };

    }]);
