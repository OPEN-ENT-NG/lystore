import {ng, template, toasts} from 'entcore';
import {Structure, Structures, Title, TitleImporter, Titles, Utils} from '../../model';
import {titleService} from "../../services";

declare let window: any;

export const titleController = ng.controller('TitleController',
    ['$scope', '$routeParams', ($scope, $routeParams) => {
        $scope.campaign = $scope.campaigns.get(parseInt($routeParams.idCampaign));
        titleService.syncStructuresTitle($scope.campaign.id).then( (result:Structures) =>{
            $scope.structures = result;
            $scope.$apply();
        } );

        $scope.checkIfTitleSelected = () : boolean =>{
           return $scope.structures.all.find(structure => {
                return structure.titles.selected.length !== 0;
            }) !== undefined;
        }
        $scope.lightbox = {
            open: false
        };

        $scope.sort = {
            title: {
                type: 'name',
                reverse: false
            }
        };

        $scope.openTitleImporter = (): void => {
            $scope.importer = new TitleImporter($scope.campaign.id);
            template.open('title.lightbox', 'administrator/campaign/title/import-titles-form');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

        $scope.importTitles = async (importer: TitleImporter): Promise<void> => {
            try {
                await importer.validate();
            } catch (err) {
                importer.message = err.message;
            } finally {
                if (!importer.message) {
                    await $scope.campaign.titles.sync($scope.campaign.id);
                    $scope.lightbox.open = false;
                    delete $scope.importer;
                } else {
                    importer.files = [];
                }
                Utils.safeApply($scope);
            }
        };

        $scope.deleteTitles = async () => {
            try {
                let titles:Titles = new Titles();
                titles.all = $scope.getStructureWithSelectedTitle().flatMap((structure: Structure) => structure.titles.selected)
                titleService.delete($scope.campaign.id, titles);
                await $scope.campaign.titles.sync($scope.campaign.id);
                $scope.lightbox.open = false;
                toasts.confirm('lystore.campaign.titles.delete.success');
                Utils.safeApply($scope);
            } catch (err) {
                toasts.warning('lystore.campaign.titles.delete.error');
            }
        };

        $scope.openDeleteConfirmation = ():void => {
            template.open('title.lightbox', 'administrator/campaign/title/title-deletion-confirmation');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

        $scope.getStructureWithSelectedTitle = ():Structure[] =>{

            return $scope.structures.all.filter(structure => {
                return structure.titles.selected.length !== 0;
            })
        }

        $scope.deselectAllTitles = ():void =>{
            $scope.getStructureWithSelectedTitle()
                .flatMap((structure: Structure) => structure.titles)
                .forEach((title: Title) => title.selected = false)
            Utils.safeApply($scope)
        }

        $scope.selectAllTitles = ():void =>{
            $scope.getStructureWithSelectedTitle()
                .flatMap((structure: Structure) => structure.titles)
                .forEach((title: Title) => title.selected = true)
            Utils.safeApply($scope)

        }


    }]);