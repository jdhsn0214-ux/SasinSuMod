package com.example.sasinsu.item;

import com.example.sasinsu.network.SkillSequenceManager;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
        if (player.isCrouching()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            activateSkill(serverPlayer, 1, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public void activateSkill(ServerPlayer player, int skill, ItemStack stack) {
        if (skill < 1 || skill > 4 || !(player.level() instanceof ServerLevel level)) return;
        String key = "sasinsu_cd_" + beast.name() + "_" + skill;
        long now = level.getGameTime();
        long readyAt = player.getPersistentData().getLong(key);
        if (now < readyAt) return;

        player.getPersistentData().putLong(key, now + cooldownTicks(skill));
        InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        // A visible wind-up gives every skill a readable start before its hit window.
        Vec3 windupOrigin = player.position().add(0, 0.15, 0);
        ring(level, windupOrigin, skill == 3 ? 2.8 : 1.55, windupParticle(), skill == 3 ? 40 : 24);
        burst(level, windupParticle(), player.getX(), player.getY() + 1.0, player.getZ(),
                skill == 3 ? 36 : 14, .35, .55, .35, .025);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, skill == 3 ? 1.5f : .8f, skill == 3 ? .55f : 1.15f);

        SkillSequenceManager.schedule(player, windupTicks(skill), caster -> {
            ItemStack currentStack = caster.getItemInHand(hand);
            if (currentStack.getItem() != this) return;

            ServerLevel impactLevel = (ServerLevel) caster.level();
            caster.swing(hand, true);
            Vec3 origin = caster.position().add(0, 1.0, 0);
            Vec3 look = caster.getLookAngle().normalize();

            switch (beast) {
                case AZURE_DRAGON -> dragonSkill(impactLevel, caster, origin, look, skill);
                case WHITE_TIGER -> tigerSkill(impactLevel, caster, origin, look, skill);
                case VERMILION_BIRD -> phoenixSkill(impactLevel, caster, origin, look, skill);
                case BLACK_TORTOISE -> tortoiseSkill(impactLevel, caster, origin, look, skill);
            }

            impactLevel.playSound(null, caster.blockPosition(), soundFor(skill),
                    SoundSource.PLAYERS, skill == 3 ? 2.4f : 1.5f,
                    skill == 3 ? 0.65f : 0.9f + impactLevel.random.nextFloat() * 0.2f);

            // A short recovery pulse marks the end of the combo and keeps the impact readable.
            Vec3 recoveryCenter = origin.add(look.scale(skill == 3 ? 6.0 : skill == 2 ? 3.5 : 2.2));
            SkillSequenceManager.schedule(caster, skill == 3 ? 7 : 3, recovered -> {
                if (!(recovered.level() instanceof ServerLevel recoveryLevel)) return;
                ring(recoveryLevel, recoveryCenter, skill == 3 ? 3.4 : 1.8,
                        windupParticle(), skill == 3 ? 36 : 20);
                burst(recoveryLevel, windupParticle(), recoveryCenter.x, recoveryCenter.y,
                        recoveryCenter.z, skill == 3 ? 28 : 12, .45, .35, .45, .018);
            });

            if (currentStack.isDamageableItem()) {
                currentStack.hurtAndBreak(1, caster, p -> p.broadcastBreakEvent(hand));
            }
        });
    }

    private int windupTicks(int skill) {
        return switch (skill) {
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 16;
            default -> 3;
        };
    }

    private net.minecraft.core.particles.SimpleParticleType windupParticle() {
        return switch (beast) {
            case AZURE_DRAGON -> ParticleTypes.ELECTRIC_SPARK;
            case WHITE_TIGER -> ParticleTypes.CRIT;
            case VERMILION_BIRD -> ParticleTypes.FLAME;
            case BLACK_TORTOISE -> ParticleTypes.SNOWFLAKE;
        };
    }

    private int cooldownTicks(int skill) {
        return switch (skill) {
            case 1 -> 35;
            case 2 -> 100;
            case 3 -> 320;
            default -> 75;
        };
    }

    private net.minecraft.sounds.SoundEvent soundFor(int skill) {
        if (skill == 3) return SoundEvents.LIGHTNING_BOLT_THUNDER;
        return switch (beast) {
            case AZURE_DRAGON -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case WHITE_TIGER -> SoundEvents.PLAYER_ATTACK_STRONG;
            case VERMILION_BIRD -> SoundEvents.FIRECHARGE_USE;
            case BLACK_TORTOISE -> SoundEvents.SHIELD_BLOCK;
        };
    }

    private void dragonSkill(ServerLevel level, ServerPlayer player, Vec3 o, Vec3 d, int skill) {
        if (skill == 1) {
            for (int i = 1; i <= 12; i++) {
                Vec3 p = o.add(d.scale(i * 0.75));
                ring(level, p, 0.8 + i * 0.08, ParticleTypes.ELECTRIC_SPARK, 18);
                burst(level, ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 10, .3, .3, .3, .02);
                damage(level, player, p, 1.15, 4.5f);
            }
            scheduleFollowupStrikes(player, o.add(d.scale(5.0)), d, 1.5f, 1.35);
        } else if (skill == 2) {
            Vec3 center = o.add(d.scale(4));
            for (int wave = 0; wave < 5; wave++) {
                double radius = 1.2 + wave * .65;
                ring(level, center.add(0, wave * .28, 0), radius, ParticleTypes.ELECTRIC_SPARK, 48);
                ring(level, center.add(0, wave * .28, 0), radius * .78, ParticleTypes.DRAGON_BREATH, 36);
            }
            damage(level, player, center, 4.8, 10.0f);
            burst(level, ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, .5, .5, .5, 0);
            scheduleFollowupStrikes(player, center, d, 3.0f, 3.8);
        } else if (skill == 3) {
            Vec3 center = o.add(d.scale(8));
            for (int y = 0; y < 9; y++) {
                double radius = 4.8 - y * .35;
                ring(level, center.add(0, y * .48, 0), radius, ParticleTypes.ELECTRIC_SPARK, 72);
                ring(level, center.add(0, y * .48, 0), radius * .72, ParticleTypes.SOUL_FIRE_FLAME, 54);
            }
            burst(level, ParticleTypes.DRAGON_BREATH, center.x, center.y + 2, center.z, 500, 3.8, 2.8, 3.8, .12);
            burst(level, ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 8, 2, 1, 2, 0);
            damage(level, player, center, 8.0, 24.0f);
            damage(level, player, o.add(d.scale(4)), 5.0, 12.0f);
            scheduleUltimateAftershocks(player, center);
        } else {
            Vec3 end = o.add(d.scale(7));
            burst(level, ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 180, 1.1, 1, 1.1, .2);
            player.teleportTo(end.x, end.y, end.z);
            burst(level, ParticleTypes.DRAGON_BREATH, end.x, end.y, end.z, 120, 1.2, 1, 1.2, .08);
            damage(level, player, end, 3.2, 8.0f);
        }
    }

    /**
     * Adds two delayed hit beats to basic and power skills. These waves deal real
     * server-side damage as well as particles, so the follow-through is not cosmetic only.
     */
    private void scheduleFollowupStrikes(ServerPlayer player, Vec3 center, Vec3 direction,
                                         float damagePerWave, double radius) {
        net.minecraft.core.particles.SimpleParticleType particle = switch (beast) {
            case AZURE_DRAGON -> ParticleTypes.ELECTRIC_SPARK;
            case WHITE_TIGER -> ParticleTypes.CRIT;
            case VERMILION_BIRD -> ParticleTypes.FLAME;
            case BLACK_TORTOISE -> ParticleTypes.SNOWFLAKE;
        };
        for (int beat = 1; beat <= 2; beat++) {
            final int stage = beat;
            SkillSequenceManager.schedule(player, stage * 4, caster -> {
                if (!(caster.level() instanceof ServerLevel impactLevel)) return;
                Vec3 strikeCenter = center.add(direction.scale(stage * 0.55));
                ring(impactLevel, strikeCenter, radius + stage * 0.25, particle, 36);
                ring(impactLevel, strikeCenter, radius * 0.68, particle, 24);
                burst(impactLevel, particle, strikeCenter.x, strikeCenter.y, strikeCenter.z,
                        30, radius * 0.3, 0.35, radius * 0.3, 0.025);
                damage(impactLevel, caster, strikeCenter, radius, damagePerWave);
                impactLevel.playSound(null, caster.blockPosition(), soundFor(1),
                        SoundSource.PLAYERS, 0.65f, 0.9f + stage * 0.12f);
            });
        }
    }

    /** Three delayed shockwaves turn each ultimate into a readable multi-stage finisher. */
    private void scheduleUltimateAftershocks(ServerPlayer player, Vec3 center) {
        for (int phase = 1; phase <= 3; phase++) {
            final int stage = phase;
            SkillSequenceManager.schedule(player, phase * 4, caster -> {
                if (!(caster.level() instanceof ServerLevel impactLevel)) return;
                double radius = 2.6 + stage * 1.35;
                net.minecraft.core.particles.SimpleParticleType particle = switch (beast) {
                    case AZURE_DRAGON -> ParticleTypes.ELECTRIC_SPARK;
                    case WHITE_TIGER -> ParticleTypes.CRIT;
                    case VERMILION_BIRD -> ParticleTypes.FLAME;
                    case BLACK_TORTOISE -> ParticleTypes.SNOWFLAKE;
                };
                Vec3 waveCenter = center.add(0, stage * 0.22, 0);
                ring(impactLevel, waveCenter, radius, particle, 48);
                ring(impactLevel, waveCenter, radius * 0.72,
                        beast == BEAST.VERMILION_BIRD ? ParticleTypes.SOUL_FIRE_FLAME : particle, 36);
                burst(impactLevel, particle, waveCenter.x, waveCenter.y, waveCenter.z,
                        48, radius * 0.28, 0.45, radius * 0.28, 0.035);
                if (stage == 3) {
                    burst(impactLevel, ParticleTypes.EXPLOSION, waveCenter.x, waveCenter.y,
                            waveCenter.z, 5, 1.8, 0.8, 1.8, 0.02);
                }
            });
        }
    }

    private void tigerSkill(ServerLevel level, ServerPlayer player, Vec3 o, Vec3 d, int skill) {
        Vec3 center = o.add(d.scale(skill == 1 ? 3 : skill == 2 ? 4 : 6));
        if (skill == 1) {
            tigerClaw(level, player, center, 2.7, 6.0f);
            scheduleFollowupStrikes(player, center, d, 2.0f, 2.5);
        } else if (skill == 2) {
            for (int n = 0; n < 3; n++) {
                Vec3 wave = center.add(d.scale(n * 1.1));
                ring(level, wave, 2.0 + n * .6, ParticleTypes.CRIT, 56);
                damage(level, player, wave, 2.6, 5.0f);
            }
            burst(level, ParticleTypes.CLOUD, center.x, center.y, center.z, 150, 2, .5, 2, .12);
            scheduleFollowupStrikes(player, center, d, 2.2f, 2.8);
        } else if (skill == 3) {
            for (int n = 0; n < 7; n++) {
                double r = 2.0 + n * .72;
                ring(level, center.add(0, n * .12, 0), r, ParticleTypes.SWEEP_ATTACK, 64);
                ring(level, center.add(0, n * .12, 0), r * .82, ParticleTypes.CRIT, 64);
            }
            burst(level, ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 12, 2.5, 1, 2.5, 0);
            burst(level, ParticleTypes.CLOUD, center.x, center.y, center.z, 450, 4, 1.2, 4, .2);
            damage(level, player, center, 9.0, 26.0f);
            scheduleUltimateAftershocks(player, center);
        } else {
            for (int n = 0; n < 4; n++) {
                Vec3 slash = o.add(d.scale(1.5 + n * 1.2));
                tigerClaw(level, player, slash, 2.0, 3.5f);
            }
        }
    }

    private void tigerClaw(ServerLevel level, ServerPlayer player, Vec3 center, double radius, float power) {
        for (int claw = -1; claw <= 1; claw++) {
            Vec3 c = center.add(claw * .8, claw * .18, 0);
            ring(level, c, radius, ParticleTypes.SWEEP_ATTACK, 28);
            burst(level, ParticleTypes.CRIT, c.x, c.y, c.z, 28, .45, .35, .45, .12);
        }
        damage(level, player, center, radius, power);
    }

    private void phoenixSkill(ServerLevel level, ServerPlayer player, Vec3 o, Vec3 d, int skill) {
        Vec3 center = o.add(d.scale(skill == 3 ? 7 : 3.5));
        if (skill == 1) {
            for (int i = 1; i <= 7; i++) {
                Vec3 p = o.add(d.scale(i));
                burst(level, ParticleTypes.FLAME, p.x, p.y, p.z, 28, .35, .35, .35, .06);
                burst(level, ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 8, .2, .2, .2, .03);
                damage(level, player, p, 1.15, 3.5f);
            }
            scheduleFollowupStrikes(player, o.add(d.scale(4.0)), d, 1.5f, 1.5);
        } else if (skill == 2) {
            for (int n = 0; n < 5; n++) {
                Vec3 p = center.add(0, n * .55, 0);
                ring(level, p, 1.5 + n * .55, ParticleTypes.FLAME, 48);
                burst(level, ParticleTypes.LAVA, p.x, p.y, p.z, 24, .8, .5, .8, .03);
            }
            damage(level, player, center, 5.0, 11.0f);
            scheduleFollowupStrikes(player, center, d, 2.8f, 4.2);
        } else if (skill == 3) {
            for (int y = 0; y < 10; y++) {
                double radius = 5.5 - y * .38;
                ring(level, center.add(0, y * .42, 0), radius, ParticleTypes.FLAME, 80);
                ring(level, center.add(0, y * .42, 0), radius * .72, ParticleTypes.SOUL_FIRE_FLAME, 64);
            }
            burst(level, ParticleTypes.LAVA, center.x, center.y + 1, center.z, 180, 3.5, 2, 3.5, .04);
            burst(level, ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 10, 2, 1, 2, 0);
            damage(level, player, center, 9.0, 25.0f);
            scheduleUltimateAftershocks(player, center);
        } else {
            Vec3 end = o.add(d.scale(6));
            player.teleportTo(end.x, end.y, end.z);
            burst(level, ParticleTypes.FLAME, end.x, end.y, end.z, 180, 1.2, 1, 1.2, .1);
            burst(level, ParticleTypes.SOUL_FIRE_FLAME, end.x, end.y, end.z, 70, .8, .8, .8, .06);
            damage(level, player, end, 3.0, 8.0f);
        }
    }

    private void tortoiseSkill(ServerLevel level, ServerPlayer player, Vec3 o, Vec3 d, int skill) {
        if (skill == 1) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            ring(level, o, 2.5, ParticleTypes.SOUL_FIRE_FLAME, 64);
            ring(level, o.add(0, .8, 0), 2.0, ParticleTypes.ENCHANT, 48);
        } else if (skill == 2) {
            Vec3 center = o.add(d.scale(3.5));
            for (int n = 0; n < 5; n++) ring(level, center.add(0, n * .35, 0),
                    1.8 + n * .55, ParticleTypes.SNOWFLAKE, 56);
            burst(level, ParticleTypes.CLOUD, center.x, center.y, center.z, 180, 2, .6, 2, .08);
            damage(level, player, center, 4.8, 9.0f);
            scheduleFollowupStrikes(player, center, d, 2.4f, 3.8);
        } else if (skill == 3) {
            Vec3 center = o.add(d.scale(5));
            for (int y = 0; y < 8; y++) {
                double radius = 5.0 - y * .42;
                ring(level, center.add(0, y * .38, 0), radius, ParticleTypes.SNOWFLAKE, 72);
                ring(level, center.add(0, y * .38, 0), radius * .78, ParticleTypes.SOUL_FIRE_FLAME, 64);
            }
            burst(level, ParticleTypes.TOTEM_OF_UNDYING, center.x, center.y + 1, center.z, 220, 3, 2, 3, .14);
            burst(level, ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 8, 2, 1, 2, 0);
            damage(level, player, center, 8.0, 20.0f);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 3));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 240, 3));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 2));
            scheduleUltimateAftershocks(player, center);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 3));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 1));
            ring(level, o, 3.4, ParticleTypes.SNOWFLAKE, 96);
            burst(level, ParticleTypes.TOTEM_OF_UNDYING, o.x, o.y + .7, o.z, 100, 1.2, 1, 1.2, .12);
            damage(level, player, o.add(d.scale(2)), 2.8, 5.0f);
        }
    }

    private void ring(ServerLevel level, Vec3 center, double radius,
                      net.minecraft.core.particles.SimpleParticleType particle, int count) {
        count = Math.min(count, 48);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            burst(level, particle, center.x + Math.cos(angle) * radius, center.y + .08,
                    center.z + Math.sin(angle) * radius, 1, 0, .02, 0, 0);
        }
    }

    /** Caps one particle burst so a single skill cannot flood nearby clients. */
    private void burst(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                       int count, double xOffset, double yOffset, double zOffset, double speed) {
        int budgetedCount = Math.max(0, Math.min(count, 96));
        if (budgetedCount == 0) return;
        level.sendParticles(particle, x, y, z, budgetedCount,
                xOffset, yOffset, zOffset, speed);
    }

    private void damage(ServerLevel level, ServerPlayer player, Vec3 center, double radius, float amount) {
        AABB area = new AABB(center, center).inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player));
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), amount);
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(0.85);
            target.push(knockback.x, 0.28, knockback.z);
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
