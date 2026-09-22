package fat.vonware.features.modules.misc;

import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.RenderUtil;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import org.lwjgl.input.Mouse;

public class AirPlace extends Module {
   private final Setting<Boolean> packet = new Setting<Boolean>("PacketPlace", false, "Places the block using a placement packet instead of the normal controller action.");
   private final Setting<Boolean> box = new Setting<Boolean>("Box", false, "Renders a filled box at the air placement position.");
   private final Setting<Boolean> outline = new Setting<Boolean>("Outline", true, "Renders an outline around the air placement position.");
   private final Setting<Float> lineWidth = new Setting<Float>("LineWidth", 1.0F, 0.1F, 3.0F, "Controls the thickness of the air placement outline.");

   public AirPlace() {
      super("AirPlace", "Places blocks in the air", Module.Category.MISC, true, false, false);
      this.register(this.box);
      this.register(this.outline);
      this.register(this.lineWidth);
      this.register(this.packet);
   }

   public void onTick() {
      if (mc.objectMouseOver != null && mc.player.inventory.getStackInSlot(mc.player.inventory.currentItem).getItem() instanceof ItemBlock && mc.objectMouseOver.typeOfHit == Type.MISS && mc.world.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() == Blocks.AIR && Mouse.isButtonDown(1)) {
         RayTraceResult trace = mc.objectMouseOver;
         if ((Boolean)this.packet.getValue()) {
            mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(trace.getBlockPos(), trace.sideHit, EnumHand.MAIN_HAND, (float)(trace.hitVec.x - (double)trace.getBlockPos().getX()), (float)(trace.hitVec.y - (double)trace.getBlockPos().getY()), (float)(trace.hitVec.z - (double)trace.getBlockPos().getZ())));
         } else {
            mc.playerController.processRightClickBlock(mc.player, mc.world, trace.getBlockPos(), trace.sideHit, trace.hitVec, EnumHand.MAIN_HAND);
         }
      }

   }

   public void onRender3D(Render3DEvent event) {
      if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == Type.MISS && mc.world.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() == Blocks.AIR) {
         RenderUtil.drawAirBlockBox(mc.objectMouseOver.getBlockPos(), 0, -16777216, (Boolean)this.box.getValue(), (Boolean)this.outline.getValue(), (Float)this.lineWidth.getValue());
      }

   }
}
