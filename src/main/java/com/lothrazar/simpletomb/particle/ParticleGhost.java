package com.lothrazar.simpletomb.particle;

import com.lothrazar.simpletomb.helper.WorldHelper;
import com.lothrazar.simpletomb.proxy.ClientUtils;
import java.util.Random;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticleGhost extends TransparentParticle {

  private final IAnimatedSprite spriteSet;
  private final double mX, mZ;

  private ParticleGhost(IAnimatedSprite spriteSet, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
    super(world, x, y + 1d, z);
    this.mX = motionX;
    this.mZ = motionZ;
    this.xd = this.yd = this.zd = 0d;
    setLifetime(200);
    this.hasPhysics = false;
    scale(8f);
    setColor(1f, 1f, 1f);
    this.spriteSet = spriteSet;
    setSpriteFromAge(this.spriteSet);
  }

  @Override
  public void tick() {
    super.tick();
    if (isAlive()) {
      if (this.age == 10) {
        this.xd = mX;
        this.zd = mZ;
      }
      float ratio = this.age / (float) this.lifetime;
      setAlpha((1f - ratio) * 0.8f);
      setSpriteFromAge(this.spriteSet);
      if (level.isClientSide) {
        ClientUtils.produceGraveSmoke(this.level, this.x, this.y - 1d, this.z);
      }
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

  public static class Factory implements IParticleFactory<BasicParticleType> {

    private IAnimatedSprite spriteSet;

    public Factory(IAnimatedSprite spriteSet) {
      this.spriteSet = spriteSet;
    }

    @Override
    public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
      Random rand = world == null || world.random == null ? new Random() : world.random;
      return new ParticleGhost(this.spriteSet, world, x, y, z, WorldHelper.getRandom(rand, -0.05d, 0.05d), 0d, WorldHelper.getRandom(rand, -0.05d, 0.05d));
    }
  }
}
