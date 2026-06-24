package com.example.home.domain.scenario.model;

public enum PersonaType {
    END_USER("실수요자"),
    INVESTOR("투자자"),
    MOVER("갈아타기 수요층");

    private final String label;

    PersonaType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
