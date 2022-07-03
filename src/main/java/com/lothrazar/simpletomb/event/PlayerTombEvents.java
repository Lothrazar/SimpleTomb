package com.lothrazar.simpletomb.event;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.Level;
import com.lothrazar.simpletomb.ConfigTomb;
import com.lothrazar.simpletomb.ModTomb;
import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.block.BlockEntityTomb;
import com.lothrazar.simpletomb.block.BlockTomb;
import com.lothrazar.simpletomb.data.DeathHelper;
import com.lothrazar.simpletomb.data.LocationBlockPos;
import com.lothrazar.simpletomb.data.MessageType;
import com.lothrazar.simpletomb.data.PlayerTombRecords;
import com.lothrazar.simpletomb.helper.EntityHelper;
import com.lothrazar.simpletomb.helper.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.world.ExplosionEvent.Detonate;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class PlayerTombEvents {

  public Map<UUID, PlayerTombRecords> grv = new HashMap<>();
  private static final String TOMB_FILE_EXT = ".mctomb";
  private static final String TB_SOULBOUND_STACKS = "tb_soulbound_stacks";

  public PlayerTombRecords findGrave(UUID id) {
    if (grv.containsKey(id)) {
      return grv.get(id);
    }
    return null;
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public void onPlayerLogged(PlayerLoggedInEvent event) {
    if (EntityHelper.isValidPlayerMP(event.getPlayer())) {
      ServerPlayer player = (ServerPlayer) event.getPlayer();
      assert player.getServer() != null;
      CompoundTag playerData = player.getPersistentData();
      CompoundTag persistantData;
      if (playerData.contains(EntityHelper.NBT_PLAYER_PERSISTED)) {
        persistantData = playerData.getCompound(EntityHelper.NBT_PLAYER_PERSISTED);
      }
      else {
        persistantData = new CompoundTag();
        playerData.put(EntityHelper.NBT_PLAYER_PERSISTED, persistantData);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onDetonate(Detonate event) {
    event.getAffectedBlocks().removeIf(blockPos -> (event.getWorld().getBlockState(blockPos).getBlock() instanceof BlockTomb));
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    if (EntityHelper.isValidPlayerMP(player) && !player.isSpectator()) {
      CompoundTag persistentTag = EntityHelper.getPersistentTag(player);
      ListTag stackList = persistentTag.getList(TB_SOULBOUND_STACKS, 10);
      for (int i = 0; i < stackList.size(); ++i) {
        //        ItemStack.
        ItemStack stack = ItemStack.of(stackList.getCompound(i));
        if (!stack.isEmpty()) {
          ItemHandlerHelper.giveItemToPlayer(player, stack);
        }
      }
      persistentTag.remove(TB_SOULBOUND_STACKS);
      player.inventoryMenu.broadcastChanges();
    }
  }

  private void storeSoulboundsOnBody(Player player, List<ItemStack> keys) {
    CompoundTag persistentTag = EntityHelper.getPersistentTag(player);
    ListTag stackList = new ListTag();
    persistentTag.put(TB_SOULBOUND_STACKS, stackList);
    for (ItemStack key : keys) {
      stackList.add(key.serializeNBT());
    }
    keys.clear();
  }
  //  private void storeIntegerStorageMap(PlayerEntity player) {
  //    //  for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
  //    //      ModTomb.LOGGER.info(i + " player inventory = " + player.inventory.getStackInSlot(i));
  //    //TODO: create an ITEMIDSLOT -> MAP
  //    //to remap those first
  //    //
  //    //
  //    // }
  //  }
  //
  //  @SubscribeEvent
  //  public void onPlayerDeath(LivingDeathEvent event) {
  //    if (!ConfigTomb.TOMBENABLED.get()) {
  //      return;
  //    }
  //    if (event.getEntityLiving() instanceof PlayerEntity) {
  //      PlayerEntity player = (PlayerEntity) event.getEntityLiving();
  //      storeIntegerStorageMap(player);
  //    }
  //  }

  @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
  public void onPlayerDrops(LivingDropsEvent event) {
    if (!ConfigTomb.TOMBENABLED.get()) {
      return;
    }
    if (!EntityHelper.isValidPlayer(event.getEntityLiving()) ||
        WorldHelper.isRuleKeepInventory((Player) event.getEntityLiving())) {
      return;
    }
    saveBackup(event);
    placeTombstone(event);
  }

  @SubscribeEvent
  public void onSaveFile(PlayerEvent.SaveToFile event) {
    Player player = event.getPlayer();
    File mctomb = new File(event.getPlayerDirectory(), player.getUUID() + TOMB_FILE_EXT);
    //
    //save player data to the file 
    if (grv.containsKey(player.getUUID())) {
      //yes i have data to save
      PlayerTombRecords dataToSave = grv.get(player.getUUID());
      CompoundTag data = dataToSave.write();
      try {
        FileOutputStream fileoutputstream = new FileOutputStream(mctomb);
        NbtIo.writeCompressed(data, fileoutputstream);
        fileoutputstream.close();
      }
      catch (IOException e) {
        ModTomb.LOGGER.error("IO", e);
      }
    }
  }

  @SubscribeEvent
  public void onLoadFile(PlayerEvent.LoadFromFile event) {
    Player player = event.getPlayer();
    File mctomb = new File(event.getPlayerDirectory(), player.getUUID() + TOMB_FILE_EXT);
    if (mctomb.exists()) {
      try {
        FileInputStream fileinputstream = new FileInputStream(mctomb);
        CompoundTag data = NbtIo.readCompressed(fileinputstream);
        fileinputstream.close();
        PlayerTombRecords dataLoaded = new PlayerTombRecords();
        dataLoaded.read(data, player.getUUID());
        if (grv.containsKey(player.getUUID())) {
          //overwrite list
          grv.put(player.getUUID(), dataLoaded);
        }
        else {
          //set list
          grv.put(player.getUUID(), dataLoaded);
        }
      }
      catch (Exception e) {
        ModTomb.LOGGER.error("IO", e);
      }
    }
    //LOAD player data
  }

  private void saveBackup(LivingDropsEvent event) {
    ServerPlayer player = (ServerPlayer) event.getEntityLiving();
    //    ServerWorld world = player.getServerWorld();
    Iterator<ItemEntity> it = event.getDrops().iterator();
    ListTag drops = new ListTag();
    boolean isEmpty = true; //empty unless one non-key item found
    CompoundTag tombstoneTag = new CompoundTag();
    while (it.hasNext()) {
      ItemEntity entityItem = it.next();
      if (entityItem != null && !entityItem.getItem().isEmpty()) {
        ItemStack stack = entityItem.getItem();
        //        stuff.add(stack);
        drops.add(stack.save(new CompoundTag()));
        if (stack.getItem() != TombRegistry.GRAVE_KEY.get()) {
          isEmpty = false;
        }
      }
    }
    if (!isEmpty) {
      //NEW data model. write to string
      //timestamp 
      tombstoneTag.putLong("timestamp", System.currentTimeMillis());
      tombstoneTag.put("drops", drops);
      tombstoneTag.put("pos", NbtUtils.writeBlockPos(player.blockPosition()));
      tombstoneTag.putString("dimension", player.level.dimension().location().toString());
      UUID pid = player.getUUID();
      tombstoneTag.putString("playerid", pid.toString());
      tombstoneTag.putString("playername", player.getDisplayName().getString());
      //    world.getSavedData().ge
      //save to file
      if (grv.containsKey(pid)) {
        grv.get(pid).playerGraves.add(tombstoneTag);
      }
      else {
        grv.put(pid, new PlayerTombRecords(pid, tombstoneTag));
      }
    }
  }

  private void placeTombstone(LivingDropsEvent event) {
    ServerPlayer player = (ServerPlayer) event.getEntityLiving();
    ServerLevel world = player.getLevel();
    Iterator<ItemEntity> it = event.getDrops().iterator();
    ArrayList<ItemStack> keys = new ArrayList<>();
    while (it.hasNext()) {
      ItemEntity entityItem = it.next();
      if (entityItem != null && !entityItem.getItem().isEmpty()) {
        ItemStack stack = entityItem.getItem();
        if (stack.getItem() == TombRegistry.GRAVE_KEY.get()) {
          keys.add(stack.copy());
          it.remove();
        }
      }
    }
    List<ItemEntity> itemsPickedUpFromGround = pickupFromGround(player, keys);
    this.storeSoulboundsOnBody(player, keys);
    boolean hasDrop = event.getDrops().size() > 0 || itemsPickedUpFromGround.size() > 0;
    if (!hasDrop) {
      MessageType.MESSAGE_NO_LOOT_FOR_GRAVE.sendSpecialMessage(player);
      return;
    }
    BlockPos initPos = WorldHelper.getInitialPos(world, new BlockPos(player.blockPosition()));
    LocationBlockPos spawnPos = WorldHelper.findGraveSpawn(player, initPos);
    if (spawnPos == null || spawnPos.toBlockPos() == null) {
      //found a block but its not air, cant use it
      MessageType.MESSAGE_NO_PLACE_FOR_GRAVE.sendSpecialMessage(player);
      ModTomb.LOGGER.log(Level.INFO, MessageType.MESSAGE_NO_PLACE_FOR_GRAVE.getTranslation());
      return;
    }
    Direction facing = player.getDirection().getOpposite();
    BlockState state = getRandomGrave(world, facing);
    boolean wasPlaced = WorldHelper.placeGrave(world, spawnPos.toBlockPos(), state);
    if (!wasPlaced) {
      sendFailMessage(player);
      return;
    }
    BlockEntity tile = world.getBlockEntity(spawnPos.toBlockPos());
    IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
    if (!(tile instanceof BlockEntityTomb)
        || itemHandler == null) {
      //either block failed to place, or tile entity wasnt started somehow
      sendFailMessage(player);
      return;
    }
    //else grave success
    BlockEntityTomb grave = (BlockEntityTomb) tile;
    grave.initTombstoneOwner(player);
    if (ConfigTomb.KEYGIVEN.get()) {
      ItemStack key = new ItemStack(TombRegistry.GRAVE_KEY.get());
      TombRegistry.GRAVE_KEY.get().setTombPos(key, spawnPos);
      setKeyName(player, key);
      keys.add(key);
    }
    this.storeSoulboundsOnBody(player, keys);
    // we know itemHandler is not null now
    for (ItemEntity entityItem : event.getDrops()) {
      if (!entityItem.getItem().isEmpty()) {
        ItemHandlerHelper.insertItemStacked(itemHandler, entityItem.getItem().copy(), false);
        entityItem.setItem(ItemStack.EMPTY);
      }
    }
    for (ItemEntity entityItem : itemsPickedUpFromGround) {
      ItemHandlerHelper.insertItemStacked(itemHandler, entityItem.getItem(), false);
      entityItem.setItem(ItemStack.EMPTY);
    }
    world.sendBlockUpdated(spawnPos.toBlockPos(), state, state, 2);
    //it has been placed
    DeathHelper.INSTANCE.putLastGrave(player, spawnPos);
    if (ConfigTomb.TOMBLOG.get()) {
      ModTomb.LOGGER.info(MessageType.MESSAGE_NEW_GRAVE.getTranslation()
          + String.format("(%d, %d, %d) " + spawnPos.dim, spawnPos.x, spawnPos.y, spawnPos.z));
    }
    if (ConfigTomb.TOMBCHAT.get()) {
      MessageType.MESSAGE_NEW_GRAVE.sendSpecialMessage(player);
      MessageType.MESSAGE_JOURNEYMAP.sendSpecialMessage(player, spawnPos.x, spawnPos.y, spawnPos.z, spawnPos.dim);
    }
  }

  public static void sendFailMessage(ServerPlayer player) {
    MessageType.MESSAGE_FAIL_TO_PLACE_GRAVE.sendSpecialMessage(player);
    ModTomb.LOGGER.log(Level.INFO, MessageType.MESSAGE_FAIL_TO_PLACE_GRAVE.getTranslation());
  }

  public static void setKeyName(ServerPlayer player, ItemStack key) {
    putKeyName(player.getName().getString(), key);
  }

  public static void putKeyName(String player, ItemStack key) {
    if (ConfigTomb.KEYNAMED.get()) {
      MutableComponent text = Component.translatable(player);
      text.append(Component.literal(" "));
      text.append(key.getHoverName());
      text.withStyle(ChatFormatting.GOLD);
      key.setHoverName(text);
    }
  }

  static BlockState getRandomGrave(ServerLevel world, Direction facing) {
    //TODO: CONFIG or other selection of what the player wants
    BlockTomb[] graves = new BlockTomb[] {
        TombRegistry.GRAVE_SIMPLE.get(),
        TombRegistry.GRAVE_NORMAL.get(),
        TombRegistry.GRAVE_CROSS.get(),
        TombRegistry.TOMBSTONE.get(),
    };
    BlockState state = graves[world.random.nextInt(graves.length)].defaultBlockState();
    state = state.setValue(BlockTomb.FACING, facing);
    state = state.setValue(BlockTomb.MODEL_TEXTURE, world.random.nextInt(2));
    return state;
  }

  private List<ItemEntity> pickupFromGround(Player player, ArrayList<ItemStack> keys) {
    double range = ConfigTomb.TOMBEXTRAITEMS.get();
    if (range == 0) {
      return new ArrayList<>();
    }
    int posX = player.blockPosition().getX();
    int posY = player.blockPosition().getY();
    int posZ = player.blockPosition().getZ();
    return player.level.getEntitiesOfClass(ItemEntity.class, new AABB(
        posX - range,
        posY - range,
        posZ - range,
        posX + range,
        posY + range,
        posZ + range));
  }
}
