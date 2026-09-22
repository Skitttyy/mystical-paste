package fat.vonware.features.modules.client;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import fat.vonware.util.Phase.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.client.CPacketConfirmTransaction;
import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.network.play.server.SPacketConfirmTransaction;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraft.network.play.server.SPacketTabComplete;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class FastLatency extends Module {
   protected final Setting<Float> delay = new Setting<Float>("Delay", 5.0F, 0.0F, 10.0F);
   protected final Setting<Boolean> overrideGetLatency = new Setting<Boolean>("OverrideGetLatency", false);
   protected final Timer timer = new Timer();
   protected boolean waitingForPacket;
   protected long sentTime;
   protected long ping;

   public FastLatency() {
      super("FastLatency", "Pings the server faster to get a better network latency result", Module.Category.CLIENT, true, false, false);
      this.register(this.delay);
      this.register(this.overrideGetLatency);
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (event.phase == Phase.END) {
         if (mc.player != null) {
            if (!mc.isSingleplayer()) {
               if (this.timer.passed(15000L) && this.waitingForPacket) {
                  this.ping = 15000L;
                  this.waitingForPacket = false;
               } else {
                  if (this.timer.passed((long)((Float)this.delay.getValue() * 1000.0F)) && !this.waitingForPacket) {
                     PacketUtil.send(new CPacketTabComplete(mc.player.getName(), (BlockPos)null, false));
                     this.sentTime = Minecraft.getSystemTime();
                     this.waitingForPacket = true;
                  }

               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!mc.isSingleplayer()) {
         if (event.getPacket() instanceof SPacketKeepAlive) {
            PacketUtil.send(new CPacketKeepAlive(((SPacketKeepAlive)event.getPacket()).getId()));
            event.setCanceled(true);
         }

         if (event.getPacket() instanceof SPacketConfirmTransaction) {
            SPacketConfirmTransaction packet = (SPacketConfirmTransaction)event.getPacket();
            Container container = null;
            EntityPlayer player = mc.player;
            if (packet.getWindowId() == 0) {
               container = player.inventoryContainer;
            } else if (packet.getWindowId() == player.openContainer.windowId) {
               container = player.openContainer;
            }

            if (container != null && !packet.wasAccepted()) {
               PacketUtil.send(new CPacketConfirmTransaction(packet.getWindowId(), packet.getActionNumber(), true));
            }

            event.setCanceled(true);
         }

         if (event.getPacket() instanceof SPacketTabComplete) {
            SPacketTabComplete packet = (SPacketTabComplete)event.getPacket();
            if (this.isAwaiting(packet)) {
               this.ping = Minecraft.getSystemTime() - this.sentTime;
               this.waitingForPacket = false;
               this.timer.reset();
               event.setCanceled(true);
            }
         }

      }
   }

   private boolean isAwaiting(SPacketTabComplete packet) {
      return this.waitingForPacket && this.isValid(packet.getMatches());
   }

   private boolean isValid(String[] lel) {
      for(String s : lel) {
         if (s.equalsIgnoreCase(mc.player.getName())) {
            return true;
         }
      }

      return false;
   }

   public String getDisplayInfo() {
      return this.ping + "ms";
   }

   public void onEnable() {
      this.ping = 0L;
      this.waitingForPacket = false;
      this.timer.reset();
   }

   public void onLogout() {
      this.waitingForPacket = false;
      this.ping = 0L;
      this.sentTime = 0L;
   }

   public boolean isOverride() {
      return this.isEnabled() && (Boolean)this.overrideGetLatency.getValue();
   }

   public long getLastTransaction() {
      return this.ping;
   }
}
