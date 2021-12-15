package fr.openent.lystore.model;

public enum InstructionStatus {
    ADOPTED("Adopted"),
    REJECTED("Rejected"),
    WAITING("Waiting");

    private final String instructionStatus;

    InstructionStatus(String instructionStatus) {
        this.instructionStatus = instructionStatus;
    }

    @Override
    public String toString() {
        return instructionStatus;
    }
}
