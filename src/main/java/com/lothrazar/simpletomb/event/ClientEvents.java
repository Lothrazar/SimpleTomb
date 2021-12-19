package com.lothrazar.simpletomb.event;

import com.lothrazar.simpletomb.ModTomb;
import com.lothrazar.simpletomb.TombRegistry;
import com.lothrazar.simpletomb.client.LineRenderType;
import com.lothrazar.simpletomb.data.LocationBlockPos;
import com.lothrazar.simpletomb.helper.WorldHelper;
import com.lothrazar.simpletomb.particle.ParticleGraveSmoke;
import com.lothrazar.simpletomb.particle.ParticleGraveSoul;
import com.lothrazar.simpletomb.particle.ParticleRotatingSmoke;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModTomb.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class ClientEvents {

  @SubscribeEvent(priority = EventPriority.LOW)
  public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
    ParticleEngine r = Minecraft.getInstance().particleEngine;
    r.register(TombRegistry.GRAVE_SMOKE, ParticleGraveSmoke.Factory::new);
    r.register(TombRegistry.ROTATING_SMOKE, ParticleRotatingSmoke.Factory::new);
    r.register(TombRegistry.SOUL, ParticleGraveSoul.Factory::new);
  }

  @SubscribeEvent
  public void render(RenderLevelLastEvent event) {
    LocalPlayer player = Minecraft.getInstance().player;
    if (player != null && player.level != null) {
      ItemStack stack = player.getMainHandItem();
      if (stack.getItem() == TombRegistry.GRAVE_KEY) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        LocationBlockPos location = TombRegistry.GRAVE_KEY.getTombPos(stack);
        if (location != null && !location.isOrigin() &&
            location.dim.equalsIgnoreCase(WorldHelper.dimensionToString(player.level)) &&
            player.level.isInWorldBounds(location.toBlockPos())) {
          PoseStack poseStack = event.getPoseStack();
          poseStack.pushPose();
          createBox(bufferSource, poseStack, location.x, location.y, location.z, 1.0F);
          poseStack.popPose();
        }
      }
    }
  }

  private static void createBox(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, float x, float y, float z, float offset) {
    long c = (System.currentTimeMillis() / 15L) % 360L;
    float[] color = WorldHelper.getHSBtoRGBF(c / 360f, 1f, 1f);
    Minecraft mc = Minecraft.getInstance();
    Vec3 cameraPosition = mc.gameRenderer.getMainCamera().getPosition();
    // get a closer pos if too far
    Vec3 vec = new Vec3(x, y, z).subtract(cameraPosition);
    if (vec.distanceTo(Vec3.ZERO) > 200d) { // could be 300
      vec = vec.normalize().scale(200d);
      x += vec.x;
      y += vec.y;
      z += vec.z;
    }
    RenderSystem.disableDepthTest();
    RenderType renderType = LineRenderType.tombLinesType();
    VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
    poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
    Matrix4f pose = poseStack.last().pose();
    vertexConsumer.vertex(pose, x, y, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y + offset, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y + offset, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y + offset, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y + offset, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y + offset, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y + offset, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y + offset, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y + offset, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y + offset, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y + offset, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y + offset, z).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x + offset, y, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    vertexConsumer.vertex(pose, x, y + offset, z + offset).color(color[0], color[1], color[2], 1.0F).endVertex();
    bufferSource.endBatch(renderType);
    RenderSystem.enableDepthTest();
  }
}
