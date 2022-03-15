package com.lothrazar.simpletomb.helper;

import javax.annotation.Nullable;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.ItemHandlerHelper;

public class EntityHelper {

  public static final String NBT_PLAYER_PERSISTED = "PlayerPersisted";

  public static boolean autoEquip(ItemStack stack, PlayerEntity player) {
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
        if (stack.getItem().isShield(stack, player) && player.setSlot(99, stack.copy())) {
          return true;
        }
      }
      EquipmentSlotType slot = stack.getItem().getEquipmentSlot(stack);
      boolean isElytra = false;
      if (slot == null) {
        if (stack.getItem() instanceof ArmorItem) {
          slot = ((ArmorItem) stack.getItem()).getSlot();
        }
        else {
          if (!(stack.getItem() instanceof ElytraItem)) {
            return false;
          }
          slot = EquipmentSlotType.CHEST;
          isElytra = true;
        }
      }
      else if (slot == EquipmentSlotType.CHEST) {
        isElytra = stack.getItem() instanceof ElytraItem;
      }
      int slotId = slot.getIndex();
      ItemStack stackInSlot = player.inventory.armor.get(slotId);
      if (stackInSlot.isEmpty()) {
        player.inventory.armor.set(slotId, stack.copy());
        return true;
      }
      if (slot != EquipmentSlotType.CHEST) {
        return false;
      }
      if (isElytra) {
        ItemHandlerHelper.giveItemToPlayer(player, stackInSlot.copy());
        player.inventory.armor.set(slotId, stack.copy());
        return true;
      }
    }
    return false;
  }

  public static boolean isValidPlayer(@Nullable Entity entity) {
    return entity instanceof PlayerEntity && !(entity instanceof FakePlayer);
  }

  public static boolean isValidPlayerMP(@Nullable Entity entity) {
    return isValidPlayer(entity) && !entity.level.isClientSide;
  }

  public static CompoundNBT getPersistentTag(PlayerEntity player) {
    CompoundNBT persistentData = player.getPersistentData();
    CompoundNBT persistentTag;
    if (persistentData.contains(NBT_PLAYER_PERSISTED)) {
      persistentTag = (CompoundNBT) persistentData.get(NBT_PLAYER_PERSISTED);
      return persistentTag;
    }
    else {
      persistentTag = new CompoundNBT();
      persistentData.put(NBT_PLAYER_PERSISTED, persistentTag);
      return persistentTag;
    }
  }
}
