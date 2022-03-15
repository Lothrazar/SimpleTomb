package com.lothrazar.simpletomb.particle;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticleGraveSoul extends SpriteTexturedParticle {

  private final IAnimatedSprite spriteSet;
  private final double radius, centerX, centerZ;

  private ParticleGraveSoul(IAnimatedSprite spriteSet, ClientWorld world, double x, double y, double z, double radius) {
    super(world, x, y + 0.85d, z);
    this.lifetime = 100;
    this.quadSize = 0.03f;
    this.centerX = x + 0.5d;
    this.centerZ = z + 0.5d;
    this.radius = radius;
    updatePosition();
    setAlpha(0.7f);
    setColor(81f / 255f, 25f / 255f, 139f / 255f);
    this.hasPhysics = false;
    this.spriteSet = spriteSet;
    setSpriteFromAge(this.spriteSet);
  }

  private void updatePosition() {
    double ratio = this.age / (double) this.lifetime;
    this.xd = this.yd = this.zd = 0d;
    this.xo = this.x = this.centerX + this.radius * Math.cos(2 * Math.PI * ratio);
    this.yo = this.y;
    this.zo = this.z = this.centerZ + this.radius * Math.sin(2 * Math.PI * ratio);
  }

  @Override
  public void tick() {
    super.tick();
    if (isAlive()) {
      setSpriteFromAge(this.spriteSet);
      updatePosition();
    }
  }

  @Override
  public IParticleRenderType getRenderType() {
    return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
  }

  public static class Factory implements IParticleFactory<BasicParticleType> {

    private IAnimatedSprite spriteSet;

    public Factory(IAnimatedSprite spriteSet) {
      this.spriteSet = spriteSet;
    }

    @Override
    public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
      return new ParticleGraveSoul(this.spriteSet, world, x, y, z, 0.3d);
    }
  }
}
