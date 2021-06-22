    import {_, idiom as lang, ng, notify, template, toasts} from 'entcore';
import {
    Notification,
    Operation,
    OrderRegion,
    OrdersRegion,
    Structure,
    StructureGroup,
    StructureGroups,
    Structures,
    Titles,
    Utils,
    Equipments, ContractType, ContractTypes, Contracts, Order, Basket
} from "../../model";
import http from "axios";

declare let window: any;
export const orderRegionController = ng.controller('orderRegionController',
    ['$scope', '$location', '$routeParams', ($scope, $location, $routeParams) => {

        $scope.orderToCreate = new OrderRegion();
        $scope.structure_groups = new StructureGroups();
        $scope.structuresToDisplay = new Structures();
        $scope.titles = new Titles();
        $scope.orderRegion.files = new Array();
        $scope.nbFiles = 0;
        $scope.display = {
            lightbox: {
                validOrder: false,
                addDocuments: false,
                addDocumentsRegion : false,
            },
        };
        $scope.translate = (key: string):string => lang.translate(key);

        $scope.updateCampaign = async ():Promise<void> => {
            $scope.orderToCreate.rows = undefined;
            $scope.orderToCreate.project = undefined;
            $scope.orderToCreate.operation = undefined;
            await $scope.titles.syncAdmin($scope.orderToCreate.campaign.id);
            await $scope.structure_groups.syncByCampaign($scope.orderToCreate.campaign.id);
            let structures = new Structures();
            $scope.structure_groups.all.map(structureGR => {
                structureGR.structures.map(structureId => {
                    let newStructure = new Structure();
                    newStructure.id = structureId;
                    newStructure = $scope.structures.all.find(s => s.id === newStructure.id);
                    if (structures.all.indexOf(newStructure) === -1)
                        structures.push(newStructure);
                })
            });
            $scope.structuresToDisplay = structures;
            $scope.structuresToDisplay.all.sort((firstStructure, secondStructure) => {
                if (firstStructure.uai > secondStructure.uai) return 1;
                if (firstStructure.uai < secondStructure.uai) return -1;
                return 0;
            });
            Utils.safeApply($scope);
        };

        $scope.operationSelected = async (operation: Operation):Promise<void> => {
            $scope.isOperationSelected = true;
            $scope.operation = operation;
            if (!$scope.orderToUpdate.id_operation) {
                let orderRegionCreate = new OrderRegion();
                orderRegionCreate.createFromOrderClient($scope.orderToUpdate);
                orderRegionCreate.id_operation = operation.id;
                orderRegionCreate.files = $scope.orderRegion.files;
                orderRegionCreate.equipment_key = $scope.orderToUpdate.equipment_key;
                orderRegionCreate.technical_spec = $scope.orderToUpdate.equipment.technical_specs;
                orderRegionCreate.id_contract = $scope.orderToUpdate.equipment.id_contract;
                const { status } = await orderRegionCreate.create();
                if (status === 200) {
                    toasts.confirm('lystore.order.region.update');
                    await $scope.ordersClient.addOperationInProgress(operation.id, [$routeParams.idOrder]);
                    // $scope.operationId =  $scope.operation.id
                    $scope.cancelUpdate();
                }
                else {
                    notify.error('lystore.admin.order.update.err');
                }
                Utils.safeApply($scope);

            }
        };

        $scope.isOperationsIsEmpty = false;
        $scope.selectOperationForOrder = async ():Promise<void> => {
            await $scope.initOperation();
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation => operation.status === 'true');
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };

        $scope.cancelUpdate = ():void => {
            if($scope.operationId) {
                $scope.redirectTo(`/operation/order/${$scope.operationId}`)
                $scope.operationId = undefined;
            }
            else if ($scope.fromWaiting)
                $scope.redirectTo('/order/waiting');
            else
                window.history.back();
        };

         $scope.deleteOrderRegionFile = async () => {
            try {
                await http.delete(`/lystore/order/update`);
            } catch (err) {
                throw err;
            }
        }

        $scope.updateOrderConfirm = async ():Promise<void> => {
            await $scope.selectOperationForOrder();
            await $scope.deleteOrderRegionFile();
        };

        $scope.updateLinkedOrderConfirm = async ():Promise<void> => {
            let orderRegion = new OrderRegion();
            orderRegion.createFromOrderClient($scope.orderToUpdate);
            orderRegion.equipment_key = $scope.orderToUpdate.equipment_key;
            orderRegion.id_contract = orderRegion.equipment.id_contract;
            $scope.cancelUpdate();
            if($scope.orderToUpdate.typeOrder === "region"){
                await orderRegion.update($scope.orderToUpdate.id);
            } else {
                await orderRegion.create();
            }
            toasts.confirm('lystore.order.region.update');
        };
        $scope.isValidFormUpdate = ():boolean => {
            return $scope.orderToUpdate &&  $scope.orderToUpdate.equipment_key
                &&  $scope.orderToUpdate.equipment
                && $scope.orderToUpdate.price_single_ttc
                && $scope.orderToUpdate.amount
                && ((($scope.orderToUpdate.rank>0 &&
                    $scope.orderToUpdate.rank > 0  ||
                    $scope.orderToUpdate.rank === null)) ||
                    !$scope.orderToUpdate.campaign.orderPriorityEnable())
        };

        function checkRow(row):boolean {
            return row.equipment && row.price && row.structure && row.amount
        }

        $scope.oneRow = ():boolean => {
            let oneValidRow = false;
            if ($scope.orderToCreate.rows)
                $scope.orderToCreate.rows.map(row => {
                    if (checkRow(row))
                        oneValidRow = true;
                });
            return oneValidRow;
        };

        $scope.validForm = ():boolean => {
            return $scope.orderToCreate.campaign
                && $scope.orderToCreate.project
                && $scope.orderToCreate.operation
                && $scope.oneRow()
                && ($scope.orderToCreate.rows.every( row => (row.rank>0 &&
                    row.rank<11  ||
                    row.rank === null))
                    || !$scope.orderToCreate.campaign.orderPriorityEnable());
        };

        $scope.addRow = ():void => {
            let row = {
                equipment: undefined,
                equipments: new Equipments(),
                allEquipments : [],
                contracts : new Contracts(),
                structure: undefined,
                price: undefined,
                amount: undefined,
                comment: "",
                display: {
                    struct: false
                }
            };
            if (!$scope.orderToCreate.rows)
                $scope.orderToCreate.rows = [];
            $scope.orderToCreate.rows.push(row);
            Utils.safeApply($scope)

        };

        $scope.dropRow = (index:number):void => {
            $scope.orderToCreate.rows.splice(index, 1);
        };

        $scope.duplicateRow = (index:number):void => {
            let row = JSON.parse(JSON.stringify($scope.orderToCreate.rows[index]));
            row.equipments = new Equipments();
            row.contracts = new Contracts();
            if (row.structure){
                if (row.structure.structures) {
                    row.structure = $scope.structure_groups.all.find(struct => row.structure.id === struct.id);
                } else {
                    row.structure = $scope.structures.all.find(struct => row.structure.id === struct.id);
                }
            }
            //duplicate contracttypes
            row.ct_enabled =  $scope.orderToCreate.rows[index].ct_enabled;

            $scope.orderToCreate.rows[index].contracts.all.forEach(ct=>{
                row.contracts.all.push(ct);
            })
            if($scope.orderToCreate.rows[index].contract_type)
                row.contract_type = JSON.parse(JSON.stringify($scope.orderToCreate.rows[index].contract_type));

            $scope.orderToCreate.rows[index].equipments.forEach(equipment => {
                row.equipments.push(equipment);
                if (row.equipment && row.equipment.id === equipment.id)
                {
                    row.equipment = JSON.parse(JSON.stringify(equipment));
                }
            });
            $scope.orderToCreate.rows.splice(index + 1, 0, row)
        };
        $scope.cancelBasketDelete = ():void => {
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
        };

        $scope.switchStructure = async (row:any, structure:Structure):Promise<void> => {
            await row.equipments.syncAll($scope.orderToCreate.campaign.id, (structure) ? structure.id : undefined);
            await row.contracts.sync();
            row.contract_type = undefined;
            row.ct_enabled = undefined;
            let contracts = [];
            row.equipments.all.forEach(e => {
                row.allEquipments.push(e);
                row.contracts.all.map(contract =>{
                    if(contract.id === e.id_contract  && !contract.isPresent ){
                        contract.isPresent = true;
                        contracts.push(contract);
                    }
                })
            });
            row.contracts.all = contracts;
            row.equipment = undefined;
            row.price = undefined;
            row.amount = undefined;
            Utils.safeApply($scope);
        };
        $scope.initEquipmentData = (row:OrderRegion):void => {
            let roundedString = row.equipment.priceTTC.toFixed(2);
            let rounded = Number(roundedString);
            row.price = Number(rounded);
            row.amount = 1;
        };
        $scope.initContractType = async (row) => {
            if (row.contract && ( !row.equipment  || row.equipment.id_contract !== row.contract.id)) {
                row.ct_enabled = true;
                row.equipment = undefined;
                row.price = undefined;
                row.amount = undefined;
                row.equipments.all = row.allEquipments.filter(equipment => row.contract.id === equipment.id_contract);
                Utils.safeApply($scope);

            }
        };

        $scope.swapTypeStruct = (row):void => {
            row.display.struct = !row.display.struct;
            row.equipment = undefined;
            row.price = undefined;
            row.amount = undefined;
            row.comment ="";
            row.structure = undefined;
            Utils.safeApply($scope);
        };


        $scope.createOrder = async ():Promise<void> => {
            let ordersToCreate = new OrdersRegion();
            $scope.orderToCreate.rows.map(row => {
                if (checkRow(row)) {
                    if (row.structure instanceof StructureGroup) {
                        row.structure.structures.map(s => {
                            let orderRegionTemp = new OrderRegion();
                            orderRegionTemp.id_campaign = $scope.orderToCreate.campaign.id;
                            orderRegionTemp.id_structure = s;
                            orderRegionTemp.title_id = $scope.orderToCreate.project;
                            orderRegionTemp.id_operation = $scope.orderToCreate.operation;
                            orderRegionTemp.equipment_key = row.equipment.id;
                            orderRegionTemp.equipment = row.equipment;
                            orderRegionTemp.comment = row.comment;
                            orderRegionTemp.amount = row.amount;
                            orderRegionTemp.price = row.price;
                            orderRegionTemp.name = row.equipment.name;
                            orderRegionTemp.files = $scope.orderRegion.files;
                            orderRegionTemp.technical_spec = row.equipment.technical_specs;
                            orderRegionTemp.id_contract = row.equipment.id_contract;
                            orderRegionTemp.id_type = row.equipment.id_type;
                            if (!row.rank){
                                orderRegionTemp.rank = 0;
                            } else {
                                orderRegionTemp.rank = row.rank;
                            }
                            let struct = $scope.structures.all.find(struct => s.id === struct.id);
                            (struct) ? orderRegionTemp.name_structure = struct.name : orderRegionTemp.name_structure = "";
                            ordersToCreate.all.push(orderRegionTemp);
                        })
                    } else {
                        let orderRegionTemp = new OrderRegion();
                        orderRegionTemp.id_campaign = $scope.orderToCreate.campaign.id;
                        orderRegionTemp.id_structure = row.structure.id;
                        orderRegionTemp.title_id = $scope.orderToCreate.project;
                        orderRegionTemp.equipment = row.equipment;
                        orderRegionTemp.equipment_key = row.equipment.id;
                        orderRegionTemp.id_operation = $scope.orderToCreate.operation;
                        orderRegionTemp.comment = row.comment;
                        orderRegionTemp.amount = row.amount;
                        orderRegionTemp.price = row.price;
                        orderRegionTemp.name = row.equipment.name;
                        orderRegionTemp.files = $scope.orderRegion.files;
                        orderRegionTemp.technical_spec = row.equipment.technical_specs;
                        orderRegionTemp.id_contract = row.equipment.id_contract;
                        orderRegionTemp.name_structure = row.structure.name;
                        orderRegionTemp.id_type = row.equipment.id_type;
                        if (!row.rank){
                            orderRegionTemp.rank = 0;
                        } else {
                            orderRegionTemp.rank = row.rank;
                        }
                        ordersToCreate.all.push(orderRegionTemp);
                    }
                }
            });
            let {status} = await ordersToCreate.create();
            if (status === 201) {
                toasts.confirm('lystore.order.region.create.message');
                $scope.orderToCreate = new OrderRegion();
                $scope.titles = new Titles();
            }
            else {
                notify.error('lystore.admin.order.create.err');
            }
            await $scope.deleteOrderRegionFile();
            Utils.safeApply($scope);
        }

        $scope.openAddDocumentsLightbox = (orderRegion: OrderRegion) => {
            $scope.order = JSON.parse(JSON.stringify(orderRegion));
            $scope.files = [];
            $scope.display.lightbox.addDocuments = true;
            Utils.safeApply($scope);
        }

        $scope.openAddDocumentsRegionLightbox = (orderRegion : OrderRegion) => {
            $scope.order = JSON.parse(JSON.stringify(orderRegion));
            $scope.files = [];
            $scope.display.lightbox.addDocumentsRegion = true;
            template.open('addDocuments.lightbox', 'administrator/orderRegion/order-region-add-files');
            Utils.safeApply($scope);
        }

        $scope.endUpload = () => {
            $scope.orderRegion.files = $scope.orderRegion.files || [];
            for (let i = 0; i < $scope.orderRegion.files.length; i++) {
                $scope.orderRegion.files.push($scope.files[i]);
            }
            $scope.display.lightbox.addDocuments = false;
            $scope.display.lightbox.addDocumentsRegion = false;
            Utils.safeApply($scope);
        };

        $scope.uploadFile = async (file) => {
            file.status = 'loading';
            let formData = new FormData();
            $scope.uploadUri = "/lystore/order/upload/file";
            formData.append("file", file, file.name);
            try {
                const {data} = await http.post($scope.uploadUri, formData, {'headers': {'Content-Type': 'multipart/form-data'}});
                file.id = data.id;
                file.status = 'loaded';
            } catch (err) {
                file.status = 'failed';
            }
            Utils.safeApply($scope);
        };

        $scope.importFiles = (files = $scope.files) => {
            let file: File;
            if (files === undefined || files.length === 0) return;
            Utils.safeApply($scope);

            for (let i = 0; i < files.length; i++) {
                file = files[i];
                $scope.orderRegion.files.push(file);
                $scope.uploadFile(file);
                $scope.nbFiles += 1;
            }
        };

        $scope.deleteOrderDocument = async (orderRegion: OrderRegion, file) => {
            try {
                file.status = 'loading';
                Utils.safeApply($scope);
                await orderRegion.deleteDocument(file);
                orderRegion.files = _.reject(orderRegion.files, (doc) => doc.id === file.id);
                $scope.nbFiles -= 1;
                toasts.confirm('lystore.basket.file.delete.success');
            } catch (err) {
                toasts.warning('lystore.basket.file.delete.error');
                delete file.status;
            } finally {
                Utils.safeApply($scope);
            }
        };
    }
    ]);