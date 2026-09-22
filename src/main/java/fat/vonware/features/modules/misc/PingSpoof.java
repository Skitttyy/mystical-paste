package fat.vonware.features.modules.misc;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PingSpoof extends Module {
   private final Setting<Integer> spoofAmount = new Setting<Integer>("Delay", 1738, 0, 5000, "Amount of latency in milliseconds added to delayed packets.");
   private final Setting<Boolean> jitter = new Setting<Boolean>("Jitter", false, "Varies the spoofed latency over time instead of using one fixed delay.");
   private final Setting<Integer> minAmount = new Setting<Integer>("Min Amount", 200, 0, 5000, (v) -> (Boolean)this.jitter.getValue(), "Minimum spoofed latency used while Jitter is enabled.");
   private final Setting<Integer> maxAmount = new Setting<Integer>("Max Amount", 1200, 0, 5000, (v) -> (Boolean)this.jitter.getValue(), "Maximum spoofed latency used while Jitter is enabled.");
   private final Setting<Integer> interval = new Setting<Integer>("Interval", 3000, 100, 10000, (v) -> (Boolean)this.jitter.getValue(), "How often the jittered spoof delay is changed.");
   Timer timer = new Timer();
   Timer jitterTimer = new Timer();
   CPacketKeepAlive cPacketKeepAlive = null;
   private int currentDelay;
   private boolean useMin = true;

   public PingSpoof() {
      super("PingSpoof", "spoofs your ping", Module.Category.MISC, true, false, false);
      this.register(this.spoofAmount);
      this.register(this.jitter);
      this.register(this.minAmount);
      this.register(this.maxAmount);
      this.register(this.interval);
      this.currentDelay = (Integer)this.minAmount.getValue();
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      int delay = (Boolean)this.jitter.getValue() ? this.currentDelay : (Integer)this.spoofAmount.getValue();
      if (event.getPacket() instanceof CPacketKeepAlive && this.cPacketKeepAlive != event.getPacket() && delay != 0) {
         this.cPacketKeepAlive = (CPacketKeepAlive)event.getPacket();
         event.setCanceled(true);
         this.timer.reset();
      }

   }

   public void onUpdate() {
      if ((Boolean)this.jitter.getValue() && this.jitterTimer.passed((long)(Integer)this.interval.getValue())) {
         this.currentDelay = this.useMin ? (Integer)this.minAmount.getValue() : (Integer)this.maxAmount.getValue();
         this.useMin = !this.useMin;
         this.jitterTimer.reset();
      }

      int delay = (Boolean)this.jitter.getValue() ? this.currentDelay : (Integer)this.spoofAmount.getValue();
      if (this.timer.passed((long)delay) && this.cPacketKeepAlive != null) {
         mc.player.connection.sendPacket(this.cPacketKeepAlive);
         this.cPacketKeepAlive = null;
      }

   }
}
