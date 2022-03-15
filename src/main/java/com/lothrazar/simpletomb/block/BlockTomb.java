package com.lothrazar.simpletomb.block;

import com.lothrazar.simpletomb.ModTomb;
import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.data.DeathHelper;
import com.lothrazar.simpletomb.data.LocationBlockPos;
import com.lothrazar.simpletomb.data.MessageType;
import com.lothrazar.simpletomb.helper.EntityHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

public class BlockTomb extends Block {

  public static final EnumProperty<Direction> FACING = HorizontalBlock.FACING;
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
  public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
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
  public boolean isToolEffective(BlockState state, ToolType tool) {
    return false;
  }

  @Override
  public boolean dropFromExplosion(Explosion explosionIn) {
    return false;
  }

  @Override
  public void onBlockExploded(BlockState state, World world, BlockPos pos, Explosion explosion) {
    //  dont destroy/setair  super.onBlockExploded(state, world, pos, explosion);
  }

  public static TileEntityTomb getTileEntity(World world, BlockPos pos) {
    TileEntity tile = world.getBlockEntity(pos);
    return tile instanceof TileEntityTomb ? (TileEntityTomb) tile : null;
  }

  @Override
  public TileEntity createTileEntity(BlockState state, IBlockReader world) {
    return new TileEntityTomb();
  }

  @Override
  public boolean hasTileEntity(BlockState state) {
    return true;
  }

  @Override
  protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
    builder.add(FACING).add(IS_ENGRAVED).add(MODEL_TEXTURE);
  }

  @Override
  public void entityInside(BlockState state, World world, BlockPos pos, Entity entity) {
    if (!world.isClientSide && entity.isShiftKeyDown() && entity.isAlive() &&
        EntityHelper.isValidPlayer(entity)) {
      activatePlayerGrave(world, pos, state, (ServerPlayerEntity) entity);
    }
  }

  public static void activatePlayerGrave(World world, BlockPos pos, BlockState state, PlayerEntity player) {
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
