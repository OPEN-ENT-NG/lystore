// @ts-ignore
import {ng,idiom as lang, template} from 'entcore';
import {Project, Utils} from '../../model';

export const projectController = ng.controller("projectController",
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.project = new Project();
        $scope.project.eventer.on('init:start', () => {
        });
        $scope.project.eventer.on('init:end', () => {
            Utils.safeApply($scope);
        });
        $scope.project.init($scope.campaign.id, $scope.current.structure.id);

        $scope.confirmProject = (project: Project) => {
            project.create($scope.campaign.id,$scope.current.structure.id);
            $scope.display.title = project.title ? project.title.name : undefined;
            $scope.display.grade = project.grade ? project.grade.name : undefined;
            $scope.display.lightbox.createProject = false;
            template.close('basket.project');
            Utils.safeApply($scope);
        };

        $scope.getPlaceHolder = () =>{
            return `${lang.translate("lystore.project.placeholder")} ${$scope.regionTypeName}`;
        } ;
        $scope.project.eventer.on('create:end', () => {
            $scope.takeClientOrder($scope.baskets, $scope.project.id);
        });

        $scope.cancelProjectConfirmation = () => {
            $scope.display.lightbox.createProject = false;
            template.close('basket.project');
            Utils.safeApply($scope);
        }
    }]);