package fat.vonware.features.modules.misc;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SmartDisconnect extends Module {
   public Setting<Integer> timeout = new Setting<Integer>("Timeout", 3, 1, 10, "Disconnects if the server sends no packets for this many seconds.");
   public Setting<Boolean> fpsStutter = new Setting<Boolean>("FpsStutter", false, "Disconnects if FPS stays below the module's safety threshold for several seconds.");
   private long last = Long.MAX_VALUE;
   private long joinTime = -1L;
   private long lowFpsSince = -1L;

   public SmartDisconnect() {
      super("SmartDisconnect", "Automatically disconnects from unresponsive servers", Module.Category.MISC, true, false, false);
      this.register(this.timeout);
      this.register(this.fpsStutter);
   }

   public void onTick() {
      if (mc.player != null && mc.world != null) {
         if (!mc.isSingleplayer()) {
            if (mc.getConnection() != null) {
               if (this.joinTime == -1L) {
                  this.joinTime = System.currentTimeMillis();
                  this.last = System.currentTimeMillis();
               }

               String server = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Unknown Server";
               int timeoutMs = (Integer)this.timeout.getValue() * 1000;
               if (System.currentTimeMillis() - this.last >= (long)timeoutMs) {
                  mc.getConnection().getNetworkManager().closeChannel(new TextComponentString("[SmartDisconnect] " + server + " has not replied for " + timeoutMs + "ms."));
               } else {
                  boolean gracePeriodOver = System.currentTimeMillis() - this.joinTime >= 5000L;
                  if ((Boolean)this.fpsStutter.getValue() && gracePeriodOver) {
                     if (Minecraft.getDebugFPS() < 30) {
                        if (this.lowFpsSince == -1L) {
                           this.lowFpsSince = System.currentTimeMillis();
                        }

                        if (System.currentTimeMillis() - this.lowFpsSince >= 3000L) {
                           mc.getConnection().getNetworkManager().closeChannel(new TextComponentString("[SmartDisconnect] " + server + " caused FPS to drop below 30."));
                        }
                     } else {
                        this.lowFpsSince = -1L;
                     }
                  }

               }
            }
         }
      } else {
         this.joinTime = -1L;
         this.lowFpsSince = -1L;
      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      this.last = System.currentTimeMillis();
   }
}
