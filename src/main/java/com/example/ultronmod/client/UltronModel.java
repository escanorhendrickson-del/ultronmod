package com.example.ultronmod.client;

import com.example.ultronmod.UltronMod;
import com.example.ultronmod.entity.UltronEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Custom Ultron model. Built at 2x detail; the renderer scales it by 0.5. */
public class UltronModel extends EntityModel<UltronEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation(UltronMod.MODID, "ultron"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public UltronModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -16.0F, -6.0F, 12.0F, 16.0F, 12.0F)
                .texOffs(152, 48).addBox(-5.0F, -13.0F, -8.0F, 10.0F, 10.0F, 2.0F)
                .texOffs(16, 78).addBox(-4.0F, -3.0F, -8.0F, 8.0F, 4.0F, 2.0F)
                .texOffs(36, 78).addBox(-7.0F, -15.0F, -9.0F, 14.0F, 2.0F, 4.0F)
                .texOffs(120, 48).addBox(6.0F, -14.0F, -3.0F, 2.0F, 8.0F, 6.0F)
                .texOffs(136, 48).addBox(-8.0F, -14.0F, -3.0F, 2.0F, 8.0F, 6.0F)
                , PartPose.offset(0.0F, -16.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(120, 66).addBox(-3.0F, -16.0F, -3.0F, 6.0F, 4.0F, 6.0F)
                .texOffs(48, 0).addBox(-10.0F, -14.0F, -6.0F, 20.0F, 14.0F, 12.0F)
                .texOffs(176, 48).addBox(1.0F, -13.0F, -10.0F, 8.0F, 8.0F, 4.0F)
                .texOffs(200, 48).addBox(-9.0F, -13.0F, -10.0F, 8.0F, 8.0F, 4.0F)
                .texOffs(0, 28).addBox(-6.0F, 0.0F, -4.0F, 12.0F, 12.0F, 8.0F)
                .texOffs(112, 0).addBox(-2.0F, -12.0F, 6.0F, 4.0F, 20.0F, 2.0F)
                .texOffs(40, 28).addBox(-8.0F, 12.0F, -5.0F, 16.0F, 8.0F, 10.0F)
                .texOffs(192, 66).addBox(-6.0F, 14.0F, -7.0F, 12.0F, 6.0F, 2.0F)
                , PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(48, 48).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 6.0F, 10.0F)
                .texOffs(92, 28).addBox(-3.0F, 1.0F, -3.0F, 6.0F, 12.0F, 6.0F)
                .texOffs(224, 48).addBox(-4.0F, 13.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                .texOffs(124, 0).addBox(-4.0F, 17.0F, -4.0F, 8.0F, 14.0F, 8.0F)
                .texOffs(32, 66).addBox(-3.0F, 31.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(220, 66).addBox(-3.0F, 37.0F, -3.0F, 2.0F, 6.0F, 2.0F)
                .texOffs(236, 66).addBox(-1.0F, 37.0F, -3.0F, 2.0F, 6.0F, 2.0F)
                .texOffs(0, 78).addBox(1.0F, 37.0F, -3.0F, 2.0F, 6.0F, 2.0F)
                , PartPose.offset(14.0F, -10.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(84, 48).addBox(-4.0F, -5.0F, -5.0F, 8.0F, 6.0F, 10.0F)
                .texOffs(116, 28).addBox(-3.0F, 1.0F, -3.0F, 6.0F, 12.0F, 6.0F)
                .texOffs(0, 66).addBox(-4.0F, 13.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                .texOffs(156, 0).addBox(-4.0F, 17.0F, -4.0F, 8.0F, 14.0F, 8.0F)
                .texOffs(56, 66).addBox(-3.0F, 31.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(228, 66).addBox(1.0F, 37.0F, -3.0F, 2.0F, 6.0F, 2.0F)
                .texOffs(244, 66).addBox(-1.0F, 37.0F, -3.0F, 2.0F, 6.0F, 2.0F)
                .texOffs(8, 78).addBox(-3.0F, 37.0F, -3.0F, 2.0F, 6.0F, 2.0F)
                , PartPose.offset(-14.0F, -10.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(188, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 14.0F, 8.0F)
                .texOffs(144, 66).addBox(-4.0F, 11.0F, -6.0F, 8.0F, 6.0F, 4.0F)
                .texOffs(140, 28).addBox(-4.0F, 14.0F, -4.0F, 8.0F, 10.0F, 8.0F)
                .texOffs(80, 66).addBox(-3.0F, 15.0F, 4.0F, 6.0F, 8.0F, 4.0F)
                .texOffs(204, 28).addBox(-5.0F, 24.0F, -9.0F, 10.0F, 4.0F, 14.0F)
                , PartPose.offset(5.0F, 20.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(220, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 14.0F, 8.0F)
                .texOffs(168, 66).addBox(-4.0F, 11.0F, -6.0F, 8.0F, 6.0F, 4.0F)
                .texOffs(172, 28).addBox(-4.0F, 14.0F, -4.0F, 8.0F, 10.0F, 8.0F)
                .texOffs(100, 66).addBox(-3.0F, 15.0F, 4.0F, 6.0F, 8.0F, 4.0F)
                .texOffs(0, 48).addBox(-5.0F, 24.0F, -9.0F, 10.0F, 4.0F, 14.0F)
                , PartPose.offset(-5.0F, 20.0F, 0.0F));
        return LayerDefinition.create(mesh, 256, 96);
    }

    @Override
    public void setupAnim(UltronEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        final float deg = (float) Math.PI / 180.0F;
        float swing = limbSwing * 0.55F;
        float amt = Math.min(1.0F, limbSwingAmount);

        head.yRot = netHeadYaw * deg;
        head.xRot = headPitch * deg;
        head.zRot = 0.0F;
        body.xRot = 0.0F;
        body.yRot = 0.0F;
        body.zRot = 0.0F;

        rightLeg.xRot = Mth.cos(swing) * amt;
        leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * amt;
        rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.7F * amt;
        leftArm.xRot = Mth.cos(swing) * 0.7F * amt;
        rightArm.yRot = 0.0F;
        leftArm.yRot = 0.0F;

        float sway = Mth.sin(ageInTicks * 0.07F) * 0.03F;
        rightArm.zRot = 0.06F + sway;
        leftArm.zRot = -0.06F - sway;

        int state = entity.getState();
        if (state == UltronEntity.STATE_UNIBEAM) {
            rightArm.xRot = -0.35F;
            leftArm.xRot = -0.35F;
            rightArm.zRot = 1.0F;
            leftArm.zRot = -1.0F;
        } else if (state == UltronEntity.STATE_SLAM_WINDUP) {
            rightArm.xRot = -3.0F;
            leftArm.xRot = -3.0F;
            rightArm.zRot = 0.15F;
            leftArm.zRot = -0.15F;
        } else if (state == UltronEntity.STATE_SLAM_STRIKE) {
            rightArm.xRot = -0.9F;
            leftArm.xRot = -0.9F;
        } else if (entity.isAggressive()) {
            rightArm.xRot = -1.45F + head.xRot;
            rightArm.zRot = 0.0F;
        }

        if (entity.isEnraged()) {
            rightArm.zRot += Mth.sin(ageInTicks * 1.3F) * 0.02F;
            leftArm.zRot -= Mth.sin(ageInTicks * 1.1F) * 0.02F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
