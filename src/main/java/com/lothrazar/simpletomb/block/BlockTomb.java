package com.lothrazar.simpletomb.block;

import com.lothrazar.simpletomb.ModTomb;
import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.data.DeathHelper;
import com.lothrazar.simpletomb.data.LocationBlockPos;
import com.lothrazar.simpletomb.data.MessageType;
import com.lothrazar.simpletomb.helper.EntityHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockTomb extends BaseEntityBlock {

  public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
  public static final IntegerProperty MODEL_TEXTURE = IntegerProperty.create("model_texture", 0, 1);
  public static final BooleanProperty IS_ENGRAVED = BooleanProperty.create("is_engraved");
  private static final VoxelShape GROUND = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4D, 16.0D);
  protected final String name;
  protected final ModelTomb graveModel;

  public BlockTomb(Block.Properties properties, ModelTomb graveModel) {
    super(properties.noOcclusion().strength(-1.0F, 3600000.0F).noDrops());
    this.graveModel = graveModel;
    this.name = graveModel.getSerializedName();
  }

  @Override
  public RenderShape getRenderShape(BlockState bs) {
    return RenderShape.MODEL;
  }

  @Override
  public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
    return GROUND;
  }

  public ModelTomb getGraveType() {
    return this.graveModel;
  }

  @Override
  public String getDescriptionId() {
    return ModTomb.MODID + ".grave." + this.name;
  }

  @Override
  public boolean dropFromExplosion(Explosion explosionIn) {
    return false;
  }

  @Override
  public void onBlockExploded(BlockState state, Level world, BlockPos pos, Explosion explosion) {
    //  dont destroy/setair  super.onBlockExploded(state, world, pos, explosion);
  }

  public static TileEntityTomb getTileEntity(Level world, BlockPos pos) {
    BlockEntity tile = world.getBlockEntity(pos);
    return tile instanceof TileEntityTomb ? (TileEntityTomb) tile : null;
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new TileEntityTomb(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
    return createTickerHelper(type, TombRegistry.TOMBSTONETILEENTITY, world.isClientSide ? TileEntityTomb::clientTick : TileEntityTomb::serverTick);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING).add(IS_ENGRAVED).add(MODEL_TEXTURE);
  }

  @Override
  public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
    if (!world.isClientSide && entity.isShiftKeyDown() && entity.isAlive() &&
        EntityHelper.isValidPlayer(entity)) {
      activatePlayerGrave(world, pos, state, (ServerPlayer) entity);
    }
  }

  public static void activatePlayerGrave(Level world, BlockPos pos, BlockState state, Player player) {
    TileEntityTomb tile = BlockTomb.getTileEntity(world, pos);
    if (tile != null && player.isAlive()) {
      if (tile.onlyOwnersCanAccess() && !tile.isOwner(player)) {
        MessageType.MESSAGE_OPEN_GRAVE_NEED_OWNER.sendSpecialMessage(player);
        return;
      }
      //either you are the owner, or it has setting that says anyone can access
      tile.giveInventory(player);
      //clear saved loc
      DeathHelper.INSTANCE.deleteLastGrave(player);
      TombRegistry.GRAVE_KEY.removeKeyForGraveInInventory(player, new LocationBlockPos(pos, world));
    }
  }
}
