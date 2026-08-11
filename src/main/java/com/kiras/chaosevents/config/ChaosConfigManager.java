package com.kiras.chaosevents.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kiras.chaosevents.ChaosEvents;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

/** Persistent server configuration used by the in-game configuration books. */
public final class ChaosConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("chaosevents-settings.json");
    private static final EnumMap<ChaosConfigCategory, Set<String>> DISABLED = new EnumMap<>(ChaosConfigCategory.class);
    private static final EnumMap<ChaosConfigCategory, IntervalRange> INTERVALS = new EnumMap<>(ChaosConfigCategory.class);

    static {
        for (ChaosConfigCategory category : ChaosConfigCategory.values()) {
            DISABLED.put(category, new HashSet<>());
            INTERVALS.put(category, defaultInterval(category));
        }
    }

    private ChaosConfigManager() {
    }

    public static synchronized void load() {
        clearAll();
        resetIntervalsToDefaults();
        if (!Files.isRegularFile(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            for (ChaosConfigCategory category : ChaosConfigCategory.values()) {
                JsonArray array = root.getAsJsonArray(category.id());
                if (array != null) {
                    Set<String> disabled = DISABLED.get(category);
                    for (JsonElement element : array) {
                        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                            disabled.add(element.getAsString());
                        }
                    }
                }
            }

            JsonObject intervals = root.getAsJsonObject("intervals");
            if (intervals != null) {
                for (ChaosConfigCategory category : ChaosConfigCategory.values()) {
                    JsonObject interval = intervals.getAsJsonObject(category.id());
                    if (interval == null) {
                        continue;
                    }
                    int min = readInt(interval, "minSeconds", category.defaultMinIntervalSeconds());
                    int max = readInt(interval, "maxSeconds", category.defaultMaxIntervalSeconds());
                    INTERVALS.put(category, sanitizeInterval(category, min, max));
                }
            }
        } catch (Exception exception) {
            ChaosEvents.LOGGER.error("Failed to read Chaos Events settings from {}", CONFIG_PATH, exception);
        }
    }

    public static synchronized void save() {
        JsonObject root = new JsonObject();
        for (ChaosConfigCategory category : ChaosConfigCategory.values()) {
            JsonArray array = new JsonArray();
            DISABLED.get(category).stream().sorted().forEach(array::add);
            root.add(category.id(), array);
        }

        JsonObject intervals = new JsonObject();
        for (ChaosConfigCategory category : ChaosConfigCategory.values()) {
            IntervalRange range = INTERVALS.get(category);
            JsonObject interval = new JsonObject();
            interval.addProperty("minSeconds", range.minSeconds());
            interval.addProperty("maxSeconds", range.maxSeconds());
            intervals.add(category.id(), interval);
        }
        root.add("intervals", intervals);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            ChaosEvents.LOGGER.error("Failed to save Chaos Events settings to {}", CONFIG_PATH, exception);
        }
    }

    public static synchronized boolean isEnabled(ChaosConfigCategory category, String id) {
        return category != null && id != null && !DISABLED.get(category).contains(id);
    }

    public static synchronized Set<String> getDisabled(ChaosConfigCategory category) {
        if (category == null) {
            return Set.of();
        }
        return Set.copyOf(DISABLED.get(category));
    }

    public static synchronized void replaceDisabled(ChaosConfigCategory category, Collection<String> disabledIds) {
        if (category == null) {
            return;
        }
        Set<String> disabled = DISABLED.get(category);
        disabled.clear();
        if (disabledIds != null) {
            disabled.addAll(disabledIds);
        }
    }

    public static synchronized int getMinIntervalSeconds(ChaosConfigCategory category) {
        return intervalFor(category).minSeconds();
    }

    public static synchronized int getMaxIntervalSeconds(ChaosConfigCategory category) {
        return intervalFor(category).maxSeconds();
    }

    public static synchronized void setIntervalSeconds(ChaosConfigCategory category, int minSeconds, int maxSeconds) {
        if (category == null) {
            return;
        }
        INTERVALS.put(category, sanitizeInterval(category, minSeconds, maxSeconds));
    }

    public static String encodeDisabled(Collection<String> disabledIds) {
        if (disabledIds == null || disabledIds.isEmpty()) {
            return "";
        }
        return String.join("\n", disabledIds);
    }

    public static Set<String> decodeDisabled(String encoded) {
        Set<String> result = new HashSet<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        for (String value : encoded.split("\\n")) {
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return result;
    }

    private static int readInt(JsonObject object, String key, int fallback) {
        try {
            JsonElement element = object.get(key);
            if (element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private static IntervalRange intervalFor(ChaosConfigCategory category) {
        if (category == null) {
            return new IntervalRange(60, 60);
        }
        return INTERVALS.getOrDefault(category, defaultInterval(category));
    }

    private static IntervalRange sanitizeInterval(ChaosConfigCategory category, int minSeconds, int maxSeconds) {
        int min = Math.max(ChaosConfigCategory.MIN_ALLOWED_INTERVAL_SECONDS,
                Math.min(ChaosConfigCategory.MAX_ALLOWED_INTERVAL_SECONDS, minSeconds));
        int max = Math.max(ChaosConfigCategory.MIN_ALLOWED_INTERVAL_SECONDS,
                Math.min(ChaosConfigCategory.MAX_ALLOWED_INTERVAL_SECONDS, maxSeconds));
        if (max < min) {
            max = min;
        }
        return new IntervalRange(min, max);
    }

    private static IntervalRange defaultInterval(ChaosConfigCategory category) {
        return new IntervalRange(category.defaultMinIntervalSeconds(), category.defaultMaxIntervalSeconds());
    }

    private static void clearAll() {
        for (Set<String> disabled : DISABLED.values()) {
            disabled.clear();
        }
    }

    private static void resetIntervalsToDefaults() {
        for (ChaosConfigCategory category : ChaosConfigCategory.values()) {
            INTERVALS.put(category, defaultInterval(category));
        }
    }

    private record IntervalRange(int minSeconds, int maxSeconds) {
    }
}
