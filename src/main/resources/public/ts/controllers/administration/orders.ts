import {_, $,idiom as lang,angular, model, ng, template, toasts,moment} from 'entcore';
import http from "axios";
import {
    Campaign, Notification, Operation, OrderClient, OrdersClient, orderWaiting, PRIORITY_FIELD, Userbook, Order,
    Utils, RejectOrders
} from '../../model';
import {Mix} from 'entcore-toolkit';
import {parameterSettingService} from "../../services";


declare let window: any;
export const orderController = ng.controller('orderController',
    ['$scope', '$location', ($scope, $location,) => {
        ($scope.ordersClient.selected[0]) ? $scope.orderToUpdate = $scope.ordersClient.selected[0] : $scope.orderToUpdate = new OrderClient();
        $scope.allOrdersSelected = false;
        $scope.tableFields = orderWaiting;
        $scope.campaignSelectionMulti = [];
        let isPageOrderWaiting = $location.path() === "/order/waiting";
        let isPageOrderSent = $location.path() === "/order/sent";
        // Don't forget to modify the number in attribute "bottom-scroll" in html page "order-waiting.html" if you modify this number
        const NBDISPLAYEDORDERS = 25;

        parameterSettingService.getBCoptions().then(bcOptions =>{
            $scope.bcOptions = bcOptions;
            Utils.safeApply($scope);
        })
        $scope.rejectedOrders = new RejectOrders();
        if(isPageOrderSent)
            $scope.displayedOrdersSent = $scope.displayedOrders;
        $scope.sort = {
            order : {
                type: 'name_structure',
                reverse: false
            }
        };
        $scope.search = {
            filterWord : '',
            filterWords : []
        };
        $scope.filterDisplayedOrders = async () => {
            let searchResult = [];
            let regex;
            const matchStructureGroups = (structureGroups: string[]): boolean => {
                let bool: boolean = false;
                if (typeof structureGroups === 'string') structureGroups = Utils.parsePostgreSQLJson(structureGroups);
                structureGroups.map((groupName) => bool = bool || regex.test(groupName.toLowerCase()));
                return bool;
            };
            if($scope.search.filterWords.length > 0){
                if(isPageOrderWaiting)await $scope.selectCampaignShow($scope.campaign);
                $scope.search.filterWords.map((searchTerm: string, index: number): void => {
                    let searchItems: OrderClient[] = index === 0 ? $scope.displayedOrders.all : searchResult;
                    regex = generateRegexp([searchTerm]);

                    searchResult = _.filter(searchItems, (order: OrderClient) => {
                        return ('name_structure' in order ? regex.test(order.name_structure.toLowerCase()) : false)
                            || ('structure' in order && order.structure['name'] ? regex.test(order.structure.name.toLowerCase()): false)
                            || ('structure' in order && order.structure['city'] ? regex.test(order.structure.city.toLocaleLowerCase()) : false)
                            || ('structure' in order && order.structure['academy'] ? regex.test(order.structure.academy.toLowerCase()) : false)
                            || ('structure' in order && order.structure['type'] ? regex.test(order.structure.type.toLowerCase()) : false)
                            || ('project' in order ? regex.test(order.project.title['name'].toLowerCase()) : false)
                            || ('contract_type' in order ? regex.test(order.contract_type.name.toLowerCase()) : false)
                            || regex.test('contract' in (order as OrderClient)
                                ? order.contract.name.toLowerCase()
                                : order.contract_name)
                            || regex.test('supplier' in (order as OrderClient)
                                ? order.supplier.name.toLowerCase()
                                : order.supplier_name)
                            || ('campaign' in order ? regex.test(order.campaign.name.toLowerCase()) : false)
                            || ('name' in order ? regex.test(order.name.toLowerCase()) : false)
                            || matchStructureGroups(order.structure_groups)
                            || (order.number_validation !== null
                                ? regex.test(order.number_validation.toLowerCase())
                                : false)
                            || (order.order_number !== null && 'order_number' in order
                                ? regex.test(order.order_number.toLowerCase())
                                : false)
                            || (order.label_program !== null && 'label_program' in order
                                ? regex.test(order.label_program.toLowerCase())
                                : false)
                            || ('supplier_name' in order ?  regex.test(order.supplier_name.toLowerCase()) : false );
                    });
                });
                $scope.displayedOrders.all = searchResult;
            } else {
                if(isPageOrderWaiting)
                    $scope.selectCampaignShow($scope.campaign)
                else {
                    $scope.displayedOrders.all = $scope.ordersClient.all ;
                }

            }
        };

        $scope.display = {
            ordersClientOptionOption : [],
            lightbox : {
                deleteOrder : false,
                sendOrder : false,
                validOrder : false,
                rejectOrder : false,
            },
            generation: {
                type: 'ORDER'
            }
        };

        $scope.switchAll = (model: boolean, collection) => {
            model ? collection.selectAll() : collection.deselectAll();
            $scope.setScrollDisplay();
            Utils.safeApply($scope);
        };
        $scope.calculateTotal = (orderClient: OrderClient, roundNumber: number) => {
            let totalPrice = $scope.calculatePriceOfEquipment(orderClient, false, roundNumber) * orderClient.amount;
            return totalPrice.toFixed(roundNumber);
        };

        $scope.savePreference = () =>{
            let elements = document.getElementsByClassName('vertical-array-scroll');
            if(elements[0])
                elements[0].scrollLeft = $(".vertical-array-scroll").scrollLeft() ;
            Utils.safeApply($scope);
            $scope.ub.putPreferences("ordersWaitingDisplay", $scope.jsonPref($scope.tableFields));
        };

        $scope.jsonPref = (prefs) =>{
            let json = {};
            prefs.forEach(pref =>{
                json[pref.fieldName]= pref.display;
            });
            return json;
        };

        async function reloadAfterFilter() {
            $scope.loadingArray = true;
            Utils.safeApply($scope);
            if (isPageOrderWaiting) {
                await $scope.syncOrders('WAITING', $scope.campaignSelection);
                $scope.displayedOrders.all = $scope.displayedOrders.all.filter(order => order.id_campaign === $scope.campaign.id || $scope.campaign.id === -1);
            } else if (isPageOrderSent)
                await $scope.syncOrders('SENT');
            else {
                await $scope.syncOrders('VALID');
            }
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        }

        $scope.addOrderFilter = async (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                if (!_.contains($scope.ordersClient.filters, event.target.value)) {
                    $scope.ordersClient.filters = [...$scope.ordersClient.filters, event.target.value];
                }
                event.target.value = '';
                await reloadAfterFilter();

            }else if (event.handleObj.type === 'click'){
                if (!_.contains($scope.ordersClient.filters, $scope.search.filterWord)) {
                    $scope.ordersClient.filters = [...$scope.ordersClient.filters, $scope.search.filterWord];
                }
                $scope.search.filterWord='';
                await reloadAfterFilter();
            }
            $scope.setScrollDisplay();
        };

        $scope.dropOrderFilter = async (filter: string) => {
            $scope.loadingArray = true;
            $scope.setScrollDisplay();
            Utils.safeApply($scope);
            $scope.ordersClient.filters = $scope.ordersClient.filters.filter( filterWord => filterWord !== filter);
            await reloadAfterFilter();
            $scope.loadingArray = false;

            Utils.safeApply($scope);
        };
        $scope.dropCampaign = async (campaign) =>{
            $scope.campaignSelection = $scope.campaignSelection.filter( c => c !== campaign);
            if($scope.campaignSelection.length === 0){
                await $scope.selectCampaignAndInitFilter();
                $scope.displayedOrders.all = [];
                Utils.safeApply($scope);
            }
            else {
                await $scope.syncOrders('WAITING', $scope.campaignSelection);
                await  $scope.selectCampaignAndInitFilter();
            }
            $scope.setScrollDisplay();
        }

        $scope.addFilter = (filterWord: string, event?) => {
            if (event && (event.which === 13 || event.keyCode === 13 )) {
                $scope.addFilterWords(filterWord);
                $scope.filterDisplayedOrders();
            }
            $scope.setScrollDisplay();
        };

        $scope.switchAllOrders = () => {
            $scope.displayedOrders.all.map((order) => order.selected = $scope.allOrdersSelected);
        };

        $scope.getSelectedOrders = () => $scope.ordersClient.selected;

        $scope.getStructureGroupsList = (structureGroups : any): string => {
            try{
                return structureGroups.join(', ');
            }catch (e) {
                let result = "";
                result = structureGroups.replaceAll("\"","").replace("[","").replace("]","")
                return result
            }
        };

        $scope.addFilterWords = (filterWord) => {
            if (filterWord !== '') {
                $scope.search.filterWords = _.union($scope.search.filterWords, [filterWord]);
                $scope.search.filterWord = '';
                Utils.safeApply($scope);
            }
            $scope.setScrollDisplay();
        };

        function generateRegexp (words: string[]): RegExp {
            function escapeRegExp(str: string) {
                return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, '\\$&');
            }
            let reg;
            if (words.length > 0) {
                reg = '.*(';
                words.map((word: string) => reg += `${escapeRegExp(word.toLowerCase())}|`);
                reg = reg.slice(0, -1);
                reg += ').*';
            } else {
                reg = '.*';
            }
            return new RegExp(reg);
        }



        $scope.pullFilterWord = (filterWord) => {
            $scope.search.filterWords = _.without( $scope.search.filterWords , filterWord);
            $scope.filterDisplayedOrders();
            $scope.setScrollDisplay();
        };
        $scope.validateOrders = async (orders: OrderClient[]) => {
            let ordersToValidat  = new OrdersClient();
            ordersToValidat.all = Mix.castArrayAs(OrderClient, orders);
            let { status, data } = await ordersToValidat.updateStatus('VALID');
            if (status === 200) {
                $scope.orderValidationData = {
                    agents: _.uniq(data.agent),
                    number_validation: data.number_validation,
                    structures: _.uniq(_.pluck(ordersToValidat.all, 'name_structure'))
                } ;
                template.open('validOrder.lightbox', 'administrator/order/order-valid-confirmation');
                $scope.display.lightbox.validOrder = true;
            }
            Utils.safeApply($scope);
        };
        $scope.validateNotifications = async (orders: OrderClient[]) => {
            let ordersToNotif = new OrdersClient();
            ordersToNotif.all = Mix.castArrayAs(OrderClient, orders);
            ordersToNotif.bc_number = orders[0].order_number;
            let {status, data} = await ordersToNotif.notificationEtabl();
            if (status === 200) {
                $scope.orderNotificationData = {
                    agents: _.uniq(data.agent),
                    number_validation: data.number_validation,
                } ;
                toasts.info("lystore.order.notification.mail.info")

            }else{
                toasts.warning("lystore.order.notification.mail.error")
            }
            unselectOrders();
        }

        $scope.validateNotificationsRegion = async (orders:OrderClient[]) =>{
            let ordersToNotif = new OrdersClient();
            ordersToNotif.all = Mix.castArrayAs(OrderClient, orders);
            ordersToNotif.bc_number = orders[0].order_number;
            let {status, data} = await ordersToNotif.notificationRegion();
            if (status === 200) {
                $scope.orderNotificationData = {
                    agents: _.uniq(data.agent),
                    number_validation: data.number_validation,
                } ;
                toasts.info("lystore.order.notification.mail.info")

            }else{
                toasts.warning("lystore.order.notification.mail.error")
            }
            unselectOrders();
        }

        $scope.checkOrderNumber = () => {
            let ordersSelected = $scope.getSelectedOrders();
            if (ordersSelected.find(order => order.order_number)) {
                return true;
            } else {
                return false
            }
        }
        $scope.cancelBasketDelete = () => {
            $scope.displayedOrders.all =  $scope.displayedOrders.all.filter(order => !order.selected)
            $scope.ordersClient.all =  $scope.ordersClient.all.filter(order => !order.selected)
            // $scope.getOrderWaitingFiltered($scope.campaign);
            $scope.display.lightbox.validOrder = false;
            template.close('validOrder.lightbox');
            if($scope.operationId) {
                $scope.ordersClient.selected.map(orderSelected =>{
                    $scope.displayedOrders.all = $scope.displayedOrders.all.filter(order => order.id !== orderSelected.id)
                    $scope.ordersClient.all = $scope.ordersClient.all.filter(order => order.id !== orderSelected.id)
                    orderSelected.selected = false ;
                })
                $scope.operationId = undefined;
            }
            Utils.safeApply($scope);
        };


        $scope.closedLighbtox= () =>{
            $scope.display.lightbox.validOrder = false;
            if($scope.operationId) {
                $scope.redirectTo(`/operation/order/${$scope.operationId}`)
                $scope.operationId = undefined;
            }
            Utils.safeApply($scope);

        };
        $scope.syncOrders = async (status: string,campaignsSelected?) =>{
            $scope.displayedOrders.all = [];
            $scope.loadingArray = true;
            Utils.safeApply($scope);
            if(!campaignsSelected)
                await $scope.ordersClient.sync(status, $scope.structures.all,$scope.contracts,
                    $scope.contractTypes, $scope.suppliers,$scope.campaigns, $scope.projects , $scope.titles);
            else{
                await $scope.ordersClient.syncWaiting( $scope.structures.all,$scope.contracts,
                    $scope.contractTypes, $scope.suppliers,$scope.campaigns, $scope.projects , $scope.titles,campaignsSelected);
            }
            $scope.displayedOrders.all = $scope.ordersClient.all;
            $scope.displayedOrders.all.map(order => {
                    order.selected = false;
                }
            );
            $scope.loadingArray = false;
            Utils.safeApply($scope);
        };

        $scope.windUpOrders = async (orders: Order[]) => {
            let ordersToWindUp  = new OrdersClient();
            // console.log($scope.displayedOrders.all);
            ordersToWindUp.all = Mix.castArrayAs(OrderClient, orders);
            orders.forEach(order =>{
                console.log(order.override_region);
            })
            let { status } = await ordersToWindUp.updateStatus('DONE');
            if (status === 200) {
                toasts.confirm('lystore.windUp.notif');
            }
            await $scope.syncOrders('SENT');
            while ($scope.displayedOrders.selected.length > 0){
                Utils.safeApply($scope);
            }
            $scope.allOrdersSelected = false;
            Utils.safeApply($scope);

        };
        $scope.isNotValidated = ( orders:OrderClient[]) =>{

            let order  = orders.find(order => order.status === "SENT" || order.status === "DONE")
            return order != undefined
        };

        $scope.validateSentOrders = (orders: OrderClient[]) => {
            if (_.where(orders, { status : 'SENT' }).length > 0) {
                let orderNumber = orders[0].order_number;
                return _.every(orders, (order) => order.order_number === orderNumber);
            } else {
                let id_suppliers = (_.uniq(_.pluck(orders, 'id_contract')));
                return (id_suppliers.length === 1);
            }
        };

        $scope.disableCancelValidation = (orders: OrderClient[]) => {
            return _.where(orders, { status : 'SENT' }).length > 0
                || orders.find(order => order.has_operation)  !== undefined ;
        };

        $scope.prepareSendOrder = async (orders: OrderClient[]) => {
            if ($scope.validateSentOrders(orders)) {
                try {
                    await $scope.programs.sync();
                    await $scope.initOrdersForPreview(orders);
                } catch (e) {
                    console.error(e);
                    toasts.warning('lystore.order.pdf.preview.error');
                } finally {
                    if ($scope.orderToSend.hasOwnProperty('preview')) {
                        template.open('administrator-main', 'administrator/order/order-send-prepare');
                    }
                    Utils.safeApply($scope);
                }
            }
        };
        $scope.validatePrepareSentOrders = (orderToSend: OrdersClient) => {
            return orderToSend && orderToSend.supplier && orderToSend.bc_number && orderToSend.engagement_number
                && orderToSend.bc_number !== undefined && orderToSend.engagement_number !== undefined
                && orderToSend.bc_number.trim() !== '' && orderToSend.engagement_number.trim() !== ''
                && orderToSend.id_program !== undefined;
        };
        $scope.sendOrders = async (orders: OrdersClient) => {
            let { status, data } = await orders.updateStatus('SENT');
            if (status === 201) {
                await $scope.initOrders('VALID', $scope.campaignSelection);
                template.open('administrator-main', 'administrator/order/order-valided');
                toasts.confirm( 'lystore.sent.order');
                toasts.info( 'lystore.sent.export.BC');
            }
            $scope.display.lightbox.sendOrder = false;
            Utils.safeApply($scope);
        };

        $scope.saveByteArray = (reportName, data) => {
            let blob = new Blob([data]);
            let link = document.createElement('a');
            link.href = window.URL.createObjectURL(blob);
            link.download =  reportName + '.pdf';
            document.body.appendChild(link);
            link.click();
            setTimeout(function() {
                document.body.removeChild(link);
                window.URL.revokeObjectURL(link.href);
            }, 100);
        };
        $scope.exportCSV = async() => {
            let params = Utils.formatKeyToParameter($scope.ordersClient.selected, 'id');
            window.location = `/lystore/orders/export?${params}`;
        };

        $scope.getUsername = () => model.me.username;

        $scope.concatOrders = () => {
            let arr = [];
            $scope.orderToSend.preview.certificates.map((certificate) => {
                arr = [...arr, ...certificate.orders];
            });
            return arr;
        };
        $scope.exportCSV = async() => {
            let params = Utils.formatKeyToParameter($scope.ordersClient.selected, 'id');
            window.location = `/lystore/orders/export?${params}`;
        };

        $scope.isValidOrdersWaitingSelection = () => {
            const orders: OrderClient[] = $scope.getSelectedOrders();
            if (orders.length > 1) {
                let isValid: boolean = true;
                let contractId = orders[0].id_contract;
                for (let i = 1; i < orders.length; i++) {
                    isValid = isValid && (contractId === orders[i].id_contract);
                }
                return isValid;
            } else {
                return true;
            }
        };
        let unselectOrders = () =>{
            $scope.displayedOrders.selected.map(order => {
                order.selected = false;
            });
            Utils.safeApply($scope);
        }
        $scope.exportOrder = async (orders: OrderClient[]) => {

            if ((_.where(orders, { status : 'SENT' }).length === orders.length || (_.where(orders, { status : 'DONE' }).length === orders.length ) && $scope.validateSentOrders(orders))) {
                let orderNumber = _.uniq(_.pluck(orders, 'order_number'));
                let  {status, data} =  await http.get(`/lystore/order?bc_number=${orderNumber}`);
                if(status === 201){
                    toasts.info('lystore.sent.export.BC');
                }
            } else {
                $scope.exportValidOrders(orders, 'order');
            }
            $scope.displayedOrders.selected.map(order => {
                order.selected = false;
            });
            Utils.safeApply($scope);
        };

        $scope.exportOrderStruct = async (orders: OrderClient[]) => {

            if ((_.where(orders, { status : 'SENT' }).length === orders.length || (_.where(orders, { status : 'DONE' }).length === orders.length ) && $scope.validateSentOrders(orders))) {
                let orderNumber = _.uniq(_.pluck(orders, 'order_number'));
                let  {status, data} =  await http.get(`/lystore/order/struct?bc_number=${orderNumber}`);
                if(status === 201){
                    toasts.info('lystore.sent.export.BC');
                }
            } else {
                let filter = "";
                orders.forEach(order => {
                    filter +="number_validation=" +  order.number_validation + "&";
                });
                filter = filter.substring(filter.length-1,0);
                let  {status, data} = await http.get(`/lystore/order/struct?${filter}`);
                if(status === 201){
                    toasts.info('lystore.sent.export.BC');
                }
            }
            $scope.displayedOrders.selected.map(order => {
                order.selected = false;
            });
            Utils.safeApply($scope);
        };
        $scope.exportValidOrders = async  (orders: OrderClient[], fileType: string) => {
            let params = '';
            orders.map((order: OrderClient) => {
                params += `number_validation=${order.number_validation}&`;
            });
            params = params.slice(0, -1);
            if(fileType ==='structure_list'){
                toasts.info('lystore.sent.export.BC');
                await orders[0].exportListLycee(params);
                $scope.displayedOrders.selected[0].selected = false;
                Utils.safeApply($scope);
            }else
            if(fileType === 'certificates'){
                window.location = `/lystore/orders/valid/export/${fileType}?${params}`;
            }
            else{
                let  {status, data} = await http.get(`/lystore/orders/valid/export/${fileType}?${params}`);
                if(status === 201){
                    toasts.info('lystore.sent.export.BC');
                }
            }
        };

        $scope.cancelValidation = async (orders: OrderClient[]) => {
            try {
                await $scope.displayedOrders.cancel(orders);
                await $scope.syncOrders('VALID');
                toasts.confirm('lystore.orders.valid.cancel.confirmation');
            } catch (e) {
                toasts.warning('lystore.orders.valid.cancel.error');
            } finally {
                Utils.safeApply($scope);
            }
        };

        $scope.getProgramName = (idProgram: number) => idProgram !== undefined
            ? _.findWhere($scope.programs.all, { id: idProgram }).name
            : '';

        $scope.isOperationsIsEmpty = false;

        $scope.selectOperationForOrder = async () =>{
            await $scope.initOperation(false);
            console.log($scope.operations)
            $scope.isOperationsIsEmpty = !$scope.operations.all.some(operation =>operation.status == 'true'
                && ((!!operation.instruction && operation.instruction.cp_adopted === 'WAITING')
                    || !operation.instruction
                    || !operation.instruction.cp_adopted)
            );
            template.open('validOrder.lightbox', 'administrator/order/order-select-operation');
            $scope.display.lightbox.validOrder = true;
        };

        $scope.operationSelected = async (operation:Operation) => {
            $scope.operation = operation;
            let idsOrder = $scope.ordersClient.selected.map(order => order.id);
            await $scope.ordersClient.addOperationInProgress(operation.id, idsOrder);
            // await $scope.getOrderWaitingFiltered($scope.campaign);
            template.open('validOrder.lightbox', 'administrator/order/order-valid-add-operation');
            $scope.operationId= operation.id;
        };

        $scope.openConfirmationRejectOrder = () => {
            $scope.display.lightbox.rejectOrder = true;
            template.open('rejectOrder.lightbox', 'administrator/order/order-reject-operation');
            Utils.safeApply($scope);
        };

        $scope.rejectOrders = async (comment: string) => {
            let {status} = await $scope.ordersClient.rejectOrders(comment);
            if (status === 200) {
                for(let i = $scope.ordersClient.selected.length - 1 ; i >= 0 ; i--){
                    let reject = $scope.ordersClient.selected[i]
                    let index = $scope.displayedOrders.all.findIndex(oce => oce.id === reject.id )
                    reject.selected = false ;
                    $scope.displayedOrders.all.splice(index,1);
                }
            } else {
                toasts.warning('lystore.reject.orders.err')
            }
            await $scope.cancelOrderReject();
            toasts.confirm('lystore.reject.orders.confirm');
            Utils.safeApply($scope);
        };

        $scope.cancelOrderReject = () => {
            $scope.display.lightbox.rejectOrder = false;
            template.close('rejectOrder.lightbox');
            Utils.safeApply($scope);
        };

        $scope.inProgressOrders = async (orders: OrderClient[]) => {
            let ordersToValidat = new OrdersClient();
            ordersToValidat.all = Mix.castArrayAs(OrderClient, orders);
            let {status, data} = await ordersToValidat.updateStatus('IN PROGRESS');
            if (status === 200) {
                $scope.orderValidationData = {
                    agents: _.uniq(data.agent),
                    number_validation: data.number_validation,
                    structures: _.uniq(_.pluck(ordersToValidat.all, 'name_structure'))
                };
                template.open('validOrder.lightbox', 'administrator/order/order-valid-confirmation');
                $scope.display.lightbox.validOrder = true;
            }
            await $scope.syncOrders('WAITING');
            Utils.safeApply($scope);
        };

        $scope.orderShow = (order:OrderClient) => {
            if(order.rank !== undefined){
                if(order.campaign.priority_field === PRIORITY_FIELD.ORDER && order.campaign.orderPriorityEnable()){
                    return order.rank = order.rank + 1;
                } else if (order.campaign.priority_field === PRIORITY_FIELD.PROJECT && order.project.preference !== null && order.campaign.projectPriorityEnable()){
                    return order.rank = order.project.preference + 1;
                }
            }
            return order.rank = lang.translate("lystore.order.not.prioritized");
        };
        $scope.updateOrder = (order: OrderClient) => {
            $scope.ub.putPreferences("searchFields", $scope.search.filterWords);
            $scope.redirectTo(`/order/update/${order.id}`);
        };
        $scope.selectCampaignAndInitFilter = async () =>{
            await $scope.selectCampaignShow(new Campaign(),$scope.campaignSelection);
            $scope.switchAllOrders();
            $scope.search.filterWords = [];
        };

        $scope.initMultiCombo = () =>{
            //copier les objets de campagne
            $scope.campaignSelection.forEach(c =>{
                if(!$scope.campaignSelectionMulti.find(campaign => campaign.id === c.id ))
                    $scope.campaignSelectionMulti.push($scope.campaigns.all.find(campaign => campaign.id === c.id ))
            })
            Utils.safeApply($scope)
        }
        // $scope.test = () =>{
        //     let elements = document.getElementsByClassName('vertical-array-scroll');
        //     if(elements[0])
        //          elements[0].scrollLeft = 9000000000000;
        //     Utils.safeApply($scope);
        // };

        angular.element(document).ready(function(){
            let elements = document.getElementsByClassName('vertical-array-scroll');
            if(elements[0]) {
                elements[0].scrollLeft = 9000000000000;
            }
            Utils.safeApply($scope);
        });

        $scope.scrollDisplay = {
            limitTo : NBDISPLAYEDORDERS
        };

        $scope.setScrollDisplay = () => {
            $scope.scrollDisplay.limitTo = NBDISPLAYEDORDERS;
        }
    }]);