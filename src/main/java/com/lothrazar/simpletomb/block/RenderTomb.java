package com.lothrazar.simpletomb.block;

import com.lothrazar.simpletomb.data.MessageType;
import com.lothrazar.simpletomb.helper.WorldHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.SkullTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderTomb extends TileEntityRenderer<TileEntityTomb> {

  private static final ResourceLocation SKELETON_HEAD = new ResourceLocation("minecraft", "textures/entity/skeleton/skeleton.png");

  public RenderTomb(TileEntityRendererDispatcher d) {
    super(d);
  }

  private static final String TIME_FORMAT = "HH:mm:ss";
  private static final String DATE_FORMAT = "yyyy/MM/dd";

  @Override
  public void render(TileEntityTomb te, float partialTicks, MatrixStack matrixStack,
      IRenderTypeBuffer iRenderTypeBuffer, int light, int destroyStage) {
    if (te == null) {
      return;
    }
    if (!te.hasOwner()) {
      return;
    }
    BlockState knownState = te.getLevel().getBlockState(te.getBlockPos());
    if (!(knownState.getBlock() instanceof BlockTomb)) {
      return;
    }
    Direction facing = knownState.getValue(BlockTomb.FACING);
    BlockTomb grave = (BlockTomb) knownState.getBlock();
    ModelTomb graveModel = grave.getGraveType();
    renderHalloween(matrixStack, iRenderTypeBuffer, graveModel, facing, light, WorldHelper.isNight(te.getLevel()));
    light = 0xf000f0;
    int rotationIndex;
    float modX = 0.5F, modY, modZ = 0.5F;
    float value;
    switch (graveModel) {
      case GRAVE_CROSS:
        value = 0.25f;
        modY = 0.06375f;
      break;
      case GRAVE_NORMAL:
        value = 0.12625f;
        modY = 0.5f;
      break;
      case GRAVE_SIMPLE:
      default:
        value = 0.18875f;
        modY = 0.4f;
      break;
    }
    boolean isCross = graveModel == ModelTomb.GRAVE_CROSS;
    switch (facing) {
      case SOUTH:
        rotationIndex = 0;
        if (isCross) {
          modZ = 1f - value;
        }
        else {
          modZ = value;
        }
      break;
      case WEST:
        rotationIndex = -1;
        if (isCross) {
          modX = value;
        }
        else {
          modX = 1f - value;
        }
      break;
      case EAST:
        rotationIndex = 1;
        if (isCross) {
          modX = 1f - value;
        }
        else {
          modX = value;
        }
      break;
      case NORTH:
      default:
        rotationIndex = 2;
        if (isCross) {
          modZ = value;
        }
        else {
          modZ = 1f - value;
        }
    }
    matrixStack.pushPose();
    matrixStack.translate(modX, modY, modZ);
    matrixStack.mulPose(Vector3f.XP.rotationDegrees(180f));
    if (isCross) {
      switch (facing) {
        case SOUTH:
          matrixStack.mulPose(Vector3f.XP.rotationDegrees(-90f));
        break;
        case WEST:
          matrixStack.mulPose(Vector3f.ZP.rotationDegrees(90f));
        break;
        case EAST:
          matrixStack.mulPose(Vector3f.ZP.rotationDegrees(-90f));
        break;
        case NORTH:
        default:
          matrixStack.mulPose(Vector3f.XP.rotationDegrees(90f));
        break;
      }
    }
    matrixStack.mulPose(Vector3f.YP.rotationDegrees(-90f * rotationIndex)); // horizontal rot
    FontRenderer fontRender = this.renderer.font;
    int textColor = 0xFFFFFFFF;
    // rip message
    showString(TextFormatting.BOLD + MessageType.MESSAGE_RIP.getTranslation(), matrixStack, iRenderTypeBuffer, fontRender, 0,
        textColor, 0.007f, light);
    // owner message
    showString(TextFormatting.BOLD + te.getOwnerName(), matrixStack, iRenderTypeBuffer, fontRender, 11, textColor, 0.005f, light);
    // death date message
    float scaleForDate = 0.004f;
    // time goes 72 times faster than real time
    long days = te.timer / 24000; // TODO incorrect, tiles don't always tick, store gametime
    String dateString = MessageType.MESSAGE_DAY.getTranslation(days);
    showString(TextFormatting.BOLD + dateString, matrixStack, iRenderTypeBuffer, fontRender, 20, textColor, scaleForDate, light);
    Date date = new Date(te.getOwnerDeathTime());
    String fdateString = new SimpleDateFormat(DATE_FORMAT).format(date);
    String timeString = new SimpleDateFormat(TIME_FORMAT).format(date);
    showString(TextFormatting.BOLD + fdateString, matrixStack, iRenderTypeBuffer, fontRender, 36, textColor, scaleForDate, light);
    showString(TextFormatting.BOLD + timeString, matrixStack, iRenderTypeBuffer, fontRender, 46, textColor, scaleForDate, light);
    matrixStack.popPose();
  }

  private void showString(String content, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, FontRenderer fontRenderer, int posY, int color, float scale, int light) {
    matrixStack.pushPose();
    matrixStack.scale(scale, scale, scale);
    fontRenderer.drawInBatch(content, -fontRenderer.width(content) / 2, posY - 30, color, false, matrixStack.last().pose(), iRenderTypeBuffer, false, 0, light);
    matrixStack.popPose();
  }

  @SuppressWarnings("deprecation")
  private void renderHalloween(MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, ModelTomb graveModel, Direction facing, int light, boolean isNight) {
    RenderSystem.enableRescaleNormal();
    RenderSystem.pushMatrix();
    RenderSystem.disableCull();
    RenderSystem.enableAlphaTest();
    float decoX = 0.5f, decoY = 0.07f, decoZ = 0.5f;
    switch (graveModel) {
      case GRAVE_NORMAL:
        decoY += 0.35f;
      break;
      case GRAVE_CROSS:
        if (facing == Direction.SOUTH) {
          decoX -= 0.2f;
        }
        else if (facing == Direction.WEST) {
          decoZ -= 0.2f;
        }
        else if (facing == Direction.EAST) {
          decoZ += 0.2f;
        }
        else {
          decoX += 0.2f;
        }
      break;
      case GRAVE_SIMPLE:
      default:
        decoY += 0.1f;
      break;
    }
    Minecraft.getInstance().textureManager.bind(SKELETON_HEAD);
    matrixStack.pushPose();
    matrixStack.translate(decoX, decoY, decoZ);
    matrixStack.mulPose(Vector3f.YP.rotationDegrees(facing.toYRot() + (facing == Direction.SOUTH || facing == Direction.NORTH ? 180 : 0)));
    if (graveModel == ModelTomb.GRAVE_NORMAL || graveModel == ModelTomb.GRAVE_SIMPLE) {
      matrixStack.scale(0.2f, 0.2f, 0.2f);
      ItemStack stack = new ItemStack(isNight ? Blocks.JACK_O_LANTERN : Blocks.PUMPKIN);
      Minecraft.getInstance().getItemRenderer().render(stack, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.NONE, false, matrixStack, iRenderTypeBuffer, 15728880,
          net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, Minecraft.getInstance().getItemRenderer().getModel(stack, null, null));
    }
    else {
      matrixStack.scale(0.3f, 0.3f, 0.3f);
      SkullTileEntityRenderer.renderSkull(null, 1f, SkullBlock.Types.SKELETON, null, 0f, matrixStack, iRenderTypeBuffer, isNight ? 0xf000f0 : light);
    }
    matrixStack.popPose();
    RenderSystem.popMatrix();
  }
}
