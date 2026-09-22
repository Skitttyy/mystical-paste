package fat.vonware.features.modules.player;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Phase.PacketUtil;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoRotate extends Module {
   protected final Setting<Boolean> noForceLook = new Setting<Boolean>("NoForceLook", true, "Prevents server position-correction packets from forcing your camera yaw and pitch.");
   protected final Setting<Boolean> async = new Setting<Boolean>("Async", false, "Uses the alternate asynchronous NoRotate handling path.");

   public NoRotate() {
      super("NoRotate", "no force look", Module.Category.PLAYER, true, false, false);
      this.register(this.noForceLook);
      this.register(this.async);
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketPlayerPosLook && (Boolean)this.noForceLook.getValue() && !event.isCanceled()) {
         SPacketPlayerPosLook packet = (SPacketPlayerPosLook)event.getPacket();
         event.setCanceled(true);
         if ((Boolean)this.async.getValue()) {
            PacketUtil.handlePosLook(packet, mc.player, true);
         } else {
            mc.addScheduledTask(() -> PacketUtil.handlePosLook(packet, mc.player, true));
         }
      }

   }
}
