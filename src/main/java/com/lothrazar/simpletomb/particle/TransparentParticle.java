package com.lothrazar.simpletomb.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;

public class TransparentParticle extends SpriteTexturedParticle {

  protected TransparentParticle(ClientWorld world, double x, double y, double z) {
    super(world, x, y, z);
  }

  @Override
  public IParticleRenderType getRenderType() {
    return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  @Override
  public void render(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
    RenderSystem.depthMask(false);
    super.render(buffer, renderInfo, partialTicks);
  }
}
