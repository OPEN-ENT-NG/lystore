import {ng, template, toasts} from 'entcore';
import {Campaign, Structure, Structures, Title, TitleImporter, Utils} from '../../model';
import {TitleService} from "../../services";
import {IScope} from "angular";

interface IViewModel extends ng.IController {
    structures: Structures;
    lightbox: {
        open: boolean
    };
    sort: {
        title: {
            type: string,
            reverse: boolean
        };
    };

    campaign: Campaign;
    importer: TitleImporter;

    syncStructuresTitle(idCampaign: number): Promise<void>;

    checkIfTitleSelected(): boolean;

    openTitleImporter(): void;

    importTitles(importer: TitleImporter): Promise<void>;

    deleteTitles(): void;

    openDeleteConfirmation(): void;

    getStructureWithSelectedTitle(): Array<Structure>;

    deselectAllTitles(): void;

    selectAllTitles(): void;
}

interface IMainScope extends IScope {
    vm: IViewModel;
}


class Controller implements IViewModel {
    lightbox: {
        open: boolean
    };

    sort: {
        title: {
            type: string,
            reverse: boolean
        }
    };

    structures: Structures;
    campaign: Campaign;
    importer: TitleImporter;

    constructor(private $scope: IMainScope, private $routeParams,
                private titleService: TitleService
                /*  inject service etc..just as we do in controller */) {
        this.$scope.vm = this;
        this.structures = new Structures();
        this.campaign = new Campaign();
        this.lightbox = {
            open: false
        };
        this.sort = {
            title: {
                type: 'name',
                reverse: false
            }
        };
    }
   async $onInit() {
        await this.campaign.sync(parseInt(this.$routeParams.idCampaign))
        await this.syncStructuresTitle(this.$routeParams.idCampaign);
        Utils.safeApply(this.$scope)
    }

    $onDestroy() {
    }

    async syncStructuresTitle(idCampaign: number): Promise<void> {
        await this.titleService.syncStructuresTitle(idCampaign).then((result: Structures) => {
            this.structures = result;
            this.$scope.$apply();
        });
    }

    checkIfTitleSelected(): boolean {
        return this.structures.all.find((structure: Structure) =>
            structure.titles.selected.length !== 0
        ) !== undefined;
    }

    openTitleImporter(): void {
        this.importer = new TitleImporter(this.campaign.id);
        template.open('title.lightbox', 'administrator/campaign/title/import-titles-form');
        this.lightbox.open = true;
        Utils.safeApply(this.$scope);
    };

    async importTitles(importer: TitleImporter): Promise<void> {
        try {
            await importer.validate();
        } catch (err) {
            importer.message = err.message;
        } finally {
            if (!importer.message) {
                await this.syncStructuresTitle(this.campaign.id);
                this.lightbox.open = false;
                delete this.importer;
            } else {
                importer.files = [];
            }
            Utils.safeApply(this.$scope);
        }
    };

    async deleteTitles() {
        try {
            let structures: Structures = new Structures();
            structures.all = this.getStructureWithSelectedTitle()
            await this.titleService.delete(this.campaign.id, structures);
            await this.syncStructuresTitle(this.campaign.id);
            this.lightbox.open = false;
            toasts.confirm('lystore.campaign.titles.delete.success');
            Utils.safeApply(this.$scope);
        } catch (err) {
            toasts.warning('lystore.campaign.titles.delete.error');
        }
    };

    openDeleteConfirmation(): void {
        template.open('title.lightbox', 'administrator/campaign/title/title-deletion-confirmation');
        this.lightbox.open = true;
        Utils.safeApply(this.$scope);
    };

    getStructureWithSelectedTitle(): Array<Structure> {

        return this.structures.all.filter((structure: Structure) =>
            structure.titles.selected.length !== 0
        );
    }

    deselectAllTitles(): void {
        (<any>this.getStructureWithSelectedTitle())
            .flatMap((structure: Structure) => structure.titles.all)
            .forEach((title: Title) => title.selected = false);
        Utils.safeApply(this.$scope);
    }

    selectAllTitles(): void {
        (<any>this.structures.all)
            .flatMap((structure: Structure) => structure.titles.all)
            .forEach((title: Title) => title.selected = true);
        Utils.safeApply(this.$scope);

    }
}


export const titleController = ng.controller('TitleController', ['$scope', '$routeParams', 'TitleService', Controller]);

// export const titleController = ng.controller('TitleController',
//     ['$scope', '$routeParams', 'TitleService', ($scope, $routeParams, titleService: TitleService) => {


