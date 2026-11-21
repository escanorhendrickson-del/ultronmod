package com.example.ultronmod.item;

import com.example.ultronmod.AbilityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class UltronGauntletItem extends Item {
    private static final double RANGE = 40.0D;

    public UltronGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                toggleFlight(player);
            } else {
                fireRepulsor(level, player);
                player.getCooldowns().addCooldown(this, 30);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void toggleFlight(Player player) {
        boolean now = !player.getPersistentData().getBoolean(AbilityEvents.FLIGHT);
        player.getPersistentData().putBoolean(AbilityEvents.FLIGHT, now);
        player.displayClientMessage(Component.literal("Ultron Flight: " + (now ? "ON" : "OFF")), true);
    }

    private void fireRepulsor(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 stop = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        AABB box = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, stop, box,
                e -> !e.isSpectator() && e.isPickable(), RANGE * RANGE);
        if (entityHit != null) {
            stop = entityHit.getLocation();
            Entity target = entityHit.getEntity();
            target.hurt(player.damageSources().playerAttack(player), 10.0F);
        }

        if (level instanceof ServerLevel serverLevel) {
            Vec3 diff = stop.subtract(eye);
            double length = diff.length();
            Vec3 step = diff.normalize().scale(0.5D);
            Vec3 p = eye.add(look.scale(1.0D)).add(0.0D, -0.2D, 0.0D);
            for (double d = 1.0D; d < length; d += 0.5D) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                p = p.add(step);
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.5F, 0.6F);
        level.explode(player, stop.x, stop.y, stop.z, 1.5F, Level.ExplosionInteraction.NONE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click: Repulsor blast"));
        tooltip.add(Component.literal("Sneak + Right-click: Toggle flight"));
        tooltip.add(Component.literal("In hand: Resistance + Regeneration"));
    }
}
