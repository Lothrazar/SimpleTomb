package com.lothrazar.simpletomb.block;

import java.util.UUID;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.data.MessageType;
import com.lothrazar.simpletomb.helper.EntityHelper;
import com.lothrazar.simpletomb.helper.WorldHelper;
import com.lothrazar.simpletomb.proxy.ClientUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityTomb extends BlockEntity {

  private static final int SOULTIMER = 100;

  public TileEntityTomb(BlockPos pos, BlockState blockState) {
    super(TombRegistry.TOMBSTONETILEENTITY, pos, blockState);
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

  public void giveInventory(@Nullable Player player) {
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

  private void removeGraveBy(@Nullable Player player) {
    if (this.level != null) {
      WorldHelper.removeNoEvent(this.level, this.worldPosition);
      if (player != null) {
        this.level.playSound(player,
            player.blockPosition(),
            SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.0F);
      }
    }
  }

  public void initTombstoneOwner(Player owner) {
    this.deathDate = System.currentTimeMillis();
    this.ownerName = owner.getDisplayName().getString();
    this.ownerId = owner.getUUID();
  }

  public void initTombstoneOwner(GameProfile owner) {
    this.deathDate = 0;
    this.ownerName = owner.getName();
    this.ownerId = owner.getId();
  }

  public boolean isOwner(Player owner) {
    if (ownerId == null || owner == null || !hasOwner()) {
      return false;
    }
    //dont match on name. id is always set anyway 
    return this.ownerId.equals(owner.getUUID());
  }

  @Override
  public AABB getRenderBoundingBox() {
    double renderExtension = 1.0D;
    return new AABB(
        this.worldPosition.getX() - renderExtension,
        this.worldPosition.getY() - renderExtension,
        this.worldPosition.getZ() - renderExtension,
        this.worldPosition.getX() + 1 + renderExtension,
        this.worldPosition.getY() + 1 + renderExtension,
        this.worldPosition.getZ() + 1 + renderExtension);
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
  public CompoundTag save(CompoundTag compound) {
    compound.putString("ownerName", this.ownerName);
    compound.putLong("deathDate", this.deathDate);
    compound.putInt("countTicks", this.timer);
    if (this.ownerId != null) {
      compound.putUUID("ownerid", this.ownerId);
    }
    handler.ifPresent(h -> {
      CompoundTag ct = ((INBTSerializable<CompoundTag>) h).serializeNBT();
      compound.put("inv", ct);
    });
    compound.putBoolean("onlyOwnersAccess", this.onlyOwnersAccess);
    return super.save(compound);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void load(CompoundTag compound) {
    this.ownerName = compound.getString("ownerName");
    this.deathDate = compound.getLong("deathDate");
    this.timer = compound.getInt("countTicks");
    CompoundTag invTag = compound.getCompound("inv");
    handler.ifPresent(h -> ((INBTSerializable<CompoundTag>) h).deserializeNBT(invTag));
    if (compound.hasUUID("ownerid")) {
      this.ownerId = compound.getUUID("ownerid");
    }
    this.onlyOwnersAccess = compound.getBoolean("onlyOwnersAccess");
    super.load(compound);
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
          Containers.dropItemStack(
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
  public CompoundTag getUpdateTag() {
    CompoundTag compound = new CompoundTag();
    super.save(compound);
    compound.putString("ownerName", this.ownerName);
    compound.putLong("deathDate", this.deathDate);
    compound.putInt("countTicks", this.timer);
    return compound;
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this); // this.worldPosition, -1, getUpdateTag());
  }

  @Override
  public boolean triggerEvent(int id, int type) {
    return true;
  }

  @Override
  public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
    load(pkt.getTag());
  }
  //  public void tick() {
  //    this.timer++;
  //    if (this.timer % SOULTIMER == 0) {
  //      this.timer = 1;
  //      if (this.level.isClientSide) {
  //        ClientUtils.produceGraveSoul(this.level, this.worldPosition);
  //      }
  //    }
  //    if (this.level.isClientSide) {
  //      ClientUtils.produceGraveSmoke(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
  //    }
  //  }

  public static void clientTick(Level level, BlockPos blockPos, BlockState blockState, TileEntityTomb tile) {
    ClientUtils.produceGraveSmoke(level, tile.worldPosition.getX(), tile.worldPosition.getY(), tile.worldPosition.getZ());
    tile.timer++;
    if (tile.timer % SOULTIMER == 0) {
      ClientUtils.produceGraveSoul(level, tile.worldPosition);
      tile.timer = 1;
    }
  }

  public static <E extends BlockEntity> void serverTick(Level level, BlockPos blockPos, BlockState blockState, TileEntityTomb tile) {
    tile.timer++;
    if ((tile.timer - 1) % SOULTIMER == 0) {
      tile.timer = 1;
    }
  }
}
