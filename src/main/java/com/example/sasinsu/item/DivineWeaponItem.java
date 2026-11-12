package com.example.sasinsu.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
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
            player.swing(hand, true);
            castSkill(server, player);
            summonSpirit(server, player);
            player.getCooldowns().addCooldown(this, cooldownTicks());
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private int cooldownTicks() {
        return switch (beast) {
            case AZURE_DRAGON -> 240;
            case WHITE_TIGER -> 220;
            case VERMILION_BIRD -> 180;
            case BLACK_TORTOISE -> 300;
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
        }, SoundSource.PLAYERS, 1.5f, 0.75f + level.random.nextFloat() * 0.35f);

        switch (beast) {
            case AZURE_DRAGON -> dragonBurst(level, player, origin, look);
            case WHITE_TIGER -> tigerCleave(level, player, origin, look);
            case VERMILION_BIRD -> phoenixFlurry(level, player, origin, look);
            case BLACK_TORTOISE -> tortoiseWard(level, player, origin);
        }
    }

    private void summonSpirit(ServerLevel level, Player player) {
        Mob spirit;
        switch (beast) {
            case AZURE_DRAGON -> spirit = EntityType.PHANTOM.create(level);
            case WHITE_TIGER -> spirit = EntityType.WOLF.create(level);
            case VERMILION_BIRD -> spirit = EntityType.PARROT.create(level);
            case BLACK_TORTOISE -> spirit = EntityType.TURTLE.create(level);
            default -> { return; }
        }
        if (spirit == null) return;

        Vec3 forward = player.getLookAngle().normalize();
        spirit.moveTo(player.getX() + forward.x * 2.0, player.getY() + 0.3,
                player.getZ() + forward.z * 2.0, player.getYRot(), 0);
        spirit.setNoAi(true);
        spirit.setPersistenceRequired();
        spirit.setGlowingTag(true);
        spirit.setCustomName(Component.literal(switch (beast) {
            case AZURE_DRAGON -> "§b청룡의 영령";
            case WHITE_TIGER -> "§f백호의 영령";
            case VERMILION_BIRD -> "§c주작의 영령";
            case BLACK_TORTOISE -> "§3현무의 영령";
        }));
        spirit.setCustomNameVisible(true);
        spirit.getPersistentData().putBoolean("sasinsu_summon", true);
        spirit.getPersistentData().putString("sasinsu_beast", beast.name());
        spirit.getPersistentData().putUUID("sasinsu_owner", player.getUUID());
        level.addFreshEntity(spirit);

        var particle = switch (beast) {
            case AZURE_DRAGON -> ParticleTypes.ELECTRIC_SPARK;
            case WHITE_TIGER -> ParticleTypes.CRIT;
            case VERMILION_BIRD -> ParticleTypes.FLAME;
            case BLACK_TORTOISE -> ParticleTypes.SOUL_FIRE_FLAME;
        };
        level.sendParticles(particle, spirit.getX(), spirit.getY() + 0.8, spirit.getZ(),
                65, 0.8, 0.8, 0.8, 0.08);
    }

    private void dragonBurst(ServerLevel level, Player player, Vec3 o, Vec3 dir) {
        for (int i = 1; i <= 22; i++) {
            Vec3 p = o.add(dir.scale(i * 0.72));
            ring(level, p, 1.0 + i * 0.10, ParticleTypes.ELECTRIC_SPARK, 20);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 16, .4, .4, .4, .025);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 5, .25, .25, .25, .04);
            damage(level, player, p, 1.35, 7.0f);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, o.x + dir.x * 12, o.y,
                o.z + dir.z * 12, 2, .6, .6, .6, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, o.x, o.y, o.z, 150, 1.4, 1.0, 1.4, .14);
    }

    private void tigerCleave(ServerLevel level, Player player, Vec3 o, Vec3 dir) {
        Vec3 center = o.add(dir.scale(3.2));
        for (int arc = 0; arc < 4; arc++) {
            for (int i = 0; i < 36; i++) {
                double angle = Math.PI * (i / 35.0) - Math.PI / 2;
                Vec3 p = center.add(Math.cos(angle) * (2.0 + arc * .42), .15 + arc * .18,
                        Math.sin(angle) * (2.0 + arc * .42));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 4, .08, .08, .08, .08);
            }
        }
        damage(level, player, center, 5.0, 14.0f);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, .5, .5, .5, 0);
        level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z, 130, 2.0, .45, 2.0, .14);
    }

    private void phoenixFlurry(ServerLevel level, Player player, Vec3 o, Vec3 dir) {
        for (int hit = 0; hit < 7; hit++) {
            Vec3 p = o.add(dir.scale(1.2 + hit * 0.95));
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 45, .6, .55, .6, .07);
            level.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 10, .3, .3, .3, .025);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 12, .35, .35, .35, .04);
            damage(level, player, p, 1.25, 3.5f);
        }
        Vec3 end = o.add(dir.scale(7.5));
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 2, .6, .6, .6, 0);
        level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 180, 1.6, 1.2, 1.6, .14);
    }

    private void tortoiseWard(ServerLevel level, Player player, Vec3 o) {
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 240, 2));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.ABSORPTION, 240, 2));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
        ring(level, o, 3.2, ParticleTypes.HAPPY_VILLAGER, 96);
        ring(level, o, 2.5, ParticleTypes.SOUL_FIRE_FLAME, 72);
        ring(level, o.add(0, 1.2, 0), 2.0, ParticleTypes.ENCHANT, 64);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, o.x, o.y + .7, o.z, 120, 1.2, 1.1, 1.2, .14);
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
