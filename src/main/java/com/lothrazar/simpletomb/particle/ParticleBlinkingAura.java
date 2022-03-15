package com.lothrazar.simpletomb.particle;

import com.lothrazar.simpletomb.helper.WorldHelper;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticleBlinkingAura extends TransparentParticle {

  private final IAnimatedSprite spriteSet;
  private final float[] colorCodeMin;
  private final float[] colorCodeMax;

  private ParticleBlinkingAura(IAnimatedSprite spriteSet, ClientWorld world, double x, double y, double z, int colorMin, int colorMax) {
    super(world, x, y, z);
    this.xd = this.yd = this.zd = 0d;
    setAlpha(0.15f);
    scale(WorldHelper.getRandom(world.random, 0.6f, 0.8f));
    setLifetime(7);
    this.hasPhysics = false;
    this.colorCodeMin = WorldHelper.getRGBColor3F(colorMin);
    this.colorCodeMax = WorldHelper.getRGBColor3F(colorMax);
    setColor(this.colorCodeMin[0], this.colorCodeMin[1], this.colorCodeMin[2]);
    this.spriteSet = spriteSet;
    setSpriteFromAge(this.spriteSet);
  }

  @Override
  public void tick() {
    super.tick();
    if (isAlive()) {
      setColor(WorldHelper.getRandom(this.level.random, this.colorCodeMin[0], this.colorCodeMax[0]),
          WorldHelper.getRandom(this.level.random, this.colorCodeMin[1], this.colorCodeMax[1]),
          WorldHelper.getRandom(this.level.random, this.colorCodeMin[2], this.colorCodeMax[2]));
      setSpriteFromAge(this.spriteSet);
    }
  }

  @Override
  protected int getLightColor(float partialTick) {
    int skylight = 15;
    int blocklight = 15;
    return skylight << 20 | blocklight << 4;
  }

  @Override
  public IParticleRenderType getRenderType() {
    return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  public static class Factory implements IParticleFactory<ParticleDataTwoInt> {

    private IAnimatedSprite spriteSet;

    public Factory(IAnimatedSprite spriteSet) {
      this.spriteSet = spriteSet;
    }

    @Override
    public Particle createParticle(ParticleDataTwoInt type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
      return new ParticleBlinkingAura(this.spriteSet, world, x, y, z, type.oneInt, type.twoInt);
    }
  }
}
