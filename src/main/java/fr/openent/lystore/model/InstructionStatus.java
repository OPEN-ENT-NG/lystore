package fr.openent.lystore.model;

import static fr.openent.lystore.constants.EnumConstant.*;

public enum InstructionStatus {
    ADOPTED(VALID_CP_STATUS),
    REJECTED(REJECTED_CP_STATUS),
    WAITING(WAITING_CP_STATUS);

    private final String instructionStatus;

    InstructionStatus(String instructionStatus) {
        this.instructionStatus = instructionStatus;
    }

    @Override
    public String toString() {
        return instructionStatus;
    }
}
