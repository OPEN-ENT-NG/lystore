import {_, ng, template, moment} from 'entcore';
import {Label, Utils} from "../../model";

export const labelOperationController = ng.controller('labelOperationController',
    ['$scope',($scope) => {

        $scope.sort = {
            label_operation : {
                type: 'label',
                reverse: false
            }
        }
        $scope.display = {
            lightbox : {
                label: false,
            }
        };



        $scope.openLabelForm = (action: string, labelToHandle:Label) => {
            if(action === 'create'){
                $scope.newLabel = new Label();
                $scope.display.lightbox.label = true;
                template.open('label.lightbox', 'administrator/operation-label/label-form');
                Utils.safeApply($scope);
            } else
            if(labelToHandle.is_used < 1 && action === 'edit'){
                    $scope.newLabel = Object.assign(new Label(), labelToHandle);
                    $scope.display.lightbox.label = true;
                    template.open('label.lightbox', 'administrator/operation-label/label-form');
                    Utils.safeApply($scope);
            }
        };

        $scope.isValidLabelDate = (label:Label) => {
            return moment(label.start_date).isBefore(moment(label.end_date), 'days', '[]');
        };

        $scope.isValidLabelDateUsed = (label:Label) => {
            if(label.is_used && label.is_used > 0) {
                return moment(label.end_date).isAfter(moment(label.max_creation_date), 'days', '[]');
            }
            return true;
        };

        $scope.isValidLabelLength = (label:Label) => {
            return label.label.length > 0;
        };

        $scope.isValidLabelName = (label:Label) => {
            if(label.id === $scope.newLabel.id) {
                return !$scope.labelOperation.all.some(l => l.label.toUpperCase() === label.label.toUpperCase() && label.id !== l.id);
            }
            return !$scope.labelOperation.all.some(l => l.label.toUpperCase() === label.label.toUpperCase());
        }

        $scope.initLabelDate = (label:Label) => {
            if(!label.start_date && !label.end_date) {
                label.start_date = moment().add().format('YYYY-MM-DD');
                label.end_date = moment(new Date('2099-12-31'));
            }
        };

        $scope.validLabelForm = (label:Label) => {
            return label.label && $scope.isValidLabelLength(label) && $scope.isValidLabelName(label) && $scope.isValidLabelDate(label) && $scope.isValidLabelDateUsed(label);
        };

        $scope.validLabel = async (label:Label) => {
            await label.save();
            await $scope.cancelLabelForm();
            await $scope.initLabel();
            Utils.safeApply($scope);
        };

        $scope.deleteLabels = async () => {
            await $scope.labelOperation.delete();
            await $scope.initLabel();
            template.close('label.lightbox');
            $scope.display.lightbox.label = false;
            Utils.safeApply($scope);
        };

        $scope.trashLabel = async (label:Label) => {
            await label.delete();
            await $scope.initLabel();
            template.close('label.lightbox');
            $scope.display.lightbox.label = false;
            Utils.safeApply($scope);
        };

        $scope.disabledDeleteToaster = () => {
            let result = false;
            $scope.labelOperation.selected.map(
                label => { if(label.is_used){
                    result = true;
                }}
            )
            return result;
        };

        $scope.cancelLabelForm = async () => {
            $scope.display.lightbox.label = false;
            template.close('label.lightbox');
            Utils.safeApply($scope);
            $scope.newLabel = new Label();
        };

        $scope.addLabelFilter = async (event?) => {
            if (event && (event.which === 13 || event.keyCode === 13) && event.target.value.trim() !== '') {
                if(!_.contains($scope.labelOperation.filters, event.target.value)){
                    $scope.labelOperation.filters = [...$scope.labelOperation.filters, event.target.value];
                }
                event.target.value = '';
                await $scope.initLabel();
                Utils.safeApply($scope);
            }
        };

        $scope.dropLabelFilter = async (filter: string) => {
            $scope.labelOperation.filters = $scope.labelOperation.filters.filter( filterWord => filterWord !== filter);
            await $scope.initLabel();
            Utils.safeApply($scope);
        };

        $scope.openLightboxDeleteLabel = () => {
            $scope.display.lightbox.label = true;
            template.open('label.lightbox', 'administrator/operation-label/operation-label-delete-lightbox');
            Utils.safeApply($scope);
        };

        $scope.openLightboxTrashLabel = (label:Label) => {
            if(label.is_used > 0) {
                return false;
            } else {
                $scope.label = label;
                $scope.display.lightbox.label = true;
                template.open('label.lightbox', 'administrator/operation-label/operation-label-trash-lightbox');
                Utils.safeApply($scope);
            }
        };
    }]);