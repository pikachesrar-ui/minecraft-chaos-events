package com.kiras.chaosevents.config;

import java.util.Locale;

public enum ChaosConfigCategory {
    BIG("big", "Большие события"),
    PRANK("prank", "Микроподлянки"),
    TRIVIA("trivia", "Викторина"),
    SWAP("swap", "Свапы игроков");

    private final String id;
    private final String displayName;

    ChaosConfigCategory(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static ChaosConfigCategory fromId(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (ChaosConfigCategory category : values()) {
            if (category.id.equals(normalized)) {
                return category;
            }
        }
        return null;
    }
}
