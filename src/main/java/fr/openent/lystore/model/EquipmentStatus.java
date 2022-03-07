package fr.openent.lystore.model;

public enum EquipmentStatus {
    AVAILABLE("Available"),
    OUT_OF_STOCK("Out_of_stock"),
    UNAVAILABLE("Unavailable");

    private final String equipmentStatus;

    EquipmentStatus(String equipmentStatus) {
        this.equipmentStatus = equipmentStatus;
    }

    @Override
    public String toString() {
        return equipmentStatus;
    }
}
