package com.lothrazar.simpletomb.proxy;

import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.block.RenderTomb;
import com.lothrazar.simpletomb.event.ClientEvents;
import com.lothrazar.simpletomb.particle.ParticleCasting;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fmlclient.registry.ClientRegistry;

@OnlyIn(Dist.CLIENT)
public class ClientUtils {

  public static void setup() {
    MinecraftForge.EVENT_BUS.register(new ClientEvents());
//    ClientRegistry.bindTileEntityRenderer(TombRegistry.TOMBSTONETILEENTITY, RenderTomb::new);
  }

  public static void produceGraveSmoke(Level world, double x, double y, double z) {
    Minecraft.getInstance().particleEngine.createParticle(TombRegistry.GRAVE_SMOKE, x + world.random.nextGaussian(), y, z + world.random.nextGaussian(), 0d, 0d, 0d);
  }

  public static void produceGraveSoul(Level world, BlockPos pos) {
    world.addParticle(TombRegistry.SOUL, pos.getX(), pos.getY(), pos.getZ(), 0d, 0d, 0d);
  }

  public static void produceParticleCasting(LivingEntity caster, Predicate<LivingEntity> predic) {
    Minecraft mc = Minecraft.getInstance();
    if (caster != null && caster.level instanceof ClientLevel) {
      ParticleCasting particle;
      for (int i = 1; i <= 2; i++) {
        ClientLevel cworld = (ClientLevel) caster.level;
        particle = new ParticleCasting(cworld, caster, predic, 0d, i * 0.5d);
        mc.particleEngine.add(particle);
        particle = new ParticleCasting(cworld, caster, predic, 0.5d, (i + 1) * 0.5d);
        mc.particleEngine.add(particle);
        particle = new ParticleCasting(cworld, caster, predic, 1d, i * 0.5d);
        mc.particleEngine.add(particle);
        particle = new ParticleCasting(cworld, caster, predic, 1.5d, (i + 1) * 0.5d);
        mc.particleEngine.add(particle);
        particle = new ParticleCasting(cworld, caster, predic, 2d, i * 0.5d);
        mc.particleEngine.add(particle);
      }
    }
  }
}
