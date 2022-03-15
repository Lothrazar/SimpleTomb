package com.lothrazar.simpletomb.data;

import com.lothrazar.simpletomb.ModTomb;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public enum MessageType {

  MESSAGE_FAIL_TO_PLACE_GRAVE("message.fail_to_place_grave"),
  //open grave
  MESSAGE_OPEN_GRAVE_SUCCESS("message.open_grave.success"), MESSAGE_OPEN_GRAVE_NEED_OWNER("message.open_grave.need_owner"),
  //
  MESSAGE_NO_LOOT_FOR_GRAVE("message.no_loot_for_grave"), MESSAGE_NO_PLACE_FOR_GRAVE("message.no_place_for_grave"),
  //rendered as text in world on gravestone
  MESSAGE_DAY("message.day"), MESSAGE_RIP("message.rip"),
  //logs or tooltips
  MESSAGE_NEW_GRAVE("message.new_grave"), MESSAGE_JOURNEYMAP("message.journeymap"), MESSAGE_DISTANCE("message.distance");

  public static final Style MESSAGE_SPECIAL;
  static {
    MESSAGE_SPECIAL = Style.EMPTY.withColor(TextFormatting.GOLD);
  }
  private final String key;

  MessageType(String key) {
    this.key = key;
  }

  public String getKey() {
    return ModTomb.MODID + "." + this.key;
  }

  public ITextComponent getTranslationWithStyle(Style style, Object... params) {
    return new TranslationTextComponent(getKey(), params).setStyle(style);
  }

  public String getTranslation(Object... params) {
    return new TranslationTextComponent(getKey(), params).getString();
  }

  public void sendSpecialMessage(PlayerEntity sender, Object... params) {
    // 
    sender.sendMessage(this.getTranslationWithStyle(MESSAGE_SPECIAL, params), sender.getUUID());
  }
}
