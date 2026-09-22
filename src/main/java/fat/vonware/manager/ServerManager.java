package fat.vonware.manager;

import fat.vonware.features.Feature;
import fat.vonware.util.Timer;
import fat.vonware.util.Util;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.network.NetHandlerPlayClient;

public class ServerManager extends Feature {
   private final float[] tpsCounts = new float[10];
   private final Timer timer = new Timer();
   private float TPS = 20.0F;
   private long lastUpdate = -1L;
   private float currentTps;

   public void onPacketReceived() {
      this.timer.reset();
   }

   public void update() {
      long currentTime = System.currentTimeMillis();
      if (this.lastUpdate == -1L) {
         this.lastUpdate = currentTime;
      } else {
         long timeDiff = currentTime - this.lastUpdate;
         this.lastUpdate = currentTime;
         if (timeDiff != 0L) {
            float tps = Math.min(20.0F, 20000.0F / (float)timeDiff);
            System.arraycopy(this.tpsCounts, 0, this.tpsCounts, 1, this.tpsCounts.length - 1);
            this.tpsCounts[0] = tps;
            double total = (double)0.0F;

            for(float f : this.tpsCounts) {
               total += (double)f;
            }

            total = Math.min((double)20.0F, total / (double)this.tpsCounts.length);
            this.TPS = (float)Math.round(total * (double)1000.0F) / 1000.0F;
            this.currentTps = tps;
         }
      }
   }

   public void reset() {
      Arrays.fill(this.tpsCounts, 20.0F);
      this.TPS = 20.0F;
   }

   public float getTPS() {
      return this.TPS;
   }

   public float getFactor() {
      return this.TPS / 20.0F;
   }

   public float getCurrentTps() {
      return this.currentTps;
   }

   public int getPing() {
      if (fullNullCheck()) {
         return 0;
      } else {
         try {
            return ((NetHandlerPlayClient)Objects.requireNonNull(Util.mc.getConnection())).getPlayerInfo(Util.mc.getConnection().getGameProfile().getId()).getResponseTime();
         } catch (Exception var2) {
            return 0;
         }
      }
   }
}
