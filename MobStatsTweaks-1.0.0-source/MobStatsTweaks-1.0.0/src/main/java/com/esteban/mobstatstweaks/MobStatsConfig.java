package com.esteban.mobstatstweaks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads the editable JSON config. Values are multipliers, where 1.0 means unchanged. */
public final class MobStatsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile Settings current = Settings.defaults();

    private MobStatsConfig() {}

    public static Settings get() {
        return current;
    }

    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve("mobstatstweaks.json");
    }

    public static synchronized void loadAtStartup() {
        try {
            reload();
        } catch (Exception exception) {
            MobStatsTweaks.LOGGER.error("Could not read mobstatstweaks.json; keeping safe defaults.", exception);
            current = Settings.defaults();
        }
    }

    /** Reloads atomically: invalid JSON leaves the previous active config untouched. */
    public static synchronized void reload() throws IOException {
        Path path = getConfigPath();
        Files.createDirectories(path.getParent());
        if (Files.notExists(path)) {
            Files.writeString(path, GSON.toJson(defaultJson()), StandardCharsets.UTF_8);
        }

        JsonObject root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IOException("Top-level JSON value must be an object.");
            }
            root = parsed.getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Invalid JSON in " + path + ": " + exception.getMessage(), exception);
        }

        boolean enabled = getBoolean(root, "enabled", true);
        boolean vanillaOnly = getBoolean(root, "vanilla_only", true);
        boolean hostileOnly = getBoolean(root, "hostile_only", true);
        Map<String, Double> global = readMultipliers(root.getAsJsonObject("global"));
        Map<String, Map<String, Double>> entities = new LinkedHashMap<>();
        JsonObject entityObject = root.getAsJsonObject("entities");
        if (entityObject != null) {
            for (Map.Entry<String, JsonElement> entry : entityObject.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    MobStatsTweaks.LOGGER.warn("Ignoring non-object rule for entity {}", entry.getKey());
                    continue;
                }
                Map<String, Double> values = readMultipliers(entry.getValue().getAsJsonObject());
                if (!values.isEmpty()) {
                    entities.put(entry.getKey(), values);
                }
            }
        }
        current = new Settings(enabled, vanillaOnly, hostileOnly, Map.copyOf(global), Map.copyOf(entities));
        MobStatsTweaks.LOGGER.info("Loaded Mob Stats Tweaks config: {} entity-specific rules", entities.size());
    }

    private static boolean getBoolean(JsonObject object, String name, boolean fallback) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return value.getAsBoolean();
    }

    private static Map<String, Double> readMultipliers(JsonObject object) {
        Map<String, Double> values = new LinkedHashMap<>();
        if (object == null) return values;
        for (String key : MobStatsApplier.STAT_KEYS) {
            JsonElement element = object.get(key);
            if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) continue;
            double value;
            try {
                value = element.getAsDouble();
            } catch (NumberFormatException exception) {
                continue;
            }
            if (Double.isFinite(value) && value > 0.0 && value <= 100.0) {
                values.put(key, value);
            } else {
                MobStatsTweaks.LOGGER.warn("Ignoring invalid multiplier for {}: {} (valid range: > 0 and <= 100)", key, value);
            }
        }
        return values;
    }

    private static JsonObject defaultJson() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        root.addProperty("vanilla_only", true);
        root.addProperty("hostile_only", true);
        root.addProperty("_comment", "Multipliers: 1.0 = vanilla. Edit values, then run /mobstatstweaks reload.");

        JsonObject global = new JsonObject();
        global.addProperty("max_health", 1.5);
        global.addProperty("attack_damage", 1.25);
        global.addProperty("movement_speed", 1.10);
        global.addProperty("armor", 1.0);
        global.addProperty("armor_toughness", 1.0);
        global.addProperty("knockback_resistance", 1.0);
        global.addProperty("follow_range", 1.10);
        root.add("global", global);

        JsonObject entities = new JsonObject();
        JsonObject zombie = new JsonObject();
        zombie.addProperty("max_health", 2.0);
        zombie.addProperty("attack_damage", 1.5);
        entities.add("minecraft:zombie", zombie);
        JsonObject skeleton = new JsonObject();
        skeleton.addProperty("max_health", 1.5);
        skeleton.addProperty("follow_range", 1.35);
        entities.add("minecraft:skeleton", skeleton);
        JsonObject creeper = new JsonObject();
        creeper.addProperty("max_health", 1.5);
        creeper.addProperty("follow_range", 1.25);
        entities.add("minecraft:creeper", creeper);
        root.add("entities", entities);
        return root;
    }

    public static final class Settings {
        public final boolean enabled;
        public final boolean vanillaOnly;
        public final boolean hostileOnly;
        public final Map<String, Double> global;
        public final Map<String, Map<String, Double>> entities;

        private Settings(boolean enabled, boolean vanillaOnly, boolean hostileOnly,
                         Map<String, Double> global, Map<String, Map<String, Double>> entities) {
            this.enabled = enabled;
            this.vanillaOnly = vanillaOnly;
            this.hostileOnly = hostileOnly;
            this.global = global;
            this.entities = entities;
        }

        public static Settings defaults() {
            Map<String, Double> global = new LinkedHashMap<>();
            global.put("max_health", 1.5);
            global.put("attack_damage", 1.25);
            global.put("movement_speed", 1.10);
            global.put("armor", 1.0);
            global.put("armor_toughness", 1.0);
            global.put("knockback_resistance", 1.0);
            global.put("follow_range", 1.10);
            return new Settings(true, true, true, Map.copyOf(global), Map.of());
        }

        public double multiplier(String entityId, String statKey) {
            Map<String, Double> specific = entities.get(entityId);
            if (specific != null && specific.containsKey(statKey)) return specific.get(statKey);
            return global.getOrDefault(statKey, 1.0);
        }

        public boolean hasSpecificRule(String entityId) {
            return entities.containsKey(entityId);
        }
    }
}
