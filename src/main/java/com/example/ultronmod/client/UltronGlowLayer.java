package com.example.ultronmod.client;

import com.example.ultronmod.UltronMod;
import com.example.ultronmod.entity.UltronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Fullbright, pulsing glow for eyes, chest, joints and tubing. */
public class UltronGlowLayer extends RenderLayer<UltronEntity, UltronModel> {
    private static final ResourceLocation GLOW =
            new ResourceLocation(UltronMod.MODID, "textures/entity/ultron_glow.png");

    public UltronGlowLayer(RenderLayerParent<UltronEntity, UltronModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, UltronEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;

        float speed = entity.isEnraged() ? 0.45F : 0.15F;
        float pulse = 0.8F + 0.2F * Mth.sin(ageInTicks * speed);
        if (entity.getState() == UltronEntity.STATE_UNIBEAM) {
            pulse = 1.0F;
        } else if (entity.isEnraged()) {
            pulse = Math.min(1.0F, pulse + 0.15F);
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(GLOW));
        this.getParentModel().renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1.0F);
    }
}
