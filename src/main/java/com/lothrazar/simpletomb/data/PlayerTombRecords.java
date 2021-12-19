package com.lothrazar.simpletomb.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.lothrazar.simpletomb.ModTomb;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;

public class PlayerTombRecords {

  UUID playerId;
  public List<CompoundTag> playerGraves = new ArrayList<>();

  public PlayerTombRecords(UUID id, CompoundTag first) {
    playerId = id;
    playerGraves.add(first);
  }

  public PlayerTombRecords() {}

  public CompoundTag getGrave(int index) {
    if (index >= playerGraves.size()) {
      return null;
    }
    return playerGraves.get(index);
  }

  public void read(CompoundTag data, UUID playerId) {
    this.playerId = playerId;
    if (data.contains(ModTomb.MODID)) {
      ListTag glist = data.getList(ModTomb.MODID, Tag.TAG_COMPOUND);
      for (int i = 0; i < glist.size(); i++) {
        this.playerGraves.add(glist.getCompound(i));
      }
    }
  }

  public CompoundTag write() {
    CompoundTag data = new CompoundTag();
    ListTag glist = new ListTag();
    for (CompoundTag g : playerGraves) {
      glist.add(g);
    }
    data.put(ModTomb.MODID, glist);
    return data;
  }

  public static BlockPos getPos(CompoundTag grave) {
    return NbtUtils.readBlockPos(grave.getCompound("pos"));
  }

  public static String getDim(CompoundTag grave) {
    return grave.getString("dimension");
  }

  public static List<ItemStack> getDrops(CompoundTag grave) {
    ListTag drops = grave.getList("drops", 10);
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
    CompoundTag gd = getGrave(i);
    return String.format("[%d] (%s) (%s) {%d}", i, getDim(gd), getCoordinatesAsString(getPos(gd)), getDrops(gd).size());
  }
}
