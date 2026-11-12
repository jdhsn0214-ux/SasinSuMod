package com.example.sasinsu.network;

import com.example.sasinsu.SasinSuMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Runs delayed parts of weapon skills on the logical server thread.
 * Keeping gameplay effects server-authoritative makes the timing consistent
 * for both single-player and multiplayer.
 */
@Mod.EventBusSubscriber(modid = SasinSuMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SkillSequenceManager {
    private static final List<PendingAction> ACTIONS = new ArrayList<>();

    private SkillSequenceManager() {}

    public static void schedule(ServerPlayer player, int delayTicks, Consumer<ServerPlayer> action) {
        ACTIONS.add(new PendingAction(player.getUUID(), Math.max(1, delayTicks), action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIONS.isEmpty()) return;

        Iterator<PendingAction> iterator = ACTIONS.iterator();
        while (iterator.hasNext()) {
            PendingAction pending = iterator.next();
            pending.remainingTicks--;
            if (pending.remainingTicks > 0) continue;

            iterator.remove();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(pending.playerId);
            if (player != null && player.isAlive()) {
                pending.action.accept(player);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIONS.clear();
    }

    private static final class PendingAction {
        private final UUID playerId;
        private int remainingTicks;
        private final Consumer<ServerPlayer> action;

        private PendingAction(UUID playerId, int remainingTicks, Consumer<ServerPlayer> action) {
            this.playerId = playerId;
            this.remainingTicks = remainingTicks;
            this.action = action;
        }
    }
}
