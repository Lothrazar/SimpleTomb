package com.lothrazar.simpletomb.event;

import com.lothrazar.simpletomb.ModTomb;
import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.block.TileEntityTomb;
import com.lothrazar.simpletomb.data.LocationBlockPos;
import com.lothrazar.simpletomb.data.PlayerTombRecords;
import com.lothrazar.simpletomb.data.TombCommands;
import com.lothrazar.simpletomb.helper.WorldHelper;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class CommandEvents {

  private static final String ARG_SELECTED = "selected";
  private static final String ARG_PLAYER = "player";

  @SubscribeEvent
  public void onRegisterCommandsEvent(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSource> r = event.getDispatcher();
    r.register(LiteralArgumentBuilder.<CommandSource> literal(ModTomb.MODID)
        .requires((p) -> {
          return p.hasPermission(3);
        })
        .then(Commands.literal(TombCommands.RESTORE.toString())
            .then(Commands.argument(ARG_PLAYER, GameProfileArgument.gameProfile()).suggests((cs, b) -> buildPlayerArg(cs, b))
                .then(Commands.argument(ARG_SELECTED, IntegerArgumentType.integer())
                    .executes(x -> {
                      return exeRestore(x, getPlayerProfile(x), IntegerArgumentType.getInteger(x, ARG_SELECTED));
                    }))))
        .then(Commands.literal(TombCommands.KEY.toString())
            .then(Commands.argument(ARG_PLAYER, GameProfileArgument.gameProfile()).suggests((cs, b) -> buildPlayerArg(cs, b))
                .then(Commands.argument(ARG_SELECTED, IntegerArgumentType.integer())
                    .executes(x -> {
                      return exeKey(x, getPlayerProfile(x), IntegerArgumentType.getInteger(x, ARG_SELECTED));
                    }))))
        .then(Commands.literal(TombCommands.LIST.toString())
            .then(Commands.argument(ARG_PLAYER, GameProfileArgument.gameProfile()).suggests((cs, b) -> buildPlayerArg(cs, b))
                .executes(x -> {
                  return exeList(x, getPlayerProfile(x));
                })))
        .then(Commands.literal(TombCommands.DELETE.toString())
            .then(Commands.argument(ARG_PLAYER, GameProfileArgument.gameProfile()).suggests((cs, b) -> buildPlayerArg(cs, b))
                .executes(x -> {
                  return exeDelete(x, getPlayerProfile(x));
                })))
    // more go here
    );
  }

  private CompletableFuture<Suggestions> buildPlayerArg(CommandContext<CommandSource> cs, SuggestionsBuilder b) {
    return ISuggestionProvider.suggest(cs.getSource().getServer().getPlayerList().getPlayers().stream().map(p -> p.getGameProfile().getName()), b);
  }

  private GameProfile getPlayerProfile(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
    return GameProfileArgument.getGameProfiles(ctx, ARG_PLAYER).stream().findFirst().orElse(null);
  }

  private int exeDelete(CommandContext<CommandSource> ctx, GameProfile target) throws CommandSyntaxException {
    PlayerTombRecords found = ModTomb.GLOBAL.findGrave(target.getId());
    if (found != null) {
      int previous = found.playerGraves.size();
      found.deleteAll();
      TranslationTextComponent msg = new TranslationTextComponent("Deleted: " + previous);
      msg.setStyle(Style.EMPTY.withColor(TextFormatting.GOLD));
      ctx.getSource().sendSuccess(msg, false);
    }
    return 0;
  }

  private int exeList(CommandContext<CommandSource> ctx, GameProfile target) throws CommandSyntaxException {
    PlayerTombRecords found = ModTomb.GLOBAL.findGrave(target.getId());
    if (found != null && found.playerGraves.size() > 0) {
      for (int i = 0; i < found.playerGraves.size(); i++) {
        TranslationTextComponent msg = new TranslationTextComponent(found.toDisplayString(i));
        msg.setStyle(Style.EMPTY.withColor(TextFormatting.GOLD));
        ctx.getSource().sendSuccess(msg, false);
      }
    }
    else {
      TranslationTextComponent msg = new TranslationTextComponent("Found: #0");
      msg.setStyle(Style.EMPTY.withColor(TextFormatting.GOLD));
      ctx.getSource().sendSuccess(msg, false);
    }
    return 0;
  }

  private int exeKey(CommandContext<CommandSource> ctx, GameProfile target, int index) throws CommandSyntaxException {
    PlayerTombRecords found = ModTomb.GLOBAL.findGrave(target.getId());
    if (found != null) {
      CompoundNBT grave = found.playerGraves.get(index);
      if (grave == null) {
        ModTomb.LOGGER.error("Invalid grave index " + index + "; try between 0 and  " + (found.playerGraves.size() - 1));
        return 1;
      }
      LocationBlockPos spawnPos = new LocationBlockPos(PlayerTombRecords.getPos(grave), PlayerTombRecords.getDim(grave));
      ItemStack key = new ItemStack(TombRegistry.GRAVE_KEY);
      TombRegistry.GRAVE_KEY.setTombPos(key, spawnPos);
      PlayerTombEvents.putKeyName(target.getName(), key);
      // key for u
      StringTextComponent msg = new StringTextComponent("Attempting to give the key for tomb [" + index + "] to player " + target.getName() + ":" + target.getId());
      ctx.getSource().sendSuccess(msg, false);

      ServerPlayerEntity user = ctx.getSource().getServer().getPlayerList().getPlayer(target.getId());
      ItemHandlerHelper.giveItemToPlayer(user, key);
    }
    return 0;
  }

  private int exeRestore(CommandContext<CommandSource> ctx, GameProfile target, int index) throws CommandSyntaxException {
    TranslationTextComponent msg = new TranslationTextComponent("Attempting to restore tomb [" + index + "] for player " + target.getName() + ":" + target.getId());
    ctx.getSource().sendSuccess(msg, false);
    PlayerTombRecords found = ModTomb.GLOBAL.findGrave(target.getId());
    if (found != null) {
      CompoundNBT grave = found.playerGraves.get(index);
      if (grave == null) {
        ModTomb.LOGGER.error("Invalid grave index " + index + "; try between 0 and  " + (found.playerGraves.size() - 1));
        return 1;
      }
      BlockPos pos = PlayerTombRecords.getPos(grave);
      String dim = PlayerTombRecords.getDim(grave);
      //      ModTomb.LOGGER.error("found  at" + pos + " in " + dim);
      List<ItemStack> drops = PlayerTombRecords.getDrops(grave);
      //      ModTomb.LOGGER.error("items contained " + drops.size());
      //TODO: is this dupe code from location class?
      RegistryKey<World> dimKey = RegistryKey.create(Registry.DIMENSION_REGISTRY, ResourceLocation.tryParse(dim));
      ServerWorld targetWorld = ctx.getSource().getLevel().getServer().getLevel(dimKey);
      BlockState state = PlayerTombEvents.getRandomGrave(targetWorld, Direction.NORTH);
      boolean wasPlaced = WorldHelper.placeGrave(targetWorld, pos, state);
      if (wasPlaced) {
        //fill it up
        TileEntityTomb tile = (TileEntityTomb) targetWorld.getBlockEntity(pos);
        tile.initTombstoneOwner(target);
        IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
        //        ItemHandlerHelper.ins
        for (ItemStack d : drops) {
          ItemHandlerHelper.insertItemStacked(itemHandler, d.copy(), false);
        }
      }
      msg = new TranslationTextComponent("Restored tomb with at [" + pos + "] in " + dim);
      ctx.getSource().sendSuccess(msg, false);
    }
    return 0;
  }
  //
  //  private void badCommandMsg(ServerPlayerEntity player) {
  //    //.setStyle(Style.EMPTY.setFormatting(TextFormatting.GOLD))
  //    player.sendMessage(new TranslationTextComponent(ModTomb.MODID + ".commands.null"), player.getUniqueID());
  //    player.sendMessage(new TranslationTextComponent("[" + String.join(", ", SUBCOMMANDS) + "]"), player.getUniqueID());
  //  }
}
