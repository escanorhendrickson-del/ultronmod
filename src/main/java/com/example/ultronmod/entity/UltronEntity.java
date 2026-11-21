package com.example.ultronmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Ultron boss.
 *  - Repulsor beam (hand), Unibeam (chest, charged), Ground Slam (shockwave)
 *  - Phase 2 "Overdrive" below 50% HP: faster, harder hitting, smoke trail, lightning
 */
public class UltronEntity extends Monster implements RangedAttackMob {

    public static final int STATE_IDLE = 0;
    public static final int STATE_UNIBEAM = 1;
    public static final int STATE_SLAM_WINDUP = 2;
    public static final int STATE_SLAM_STRIKE = 3;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(UltronEntity.class, EntityDataSerializers.INT);

    private static final UUID OVERDRIVE_SPEED = UUID.fromString("5b0d6a52-6c0e-4f4a-9a0a-2b7c4d1e8f01");
    private static final UUID OVERDRIVE_DAMAGE = UUID.fromString("c2a4f3b1-1d7e-4a62-8f55-9e3b6a4d7c02");

    private static final DustParticleOptions AMBIENT_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.08F), 0.8F);
    private static final DustParticleOptions BEAM_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.3F, 0.15F), 1.3F);
    private static final DustParticleOptions UNIBEAM_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.1F), 2.6F);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.ultronmod.ultron"),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);

    private boolean overdrive;
    private int slamCooldown = 60;
    private int unibeamCooldown = 100;

    public UltronEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, STATE_IDLE);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int state) {
        this.entityData.set(STATE, state);
    }

    /** True below 50% health. Works on both client and server. */
    public boolean isEnraged() {
        return this.getHealth() <= this.getMaxHealth() * 0.5F;
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SlamGoal(this));
        this.goalSelector.addGoal(2, new UnibeamGoal(this));
        this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0D, 30, 20.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.setAggressive(this.getTarget() != null);

        if (this.slamCooldown > 0) this.slamCooldown--;
        if (this.unibeamCooldown > 0) this.unibeamCooldown--;

        if (this.tickCount == 1) {
            this.spawnIntro();
        }
        if (this.tickCount % 40 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(2.0F);
        }
        if (!this.overdrive && this.isEnraged()) {
            this.enterOverdrive();
        }
    }

    private void spawnIntro() {
        if (this.level() instanceof ServerLevel sl) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(this.getX(), this.getY(), this.getZ());
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.2, this.getZ(),
                    60, 0.6, 1.2, 0.6, 0.3);
        }
        this.playSound(SoundEvents.WITHER_SPAWN, 0.6F, 1.4F);
    }

    private void enterOverdrive() {
        this.overdrive = true;
        applyOverdriveModifiers();
        this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        if (this.level() instanceof ServerLevel sl) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(this.getX(), this.getY(), this.getZ());
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.5, this.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.5, this.getZ(),
                    120, 1.0, 1.2, 1.0, 0.4);
        }
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.6F);
    }

    private void applyOverdriveModifiers() {
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(OVERDRIVE_SPEED) == null) {
            speed.addPermanentModifier(new AttributeModifier(OVERDRIVE_SPEED, "Ultron overdrive speed",
                    0.3D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        AttributeInstance dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(OVERDRIVE_DAMAGE) == null) {
            dmg.addPermanentModifier(new AttributeModifier(OVERDRIVE_DAMAGE, "Ultron overdrive damage",
                    0.5D, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    // ------------------------------------------------------------- helpers

    private Vec3 chestPos() {
        float yaw = this.yBodyRot * ((float) Math.PI / 180.0F);
        return new Vec3(this.getX() - Mth.sin(yaw) * 0.55, this.getY() + 1.75, this.getZ() + Mth.cos(yaw) * 0.55);
    }

    private Vec3 handPos() {
        float yaw = this.yBodyRot * ((float) Math.PI / 180.0F);
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        return new Vec3(this.getX() + rx * 0.55 + fx * 0.9, this.getY() + 1.8, this.getZ() + rz * 0.55 + fz * 0.9);
    }

    // -------------------------------------------------------- attack: beam

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        Vec3 start = this.handPos();
        Vec3 end = target.getBoundingBox().getCenter();
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        if (length < 0.01D) return;
        Vec3 dir = diff.normalize();

        if (this.level() instanceof ServerLevel sl) {
            for (double d = 0.0D; d < length; d += 0.4D) {
                Vec3 p = start.add(dir.scale(d));
                sl.sendParticles(BEAM_DUST, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
                if (((int) (d / 0.4D)) % 2 == 0) {
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.02);
                }
            }
            sl.sendParticles(ParticleTypes.END_ROD, start.x, start.y, start.z, 8, 0.15, 0.15, 0.15, 0.05);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 14, 0.3, 0.3, 0.3, 0.2);
        }
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.4F, 0.5F);
        this.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.7F);
        target.hurt(this.damageSources().mobAttack(this), this.isEnraged() ? 9.0F : 6.0F);
    }

    // ------------------------------------------------------ attack: unibeam

    private void fireUnibeam(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 start = this.chestPos();
        Vec3 aim = target.getBoundingBox().getCenter().subtract(start).normalize();
        Vec3 end = start.add(aim.scale(32.0D));
        BlockHitResult hit = sl.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS) end = hit.getLocation();

        Vec3 beam = end.subtract(start);
        double len = beam.length();
        if (len < 0.5D) return;
        Vec3 dir = beam.normalize();

        for (double d = 0.0D; d < len; d += 0.35D) {
            Vec3 p = start.add(dir.scale(d));
            sl.sendParticles(UNIBEAM_DUST, p.x, p.y, p.z, 2, 0.12, 0.12, 0.12, 0.0);
            if (((int) (d / 0.35D)) % 3 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.06, 0.06, 0.06, 0.01);
            }
        }
        sl.sendParticles(ParticleTypes.SONIC_BOOM, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.FLASH, start.x, start.y, start.z, 1, 0.0, 0.0, 0.0, 0.0);

        AABB box = new AABB(start, end).inflate(1.6D);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && !(e instanceof UltronEntity))) {
            Vec3 c = e.getBoundingBox().getCenter();
            double t = Mth.clamp(c.subtract(start).dot(dir), 0.0D, len);
            Vec3 closest = start.add(dir.scale(t));
            if (closest.distanceTo(c) <= 1.4D + e.getBbWidth() / 2.0D) {
                e.hurt(this.damageSources().mobAttack(this), this.isEnraged() ? 22.0F : 15.0F);
                e.setSecondsOnFire(5);
                e.setDeltaMovement(e.getDeltaMovement().add(dir.x, 0.3D, dir.z));
                e.hurtMarked = true;
            }
        }
        sl.explode(this, end.x, end.y, end.z, 3.0F, Level.ExplosionInteraction.NONE);
        this.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 0.8F);
    }

    // --------------------------------------------------------- attack: slam

    private void doSlam() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2, this.getZ(), 6, 1.5, 0.2, 1.5, 0.0);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.3, this.getZ(), 40, 1.5, 0.2, 1.5, 0.3);
        this.playSound(SoundEvents.GENERIC_EXPLODE, 2.0F, 0.6F);
        this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 2.0F, 0.5F);

        double radius = 8.0D;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(radius, 3.0D, radius),
                e -> e != this && !(e instanceof UltronEntity))) {
            double dx = e.getX() - this.getX();
            double dz = e.getZ() - this.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > radius) continue;
            float dmg = (float) (14.0D * (1.0D - dist / (radius + 2.0D)));
            if (this.isEnraged()) dmg *= 1.4F;
            e.hurt(this.damageSources().mobAttack(this), dmg);
            Vec3 push = dist < 0.01D ? Vec3.ZERO : new Vec3(dx / dist, 0.0D, dz / dist).scale(1.3D);
            e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.75D, push.z));
            e.hurtMarked = true;
        }
    }

    private void slamRing(int step) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        double radius = 1.0D + step;
        int n = (int) (radius * 10.0D);
        for (int i = 0; i < n; i++) {
            double a = (Math.PI * 2.0D) * i / n;
            double x = this.getX() + Math.cos(a) * radius;
            double z = this.getZ() + Math.sin(a) * radius;
            double y = this.getY();
            BlockState st = sl.getBlockState(BlockPos.containing(x, y - 0.5D, z));
            if (!st.isAir()) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), x, y + 0.1D, z,
                        2, 0.1, 0.15, 0.1, 0.05);
            }
            if (i % 3 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 0.2D, z, 1, 0.05, 0.1, 0.05, 0.05);
                sl.sendParticles(AMBIENT_DUST, x, y + 0.3D, z, 1, 0.05, 0.1, 0.05, 0.0);
            }
        }
    }

    // ---------------------------------------------------------------- goals

    private static class SlamGoal extends Goal {
        private final UltronEntity ultron;
        private int tick;

        SlamGoal(UltronEntity ultron) {
            this.ultron = ultron;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = ultron.getTarget();
            return t != null && t.isAlive() && ultron.slamCooldown <= 0
                    && ultron.onGround() && ultron.distanceToSqr(t) < 49.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return tick < 30;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            tick = 0;
            ultron.setState(STATE_SLAM_WINDUP);
            ultron.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = ultron.getTarget();
            if (t != null) ultron.getLookControl().setLookAt(t, 30.0F, 30.0F);
            tick++;
            if (tick == 1) ultron.playSound(SoundEvents.IRON_GOLEM_DAMAGE, 2.0F, 0.5F);
            if (tick == 18) {
                ultron.setState(STATE_SLAM_STRIKE);
                ultron.doSlam();
            }
            if (tick >= 18 && tick < 26) ultron.slamRing(tick - 17);
        }

        @Override
        public void stop() {
            ultron.setState(STATE_IDLE);
            ultron.slamCooldown = ultron.isEnraged() ? 70 : 110;
        }
    }

    private static class UnibeamGoal extends Goal {
        private final UltronEntity ultron;
        private int tick;

        UnibeamGoal(UltronEntity ultron) {
            this.ultron = ultron;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = ultron.getTarget();
            if (t == null || !t.isAlive() || ultron.unibeamCooldown > 0) return false;
            double d = ultron.distanceToSqr(t);
            return d > 36.0D && d < 576.0D && ultron.hasLineOfSight(t);
        }

        @Override
        public boolean canContinueToUse() {
            return tick < 52 && ultron.getTarget() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            tick = 0;
            ultron.setState(STATE_UNIBEAM);
            ultron.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = ultron.getTarget();
            if (t != null) ultron.getLookControl().setLookAt(t, 30.0F, 30.0F);
            tick++;
            if (tick == 1) ultron.playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.5F);
            if (tick == 20) ultron.playSound(SoundEvents.BEACON_POWER_SELECT, 3.0F, 0.6F);
            if (tick == 40 && t != null) ultron.fireUnibeam(t);
        }

        @Override
        public void stop() {
            ultron.setState(STATE_IDLE);
            ultron.unibeamCooldown = ultron.isEnraged() ? 120 : 220;
        }
    }

    // ------------------------------------------------------- client visuals

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            this.clientEffects();
        }
    }

    private void clientEffects() {
        float yaw = this.yBodyRot * ((float) Math.PI / 180.0F);
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double chestY = this.getY() + 1.75D;
        boolean enraged = this.isEnraged();

        if (this.random.nextInt(enraged ? 1 : 3) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2.0D;
            this.level().addParticle(AMBIENT_DUST, this.getX() + Math.cos(a) * 0.6D,
                    this.getY() + this.random.nextDouble() * 2.4D, this.getZ() + Math.sin(a) * 0.6D, 0.0D, 0.02D, 0.0D);
        }
        if (this.tickCount % 4 == 0) {
            this.level().addParticle(AMBIENT_DUST, this.getX() + fx * 0.6D,
                    chestY + (this.random.nextDouble() - 0.5D) * 0.3D, this.getZ() + fz * 0.6D, 0.0D, 0.0D, 0.0D);
        }
        if (this.tickCount % 6 == 0) {
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX() + fx * 0.45D,
                    this.getY() + 2.2D, this.getZ() + fz * 0.45D, 0.0D, 0.0D, 0.0D);
        }
        if (enraged) {
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX() - fx * 0.5D, chestY,
                        this.getZ() - fz * 0.5D, 0.0D, 0.03D, 0.0D);
            }
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.FLAME, this.getX() - fx * 0.5D,
                        chestY + 0.3D, this.getZ() - fz * 0.5D, 0.0D, 0.05D, 0.0D);
            }
        }
        if (this.getState() == STATE_UNIBEAM) {
            double cx = this.getX() + fx * 0.55D, cz = this.getZ() + fz * 0.55D;
            for (int i = 0; i < 4; i++) {
                double a = this.random.nextDouble() * Math.PI * 2.0D;
                double rad = 2.5D + this.random.nextDouble() * 1.5D;
                double px = cx + Math.cos(a) * rad;
                double py = chestY + (this.random.nextDouble() - 0.5D) * 2.0D;
                double pz = cz + Math.sin(a) * rad;
                this.level().addParticle(ParticleTypes.END_ROD, px, py, pz,
                        (cx - px) * 0.12D, (chestY - py) * 0.12D, (cz - pz) * 0.12D);
            }
        }
        if (this.getState() == STATE_SLAM_WINDUP && this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + (this.random.nextDouble() - 0.5D) * 1.5D,
                    this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * 1.5D, 0.0D, 0.02D, 0.0D);
        }
    }

    // ----------------------------------------------------------------- death

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.level() instanceof ServerLevel sl) {
            if (this.deathTime % 4 == 0) {
                double x = this.getX() + (this.random.nextDouble() - 0.5D) * 1.2D;
                double y = this.getY() + this.random.nextDouble() * 2.4D;
                double z = this.getZ() + (this.random.nextDouble() - 0.5D) * 1.2D;
                sl.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 10, 0.4, 0.4, 0.4, 0.15);
                this.playSound(SoundEvents.GENERIC_EXPLODE, 0.6F, 1.2F + this.random.nextFloat() * 0.4F);
            }
            if (this.deathTime >= 40 && !this.isRemoved()) {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.2D, this.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0);
                sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.2D, this.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0);
                this.playSound(SoundEvents.GENERIC_EXPLODE, 3.0F, 0.5F);
                this.level().broadcastEntityEvent(this, (byte) 60);
                this.remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    // --------------------------------------------------------- boss bar, nbt

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Overdrive", this.overdrive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.overdrive = tag.getBoolean("Overdrive");
        if (this.overdrive) {
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.2F, 0.7F);
    }
}
