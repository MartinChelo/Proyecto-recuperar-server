package com.esteban.mobstatstweaks;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(MobStatsTweaks.MOD_ID)
public final class MobStatsTweaks {
    public static final String MOD_ID = "mobstatstweaks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MobStatsTweaks(IEventBus modEventBus, ModContainer modContainer) {
        MobStatsConfig.loadAtStartup();
        NeoForge.EVENT_BUS.register(new MobStatsEvents());
        LOGGER.info("Mob Stats Tweaks loaded. Config: config/mobstatstweaks.json");
    }
}
