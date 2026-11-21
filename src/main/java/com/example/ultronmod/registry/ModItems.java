package com.example.ultronmod.registry;

import com.example.ultronmod.UltronMod;
import com.example.ultronmod.item.UltronGauntletItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UltronMod.MODID);

    public static final RegistryObject<Item> ULTRON_CORE = ITEMS.register("ultron_core",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> ULTRON_GAUNTLET = ITEMS.register("ultron_gauntlet",
            () -> new UltronGauntletItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> ULTRON_SPAWN_EGG = ITEMS.register("ultron_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ULTRON, 0x6B6F76, 0xD32F2F, new Item.Properties()));
}
