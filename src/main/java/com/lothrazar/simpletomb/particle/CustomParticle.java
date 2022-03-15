package com.lothrazar.simpletomb.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.TexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
abstract class CustomParticle extends TexturedParticle {

  protected CustomParticle(ClientWorld world, double x, double y, double z) {
    super(world, x, y, z);
  }

  protected CustomParticle(ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
    super(world, x, y, z, motionX, motionY, motionZ);
  }

  abstract ResourceLocation getTexture();

  @SuppressWarnings("deprecation")
  @Override
  public void render(IVertexBuilder buffer, ActiveRenderInfo entityIn, float partialTicks) {
    TextureManager textureManager = Minecraft.getInstance().textureManager;
    RenderHelper.turnOff();
    RenderSystem.depthMask(false);
    textureManager.bind(getTexture());
    RenderSystem.enableBlend();
    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    RenderSystem.alphaFunc(516, 0.003921569F);
    Tessellator.getInstance().getBuilder().begin(7, DefaultVertexFormats.PARTICLE);
    super.render(buffer, entityIn, partialTicks);
    Tessellator.getInstance().end();
  }

  @Override
  protected float getU0() {
    return 0f;
  }

  @Override
  protected float getU1() {
    return 1f;
  }

  @Override
  protected float getV0() {
    return 0f;
  }

  @Override
  protected float getV1() {
    return 1f;
  }

  @Override
  public IParticleRenderType getRenderType() {
    return IParticleRenderType.CUSTOM;
  }
}
