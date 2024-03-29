package fr.openent.lystore.model;

public enum OrderStatus {
    VALID("Validée "),
    REJECTED("Refusée "),
    IN_PROGRESS("En cours de traitement"),
    WAITING("Transmise"),
    DONE("Clôturée"),
    SENT("Transmise au fournisseur"),
    WAITING_FOR_ACCEPTANCE("En cours d'instruction");

    private final String orderStatus;

    OrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    @Override
    public String toString() {
        return orderStatus;
    }
}
