package com.lothrazar.simpletomb.helper;

import com.lothrazar.simpletomb.ConfigTomb;
import com.lothrazar.simpletomb.data.LocationBlockPos;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;

public class WorldHelper {

  public static float getRandom(Random rand, double min, double max) {
    return (float) (rand.nextDouble() * (max - min) + min);
  }

  public static String dimensionToString(World w) {
    //example: returns "minecraft:overworld" resource location
    return w.dimension().location().toString();
  }

  public static boolean isValidPlacement(World world, BlockPos myPos) {
    //0 is the bottom bedrock level
    //so if we place there, players cant place a block under it to stand safely
    if (myPos.getY() < 1 || World.isOutsideBuildHeight(myPos)) {
      // blockstate doesnt matter, out of world
      return false;
    }
    //    FluidState fluidHere = world.getFluidState(myPos);
    //only air or water. not any fluid state, and not any waterlogged block
    BlockState blockState = world.getBlockState(myPos);
    return blockState.isAir() || blockState.getBlock() == Blocks.WATER; // && fluidHere.getFluid().isIn(FluidTags.WATER));
  }

  public static LocationBlockPos findGraveSpawn(final PlayerEntity player, final BlockPos initPos) {
    final int xRange = ConfigTomb.HSEARCHRANGE.get();
    final int yRange = ConfigTomb.VSEARCHRANGE.get();
    final int zRange = ConfigTomb.HSEARCHRANGE.get();
    World world = player.level;
    //   
    //shortcut: if the death position is valid AND solid base. JUST DO THAT dont even search
    if (isValidPlacement(player.level, initPos)
        && isValidSolid(player.level, initPos)) {
      //      ModTomb.LOGGER.info(" initPos is enough =  " + initPos);
      return new LocationBlockPos(initPos, world);
    }
    //
    //    ModTomb.LOGGER.info(isValidINIT + "find initPos=  " + initPos);
    List<BlockPos> positionsWithSolidBelow = new ArrayList<>();
    List<BlockPos> positions = new ArrayList<>();
    for (int x = initPos.getX() - xRange; x < initPos.getX() + xRange; x++) {
      for (int y = initPos.getY() - yRange; y < initPos.getY() + yRange; y++) {
        for (int z = initPos.getZ() - zRange; z < initPos.getZ() + zRange; z++) {
          BlockPos myPos = new BlockPos(x, y, z);
          //
          boolean isValid = isValidPlacement(world, myPos);
          //          ModTomb.LOGGER.info("isvalid  initPos=  " + isValid);
          if (!isValid) {
            continue;
          }
          //where do we put this
          if (isValidSolid(world, myPos)) {
            //this is better
            positionsWithSolidBelow.add(myPos);
          }
          else {
            positions.add(myPos);
          }
        }
      }
    }
    //first, if we have a 'solid pase' pos, use that
    BlockPos found = null;
    if (positionsWithSolidBelow.size() > 0) {
      //use this one 
      sortByDistance(initPos, positionsWithSolidBelow);
      found = positionsWithSolidBelow.get(0);
    }
    else if (positions.size() > 0) {
      //i guess it has to float in the air
      sortByDistance(initPos, positions);
      found = positions.get(0);
    }
    else {
      return null;
    }
    return new LocationBlockPos(found, world);
  }

  private static boolean isValidSolid(World world, BlockPos myPos) {
    return world.getBlockState(myPos.below()).canOcclude();
  }

  private static void sortByDistance(final BlockPos initPos, List<BlockPos> positions) {
    positions.sort((pos0, pos1) -> {
      double dist0 = Math.sqrt(pos0.distSqr(initPos));
      double dist1 = Math.sqrt(pos1.distSqr(initPos));
      return Double.valueOf(dist0).compareTo(dist1);
    });
  }

  public static BlockPos getInitialPos(World world, BlockPos pos) {
    WorldBorder border = world.getWorldBorder();
    boolean validXZ = border.isWithinBounds(pos);
    boolean validY = !World.isOutsideBuildHeight(pos);
    if (validXZ && validY) {
      return pos;
    }
    else {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      if (!validXZ) {
        x = Math.min(Math.max(pos.getX(), (int) border.getMinX()), (int) border.getMaxX());
        z = Math.min(Math.max(pos.getZ(), (int) border.getMinZ()), (int) border.getMaxZ());
      }
      if (!validY) {
        if (y < 1) {
          y = 1;
        }
        if (y > world.getMaxBuildHeight()) {
          y = world.getMaxBuildHeight() - 1;
        }
      }
      return new BlockPos(x, y, z);
    }
  }

  public static boolean isRuleKeepInventory(PlayerEntity player) {
    return isRuleKeepInventory(player.level);
  }

  public static boolean isRuleKeepInventory(World world) {
    return world.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
  }

  public static void removeNoEvent(World world, BlockPos pos) {
    world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
  }

  public static boolean placeGrave(World world, BlockPos pos, BlockState state) {
    return world.setBlock(pos, state, 2);
  }

  public static boolean isNight(World world) {
    float angle = world.getSunAngle(0.0F);
    return angle >= 0.245F && angle <= 0.755F;
  }

  public static float[] getRGBColor3F(int color) {
    return new float[] {
        (color >> 16 & 255) / 255.0F,
        (color >> 8 & 255) / 255.0F,
        (color & 255) / 255.0F,
    };
  }

  public static float[] getHSBtoRGBF(float hue, float saturation, float brightness) {
    int r = 0;
    int g = 0;
    int b = 0;
    if (saturation == 0.0F) {
      r = g = b = (int) (brightness * 255.0F + 0.5F);
    }
    else {
      float h = (hue - (float) Math.floor(hue)) * 6.0F;
      float f = h - (float) Math.floor(h);
      float p = brightness * (1.0F - saturation);
      float q = brightness * (1.0F - saturation * f);
      float t = brightness * (1.0F - saturation * (1.0F - f));
      switch ((int) h) {
        case 0:
          r = (int) (brightness * 255.0F + 0.5F);
          g = (int) (t * 255.0F + 0.5F);
          b = (int) (p * 255.0F + 0.5F);
        break;
        case 1:
          r = (int) (q * 255.0F + 0.5F);
          g = (int) (brightness * 255.0F + 0.5F);
          b = (int) (p * 255.0F + 0.5F);
        break;
        case 2:
          r = (int) (p * 255.0F + 0.5F);
          g = (int) (brightness * 255.0F + 0.5F);
          b = (int) (t * 255.0F + 0.5F);
        break;
        case 3:
          r = (int) (p * 255.0F + 0.5F);
          g = (int) (q * 255.0F + 0.5F);
          b = (int) (brightness * 255.0F + 0.5F);
        break;
        case 4:
          r = (int) (t * 255.0F + 0.5F);
          g = (int) (p * 255.0F + 0.5F);
          b = (int) (brightness * 255.0F + 0.5F);
        break;
        case 5:
          r = (int) (brightness * 255.0F + 0.5F);
          g = (int) (p * 255.0F + 0.5F);
          b = (int) (q * 255.0F + 0.5F);
      }
    }
    return new float[] {
        r / 255.0F,
        g / 255.0F,
        b / 255.0F,
    };
  }
}
