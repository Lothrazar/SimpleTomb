package com.lothrazar.simpletomb.block;

import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.data.MessageType;
import com.lothrazar.simpletomb.helper.EntityHelper;
import com.lothrazar.simpletomb.helper.WorldHelper;
import com.lothrazar.simpletomb.proxy.ClientUtils;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityTomb extends TileEntity implements ITickableTileEntity {

  private static final int SOULTIMER = 100;

  public TileEntityTomb() {
    super(TombRegistry.TOMBSTONETILEENTITY);
  }

  private LazyOptional<IItemHandler> handler = LazyOptional.of(this::createHandler);
  protected String ownerName = "";
  protected long deathDate;
  public int timer = 0;
  protected UUID ownerId = null;
  //nothing in game sets this.  
  // a server command could set this to false to let admins or anyone in 
  //but in normal survival gameplay, it stays true and thus requires owners to access their graves
  private boolean onlyOwnersAccess = true;

  private IItemHandler createHandler() {
    return new ItemStackHandler(120);
  }

  public void giveInventory(@Nullable PlayerEntity player) {
    IItemHandler inventory = handler.orElse(null);
    if (!this.level.isClientSide && player != null && !(player instanceof FakePlayer)) {
      //
      for (int i = inventory.getSlots() - 1; i >= 0; --i) {
        if (EntityHelper.autoEquip(inventory.getStackInSlot(i), player)) {
          inventory.extractItem(i, 64, false);
        }
      }
      IntStream.range(0, inventory.getSlots()).forEach(ix -> {
        ItemStack stack = inventory.getStackInSlot(ix);
        if (!stack.isEmpty()) {
          ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
          inventory.extractItem(ix, 64, false);
        }
      });
      this.removeGraveBy(player);
      if (player.inventoryMenu != null) {
        player.inventoryMenu.broadcastChanges();
      }
      MessageType.MESSAGE_OPEN_GRAVE_SUCCESS.sendSpecialMessage(player);
    }
  }

  public boolean onlyOwnersCanAccess() {
    return this.onlyOwnersAccess;
  }

  private void removeGraveBy(@Nullable PlayerEntity player) {
    if (this.level != null) {
      WorldHelper.removeNoEvent(this.level, this.worldPosition);
      if (player != null) {
        this.level.playSound(player,
            player.blockPosition(),
            SoundEvents.WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0F, 1.0F);
      }
    }
  }

  public void initTombstoneOwner(PlayerEntity owner) {
    this.deathDate = System.currentTimeMillis();
    this.ownerName = owner.getDisplayName().getString();
    this.ownerId = owner.getUUID();
  }

  public void initTombstoneOwner(GameProfile owner) {
    this.deathDate = 0;
    this.ownerName = owner.getName();
    this.ownerId = owner.getId();
  }

  public boolean isOwner(PlayerEntity owner) {
    if (ownerId == null || owner == null || !hasOwner()) {
      return false;
    }
    //dont match on name. id is always set anyway 
    return this.ownerId.equals(owner.getUUID());
  }

  @Override
  public AxisAlignedBB getRenderBoundingBox() {
    double renderExtension = 1.0D;
    return new AxisAlignedBB(
        this.worldPosition.getX() - renderExtension,
        this.worldPosition.getY() - renderExtension,
        this.worldPosition.getZ() - renderExtension,
        this.worldPosition.getX() + 1 + renderExtension,
        this.worldPosition.getY() + 1 + renderExtension,
        this.worldPosition.getZ() + 1 + renderExtension);
  }

  @Override
  public void tick() {
    this.timer++;
    if (this.timer % SOULTIMER == 0) {
      this.timer = 1;
      if (this.level.isClientSide) {
        ClientUtils.produceGraveSoul(this.level, this.worldPosition);
      }
    }
    if (this.level.isClientSide) {
      ClientUtils.produceGraveSmoke(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
    }
  }

  String getOwnerName() {
    return this.ownerName;
  }

  boolean hasOwner() {
    return ownerName != null && ownerName.length() > 0;
  }

  long getOwnerDeathTime() {
    return this.deathDate;
  }

  @SuppressWarnings("unchecked")
  @Override
  public CompoundNBT save(CompoundNBT compound) {
    compound.putString("ownerName", this.ownerName);
    compound.putLong("deathDate", this.deathDate);
    compound.putInt("countTicks", this.timer);
    if (this.ownerId != null) {
      compound.putUUID("ownerid", this.ownerId);
    }
    handler.ifPresent(h -> {
      CompoundNBT ct = ((INBTSerializable<CompoundNBT>) h).serializeNBT();
      compound.put("inv", ct);
    });
    compound.putBoolean("onlyOwnersAccess", this.onlyOwnersAccess);
    return super.save(compound);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void load(BlockState bs, CompoundNBT compound) {
    this.ownerName = compound.getString("ownerName");
    this.deathDate = compound.getLong("deathDate");
    this.timer = compound.getInt("countTicks");
    CompoundNBT invTag = compound.getCompound("inv");
    handler.ifPresent(h -> ((INBTSerializable<CompoundNBT>) h).deserializeNBT(invTag));
    if (compound.hasUUID("ownerid")) {
      this.ownerId = compound.getUUID("ownerid");
    }
    this.onlyOwnersAccess = compound.getBoolean("onlyOwnersAccess");
    super.load(bs, compound);
  }

  @Override
  public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
    if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return handler.cast();
    }
    return super.getCapability(cap, side);
  }

  @Override
  public void invalidateCaps() {
    IItemHandler inventory = handler.orElse(null);
    if (this.level != null && !this.level.isClientSide) {
      if (this.level.getBlockState(this.worldPosition).getBlock() instanceof BlockTomb) {
        return;
      }
      for (int i = 0; i < inventory.getSlots(); ++i) {
        ItemStack stack = inventory.getStackInSlot(i);
        if (!stack.isEmpty()) {
          InventoryHelper.dropItemStack(
              this.level,
              this.worldPosition.getX(),
              this.worldPosition.getY(),
              this.worldPosition.getZ(),
              inventory.extractItem(i, stack.getCount(), false));
        }
      }
    }
    super.invalidateCaps();
  }

  @Override
  public CompoundNBT getUpdateTag() {
    CompoundNBT compound = new CompoundNBT();
    super.save(compound);
    compound.putString("ownerName", this.ownerName);
    compound.putLong("deathDate", this.deathDate);
    compound.putInt("countTicks", this.timer);
    return compound;
  }

  @Override
  public SUpdateTileEntityPacket getUpdatePacket() {
    return new SUpdateTileEntityPacket(this.worldPosition, 1, getUpdateTag());
  }

  @Override
  public boolean triggerEvent(int id, int type) {
    return true;
  }

  @Override
  public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
    load(this.getBlockState(), pkt.getTag());
  }
}
