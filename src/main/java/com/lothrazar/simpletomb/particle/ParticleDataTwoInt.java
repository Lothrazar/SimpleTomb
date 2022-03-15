package com.lothrazar.simpletomb.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Locale;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.registries.ForgeRegistries;

public class ParticleDataTwoInt implements IParticleData {

  @SuppressWarnings("deprecation")
  public static final IDeserializer<ParticleDataTwoInt> DESERIALIZER = new IDeserializer<ParticleDataTwoInt>() {

    @Override
    public ParticleDataTwoInt fromCommand(ParticleType<ParticleDataTwoInt> particleType, StringReader reader) throws CommandSyntaxException {
      if (reader.canRead()) {
        reader.expect(' ');
      }
      int oneInt = 0xffffff, twoInt = 0xffffff;
      if (reader.canRead()) {
        oneInt = reader.readInt();
      }
      if (reader.canRead()) {
        reader.expect(' ');
      }
      if (reader.canRead()) {
        twoInt = reader.readInt();
      }
      return new ParticleDataTwoInt(particleType, oneInt, twoInt);
    }

    @Override
    public ParticleDataTwoInt fromNetwork(ParticleType<ParticleDataTwoInt> particleType, PacketBuffer buf) {
      return new ParticleDataTwoInt(particleType, buf.readInt(), buf.readInt());
    }
  };
  private final ParticleType<ParticleDataTwoInt> particleType;
  public int oneInt, twoInt;

  public ParticleDataTwoInt(ParticleType<ParticleDataTwoInt> particleType, int oneInt, int twoInt) {
    this.particleType = particleType;
    this.oneInt = oneInt;
    this.twoInt = twoInt;
  }

  @Override
  public ParticleType<?> getType() {
    return this.particleType;
  }

  @Override
  public void writeToNetwork(PacketBuffer buf) {
    buf.writeInt(this.oneInt);
    buf.writeInt(this.twoInt);
  }

  @Override
  public String writeToString() {
    return String.format(Locale.ROOT, "%s %d %d", ForgeRegistries.PARTICLE_TYPES.getKey(getType()), this.oneInt, this.twoInt);
  }
}
