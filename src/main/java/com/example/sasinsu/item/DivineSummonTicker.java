package com.example.sasinsu.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "sasinsu")
public class DivineSummonTicker {
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity spirit = event.getEntity();
        if (!(spirit.level() instanceof ServerLevel level)) return;
        if (!spirit.getPersistentData().getBoolean("sasinsu_summon")) return;

        if (spirit.tickCount > 400) {
            spirit.discard();
            return;
        }

        String beast = spirit.getPersistentData().getString("sasinsu_beast");
        UUID ownerId = spirit.getPersistentData().getUUID("sasinsu_owner");
        Player owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null && owner.isAlive()) {
            double angle = spirit.tickCount * 0.055;
            double radius = 2.15;
            boolean flying = beast.equals("AZURE_DRAGON") || beast.equals("VERMILION_BIRD");
            double y = owner.getY() + (flying ? 1.45 : 0.15)
                    + Math.sin(spirit.tickCount * 0.09) * 0.22;
            spirit.setPos(owner.getX() + Math.cos(angle) * radius, y,
                    owner.getZ() + Math.sin(angle) * radius);
        }

        if (spirit.tickCount % 10 != 0) return;

        var particle = switch (beast) {
            case "AZURE_DRAGON" -> ParticleTypes.ELECTRIC_SPARK;
            case "WHITE_TIGER" -> ParticleTypes.CRIT;
            case "VERMILION_BIRD" -> ParticleTypes.FLAME;
            default -> ParticleTypes.SOUL_FIRE_FLAME;
        };
        level.sendParticles(particle, spirit.getX(), spirit.getY() + 0.5, spirit.getZ(),
                14, 0.35, 0.35, 0.35, 0.04);

        List<Monster> targets = level.getEntitiesOfClass(Monster.class,
                spirit.getBoundingBox().inflate(3.6),
                target -> target.isAlive() && target != spirit);
        for (Monster target : targets) {
            target.hurt(level.damageSources().mobAttack(spirit), 2.5f);
            Vec3 knock = target.position().subtract(spirit.position()).normalize().scale(0.35);
            target.push(knock.x, 0.12, knock.z);
        }
    }
}
