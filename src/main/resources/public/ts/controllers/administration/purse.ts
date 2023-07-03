import {ng, template, idiom as lang} from 'entcore';
import {
    PurseImporter,
    Utils,
    Purse,
    Purses,
    Structures,
    Campaign,
    TitleImporter,
    Structure,
    Campaigns
} from '../../model';
import {Mix} from 'entcore-toolkit';
import {IScope} from "angular";
import {PurseService, TitleService} from "../../services/app/admin";

declare let window: any;

interface IViewModel extends ng.IController {
    lightbox: {
        open: boolean
    };
    sort: {
        purse: {
            type: string,
            reverse: boolean
        }
    };
    lang: typeof lang;
    purse: Purse
    campaign: Campaign;
    isNegativePurse: boolean;
    allHolderSelected: boolean;
    isChecked: boolean;
    importer: PurseImporter;

    openEditPurseForm(purse: Purse): void;

    cancelPurseForm(): void;

    validPurse(purse: Purse): Promise<void>;

    openPurseImporter(importer: TitleImporter): void;

    importPurses(importer: PurseImporter): Promise<void>;

    exportPurses(id: number): void;

    checkPurses(id_Campaign: number): void;
}

interface IMainScope extends IScope {
    vm: IViewModel;
}


class Controller implements IViewModel {
    purse: Purse;
    campaign: Campaign;
    isNegativePurse: boolean;
    allHolderSelected: boolean;
    importer: PurseImporter;
    isChecked: boolean;
    lang: typeof lang;

    sort: {
        purse: {
            type: string,
            reverse: boolean
        }
    }

    lightbox = {
        open: false
    };

    constructor(private $scope: IMainScope, private $routeParams,
                private purseService: PurseService
                /*  inject service etc..just as we do in controller */) {
        this.$scope.vm = this;
        this.campaign = new Campaign()

        this.lang = lang;
        this.lightbox = {
            open: false
        };
        this.sort = {
            purse: {
                type: 'name',
                reverse: false
            }
        };
    }


    async $onInit() {
        await this.campaign.sync(parseInt(this.$routeParams.idCampaign))
        this.campaign.purses = new Purses(parseInt(this.$routeParams.idCampaign));
        await this.campaign.purses.sync().then(() => {
            this.campaign.purses.all.forEach(purse => {
                if (isNaN(purse.total_order)) {
                    purse.total_order = 0;
                }
            })
        });
        Utils.safeApply(this.$scope)
    }

    openEditPurseForm = (purse: Purse) => {
        this.purse = purse.copy();
        this.purse.initial_amount = parseFloat(this.purse.initial_amount)
        template.open('purse.lightbox', 'administrator/campaign/purse/edit-purse-form');
        this.lightbox.open = true;
        Utils.safeApply(this.$scope);
    };

    cancelPurseForm = () => {
        this.lightbox.open = false;
        delete this.purse;
    };


    validPurse = async (purse: Purse) => {
        let status = await purse.save();
        if (status === 202) {
            this.isNegativePurse = true;
        } else {
            this.lightbox.open = false;
            await this.campaign.purses.sync();
            delete this.purse;
        }
        this.allHolderSelected = false;
        Utils.safeApply(this.$scope);
    };

    openPurseImporter = (): void => {
        this.importer = new PurseImporter(this.campaign.id);
        template.open('purse.lightbox', 'administrator/campaign/purse/import-purses-form');
        this.lightbox.open = true;
        Utils.safeApply(this.$scope);
    };

    importPurses = async (importer: PurseImporter): Promise<void> => {
        try {
            await importer.validate();
        } catch (err) {
            importer.message = err.message;
        } finally {
            if (!importer.message) {
                await this.campaign.purses.sync();
                this.lightbox.open = false;
                delete this.importer;
            } else {
                importer.files = [];
            }
            Utils.safeApply(this.$scope);
        }
    };

    exportPurses = (id: number) => {
        window.location = `/lystore/campaign/${id}/purses/export`;
    };
    checkPurses = async (id_Campaign: number) => {
        this.isChecked = true;
        await this.campaign.purses.check(id_Campaign);
        Utils.safeApply(this.$scope)
    }
}

export const purseController = ng.controller('PurseController', ['$scope', '$routeParams', 'PurseService', Controller]);
