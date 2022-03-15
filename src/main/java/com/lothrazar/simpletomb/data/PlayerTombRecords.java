package com.lothrazar.simpletomb.data;

import com.lothrazar.simpletomb.ModTomb;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

public class PlayerTombRecords {

  UUID playerId;
  public List<CompoundNBT> playerGraves = new ArrayList<>();

  public PlayerTombRecords(UUID id, CompoundNBT first) {
    playerId = id;
    playerGraves.add(first);
  }

  public PlayerTombRecords() {}

  public CompoundNBT getGrave(int index) {
    if (index >= playerGraves.size()) {
      return null;
    }
    return playerGraves.get(index);
  }

  public void read(CompoundNBT data, UUID playerId) {
    this.playerId = playerId;
    if (data.contains(ModTomb.MODID)) {
      ListNBT glist = data.getList(ModTomb.MODID, Constants.NBT.TAG_COMPOUND);
      for (int i = 0; i < glist.size(); i++) {
        this.playerGraves.add(glist.getCompound(i));
      }
    }
  }

  public CompoundNBT write() {
    CompoundNBT data = new CompoundNBT();
    ListNBT glist = new ListNBT();
    for (CompoundNBT g : playerGraves) {
      glist.add(g);
    }
    data.put(ModTomb.MODID, glist);
    return data;
  }

  public static BlockPos getPos(CompoundNBT grave) {
    return NBTUtil.readBlockPos(grave.getCompound("pos"));
  }

  public static String getDim(CompoundNBT grave) {
    return grave.getString("dimension");
  }

  public static List<ItemStack> getDrops(CompoundNBT grave) {
    ListNBT drops = grave.getList("drops", 10);
    List<ItemStack> done = new ArrayList<ItemStack>();
    for (int i = 0; i < drops.size(); i++) {
      done.add(ItemStack.of(drops.getCompound(i)));
    }
    return done;
  }

  public void deleteAll() {
    this.playerGraves = new ArrayList<>();
  }

  /**
   * because mojang is pretty stupid and made this client only so copy paste
   */
  public String getCoordinatesAsString(BlockPos pos) {
    return "" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
  }

  public String toDisplayString(int i) {
    CompoundNBT gd = getGrave(i);
    return String.format("[%d] (%s) (%s) {%d}", i, getDim(gd), getCoordinatesAsString(getPos(gd)), getDrops(gd).size());
  }
}
