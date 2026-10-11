package com.esteban.mobstatstweaks;

import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.io.IOException;

public final class MobStatsEvents {
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob) {
            MobStatsApplier.update(mob, MobStatsConfig.get());
        }
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("mobstatstweaks")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload").executes(context -> {
                            try {
                                MobStatsConfig.reload();
                                int updated = MobStatsApplier.updateLoadedEntities(context.getSource().getServer());
                                context.getSource().sendSuccess(
                                        () -> Component.literal("[Mob Stats Tweaks] Config recargada. Entidades actualizadas/restauradas: " + updated),
                                        true
                                );
                                return Command.SINGLE_SUCCESS;
                            } catch (IOException | RuntimeException exception) {
                                MobStatsTweaks.LOGGER.error("Failed to reload Mob Stats Tweaks configuration", exception);
                                context.getSource().sendFailure(Component.literal(
                                        "[Mob Stats Tweaks] Error de configuración: " + exception.getMessage()
                                ));
                                return 0;
                            }
                        }))
        );
    }
}
