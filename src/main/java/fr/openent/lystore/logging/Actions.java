package fr.openent.lystore.logging;

public enum Actions {
    CREATE ("CREATE"),
    UPDATE ("UPDATE"),
    DELETE ("DELETE"),
    IMPORT ("IMPORT"),
    REJECT ("REJECT"),
    ACCESS ("ACCESS");

    private final String actionName;

    Actions (String action) {
        this.actionName = action;
    }
}
