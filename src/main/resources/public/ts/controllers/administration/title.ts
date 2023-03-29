import {ng, template, toasts} from 'entcore';
import {Structure, Structures, Title, TitleImporter, Titles, Utils} from '../../model';
import {TitleService} from "../../services";

declare let window: any;

export const titleController = ng.controller('TitleController',
    ['$scope', '$routeParams', 'TitleService', ($scope, $routeParams, titleService: TitleService) => {
        $scope.campaign = $scope.campaigns.get(parseInt($routeParams.idCampaign));


        $scope.syncStructuresTitle = async () => {
            await titleService.syncStructuresTitle($scope.campaign.id).then((result: Structures) => {
                $scope.structures = result;
                $scope.$apply();
            });
        }

        $scope.syncStructuresTitle();

        $scope.checkIfTitleSelected = (): boolean => {
            return $scope.structures.all.find((structure: Structure) =>
                structure.titles.selected.length !== 0
            ) !== undefined;
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
            // try {
            let structures: Structures = new Structures();
            structures.all = $scope.getStructureWithSelectedTitle()
            await titleService.delete($scope.campaign.id, structures);
            await $scope.syncStructuresTitle();
            $scope.lightbox.open = false;
            toasts.confirm('lystore.campaign.titles.delete.success');
            Utils.safeApply($scope);
            // } catch (err) {
            //     toasts.warning('lystore.campaign.titles.delete.error');
            // }
        };

        $scope.openDeleteConfirmation = ():void => {
            template.open('title.lightbox', 'administrator/campaign/title/title-deletion-confirmation');
            $scope.lightbox.open = true;
            Utils.safeApply($scope);
        };

        $scope.getStructureWithSelectedTitle = ():Structure[] =>{

            return $scope.structures.all.filter( (structure :Structure) =>
                structure.titles.selected.length !== 0
            );
        }

        $scope.deselectAllTitles = ():void =>{
            $scope.getStructureWithSelectedTitle()
                .flatMap((structure: Structure) => structure.titles.all)
                .forEach((title: Title) => title.selected = false);
            Utils.safeApply($scope);
        }

        $scope.selectAllTitles = ():void =>{
            $scope.structures.all
                .flatMap((structure: Structure) => structure.titles.all)
                .forEach((title: Title) => title.selected = true);
            Utils.safeApply($scope);

        }


    }]);