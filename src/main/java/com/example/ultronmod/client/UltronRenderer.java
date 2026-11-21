package com.example.ultronmod.client;

import com.example.ultronmod.UltronMod;
import com.example.ultronmod.entity.UltronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UltronRenderer extends MobRenderer<UltronEntity, UltronModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(UltronMod.MODID, "textures/entity/ultron.png");

    public UltronRenderer(EntityRendererProvider.Context context) {
        super(context, new UltronModel(context.bakeLayer(UltronModel.LAYER_LOCATION)), 0.9F);
        this.addLayer(new UltronGlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(UltronEntity entity) {
        return TEXTURE;
    }

    /** The model is built at 2x detail (80 texels tall), so shrink it by half and re-seat the feet. */
    @Override
    protected void scale(UltronEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, -1.499F, 0.0F);
    }
}
