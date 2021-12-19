package com.lothrazar.simpletomb.helper;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemHandlerHelper;

public class EntityHelper {

  public static final String NBT_PLAYER_PERSISTED = "PlayerPersisted";

  public static boolean autoEquip(ItemStack stack, Player player) {
    if (stack.isEmpty()) {
      return false;
    }
    ResourceLocation registryName = stack.getItem().getRegistryName();
    if (registryName == null) {
      return false;
    }
    if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BINDING_CURSE, stack) > 0) {
      return false;
    }
    if (stack.getMaxStackSize() == 1) {
      //
      if (ModList.get().isLoaded("curios")) {
        //then go
        if (CuriosHelper.autoEquip(stack, player)) {
          return true;
        }
      }
      //
      if (player.getOffhandItem().isEmpty()) {
        if (stack.getItem() == Items.SHIELD) { // stack.getItem().isShield(stack, player)) { // && player.setSlot(99, stack.copy())) {
          //          player.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
          player.setItemSlot(EquipmentSlot.OFFHAND, stack.copy());
          //          player.getInventory().setItem(99, stack.copy());
          return true;
        }
      }
      EquipmentSlot slot = stack.getItem().getEquipmentSlot(stack);
      boolean isElytra = false;
      if (slot == null) {
        if (stack.getItem() instanceof ArmorItem) {
          slot = ((ArmorItem) stack.getItem()).getSlot();
        }
        else {
          if (!(stack.getItem() instanceof ElytraItem)) {
            return false;
          }
          slot = EquipmentSlot.CHEST;
          isElytra = true;
        }
      }
      else if (slot == EquipmentSlot.CHEST) {
        isElytra = stack.getItem() instanceof ElytraItem;
      }
      int slotId = slot.getIndex();
      ItemStack stackInSlot = player.getInventory().armor.get(slotId);
      if (stackInSlot.isEmpty()) {
        player.getInventory().armor.set(slotId, stack.copy());
        return true;
      }
      if (slot != EquipmentSlot.CHEST) {
        return false;
      }
      if (isElytra) {
        ItemHandlerHelper.giveItemToPlayer(player, stackInSlot.copy());
        player.getInventory().armor.set(slotId, stack.copy());
        return true;
      }
    }
    return false;
  }

  public static boolean isValidPlayer(@Nullable Entity entity) {
    return entity instanceof Player && !(entity instanceof FakePlayer);
  }

  public static boolean isValidPlayerMP(@Nullable Entity entity) {
    return isValidPlayer(entity) && !entity.level.isClientSide;
  }

  public static CompoundTag getPersistentTag(Player player) {
    CompoundTag persistentData = player.getPersistentData();
    CompoundTag persistentTag;
    if (persistentData.contains(NBT_PLAYER_PERSISTED)) {
      persistentTag = (CompoundTag) persistentData.get(NBT_PLAYER_PERSISTED);
      return persistentTag;
    }
    else {
      persistentTag = new CompoundTag();
      persistentData.put(NBT_PLAYER_PERSISTED, persistentTag);
      return persistentTag;
    }
  }
}
