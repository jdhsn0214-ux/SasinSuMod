package com.example.sasinsu.item;

import com.example.sasinsu.network.SkillSequenceManager;
import com.example.sasinsu.network.TortoiseGuardHandler;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
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
        if (now < readyAt) {
            // Give immediate, readable feedback instead of silently ignoring a cooldown input.
            String feedbackKey = "sasinsu_cd_feedback_" + beast.name() + "_" + skill;
            long feedbackReadyAt = player.getPersistentData().getLong(feedbackKey);
            if (now >= feedbackReadyAt) {
                long remainingSeconds = (readyAt - now + 19L) / 20L;
                player.displayClientMessage(
                        Component.literal("§c재사용 대기 중: " + remainingSeconds + "초"), true);
                player.getPersistentData().putLong(feedbackKey, now + 10L);
            }
            return;
        }

        player.getPersistentData().putLong(key, now + cooldownTicks(skill));
        InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

        // Each beast has its own wind-up silhouette instead of sharing one generic ring.
        presentWindup(level, player, skill);

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

            // Weapon-specific recovery tells the player which fighting style just resolved.
            Vec3 recoveryCenter = origin.add(look.scale(skill == 3 ? 6.0 : skill == 2 ? 3.5 : 2.2));
            SkillSequenceManager.schedule(caster, skill == 3 ? 7 : 3, recovered -> {
                if (recovered.level() instanceof ServerLevel recoveryLevel) {
                    presentRecovery(recoveryLevel, recovered, recoveryCenter, skill);
                }
            });

            if (currentStack.isDamageableItem()) {
                currentStack.hurtAndBreak(1, caster, p -> p.broadcastBreakEvent(hand));
            }
        });
    }

    private void presentWindup(ServerLevel level, ServerPlayer player, int skill) {
        Vec3 base = player.position().add(0, 0.18, 0);
        Vec3 look = player.getLookAngle().normalize();
        switch (beast) {
            case AZURE_DRAGON -> {
                // A forward lightning lane: the sword gathers power in its attack direction.
                for (int i = 1; i <= 5; i++) {
                    Vec3 point = base.add(look.scale(i * 0.42)).add(0, 0.35 * Math.sin(i), 0);
                    burst(level, ParticleTypes.ELECTRIC_SPARK, point.x, point.y + 1, point.z,
                            skill == 3 ? 12 : 5, .12, .28, .12, .035);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER,
                        SoundSource.PLAYERS, skill == 3 ? 1.5f : .8f, .8f);
            }
            case WHITE_TIGER -> {
                // Three claw marks close toward the target, emphasizing a heavy melee weapon.
                for (int claw = -1; claw <= 1; claw++) {
                    Vec3 point = base.add(look.scale(1.0)).add(-look.z * claw * .65, .9, look.x * claw * .65);
                    burst(level, ParticleTypes.CRIT, point.x, point.y, point.z,
                            skill == 3 ? 24 : 12, .28, .3, .28, .12);
                    burst(level, ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.AXE_STRIP,
                        SoundSource.PLAYERS, skill == 3 ? 1.4f : .85f, .65f);
            }
            case VERMILION_BIRD -> {
                // A rising ember spiral, not a ground ring.
                for (int i = 0; i < 7; i++) {
                    double angle = i * Math.PI / 2.4;
                    double radius = .45 + i * .09;
                    double x = player.getX() + Math.cos(angle) * radius;
                    double z = player.getZ() + Math.sin(angle) * radius;
                    burst(level, ParticleTypes.FLAME, x, player.getY() + .25 + i * .25, z,
                            skill == 3 ? 10 : 5, .08, .12, .08, .025);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                        SoundSource.PLAYERS, skill == 3 ? 1.3f : .8f, 1.15f);
            }
            case BLACK_TORTOISE -> {
                // Two compact shell layers form a defensive tell around the wielder.
                ring(level, base, skill == 3 ? 2.5 : 1.45, ParticleTypes.SNOWFLAKE, 32);
                ring(level, base.add(0, .65, 0), skill == 3 ? 2.0 : 1.1, ParticleTypes.SOUL_FIRE_FLAME, 24);
                level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                        SoundSource.PLAYERS, skill == 3 ? 1.35f : .8f, .7f);
            }
        }
    }

    private void presentRecovery(ServerLevel level, ServerPlayer player, Vec3 center, int skill) {
        switch (beast) {
            case AZURE_DRAGON -> {
                for (int i = 0; i < 5; i++) {
                    Vec3 p = center.add(0, i * .28, 0);
                    burst(level, ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 8, .18, .18, .18, .06);
                }
            }
            case WHITE_TIGER -> {
                burst(level, ParticleTypes.CLOUD, center.x, center.y, center.z,
                        skill == 3 ? 48 : 22, .9, .18, .9, .12);
                burst(level, ParticleTypes.CRIT, center.x, center.y + .25, center.z,
                        skill == 3 ? 32 : 14, .65, .2, .65, .08);
            }
            case VERMILION_BIRD -> {
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI / 3.0;
                    double x = center.x + Math.cos(angle) * .8;
                    double z = center.z + Math.sin(angle) * .8;
                    burst(level, ParticleTypes.SOUL_FIRE_FLAME, x, center.y + i * .22, z,
                            7, .08, .12, .08, .025);
                }
            }
            case BLACK_TORTOISE -> {
                ring(level, center, skill == 3 ? 3.2 : 1.7, ParticleTypes.SNOWFLAKE, 40);
                ring(level, center.add(0, .45, 0), skill == 3 ? 2.5 : 1.25,
                        ParticleTypes.SOUL_FIRE_FLAME, 28);
            }
        }
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
            // One real vanilla lightning entity punctuates the forward lightning cut.
            summonDragonLightning(level, o.add(d.scale(5.0)));
            for (int i = 1; i <= 12; i++) {
                Vec3 p = o.add(d.scale(i * 0.75));
                ring(level, p, 0.8 + i * 0.08, ParticleTypes.ELECTRIC_SPARK, 18);
                burst(level, ParticleTypes.DRAGON_BREATH, p.x, p.y, p.z, 10, .3, .3, .3, .02);
                damage(level, player, p, 1.15, 4.5f);
            }
            scheduleWeaponFollowthrough(player, o.add(d.scale(5.0)), d, 1.5f, 1.35);
        } else if (skill == 2) {
            Vec3 center = o.add(d.scale(4));
            // Three genuine lightning strikes land in sequence across the impact zone.
            for (int strike = -1; strike <= 1; strike++) {
                final int lane = strike;
                SkillSequenceManager.schedule(player, (strike + 1) * 3, caster -> {
                    if (!(caster.level() instanceof ServerLevel strikeLevel)) return;
                    Vec3 side = new Vec3(-d.z, 0, d.x).scale(lane * 1.25);
                    summonDragonLightning(strikeLevel, center.add(side));
                });
            }
            for (int wave = 0; wave < 5; wave++) {
                double radius = 1.2 + wave * .65;
                ring(level, center.add(0, wave * .28, 0), radius, ParticleTypes.ELECTRIC_SPARK, 48);
                ring(level, center.add(0, wave * .28, 0), radius * .78, ParticleTypes.DRAGON_BREATH, 36);
            }
            damage(level, player, center, 4.8, 10.0f);
            burst(level, ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 2, .5, .5, .5, 0);
            scheduleWeaponFollowthrough(player, center, d, 3.0f, 3.8);
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
            // Cloudstep: three short lightning blinks form a chained repositioning attack.
            for (int beat = 1; beat <= 3; beat++) {
                final int stage = beat;
                SkillSequenceManager.schedule(player, stage * 2, caster -> {
                    if (!(caster.level() instanceof ServerLevel dashLevel)) return;
                    Vec3 start = caster.position().add(0, 1.0, 0);
                    Vec3 end = dashPlayer(dashLevel, caster, d, 2.35);
                    burst(dashLevel, ParticleTypes.ELECTRIC_SPARK,
                            start.x, start.y, start.z, 54, .65, .7, .65, .12);
                    burst(dashLevel, ParticleTypes.DRAGON_BREATH,
                            end.x, end.y + 1.0, end.z, 42, .7, .65, .7, .055);
                    damage(dashLevel, caster, end.add(0, .8, 0), 2.15, 3.5f);
                    summonDragonLightning(dashLevel, end);
                    dashLevel.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THUNDER,
                            SoundSource.PLAYERS, .55f, 1.25f + stage * .12f);
                });
            }
        }
    }

    /**
     * Weapon-specific follow-through. These are intentionally different combat
     * beats rather than one shared ring-and-burst routine.
     */
    private void scheduleWeaponFollowthrough(ServerPlayer player, Vec3 center, Vec3 direction,
                                             float damagePerWave, double radius) {
        switch (beast) {
            case AZURE_DRAGON -> {
                // Dragon: two advancing lightning cuts travel down the sword's line.
                for (int beat = 1; beat <= 2; beat++) {
                    final int stage = beat;
                    SkillSequenceManager.schedule(player, stage * 4, caster -> {
                        if (!(caster.level() instanceof ServerLevel impactLevel)) return;
                        Vec3 start = center.add(direction.scale(stage * 1.1));
                        for (int segment = 0; segment < 5; segment++) {
                            Vec3 point = start.add(direction.scale(segment * .42))
                                    .add(0, Math.sin(segment * 1.3 + stage) * .35, 0);
                            burst(impactLevel, ParticleTypes.ELECTRIC_SPARK,
                                    point.x, point.y, point.z, 7, .08, .16, .08, .025);
                        }
                        damage(impactLevel, caster, start.add(direction.scale(.8)), radius * .8,
                                damagePerWave);
                        impactLevel.playSound(null, caster.blockPosition(), SoundEvents.TRIDENT_THUNDER,
                                SoundSource.PLAYERS, .55f, 1.15f + stage * .12f);
                    });
                }
            }
            case WHITE_TIGER -> {
                // Tiger: acquire a target in the forward cone, rush it, then perform a heavy cleave.
                SkillSequenceManager.schedule(player, 5, caster -> {
                    if (!(caster.level() instanceof ServerLevel impactLevel)) return;
                    Vec3 facing = caster.getLookAngle().normalize();
                    LivingEntity chosen = null;
                    double nearest = 9.0 * 9.0;
                    AABB search = caster.getBoundingBox().inflate(9.0, 4.0, 9.0);
                    for (LivingEntity candidate : impactLevel.getEntitiesOfClass(LivingEntity.class,
                            search, entity -> entity != caster && entity.isAlive()
                                    && !entity.isAlliedTo(caster))) {
                        Vec3 offset = candidate.position().subtract(caster.position());
                        double distance = offset.lengthSqr();
                        if (distance < nearest && distance > .1
                                && facing.dot(offset.normalize()) > .25) {
                            nearest = distance;
                            chosen = candidate;
                        }
                    }
                    if (chosen == null) {
                        tigerClaw(impactLevel, caster, center.add(direction.scale(1.0)),
                                radius * .85, damagePerWave);
                        return;
                    }
                    Vec3 targetPoint = chosen.position();
                    Vec3 rush = targetPoint.subtract(caster.position()).normalize();
                    double rushDistance = Math.max(1.5, Math.min(3.5, Math.sqrt(nearest) - 1.2));
                    Vec3 end = dashPlayer(impactLevel, caster, rush, rushDistance);
                    burst(impactLevel, ParticleTypes.CLOUD, end.x, end.y + .1, end.z,
                            28, .55, .18, .55, .08);
                    tigerClaw(impactLevel, caster, chosen.position(), radius, damagePerWave + 1.5f);
                    impactLevel.playSound(null, chosen.blockPosition(), SoundEvents.AXE_STRIP,
                            SoundSource.PLAYERS, .8f, .7f);
                });
            }
            case VERMILION_BIRD -> {
                // Phoenix: rising three-hit aerial spiral; victims are lifted and scorched.
                for (int beat = 1; beat <= 3; beat++) {
                    final int stage = beat;
                    SkillSequenceManager.schedule(player, stage * 3, caster -> {
                        if (!(caster.level() instanceof ServerLevel impactLevel)) return;
                        double angle = stage * Math.PI * .72;
                        Vec3 strike = center.add(Math.cos(angle) * radius * .45,
                                stage * .72, Math.sin(angle) * radius * .45);
                        ring(impactLevel, strike, .85 + stage * .28, ParticleTypes.FLAME, 28);
                        burst(impactLevel, ParticleTypes.SOUL_FIRE_FLAME,
                                strike.x, strike.y, strike.z, 18, .28, .32, .28, .035);
                        AABB area = new AABB(strike, strike).inflate(radius * .65, 1.0, radius * .65);
                        for (LivingEntity target : impactLevel.getEntitiesOfClass(LivingEntity.class,
                                area, entity -> entity != caster && entity.isAlive()
                                        && !entity.isAlliedTo(caster))) {
                            if (target.hurt(impactLevel.damageSources().playerAttack(caster), damagePerWave)) {
                                presentHitFeedback(impactLevel, target);
                            }
                            target.setSecondsOnFire(2);
                            target.push(0, .28 + stage * .04, 0);
                        }
                    });
                }
            }
            case BLACK_TORTOISE -> {
                // Tortoise: a delayed freezing counter-pulse, followed by a brief guard refresh.
                SkillSequenceManager.schedule(player, 6, caster -> {
                    if (!(caster.level() instanceof ServerLevel impactLevel)) return;
                    Vec3 pulse = center.add(0, -.25, 0);
                    ring(impactLevel, pulse, radius + 1.0, ParticleTypes.SNOWFLAKE, 40);
                    ring(impactLevel, pulse.add(0, .55, 0), radius * .6,
                            ParticleTypes.SOUL_FIRE_FLAME, 28);
                    AABB area = new AABB(pulse, pulse).inflate(radius + .5, 1.5, radius + .5);
                    for (LivingEntity target : impactLevel.getEntitiesOfClass(LivingEntity.class,
                            area, entity -> entity != caster && entity.isAlive()
                                    && !entity.isAlliedTo(caster))) {
                        if (target.hurt(impactLevel.damageSources().playerAttack(caster), damagePerWave)) {
                            presentHitFeedback(impactLevel, target);
                        }
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                        target.push(0, .08, 0);
                    }
                    caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1));
                    impactLevel.playSound(null, caster.blockPosition(), SoundEvents.SHIELD_BLOCK,
                            SoundSource.PLAYERS, .9f, .65f);
                });
            }
        }
    }

    /**
     * Each beast resolves its ultimate with a different three-beat signature:
     * dragon = descending lightning lanes, tiger = converging claw impacts,
     * phoenix = rising expanding fire, tortoise = layered defensive pulses.
     */
    private void scheduleUltimateAftershocks(ServerPlayer player, Vec3 center) {
        for (int phase = 1; phase <= 3; phase++) {
            final int stage = phase;
            SkillSequenceManager.schedule(player, phase * 4, caster -> {
                if (!(caster.level() instanceof ServerLevel impactLevel)) return;
                double radius = 2.0 + stage * 1.25;
                switch (beast) {
                    case AZURE_DRAGON -> {
                        // Three lightning lanes descend visibly, segment by segment, then strike the ground.
                        for (int lane = -1; lane <= 1; lane++) {
                            Vec3 strike = center.add(lane * 1.15, 0, stage * .35);
                            for (int segment = 0; segment < 5; segment++) {
                                final int height = segment;
                                SkillSequenceManager.schedule(caster, 1 + (4 - height), boltCaster -> {
                                    if (!(boltCaster.level() instanceof ServerLevel boltLevel)) return;
                                    Vec3 boltPoint = strike.add(0, height * .72, 0);
                                    burst(boltLevel, ParticleTypes.ELECTRIC_SPARK,
                                            boltPoint.x, boltPoint.y, boltPoint.z,
                                            14, .13, .24, .13, .045);
                                    if (height == 0) {
                                        summonDragonLightning(boltLevel, strike);
                                        ring(boltLevel, strike, .95 + stage * .3,
                                                ParticleTypes.ELECTRIC_SPARK, 40);
                                        burst(boltLevel, ParticleTypes.EXPLOSION,
                                                strike.x, strike.y, strike.z, 2, .35, .18, .35, .01);
                                        damage(boltLevel, boltCaster, strike, 1.35, 3.5f);
                                        boltLevel.playSound(null, boltCaster.blockPosition(),
                                                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS,
                                                .65f, 1.0f + stage * .08f);
                                    }
                                });
                            }
                        }
                        burst(impactLevel, ParticleTypes.DRAGON_BREATH, center.x, center.y,
                                center.z, 42, radius * .35, .5, radius * .35, .035);
                    }
                    case WHITE_TIGER -> {
                        // Four sweeping claw arcs collapse into one heavy central strike.
                        for (int claw = 0; claw < 4; claw++) {
                            double angle = Math.PI * 2 * claw / 4.0 + stage * .18;
                            Vec3 slash = center.add(Math.cos(angle) * radius,
                                    .15 * stage, Math.sin(angle) * radius);
                            ring(impactLevel, slash, 1.15 + stage * .18,
                                    ParticleTypes.SWEEP_ATTACK, 28);
                            burst(impactLevel, ParticleTypes.CRIT, slash.x, slash.y,
                                    slash.z, 12, .22, .22, .22, .1);
                        }
                        burst(impactLevel, ParticleTypes.CLOUD, center.x, center.y,
                                center.z, 34, radius * .3, .22, radius * .3, .1);
                    }
                    case VERMILION_BIRD -> {
                        // Each beat rises higher while the fiery radius expands.
                        Vec3 rising = center.add(0, stage * .9, 0);
                        ring(impactLevel, rising, radius, ParticleTypes.FLAME, 48);
                        ring(impactLevel, rising.add(0, .35, 0), radius * .68,
                                ParticleTypes.SOUL_FIRE_FLAME, 36);
                        burst(impactLevel, ParticleTypes.LAVA, rising.x, rising.y,
                                rising.z, 26, radius * .22, .25, radius * .22, .025);
                    }
                    case BLACK_TORTOISE -> {
                        // Interlocking shell layers pulse outward and reinforce the wielder.
                        Vec3 shell = center.add(0, stage * .25, 0);
                        ring(impactLevel, shell, radius, ParticleTypes.SNOWFLAKE, 48);
                        ring(impactLevel, shell.add(0, .7, 0), radius * .78,
                                ParticleTypes.SOUL_FIRE_FLAME, 40);
                        ring(impactLevel, shell.add(0, 1.35, 0), radius * .52,
                                ParticleTypes.ENCHANT, 32);
                        if (stage == 3) {
                            caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1));
                            caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80, 1));
                        }
                    }
                }
                if (stage == 3 && beast != BEAST.BLACK_TORTOISE) {
                    burst(impactLevel, ParticleTypes.EXPLOSION, center.x, center.y,
                            center.z, 3, 1.2, .6, 1.2, .015);
                }
            });
        }
    }


    /**
     * Spawns a real vanilla lightning bolt for the Azure Dragon's signature.
     * Visual-only keeps vanilla fire and incidental lightning damage disabled;
     * each skill retains its own explicit, balanced damage logic.
     */
    private void summonDragonLightning(ServerLevel level, Vec3 target) {
        BlockPos targetPos = BlockPos.containing(target);
        int x = targetPos.getX();
        int z = targetPos.getZ();
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.setVisualOnly(true);
        bolt.moveTo(x + 0.5, groundY, z + 0.5);
        level.addFreshEntity(bolt);
        burst(level, ParticleTypes.ELECTRIC_SPARK, x + 0.5, groundY + 0.2, z + 0.5,
                28, .45, .25, .45, .09);
    }

    private void tigerSkill(ServerLevel level, ServerPlayer player, Vec3 o, Vec3 d, int skill) {
        Vec3 center = o.add(d.scale(skill == 1 ? 3 : skill == 2 ? 4 : 6));
        if (skill == 1) {
            tigerClaw(level, player, center, 2.7, 6.0f);
            scheduleWeaponFollowthrough(player, center, d, 2.0f, 2.5);
        } else if (skill == 2) {
            for (int n = 0; n < 3; n++) {
                Vec3 wave = center.add(d.scale(n * 1.1));
                ring(level, wave, 2.0 + n * .6, ParticleTypes.CRIT, 56);
                damage(level, player, wave, 2.6, 5.0f);
            }
            burst(level, ParticleTypes.CLOUD, center.x, center.y, center.z, 150, 2, .5, 2, .12);
            scheduleWeaponFollowthrough(player, center, d, 2.2f, 2.8);
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
            // King's Roar: a close-range shockwave that scatters and weakens surrounding enemies.
            Vec3 roarCenter = player.position().add(0, .8, 0);
            ring(level, roarCenter, 2.2, ParticleTypes.SWEEP_ATTACK, 40);
            ring(level, roarCenter, 4.2, ParticleTypes.CLOUD, 48);
            burst(level, ParticleTypes.CRIT, roarCenter.x, roarCenter.y, roarCenter.z,
                    72, 2.0, .8, 2.0, .16);
            AABB roarArea = player.getBoundingBox().inflate(5.0, 2.5, 5.0);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, roarArea,
                    entity -> entity != player && entity.isAlive() && !entity.isAlliedTo(player))) {
                if (target.hurt(level.damageSources().playerAttack(player), 5.0f)) {
                    presentHitFeedback(level, target);
                }
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                Vec3 away = target.position().subtract(player.position()).normalize().scale(1.55);
                target.push(away.x, .42, away.z);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR,
                    SoundSource.PLAYERS, 1.25f, .75f);
        }
    }

    private void tigerClaw(ServerLevel level, ServerPlayer player, Vec3 center, double radius, float power) {
        // Keep the three claw lanes perpendicular to the wielder's facing direction.
        // Using a fixed world-X offset made the attack look sideways when the player turned.
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
        for (int claw = -1; claw <= 1; claw++) {
            Vec3 c = center.add(side.scale(claw * .8)).add(0, claw * .18, 0);
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
            scheduleWeaponFollowthrough(player, o.add(d.scale(4.0)), d, 1.5f, 1.5);
        } else if (skill == 2) {
            for (int n = 0; n < 5; n++) {
                Vec3 p = center.add(0, n * .55, 0);
                ring(level, p, 1.5 + n * .55, ParticleTypes.FLAME, 48);
                burst(level, ParticleTypes.LAVA, p.x, p.y, p.z, 24, .8, .5, .8, .03);
            }
            damage(level, player, center, 5.0, 11.0f);
            scheduleWeaponFollowthrough(player, center, d, 2.8f, 4.2);
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
            // Cinderstep: three spaced dashes leave a burning trail and finish with a heavier cross-cut.
            // The 10-tick gaps also give Minecraft's hurt cooldown time to expire between hits.
            for (int beat = 1; beat <= 3; beat++) {
                final int stage = beat;
                SkillSequenceManager.schedule(player, 2 + (stage - 1) * 10, caster -> {
                    if (!(caster.level() instanceof ServerLevel dashLevel)) return;
                    Vec3 start = caster.position().add(0, 1.0, 0);
                    Vec3 dashDirection = caster.getLookAngle().normalize();
                    Vec3 end = dashPlayer(dashLevel, caster, dashDirection, stage == 3 ? 2.8 : 2.2);

                    // A short, restrained trail reads as a moving afterimage without a giant particle dump.
                    for (int segment = 0; segment <= 4; segment++) {
                        Vec3 trail = start.lerp(end, segment / 4.0);
                        burst(dashLevel, ParticleTypes.FLAME, trail.x, trail.y, trail.z,
                                stage == 3 ? 12 : 8, .12, .2, .12, .035);
                    }
                    burst(dashLevel, ParticleTypes.SOUL_FIRE_FLAME,
                            start.x, start.y + .15, start.z, 28, .42, .55, .42, .055);
                    burst(dashLevel, ParticleTypes.SOUL_FIRE_FLAME,
                            end.x, end.y + .85, end.z, stage == 3 ? 72 : 48,
                            .72, .82, .72, .075);
                    if (stage == 3) {
                        burst(dashLevel, ParticleTypes.LAVA, end.x, end.y + .6, end.z,
                                18, .45, .35, .45, .025);
                    }

                    AABB strikeArea = new AABB(end, end).inflate(stage == 3 ? 2.4 : 1.85, 1.5, stage == 3 ? 2.4 : 1.85);
                    float strikeDamage = stage == 3 ? 7.0f : 4.0f;
                    for (LivingEntity target : dashLevel.getEntitiesOfClass(LivingEntity.class,
                            strikeArea, entity -> entity != caster && entity.isAlive()
                                    && !entity.isAlliedTo(caster))) {
                        if (target.hurt(dashLevel.damageSources().playerAttack(caster), strikeDamage)) {
                            presentHitFeedback(dashLevel, target);
                        }
                        target.setSecondsOnFire(stage == 3 ? 4 : 2);
                        target.push(dashDirection.x * .48, stage == 3 ? .3 : .18,
                                dashDirection.z * .48);
                    }

                    dashLevel.playSound(null, caster.blockPosition(), SoundEvents.FIRECHARGE_USE,
                            SoundSource.PLAYERS, stage == 3 ? 1.25f : .8f,
                            .78f + stage * .16f);
                });
            }
        }
    }

    /**
     * Moves the player forward without placing their bounding box inside solid blocks.
     * Shorter distances are tried when the full dash path is obstructed.
     */
    private Vec3 dashPlayer(ServerLevel level, ServerPlayer player, Vec3 direction, double distance) {
        // Checking only the destination can let a dash pass through a thin wall
        // when the space on the far side is clear. Validate the whole path in
        // half-block increments before moving the player.
        for (double step = distance; step >= 1.5; step -= 0.5) {
            boolean pathClear = true;
            for (double travelled = 0.5; travelled <= step; travelled += 0.5) {
                Vec3 offset = direction.scale(travelled);
                if (!level.noCollision(player, player.getBoundingBox().move(offset))) {
                    pathClear = false;
                    break;
                }
            }
            if (pathClear) {
                Vec3 destination = player.position().add(direction.scale(step));
                player.teleportTo(destination.x, destination.y, destination.z);
                return destination;
            }
        }
        return player.position();
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
            scheduleWeaponFollowthrough(player, center, d, 2.4f, 3.8);
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
            // Immutable Counter: enter a real guard stance instead of performing another generic AoE.
            player.getPersistentData().putLong(TortoiseGuardHandler.GUARD_UNTIL,
                    level.getGameTime() + 70L);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12, 1));
            ring(level, o, 2.6, ParticleTypes.SNOWFLAKE, 48);
            ring(level, o.add(0, .75, 0), 2.0, ParticleTypes.ENCHANT, 36);
            burst(level, ParticleTypes.TOTEM_OF_UNDYING, o.x, o.y + .7, o.z,
                    48, .75, .9, .75, .08);
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK,
                    SoundSource.PLAYERS, 1.15f, .55f);
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
            if (target.hurt(level.damageSources().playerAttack(player), amount)) {
                presentHitFeedback(level, target);
            }
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(0.85);
            target.push(knockback.x, 0.28, knockback.z);
        }
    }

    /**
     * Compact, weapon-specific confirmation at the exact moment damage is accepted.
     * This keeps a successful hit readable without adding another large area burst.
     */
    private void presentHitFeedback(ServerLevel level, LivingEntity target) {
        Vec3 impact = target.position().add(0, Math.max(0.4, target.getBbHeight() * 0.55), 0);
        switch (beast) {
            case AZURE_DRAGON -> {
                burst(level, ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z,
                        14, .24, .28, .24, .08);
                burst(level, ParticleTypes.DRAGON_BREATH, impact.x, impact.y, impact.z,
                        5, .16, .2, .16, .025);
            }
            case WHITE_TIGER -> {
                burst(level, ParticleTypes.CRIT, impact.x, impact.y, impact.z,
                        12, .28, .24, .28, .12);
                burst(level, ParticleTypes.SWEEP_ATTACK, impact.x, impact.y, impact.z,
                        1, 0, 0, 0, 0);
            }
            case VERMILION_BIRD -> {
                burst(level, ParticleTypes.FLAME, impact.x, impact.y, impact.z,
                        12, .22, .28, .22, .055);
                burst(level, ParticleTypes.SOUL_FIRE_FLAME, impact.x, impact.y, impact.z,
                        5, .16, .2, .16, .035);
            }
            case BLACK_TORTOISE -> {
                burst(level, ParticleTypes.SNOWFLAKE, impact.x, impact.y, impact.z,
                        14, .26, .3, .26, .045);
                burst(level, ParticleTypes.ENCHANT, impact.x, impact.y, impact.z,
                        8, .2, .24, .2, .12);
            }
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
