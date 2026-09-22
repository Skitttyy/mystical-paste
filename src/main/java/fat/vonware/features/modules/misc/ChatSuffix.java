package fat.vonware.features.modules.misc;

import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatSuffix extends Module {
   public Setting<Mode> mode;
   public String suffix;

   public ChatSuffix() {
      super("ChatSuffix", "suffix but chat", Module.Category.MISC, true, false, false);
      this.mode = new Setting<Mode>("Mode", ChatSuffix.Mode.Original);
      this.register(this.mode);
   }

   public void onUpdate() {
      this.suffix = this.mode.getValue() == ChatSuffix.Mode.Original ? "\u23d0 \u2756 \uff36\u2661\u03b7\uff57\u15e9\u2c64\u03b5.\u1572\u0454\u03bd \u2756" : (this.mode.getValue() == ChatSuffix.Mode.CartelCC ? "\u23d0 \u2102\u0e04\u0280\u01ac\u0454\u14aa.\uff43\uff43" : (this.mode.getValue() == ChatSuffix.Mode.TripleSix ? " \u23d0 \u1d1b\u0280\u026a\u1d29\u029f\u1d07\u0455\u026a\u0445" : "\u23d0 \u26a1 \u1d0a\u039b\u2c64\u142f\uff49\uff53 \u26a1"));
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getPacket() instanceof CPacketChatMessage) {
         String message = ((CPacketChatMessage)event.getPacket()).message;
         if (message.startsWith("/")) {
            return;
         }

         ((CPacketChatMessage)event.getPacket()).message = message + " " + this.suffix;
      }

   }

   public static enum Mode {
      Original,
      CartelCC,
      TripleSix,
      Jarvis;
   }
}
