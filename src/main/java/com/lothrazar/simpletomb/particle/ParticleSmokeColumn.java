package com.lothrazar.simpletomb.particle;

import com.lothrazar.simpletomb.TombRegistry;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.MetaParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParticleSmokeColumn extends MetaParticle {

  private ParticleSmokeColumn(ClientWorld world, double x, double y, double z) {
    super(world, x, y, z);
  }

  @Override
  public void tick() {
    double y = this.y;
    for (int i = 0; i < 6; i++) {
      this.level.addParticle(TombRegistry.ROTATING_SMOKE, this.x - 0.1d, y, this.z - 0.1d, 0d, 0d, 0d);
      this.level.addParticle(TombRegistry.ROTATING_SMOKE, this.x - 0.1d, y, this.z + 0.1d, 0d, 0d, 0d);
      this.level.addParticle(TombRegistry.ROTATING_SMOKE, this.x + 0.1d, y, this.z - 0.1d, 0d, 0d, 0d);
      this.level.addParticle(TombRegistry.ROTATING_SMOKE, this.x + 0.1d, y, this.z + 0.1d, 0d, 0d, 0d);
      y += 0.3d;
    }
    remove();
  }

  public static class Factory implements IParticleFactory<BasicParticleType> {

    @Override
    public Particle createParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
      return new ParticleSmokeColumn(world, x, y, z);
    }
  }
}
