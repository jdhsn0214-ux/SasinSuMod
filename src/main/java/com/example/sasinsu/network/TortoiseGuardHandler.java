package com.example.sasinsu.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-authoritative guard and retaliation window for the Black Tortoise.
 */
@Mod.EventBusSubscriber
public final class TortoiseGuardHandler {
    public static final String GUARD_UNTIL = "sasinsu_tortoise_guard_until";
    private static final String COUNTER_READY_AT = "sasinsu_tortoise_counter_ready";

    private TortoiseGuardHandler() {}

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;

        long now = level.getGameTime();
        long guardUntil = player.getPersistentData().getLong(GUARD_UNTIL);
        if (now > guardUntil || guardUntil == 0L) return;

        // A successful guard meaningfully reduces incoming damage.
        event.setAmount(event.getAmount() * 0.35f);

        // Retaliate at most once per second, so rapid multi-hit attacks cannot spam counters.
        long counterReady = player.getPersistentData().getLong(COUNTER_READY_AT);
        if (now < counterReady) return;

        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != player && attacker.isAlive()) {
            player.getPersistentData().putLong(COUNTER_READY_AT, now + 20L);
            attacker.hurt(level.damageSources().playerAttack(player), 4.0f);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 1));
            Vec3 away = attacker.position().subtract(player.position()).normalize().scale(0.9);
            attacker.push(away.x, 0.22, away.z);
            level.playSound(null, player.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.55f);
        }
    }
}
