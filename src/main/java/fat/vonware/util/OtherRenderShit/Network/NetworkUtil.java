package fat.vonware.util.OtherRenderShit.Network;

import fat.vonware.Vonware;
import fat.vonware.features.modules.client.FastLatency;
import fat.vonware.util.Util;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.apache.logging.log4j.Level;

public class NetworkUtil implements Util {
   public static int getLatency() {
      FastLatency FAST_LATENCY_MODULE = (FastLatency)Vonware.moduleManager.getModuleByClass(FastLatency.class);
      if (FAST_LATENCY_MODULE.isOverride()) {
         return (int)FAST_LATENCY_MODULE.getLastTransaction();
      } else {
         try {
            NetHandlerPlayClient connection = mc.getConnection();
            if (connection != null) {
               NetworkPlayerInfo info = connection.getPlayerInfo(mc.getConnection().getGameProfile().getId());
               if (info != null) {
                  return info.getResponseTime();
               }
            }
         } catch (Throwable var3) {
            Vonware.LOGGER.log(Level.ERROR, "Failed to get latency");
         }

         return 0;
      }
   }
}
