package com.lothrazar.simpletomb.block;

import net.minecraft.util.IStringSerializable;

public enum ModelTomb implements IStringSerializable {

  GRAVE_SIMPLE("grave_simple"), GRAVE_NORMAL("grave_normal"), GRAVE_CROSS("grave_cross"), GRAVE_TOMB("tombstone");

  private final String name;

  ModelTomb(String name) {
    this.name = name;
  }

  public static ModelTomb getModel(int id) {
    return id >= 0 && id < values().length ? values()[id] : getDefault();
  }

  public static ModelTomb getDefault() {
    return GRAVE_SIMPLE;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public String getSerializedName() {
    return this.name;
  }
}
