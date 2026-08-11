package com.kiras.chaosevents.config;

import java.util.Locale;

public enum ChaosConfigCategory {
    BIG("big", "Большие события", "Перерыв между большими событиями", 5 * 60, 10 * 60),
    PRANK("prank", "Микроподлянки", "Интервал между микроподлянками", 60, 3 * 60),
    TRIVIA("trivia", "Викторина", "Интервал между вопросами", 6 * 60, 12 * 60),
    SWAP("swap", "Свапы игроков", "Интервал планового свапа", 15 * 60, 20 * 60);

    public static final int MIN_ALLOWED_INTERVAL_SECONDS = 10;
    public static final int MAX_ALLOWED_INTERVAL_SECONDS = 6 * 60 * 60;

    private final String id;
    private final String displayName;
    private final String intervalLabel;
    private final int defaultMinIntervalSeconds;
    private final int defaultMaxIntervalSeconds;

    ChaosConfigCategory(String id, String displayName, String intervalLabel,
                        int defaultMinIntervalSeconds, int defaultMaxIntervalSeconds) {
        this.id = id;
        this.displayName = displayName;
        this.intervalLabel = intervalLabel;
        this.defaultMinIntervalSeconds = defaultMinIntervalSeconds;
        this.defaultMaxIntervalSeconds = defaultMaxIntervalSeconds;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String intervalLabel() {
        return intervalLabel;
    }

    public int defaultMinIntervalSeconds() {
        return defaultMinIntervalSeconds;
    }

    public int defaultMaxIntervalSeconds() {
        return defaultMaxIntervalSeconds;
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
