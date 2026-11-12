package com.example.sasinsu.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DivineWeaponItem extends Item {
    public enum BEAST { AZURE_DRAGON, WHITE_TIGER, VERMILION_BIRD, BLACK_TORTOISE }
    private final BEAST beast;

    public DivineWeaponItem(BEAST beast, Properties properties) {
        super(properties);
        this.beast = beast;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide && level instanceof ServerLevel server) {
            castSkill(server, player);
            player.getCooldowns().addCooldown(this, cooldownTicks());
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private int cooldownTicks() {
        return switch (beast) {
            case AZURE_DRAGON -> 100;
            case WHITE_TIGER -> 80;
            case VERMILION_BIRD -> 60;
            case BLACK_TORTOISE -> 120;
        };
    }

    private void castSkill(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position().add(0, 1.0, 0);
        level.playSound(null, player.blockPosition(), switch (beast) {
            case AZURE_DRAGON -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case WHITE_TIGER -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case VERMILION_BIRD -> SoundEvents.FIRECHARGE_USE;
            case BLACK_TORTOISE -> SoundEvents.SHIELD_BLOCK;
        }, SoundSource.PLAYERS, 1.3f, 0.8f + level.random.nextFloat() * 0.4f);

        switch (beast) {
            case AZURE_DRAGON -> dragonBurst(level, player, origin, look);
            case WHITE_TIGER -> tigerCleave(level, player, origin, look);
            case VERMILION_BIRD -> phoenixFlurry(level, player, origin, look);
            case BLACK_TORTOISE -> tortoiseWard(level, player, origin);
        }
    }

    private void dragonBurst(ServerLevel level, Player player, Vec3 o, Vec3 dir) {
        for (int i = 1; i <= 18; i++) {
            Vec3 p = o.add(dir.scale(i * 0.75));
            ring(level, p, 1.0 + i * 0.12, ParticleTypes.ELECTRIC_SPARK, 18);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 12, .35, .35, .35, .02);
            damage(level, player, p, 1.5, 8.0);
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, o.x, o.y, o.z, 90, 1.2, .8, 1.2, .08);
    }

    private void tigerCleave(ServerLevel level, Player player, Vec3 o, Vec3 dir) {
        Vec3 center = o.add(dir.scale(3.2));
        for (int arc = 0; arc < 3; arc++) {
            for (int i = 0; i < 28; i++) {
                double angle = Math.PI * (i / 27.0) - Math.PI / 2;
                Vec3 p = center.add(Math.cos(angle) * (2.2 + arc * .45), .15 + arc * .2,
                        Math.sin(angle) * (2.2 + arc * .45));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 3, .08, .08, .08, .08);
            }
        }
        damage(level, player, center, 4.5, 12.0);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 100, 1.8, .35, 1.8, .12);
    }

    private void phoenixFlurry(ServerLevel level, Player player, Vec3 o, Vec3 dir) {
        for (int hit = 0; hit < 5; hit++) {
            Vec3 p = o.add(dir.scale(1.5 + hit * 1.1));
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 35, .55, .5, .55, .06);
            level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 8, .3, .3, .3, .02);
            damage(level, player, p, 1.3, 3.0);
        }
        Vec3 end = o.add(dir.scale(6.5));
        level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 4, .5, .5, .5, 0);
        level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 140, 1.4, 1.0, 1.4, .12);
    }

    private void tortoiseWard(ServerLevel level, Player player, Vec3 o) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 160, 2));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, 160, 2));
        ring(level, o, 3.2, ParticleTypes.HAPPY_VILLAGER, 72);
        ring(level, o, 2.5, ParticleTypes.HAPPY_VILLAGER, 54);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, o.x, o.y + .7, o.z, 90, 1.1, 1.0, 1.1, .12);
    }

    private void ring(ServerLevel level, Vec3 center, double radius,
                      net.minecraft.core.particles.SimpleParticleType particle, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y + .1,
                    center.z + Math.sin(a) * radius, 1, 0, .02, 0, 0);
        }
    }

    private void damage(ServerLevel level, Player player, Vec3 center, double radius, float amount) {
        AABB area = new AABB(center, center).inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), amount);
            Vec3 knock = target.position().subtract(player.position()).normalize().scale(0.8);
            target.push(knock.x, 0.25, knock.z);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.sasinsu." + switch (beast) {
            case AZURE_DRAGON -> "azure_dragon_sword";
            case WHITE_TIGER -> "white_tiger_axe";
            case VERMILION_BIRD -> "vermilion_dagger";
            case BLACK_TORTOISE -> "black_tortoise_spear";
        });
    }
}
