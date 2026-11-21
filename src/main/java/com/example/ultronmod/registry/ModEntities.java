package com.example.ultronmod.registry;

import com.example.ultronmod.UltronMod;
import com.example.ultronmod.entity.UltronEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, UltronMod.MODID);

    public static final RegistryObject<EntityType<UltronEntity>> ULTRON = ENTITIES.register("ultron",
            () -> EntityType.Builder.of(UltronEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.1F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(UltronMod.MODID, "ultron").toString()));
}
