package com.cookio.app.models;

public class CookingStep {

    private final String instruction;
    private final int minutes;

    public CookingStep(String instruction, int minutes) {
        this.instruction = instruction == null ? "" : instruction.trim();
        this.minutes = Math.max(0, minutes);
    }

    public String getInstruction() {
        return instruction;
    }

    public int getMinutes() {
        return minutes;
    }

    public boolean hasTimer() {
        return minutes > 0;
    }

    public String encode() {
        return instruction + "|" + minutes;
    }
}
