package com.example.home.domain.scenario.model;

public enum AgentStance {
    BUY("매수 탐색"),
    HOLD("유지"),
    WATCH("관망"),
    SELL("매도 압력"),
    MOVE("이동 탐색");

    private final String label;

    AgentStance(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
