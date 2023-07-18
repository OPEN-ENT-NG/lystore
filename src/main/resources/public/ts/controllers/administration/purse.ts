import {ng, template, idiom as lang, workspace, notify, toasts} from 'entcore';
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
import service = workspace.v2.service;
import {AxiosError, AxiosResponse} from "axios";
import any = jasmine.any;

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
    switchAll(purses: Purses):void;
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
                private purseService: PurseService) {
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
        this.campaign.purses  = await this.purseService.sync(this.campaign.id)
        Utils.safeApply(this.$scope)
    }

    openEditPurseForm = (purse: Purse) : void => {
        this.purse = purse.copy();
        this.purse.initial_amount = parseFloat(this.purse.initial_amount.toFixed(2))
        template.open('purse.lightbox', 'administrator/campaign/purse/edit-purse-form');
        this.lightbox.open = true;
        Utils.safeApply(this.$scope);
    };

    cancelPurseForm = (): void => {
        this.lightbox.open = false;
        this.isNegativePurse = false;
    };


    validPurse = async (): Promise<void>  => {
        await this.purseService.save(this.purse).then( async (res :AxiosResponse) =>{
            if(res.status === 202){
                this.isNegativePurse = true;
            }  else {
            this.lightbox.open = false;
             this.campaign.purses = await this.purseService.sync(this.campaign.id);
        }
        }).catch((e:AxiosResponse)=>{
            console.log(e)
            notify.error('lystore.purse.update.err');
        })
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
            await this.purseService.validateImport(importer)
                .then(async () =>{
                    this.campaign.purses = await this.purseService.sync(this.campaign.id);
                    this.lightbox.open = false;
                    importer.files = [];
                    Utils.safeApply(this.$scope);
                    toasts.confirm("lystore.purse.import.confirm")
                }).catch((e:AxiosError  )=>{
                    importer.message = (e.response.data as {error: string}).error;
                    Utils.safeApply(this.$scope);
            });

    };

    exportPurses = (id: number) :void => {
        window.location = `/lystore/campaign/${id}/purses/export`;
    };
    checkPurses = async (id_Campaign: number): Promise<void>   => {
        this.isChecked = true;
        await this.purseService.check(id_Campaign,this.campaign.purses);
        Utils.safeApply(this.$scope)
    }

    switchAll = (purses: Purses): void => {
        if (purses.selected.length === purses.all.length) {
            purses.all.forEach((purse: Purse) => {
                purse.selected = false;
            });
        } else {
            purses.all.forEach((purse: Purse) => {
                purse.selected = true;
            });
        }
    }
}

export const purseController = ng.controller('PurseController', ['$scope', '$routeParams', 'PurseService', Controller]);
