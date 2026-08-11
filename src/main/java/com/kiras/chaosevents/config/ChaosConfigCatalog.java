package com.kiras.chaosevents.config;

import com.kiras.chaosevents.event.BigEventEngine;
import com.kiras.chaosevents.prank.MicroPrankEngine;
import com.kiras.chaosevents.spatial.SpatialSwapManager;
import com.kiras.chaosevents.trivia.TriviaEngine;

import java.util.List;

public final class ChaosConfigCatalog {
    private ChaosConfigCatalog() {
    }

    public static List<ChaosConfigEntry> entries(ChaosConfigCategory category) {
        if (category == null) {
            return List.of();
        }
        return switch (category) {
            case BIG -> BigEventEngine.getConfigEntries();
            case PRANK -> MicroPrankEngine.getConfigEntries();
            case TRIVIA -> TriviaEngine.getConfigEntries();
            case SWAP -> SpatialSwapManager.getConfigEntries();
        };
    }
}
