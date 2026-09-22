package fat.vonware.features.modules.misc;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.util.Manager.NiggaUtil;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoUnicodeLag extends Module {
   public String[] list = new String[]{"\u263b", "\u2465", "\u267b", "\u2525", "\ub9a2", "\uc384", "\u7e3a", "\u2c37", "\u84b9", "\ua225", "\u84b9", "\u3ac3", "\u317e", "\u2c36", "\u3ac3", "\u347e", "\u2673", "\u7340", "\u7465", "\u2220", "\ub9a2", "\uc384", "\u773d", "\u656b", "\u3459", "\u6f4d", "\u5262", "\u4a30", "\u7156", "\u6173", "\u374f", "\u7257", "\u4439", "\u4954", "\u7a42", "\u4336", "\u5546", "\u644b", "\u675a", "\u6970", "\u3347", "\u3151", "\u5838", "\u756c", "\u6a6e", "\u3268", "\u4439", "\u4579", "\u5076", "\u4c74", "\u7835", "\u4840", "\u536d", "\u2266", "\u250a", "\ub9a2", "\uc384", "\u7e3a", "\u3935", "\u3535", "\u0101", "\u0201", "\u0301", "\u0401", "\u0501", "\u0601", "\u0701", "\u0801", "\u0901", "\u0a01", "\u0b01", "\u0e01", "\u0f01", "\u1001", "\u1101", "\u1201", "\u1301", "\u1401", "\u1501", "\u1601", "\u1701", "\u1801", "\u1901", "\u1a01", "\u1b01", "\u1c01", "\u1d01", "\u1e01", "\u7001", "\u7101", "\u7201", "\u7301", "\u7401", "\u7501", "\u7601", "\u7701", "\u7801", "\u7901", "\u7a01", "\u7b01", "\u7c01", "\u7d01", "\u7e01", "\u7f01", "\u8001", "\u8101", "\u8201", "\u8301", "\u8401", "\u8501", "\u8601", "\u8701", "\u8801", "\u8901", "\u8a01", "\u8b01", "\u8c01", "\u8d01", "\u8e01", "\u8f01", "\u9001", "\u9101", "\u9201", "\u9301", "\u9401", "\u9501", "\u9601", "\u9701", "\u9801", "\u9901", "\u9a01", "\u9b01", "\u9c01", "\u9d01", "\u9e01", "\u9f01", "\ua001", "\ua101", "\ua201", "\ua301", "\ua401", "\ua501", "\ua601", "\ua701", "\ua801", "\ua901", "\uaa01", "\uac01", "\uad01", "\uae01", "\uaf01", "\ub001", "\ub101", "\ub201", "\ub301", "\ub401", "\ub501", "\ub601", "\ub701", "\ub801", "\ub901", "\uba01", "\ubb01", "\ubc01", "\ubd01", "\u6500", "\u7300", "\u7400", "\u7200", "\u03a4", "\uc0ae", "\u135a", "\ud4fa", "\ucf90", "\u04aa", "\uc1f8", "\u2ea2", "\u18d4", "\uc4f7", "\uc792", "\u04d2", "\ub2fa", "\ud60d", "\ube94", "\ud261", "\ucd19", "\ub906", "\uc808", "\u1b8d", "\ua124", "\u0126", "\u0bf6", "\ua753", "\u2bf0", "\u22d1", "\ua8a2", "\ud1d1", "\uce9e", "\u1008", "\ud234", "\u1ee4", "\ufec6", "\ub20e", "\uc6f6", "\u1f02", "\ucca1", "\uaa2e", "\uc95f", "\uffec", "\ub60a", "\uabca", "\uccf3", "\u2f30", "\ub692", "\ub1ec", "\u2c95", "\ua470", "\ubcac", "\ucc73", "\uaed6", "\u1a59", "\u1804", "\ufb61", "\ua938", "\u0d61", "\u0c24", "\ucadf", "\ufcb4", "\ua881", "\uac98", "\u0aa6", "\u19ef", "\uf7c6", "\u24ff", "\uc437", "\u07cf", "\u09ab", "\uba45", "\u0f3a", "\u0987", "\ub869", "\ua38a", "\u28ad", "\ub3a9", "\uc23e", "\u2059", "\ud32d", "\ubac4", "\ud7e6", "\u330c", "\ua0aa", "\u2111", "\ubcae", "\ub625", "\ud0e1", "\u0aab", "\u16c9", "\uae7a", "\u1cb2", "\u2975", "\u15d9", "\ub099", "\ua969", "\u3127", "\u0547", "\u1210", "\u020e", "\u11ff", "\u3145", "\ubce9", "\u2ad3", "\u17a0", "\u232b", "\u304b", "\u1d79", "\uaff1", "\ua64f"};

   public NoUnicodeLag() {
      super("NoUnicodeLag", "Blocks unicode lag", Module.Category.MISC, true, false, false);
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getPacket() instanceof SPacketChat) {
         String message = ((SPacketChat)event.getPacket()).getChatComponent().getUnformattedText();

         for(String unicode : this.list) {
            if (message.contains(unicode)) {
               mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TextComponentString("Blocked possible unicode lag message!"), 3950211);
               if (NiggaUtil.isWhisper(message)) {
                  String playerName = NiggaUtil.extractPlayerName(message);
                  Manager.getInstance().setWhispererFromBlockedUnicode(playerName);
               }

               event.setCanceled(true);
               return;
            }
         }
      }

   }
}
