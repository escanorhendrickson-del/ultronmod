package com.example.ultronmod;

import com.example.ultronmod.registry.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltronMod.MODID)
public class AbilityEvents {
    public static final String FLIGHT = "UltronFlight";
    private static final String GRANTED = "UltronFlightGranted";

    private static boolean hasGauntlet(Player p) {
        Item g = ModItems.ULTRON_GAUNTLET.get();
        for (ItemStack s : p.getInventory().items) {
            if (s.is(g)) return true;
        }
        return p.getOffhandItem().is(g);
    }

    private static boolean flightActive(Player p) {
        return p.getPersistentData().getBoolean(FLIGHT) && hasGauntlet(p);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player p = event.player;

        boolean wants = flightActive(p);
        boolean granted = p.getPersistentData().getBoolean(GRANTED);
        boolean normalMode = !p.isCreative() && !p.isSpectator();

        if (wants && normalMode) {
            if (!p.getAbilities().mayfly) {
                p.getAbilities().mayfly = true;
                p.onUpdateAbilities();
            }
            p.getPersistentData().putBoolean(GRANTED, true);
        } else if (granted) {
            if (normalMode) {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
            p.getPersistentData().putBoolean(GRANTED, false);
        }

        Item g = ModItems.ULTRON_GAUNTLET.get();
        if (p.tickCount % 20 == 0 && (p.getMainHandItem().is(g) || p.getOffhandItem().is(g))) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && flightActive(p)) {
            event.setCanceled(true);
        }
    }
}
