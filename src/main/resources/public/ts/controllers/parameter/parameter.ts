import {appPrefix, ng, template} from "entcore";
import {ParameterService} from  "../../services"
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

        $scope.filter = {
            property: 'uai',
            desc: false,
            value: ''
        };

        const GROUP_Lystore_NAME = "RESP-AFFECT-Lystore";
        $scope.structureLystoreLists = [];
        parameterService.getStructuresLystore().then(structures => {
            $scope.structureLystoreLists = structures;
            $scope.structureLystoreLists.map((structure) => structure.number_deployed = structure.deployed ? 1 : 0);
            $scope.$apply();
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
        $scope.$apply();



        function getDeployedCounter(): void {
            let counter = 0;
            $scope.structureLystoreLists.map(({deployed}) => counter += deployed);
            $scope.counter.value = counter;
        }

        $scope.$watch(() => $scope.structureLystoreLists, getDeployedCounter);

        $scope.createLystoreGroup = async ({structureId, deployed}) => {
            let response;
            $scope.createButton = true;
            $scope.$apply();
            if (!deployed) {
                response = await parameterService.createGroupLystoreToStructure(GROUP_Lystore_NAME, structureId);
            } else {
                response = await parameterService.undeployStructure(structureId);
            }
            if (response.status === 200) {
                $scope.structureLystoreLists = await parameterService.getStructuresLystore();
                $scope.structureLystoreLists.map((structure) => structure.number_deployed = structure.deployed ? 1 : 0);
            }
            $scope.createButton = false;
            $scope.$apply();
        };

        $scope.showRespAffecLystoreGroup = function ({structureId, id}) {
            window.open(`/admin/${structureId}/groups/manual/${id}/details`);
        };

    }]);
