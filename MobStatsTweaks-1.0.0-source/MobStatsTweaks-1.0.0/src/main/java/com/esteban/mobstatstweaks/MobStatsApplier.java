package com.esteban.mobstatstweaks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

import java.util.List;

/** Applies multipliers against each entity's saved original base attributes, preventing compounding. */
public final class MobStatsApplier {
    private static final String DATA_KEY = "mobstatstweaks_originals";
    public static final List<String> STAT_KEYS = List.of(
            "max_health", "attack_damage", "movement_speed", "armor",
            "armor_toughness", "knockback_resistance", "follow_range"
    );

    private static final List<Stat> STATS = List.of(
            new Stat("max_health", Attributes.MAX_HEALTH),
            new Stat("attack_damage", Attributes.ATTACK_DAMAGE),
            new Stat("movement_speed", Attributes.MOVEMENT_SPEED),
            new Stat("armor", Attributes.ARMOR),
            new Stat("armor_toughness", Attributes.ARMOR_TOUGHNESS),
            new Stat("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
            new Stat("follow_range", Attributes.FOLLOW_RANGE)
    );

    private MobStatsApplier() {}

    public static boolean update(Mob mob, MobStatsConfig.Settings settings) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (key == null) return false;
        String entityId = key.toString();
        CompoundTag persistent = mob.getPersistentData();
        boolean hasOriginals = persistent.contains(DATA_KEY, Tag.TAG_COMPOUND);
        boolean isTarget = settings.enabled
                && (!settings.vanillaOnly || "minecraft".equals(key.getNamespace()))
                && (!settings.hostileOnly || mob instanceof Enemy || settings.hasSpecificRule(entityId));

        if (!isTarget) {
            if (hasOriginals) {
                restore(mob, persistent.getCompound(DATA_KEY));
                persistent.remove(DATA_KEY);
                return true;
            }
            return false;
        }

        boolean anyMultiplierDiffers = false;
        for (Stat stat : STATS) {
            if (Double.compare(settings.multiplier(entityId, stat.key), 1.0) != 0
                    && mob.getAttribute(stat.attribute) != null) {
                anyMultiplierDiffers = true;
                break;
            }
        }
        if (!anyMultiplierDiffers && !hasOriginals) return false;

        CompoundTag originals;
        if (hasOriginals) {
            originals = persistent.getCompound(DATA_KEY);
        } else {
            originals = new CompoundTag();
            persistent.put(DATA_KEY, originals);
        }

        float healthBefore = mob.getHealth();
        double maxHealthBefore = mob.getMaxHealth();
        double healthRatio = maxHealthBefore > 0.0 ? healthBefore / maxHealthBefore : 1.0;
        healthRatio = Math.max(0.0, Math.min(1.0, healthRatio));

        for (Stat stat : STATS) {
            AttributeInstance instance = mob.getAttribute(stat.attribute);
            if (instance == null) continue;
            if (!originals.contains(stat.key)) {
                originals.putDouble(stat.key, instance.getBaseValue());
            }
            double originalValue = originals.getDouble(stat.key);
            double multiplier = settings.multiplier(entityId, stat.key);
            instance.setBaseValue(Math.max(0.0001, originalValue * multiplier));
        }

        double maxHealthAfter = mob.getMaxHealth();
        if (maxHealthAfter > 0.0 && Float.isFinite(healthBefore)) {
            mob.setHealth((float) Math.max(0.0, Math.min(maxHealthAfter, maxHealthAfter * healthRatio)));
        }
        return true;
    }

    private static void restore(Mob mob, CompoundTag originals) {
        float healthBefore = mob.getHealth();
        double maxHealthBefore = mob.getMaxHealth();
        double healthRatio = maxHealthBefore > 0.0 ? healthBefore / maxHealthBefore : 1.0;
        healthRatio = Math.max(0.0, Math.min(1.0, healthRatio));
        for (Stat stat : STATS) {
            if (!originals.contains(stat.key)) continue;
            AttributeInstance instance = mob.getAttribute(stat.attribute);
            if (instance != null) instance.setBaseValue(originals.getDouble(stat.key));
        }
        double maxHealthAfter = mob.getMaxHealth();
        if (maxHealthAfter > 0.0 && Float.isFinite(healthBefore)) {
            mob.setHealth((float) Math.max(0.0, Math.min(maxHealthAfter, maxHealthAfter * healthRatio)));
        }
    }

    public static int updateLoadedEntities(MinecraftServer server) {
        int updated = 0;
        MobStatsConfig.Settings settings = MobStatsConfig.get();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Mob mob && update(mob, settings)) updated++;
            }
        }
        return updated;
    }

    private record Stat(String key, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute) {}
}
