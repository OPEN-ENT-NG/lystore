import {_, notify, $, Behaviours, idiom as lang, model, moment, ng, template, toasts} from 'entcore';
import {
    Agents,
    Basket,
    Baskets,
    Campaign,
    Campaigns,
    Contracts,
    ContractTypes,
    Equipment,
    Equipments,
    EquipmentTypes,
    Exercises,
    Exports,
    Instructions,
    labels,
    Logs,
    Notification,
    Operations, Order,
    OrderClient,
    OrderRegion,
    OrdersClient, OrderUtils,
    PRIORITY_FIELD,
    Programs, Projects, RejectOrder, RejectOrders,
    StructureGroups,
    Structures,
    Supplier,
    Suppliers,
    Tags,
    Taxes,
    Titles, Userbook,
    Utils,
} from '../model';
import {Mix} from "entcore-toolkit";

declare const window: any;
export const mainController = ng.controller('MainController', ['$scope', 'route', '$location', '$window', '$rootScope',
    ($scope, route, $location, $window, $rootScope) => {
        template.open('main', 'main');

        $scope.display = {
            equipment: false,
            lightbox: {lightBoxIsOpen: false,}
        };
        $scope.structures = new Structures();
        $scope.current = {};
        $scope.notifications = [];
        $scope.lang = lang;
        $scope.agents = new Agents();
        $scope.suppliers = new Suppliers();
        $scope.contractTypes = new ContractTypes();
        $scope.programs = new Programs();
        $scope.contracts = new Contracts();
        $scope.tags = new Tags();
        $scope.equipments = new Equipments();
        $scope.campaigns = new Campaigns();
        $scope.campaign = new Campaign();
        $scope.structureGroups = new StructureGroups();
        $scope.taxes = new Taxes();
        $scope.operations = new Operations();
        $scope.logs = new Logs();
        $scope.baskets = new Baskets();
        $scope.ordersClient = new OrdersClient();
        $scope.orderClient = new OrderClient();
        $scope.orderRegion = new OrderRegion();
        $scope.displayedOrders = new OrdersClient();
        $scope.equipmentTypes = new EquipmentTypes();
        $scope.instructions = new Instructions();
        $scope.exercises = new Exercises();
        $scope.exports = new Exports([]);
        $scope.ub = new Userbook();
        $scope.projects = new Projects();
        $scope.titles = new Titles();
        $scope.rejectedOrders = new RejectOrders();
        $scope.labelOperation = new labels();
        $scope.equipments.eventer.on('loading::true', () => Utils.safeApply($scope));
        $scope.equipments.eventer.on('loading::false', () => Utils.safeApply($scope));
        $scope.loadingArray = false;
        $scope.campaignSelection = [];
        $scope.isSuperAdmin = JSON.parse(window.isSuperAdmin);
        $scope.regionTypeName = lang.translate(window.regionTypeName);
        route({
            main: async () => {
                if ($scope.isManager() || $scope.isAdministrator()) {
                    $scope.redirectTo('/campaigns');
                } else {
                    await $scope.initStructures();
                    await $scope.initCampaign($scope.current.structure);
                    template.open('main-profile', 'customer/campaign/campaign-list');
                }
                Utils.safeApply($scope);
            },
            manageAgents: async () => {
                template.open('administrator-main', 'administrator/agent/manage-agents');
                await $scope.agents.sync();
                Utils.safeApply($scope);
            },
            manageSuppliers: async () => {
                template.open('administrator-main', 'administrator/supplier/manage-suppliers');
                await $scope.suppliers.sync();
                Utils.safeApply($scope);
            },
            manageContracts: async () => {
                template.open('administrator-main', 'administrator/contract/manage-contract');
                await $scope.contracts.sync();
                $scope.agents.sync();
                $scope.suppliers.sync();
                $scope.contractTypes.sync();
                Utils.safeApply($scope);
            },
            manageEquipmentTags: async () => {
                template.open('administrator-main', 'administrator/tag/manage-tags');
                await $scope.tags.sync(true);
                Utils.safeApply($scope);
            },
            manageEquipments: async () => {
                delete $scope.equipment;

                template.open('administrator-main', 'administrator/equipment/equipment-container');
                template.open('equipments-main', 'administrator/equipment/manage-equipments');
                await $scope.equipments.sync();
                await $scope.contracts.sync();
                await $scope.equipmentTypes.sync();
                $scope.taxes.sync();
                $scope.tags.sync();
                Utils.safeApply($scope);
            },
            createEquipment: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/equipments');
                }
                template.open('equipments-main', 'administrator/equipment/equipment-form');
            },
            viewLogs: async () => {
                $scope.loadingArray = true;
                $scope.logs.reset();
                template.open('administrator-main', 'administrator/log/view-logs');
                await $scope.logs.loadPage($scope.current.page);
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            manageCampaigns: async () => {
                template.open('administrator-main', 'administrator/campaign/campaign_container');
                template.open('campaigns-main', 'administrator/campaign/manage-campaign');
                await $scope.campaigns.sync();
                Utils.safeApply($scope);
            },
            createCampaigns: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            updateCampaigns: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/campaign_form');
                Utils.safeApply($scope);
            },
            managePurse: async (params) => {
                const campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                if (template.isEmpty('administrator-main') || campaign === undefined || !campaign.purse_enabled) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/purse/manage-purse');
                Utils.safeApply($scope);
            },
            manageTitles: async (params) => {
                const campaign = $scope.campaigns.get(parseInt(params.idCampaign));
                if (template.isEmpty('administrator-main') || campaign === undefined) {
                    $scope.redirectTo('/campaigns');
                }
                template.open('campaigns-main', 'administrator/campaign/title/manage-title');
                Utils.safeApply($scope);
            },
            manageStructureGroups: async () => {
                template.open('administrator-main', 'administrator/structureGroup/structureGroup-container');
                await $scope.structureGroups.sync();
                template.open('structureGroups-main', 'administrator/structureGroup/manage-structureGroup');
                $scope.structures = new Structures();
                await $scope.structures.sync();
                Utils.safeApply($scope);
            },
            createStructureGroup: async () => {
                if (template.isEmpty('administrator-main')) {
                    $scope.redirectTo('/structureGroups');
                }
                template.open('structureGroups-main', 'administrator/structureGroup/structureGroup-form');
                Utils.safeApply($scope);
            },
            campaignCatalog: async (params) => {
                if ($scope.taxes !== undefined || $scope.taxes !== 0) {
                    $scope.taxes.sync();
                }
                let idCampaign = params.idCampaign;
                $scope.fromCatalog = true
                $scope.idIsInteger(idCampaign);
                if (!$scope.current.structure)
                    await $scope.initStructures();
                if (!$scope.campaign.id) {
                    await $scope.campaigns.sync($scope.current.structure.id);
                    $scope.campaigns.all.forEach(campaign => {
                        if (campaign.id == idCampaign) {
                            $scope.campaign = campaign;
                        }
                    });
                }
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/catalog/catalog-list');
                template.close('right-side');
                $scope.display.equipment = false;
                Utils.safeApply($scope);
                $scope.current.structure ? await $scope.equipments.sync(idCampaign, $scope.current.structure.id) : null;
            },
            equipmentDetail: async (params) => {
                let idCampaign = params.idCampaign;
                let idEquipment = params.idEquipment;
                $scope.idIsInteger(idCampaign);
                $scope.idIsInteger(idEquipment);
                $scope.current.structure
                    ? await $scope.initBasketItem(parseInt(idEquipment), parseInt(idCampaign), $scope.current.structure.id)
                    : null;
                if (!$scope.fromCatalog) {
                    $scope.redirectTo(`/campaign/${idCampaign}/catalog`);
                }
                template.open('right-side', 'customer/campaign/catalog/equipment-detail');
                window.scrollTo(0, 0);
                Utils.safeApply($scope);
            },
            campaignOrder: async (params) => {
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if (!$scope.current.structure)
                    await $scope.initStructures();
                $scope.current.structure
                    ? await $scope.ordersClient.sync(null, [], [], [], [], [], [], [], idCampaign, $scope.current.structure.id)
                    : null;
                $scope.syncReject(idCampaign);
                if (!$scope.campaign.id) {
                    await $scope.campaigns.sync($scope.current.structure.id);
                    $scope.campaigns.all.forEach(campaign => {
                        if (campaign.id == idCampaign) {
                            $scope.campaign = campaign;
                        }
                    });
                }
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/order/manage-order');
                $scope.initCampaignOrderView();
                Utils.safeApply($scope);
            },
            campaignBasket: async (params) => {
                template.open('main-profile', 'customer/campaign/campaign-detail');
                template.open('campaign-main', 'customer/campaign/basket/manage-basket');
                let idCampaign = params.idCampaign;
                $scope.idIsInteger(idCampaign);
                if (!$scope.current.structure)
                    await $scope.initStructures();
                if ($scope.current.structure) {
                    await $scope.baskets.sync(idCampaign, $scope.current.structure.id);
                }
                if (!$scope.campaign.id) {
                    await $scope.campaigns.sync($scope.current.structure.id);
                    $scope.campaigns.all.forEach(campaign => {
                        if (campaign.id == idCampaign) {
                            $scope.campaign = campaign;
                        }
                    });
                }

                Utils.safeApply($scope);
            },
            orderWaiting: async () => {
                $scope.loadingArray = true;
                Utils.safeApply($scope);
                await $scope.syncCampaignInputSelected();
                await $scope.structureGroups.sync();
                $scope.preferences = await $scope.ub.getPreferences();
                if ($scope.preferences && $scope.preferences.preference) {
                    if ($scope.fromWaiting)
                        $scope.fromWaiting = false;
                    let preferences = JSON.parse($scope.preferences.preference);
                    if (preferences.ordersWaitingCampaign && preferences.ordersWaitingCampaign.length) {
                        let campaignPref;
                        $scope.campaignsForSelectInput.forEach(c => {
                            preferences.ordersWaitingCampaign.forEach(pref => {
                                if (c.id === pref
                                    && !$scope.campaignSelection.find(campaign => campaign.id === c.id)) {
                                    $scope.campaignSelection.push(c);
                                    campaignPref = c;
                                }
                            })
                        });
                        if ($scope.campaignSelection.length && $scope.campaignSelection.length !== 0) {
                            template.open('administrator-main');
                            template.open('selectCampaign', 'administrator/order/select-campaign');
                            await $scope.initOrders('WAITING', $scope.campaignSelection);
                            $scope.selectCampaignShow(campaignPref);
                        } else {
                            await $scope.openLightSelectCampaign();
                        }
                    } else
                        await $scope.openLightSelectCampaign();
                } else
                    await $scope.openLightSelectCampaign();
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            orderSent: async () => {
                template.open('administrator-main', 'administrator/order/order-sent');
                $scope.structures = new Structures();
                await $scope.initOrders('SENT', $scope.campaignSelection);
                $scope.orderToSend = null;
                Utils.safeApply($scope);
            },
            orderClientValided: () => {
                $scope.initOrders('VALID', $scope.campaignSelection);
                template.open('administrator-main', 'administrator/order/order-valided');
                Utils.safeApply($scope);
            },
            updateOrder: async (params: any): Promise<void> => {
                template.open('administrator-main', 'administrator/order/order-update-form');
                let idOrder = parseInt(params.idOrder);
                $scope.fromWaiting = true;
                await $scope.initOrderStructures();
                $scope.orderToUpdate = await $scope.orderClient.getOneOrderClient(idOrder, $scope.structures.all);
                $scope.filesMetadataTemp = Object.assign($scope.orderToUpdate.files);

                await $scope.equipments.syncAll($scope.orderToUpdate.campaign.id);
                $scope.loadingArray = false;

                $scope.orderToUpdate.equipment = $scope.equipments.all.find(findElement => findElement.id === $scope.orderToUpdate.equipment_key);
                $scope.orderParent = OrderUtils.initParentOrder($scope.orderToUpdate);
                Utils.safeApply($scope);

            },
            updateLinkedOrder: async (params: any): Promise<void> => {
                template.open('administrator-main', 'administrator/order/order-update-form');
                let idOrder = parseInt(params.idOrder);
                await $scope.initOrderStructures();
                $scope.orderToUpdate = params.typeOrder === 'client'
                    ? await $scope.orderClient.getOneOrderClient(idOrder, $scope.structures.all)
                    : await $scope.orderRegion.getOneOrderRegion(idOrder, $scope.structures.all);
                if (params.typeOrder === 'client') {
                    $scope.filesMetadataTemp = Object.assign($scope.orderToUpdate.files);
                } else {
                    $scope.filesMetadata = await $scope.orderRegion.getFilesMetadata(idOrder);
                    $scope.filesMetadataTemp = Object.assign($scope.filesMetadata);
                }
                await $scope.equipments.syncAll($scope.orderToUpdate.campaign.id);
                $scope.orderToUpdate.equipment = $scope.equipments.all.find(findElement => findElement.id === $scope.orderToUpdate.equipment_key);
                if (params.typeOrder === 'client') {
                    $scope.orderParent = OrderUtils.initParentOrder($scope.orderToUpdate);
                } else {
                    if ($scope.orderToUpdate.order_parent) {
                        $scope.orderParent = new Order(JSON.parse($scope.orderToUpdate.order_parent), $scope.structures.all);
                        $scope.orderParent.equipment = $scope.equipments.all.find(findElement => findElement.id === $scope.orderParent.equipment_key);
                        $scope.orderParent = OrderUtils.initParentOrder($scope.orderParent);
                    } else {
                        $scope.orderParent = undefined;
                    }
                }
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            instruction: async () => {
                await $scope.initInstructions();
                template.open('administrator-main', 'administrator/instruction/instruction-container');
                template.open('instruction-main', 'administrator/instruction/manage-instruction');
                Utils.safeApply($scope);
            },
            operation: async () => {
                $scope.loadingArray = true;
                await $scope.initOperation();
                template.open('administrator-main', 'administrator/operation/operation-container');
                template.open('operation-main', 'administrator/operation/manage-operation');
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },

            operationOrders: async (params) => {
                $scope.loadingArray = true;
                template.close('administrator-main');
                template.close('operation-main');
                if ($scope.operations.all.length < 1) {
                    $scope.operations = new Operations();
                    await $scope.operations.sync();
                }
                $scope.structures = new Structures();
                await $scope.structures.sync();
                $scope.operation = await $scope.operations.all.find(operationFound => operationFound.id.toString() === params.idOperation.toString());
                $scope.ordersClientByOperation = await $scope.operation.getOrders($scope.structures.all);
                template.open('administrator-main', 'administrator/operation/operation-container');
                template.open('operation-main', 'administrator/operation/operation-orders-list');
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            createRegionOrder: async () => {
                $scope.loadingArray = true;
                await $scope.campaigns.sync();
                await $scope.operations.sync(true);
                let operations = [];
                $scope.operations.all.map((operation, index) => {
                    if (operation.status == 'true' && !operation.instruction) {
                        operations.push(operation);
                    }
                });

                await $scope.contractTypes.sync();
                $scope.operations.all = operations;
                await $scope.structures.sync();
                template.open('administrator-main', 'administrator/orderRegion/order-region-create-form');
                $scope.loadingArray = false;
                Utils.safeApply($scope);
            },
            exportList: async () => {
                $scope.loadingArray = true;
                await $scope.exports.getExports();
                template.open('administrator-main', 'administrator/exports/export-list');
                $scope.loadingArray = false;
                Utils.safeApply($scope);

            },
            manageLabel: async () => {
                await $scope.labelOperation.sync();
                template.open('administrator-main', 'administrator/operation-label/manage-label-operation');
                Utils.safeApply($scope);
            },
        });
        $scope.initInstructions = async () => {
            $scope.loadingArray = true;
            await $scope.instructions.sync();
            $scope.loadingArray = false;
        };
        $scope.initCampaignOrderView = () => {
            if ($scope.campaign.priority_enabled == true && $scope.campaign.priority_field == PRIORITY_FIELD.ORDER) {
                template.open('order-list', 'customer/campaign/order/orders-by-equipment');
            } else {
                template.open('order-list', 'customer/campaign/order/orders-by-project');
            }
        };

        $scope.initOperation = async (onlylist?: boolean) => {
            $scope.labelOperation = new labels();
            await $scope.labelOperation.sync();
            await $scope.operations.sync(onlylist);
        };

        $scope.initLabel = async () => {
            await $scope.labelOperation.sync();
        };

        $scope.initBasketItem = async (idEquipment: number, idCampaign: number, structure) => {
            $scope.equipment = _.findWhere($scope.equipments.all, {id: idEquipment});
            if ($scope.equipment === undefined && !isNaN(idEquipment)) {
                $scope.equipment = new Equipment();
                await $scope.equipment.sync(idEquipment);
            }
            $scope.basket = new Basket($scope.equipment, idCampaign, structure);
        };
        $scope.idIsInteger = (id) => {
            try {
                id = parseInt(id);
                if (isNaN(id)) {
                    $scope.redirectTo(`/`);
                    Utils.safeApply($scope);
                }
            } catch (e) {
                $scope.redirectTo(`/`);
                Utils.safeApply($scope);
            }
        };

        $scope.hasAccess = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.access);
        };

        $scope.isManager = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.manager);
        };

        $scope.isAdministrator = () => {
            return model.me.hasWorkflow(Behaviours.applicationsBehaviours.lystore.rights.workflow.administrator);
        };

        $scope.redirectTo = (path: string) => {
            $location.path(path);
        };

        $scope.redirectToHref = (path: string) => {
            $window.location.href = window.location.origin + window.location.pathname + path
        };

        $rootScope.$on('eventEmitedCampaign', function (event, data) {
            $scope.campaign = data;
        });

        $scope.formatDate = (date: string | Date, format: string) => {
            return moment(date).format(format);
        };

        $scope.calculatePriceTTC = (price, tax_value, roundNumber?: number) => {
            let priceFloat = parseFloat(price);
            let taxFloat = parseFloat(tax_value);
            let price_TTC = ((priceFloat + ((priceFloat * taxFloat) / 100)));
            return (!isNaN(price_TTC)) ? (roundNumber ? price_TTC.toFixed(roundNumber) : price_TTC) : '';
        };

        $scope.calculatePriceOfEquipmentHT =
            (equipment: Equipment, selectedOptions: boolean, roundNumber: number = 2) => {

                return (!isNaN(equipment.calculatePriceHT(selectedOptions))) ?
                    (roundNumber ?
                        equipment.calculatePriceHT(selectedOptions).toFixed(roundNumber)
                        : equipment.calculatePriceHT(selectedOptions))
                    : equipment.calculatePriceHT(selectedOptions);
            };

        $scope.calculatePriceOfOptionHT =
            (option: any, roundNumber: number = 2) => {
                let price = parseFloat(option.price);
                return (!isNaN(price)) ? (roundNumber ? price.toFixed(roundNumber) : price) : price;
            }

        /**
         * Calculate the price of an equipment
         * @param {Equipment} equipment
         * @param {boolean} selectedOptions [Consider selected options or not)
         * @param {number} roundNumber [number of digits after the decimal point]
         * @returns {string | number}
         */
        $scope.calculatePriceOfEquipment = (equipment: Equipment, selectedOptions: boolean, roundNumber: number = 2) => {
            return (!isNaN(equipment.calculatePriceTTC(selectedOptions)))
                ? (roundNumber
                    ? equipment.calculatePriceTTC(selectedOptions).toFixed(roundNumber)
                    : equipment.calculatePriceTTC(selectedOptions))
                : equipment.calculatePriceTTC(selectedOptions);
        };

        $scope.calculatePriceOfOrderClient = (orderClient: OrderClient, selectedOptions: boolean, roundNumber: number = 2) => {
            return (!isNaN(orderClient.calculatePriceTTC(selectedOptions)))
                ? (roundNumber
                    ? orderClient.calculatePriceTTC(selectedOptions).toFixed(roundNumber)
                    : orderClient.calculatePriceTTC(selectedOptions))
                : orderClient.calculatePriceTTC(selectedOptions);
        };

        $scope.initStructures = async () => {
            await $scope.structures.syncUserStructures();
            $scope.current.structure = $scope.structures.all[0];
        };

        $scope.avoidDecimals = (event) => {
            return event.charCode >= 48 && event.charCode <= 57;
        };

        $scope.notifyBasket = (action: String, basket: Basket) => {
            let messageForOne = basket.amount + ' ' + lang.translate('article') + ' "'
                + basket.equipment.name + '" ' + lang.translate('lystore.basket.' + action + '.article');
            let messageForMany = basket.amount + ' ' + lang.translate('articles') + ' "'
                + basket.equipment.name + '" ' + lang.translate('lystore.basket.' + action + '.articles');
            toasts.confirm(basket.amount === 1 ? messageForOne : messageForMany);
        };

        $scope.initCampaign = async (structure) => {
            if (structure) {
                await $scope.campaigns.sync(structure.id);
                Utils.safeApply($scope);
            }
        };

        $scope.syncOrders = async (status: string) => {
            await $scope.ordersClient.sync(status, $scope.structures.all, $scope.contracts, $scope.contractTypes,
                $scope.suppliers, $scope.campaigns, $scope.projects, $scope.titles);
            $scope.displayedOrders.all = $scope.ordersClient.all;
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        $scope.initOrders = async (status, campaignSelection?: any) => {
            if ($scope.suppliers.all === undefined || $scope.suppliers.all.length === 0)
                await $scope.suppliers.sync();
            if (status === 'WAITING' || status === 'SENT') {
                if (($scope.projects.status && $scope.projects.status !== status)
                    || ($scope.projects.all === undefined || $scope.projects.all.length === 0))
                    await $scope.projects.sync(status);
                if ($scope.contracts.all === undefined || $scope.contracts.all.length === 0)
                    await $scope.contracts.sync();
                if ($scope.campaigns.all === undefined || $scope.campaigns.all.length === 0)
                    await $scope.campaigns.sync();
                if ($scope.contractTypes.all === undefined || $scope.contractTypes.all.length === 0)
                    await $scope.contractTypes.sync();
                if ($scope.titles.all === undefined || $scope.titles.all.length === 0)
                    await $scope.titles.sync();
            }
            await $scope.initOrderStructures();
            if (campaignSelection && status === "WAITING") {
                await $scope.ordersClient.syncWaiting($scope.structures.all, $scope.contracts,
                    $scope.contractTypes, $scope.suppliers, $scope.campaigns, $scope.projects, $scope.titles, campaignSelection);
            } else {
                await $scope.syncOrders(status);
            }
            Utils.safeApply($scope);
        };

        $scope.initOrderStructures = async () => {
            $scope.loadingArray = true;
            $scope.structures = new Structures();
            await $scope.structures.sync();
            await $scope.structures.getStructureType();
            // $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        $scope.initOrdersForPreview = async (orders: OrderClient[]) => {
            $scope.orderToSend = new OrdersClient(Mix.castAs(Supplier, orders[0].supplier));
            $scope.orderToSend.all = Mix.castArrayAs(Order, orders);
            $scope.orderToSend.preview = await $scope.orderToSend.getPreviewData();
            $scope.orderToSend.preview.index = 0;
        };
        $scope.syncCampaignInputSelected = async (): Promise<void> => {
            $scope.campaignsForSelectInput = [];
            await $scope.campaigns.sync();
            $scope.campaignsForSelectInput = [...$scope.campaigns.all];

        };
        $scope.openLightSelectCampaign = async (): Promise<void> => {
            template.open('administrator-main');
            template.open('selectCampaign', 'administrator/order/select-campaign');
            $scope.display.lightbox.lightBoxIsOpen = true;
            Utils.safeApply($scope);
        };
        $scope.selectCampaignWhenNoPref = async (campaign: Campaign) => {
            $scope.campaignSelection.push(campaign)
            await $scope.getOrderWaitingFiltered(campaign);
        }
        $scope.selectCampaignShow = (campaign?: Campaign, campaignSelect?: Campaign[]): void => {
            if (campaign && $scope.campaignSelection.length === 0 && campaign.id) {
                $scope.campaignSelection.push(campaign);
            }
            if (campaignSelect) {
                $scope.campaignSelection = campaignSelect;
            }
            let idsCampaign = [];
            $scope.campaignSelection.forEach(c => {
                idsCampaign.push(c.id)
            })
            $scope.ub.putPreferences("ordersWaitingCampaign", idsCampaign);
            $scope.display.lightbox.lightBoxIsOpen = false;
            template.close('selectCampaign');
            $scope.campaign = $scope.allCampaignsSelect;
            $scope.cancelSelectCampaign(true);

        };
        $scope.getOrderWaitingFiltered = async (campaign: Campaign): Promise<void> => {
            await $scope.initOrders('WAITING', $scope.campaignSelection);
            $scope.selectCampaignShow(campaign);
        };
        $scope.cancelSelectCampaign = (initOrder: boolean): void => {
            if (initOrder) {
                $scope.displayedOrders.all = $scope.ordersClient.all;
            }
            $scope.loadingArray = false;
            template.open('administrator-main', 'administrator/order/order-waiting');
            Utils.safeApply($scope);
        };

        if ($scope.isManager() || $scope.isAdministrator()) {
            template.open('main-profile', 'administrator/management-main');
        } else if ($scope.hasAccess() && !$scope.isManager() && !$scope.isAdministrator()) {
            // template.open('main-profile', 'customer/campaign/campaign-list');
        }
        Utils.safeApply($scope);

        $scope.formatDate = (date) => {
            if (date)
                return moment(date).format("DD/MM/YYYY");
            else {
                return lang.translate("no.date");
            }
        }

        $scope.syncReject = async (idCampaign) => {
            await $scope.rejectedOrders.sync(idCampaign);
            $scope.ordersClient.all.map(order => {
                order.rejectOrder = new RejectOrder()
                order.rejectOrder = $scope.rejectedOrders.all.find(reject => reject.id_order === order.id)
            });
            Utils.safeApply($scope);
        };
    }]);
