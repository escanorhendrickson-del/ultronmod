package com.example.ultronmod.client;

import com.example.ultronmod.UltronMod;
import com.example.ultronmod.entity.UltronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UltronRenderer extends HumanoidMobRenderer<UltronEntity, HumanoidModel<UltronEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(UltronMod.MODID, "textures/entity/ultron.png");

    public UltronRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(UltronEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(UltronEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }
}
