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

    static {
        for (ChaosConfigCategory category : ChaosConfigCategory.values()) {
            DISABLED.put(category, new HashSet<>());
        }
    }

    private ChaosConfigManager() {
    }

    public static synchronized void load() {
        clearAll();
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
                if (array == null) {
                    continue;
                }
                Set<String> disabled = DISABLED.get(category);
                for (JsonElement element : array) {
                    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                        disabled.add(element.getAsString());
                    }
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

    private static void clearAll() {
        for (Set<String> disabled : DISABLED.values()) {
            disabled.clear();
        }
    }
}
