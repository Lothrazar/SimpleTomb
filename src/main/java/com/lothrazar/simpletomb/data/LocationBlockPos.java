package com.lothrazar.simpletomb.data;

import com.lothrazar.simpletomb.helper.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LocationBlockPos {

  public int x;
  public int y;
  public int z;
  public String dim;
  //  public static final BlockPos ORIGIN_POS = new BlockPos(0, Integer.MIN_VALUE, );
  public static final LocationBlockPos ORIGIN = new LocationBlockPos(0, 0, 0, "");

  public LocationBlockPos(BlockPos pos, World world) {
    this(pos, WorldHelper.dimensionToString(world));
  }

  public LocationBlockPos(BlockPos pos, String dim) {
    this(pos.getX(), pos.getY(), pos.getZ(), dim);
  }

  public LocationBlockPos(int x, int y, int z, String dim) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.dim = dim;
  }

  public LocationBlockPos(Entity entity) {
    this(entity.blockPosition(), entity.level);
  }

  public BlockPos toBlockPos() {
    return new BlockPos(this.x, this.y, this.z);
  }

  public boolean equals(LocationBlockPos loc) {
    return (loc.x == this.x &&
        loc.y == this.y &&
        loc.z == this.z &&
        loc.dim.equalsIgnoreCase(this.dim));
  }

  public boolean isOrigin() {
    return this.equals(ORIGIN);
  }

  public double getDistance(BlockPos pos) {
    double deltX = this.x - pos.getX();
    double deltY = this.y - pos.getY();
    double deltZ = this.z - pos.getZ();
    return Math.sqrt(deltX * deltX + deltY * deltY + deltZ * deltZ);
  }

  @Override
  public String toString() {
    return String.format("%s (%d, %d, %d)", this.dim, this.x, this.y, this.z);
  }
}
