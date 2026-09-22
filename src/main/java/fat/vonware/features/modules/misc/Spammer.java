package fat.vonware.features.modules.misc;

import fat.vonware.Vonware;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.FileUtil;
import fat.vonware.util.Timer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.network.play.client.CPacketChatMessage;

public class Spammer extends Module {
   private static final List<String> spamMessages = new ArrayList();
   private static final Random rnd = new Random();
   private final Timer timer;
   private final Setting<Boolean> escoff = new Setting<Boolean>("EscOff", true, "Automatically disables Spammer when you log in or out.");
   public Setting<Mode> mode;
   public Setting<Double> delay;
   public Setting<String> custom;
   public Setting<String> msgTarget;
   public Setting<Boolean> greentext;
   public Setting<Boolean> random;
   public Setting<Boolean> loadFile;

   public Spammer() {
      super("Spammer", "Spams stuff. (need to fix)", Module.Category.MISC, true, false, false);
      this.mode = (new Setting("Mode", Spammer.Mode.FILE)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case FILE:
               return "FILE: Sends messages loaded from the spammer file.";
            case MSG:
               return "MSG: Repeatedly sends the configured Custom message to MsgTarget.";
            default:
               return "";
         }
      }));
      this.delay = new Setting<Double>("Delay", (double)5.0F, (double)0.0F, (double)20.0F, "Delay in seconds between spam messages.");
      this.custom = new Setting<String>("Custom", "String", (v) -> this.mode.getValue() == Spammer.Mode.MSG, "Message repeatedly sent to the target in MSG mode.");
      this.msgTarget = new Setting<String>("MsgTarget", "Target...", (v) -> this.mode.getValue() == Spammer.Mode.MSG, "Player who receives the repeated private message in MSG mode.");
      this.greentext = new Setting<Boolean>("Greentext", Boolean.FALSE, (v) -> this.mode.getValue() == Spammer.Mode.FILE, "Prefixes file-based spam messages with >.");
      this.random = new Setting<Boolean>("Random", Boolean.FALSE, (v) -> this.mode.getValue() == Spammer.Mode.FILE, "Sends file-based spam messages in random order instead of sequential order.");
      this.loadFile = new Setting<Boolean>("LoadFile", Boolean.FALSE, (v) -> this.mode.getValue() == Spammer.Mode.FILE, "Reloads the spam message file on the next tick.");
      this.timer = new Timer();
      this.register(this.mode);
      this.register(this.delay);
      this.register(this.custom);
      this.register(this.msgTarget);
      this.register(this.greentext);
      this.register(this.random);
      this.register(this.loadFile);
   }

   public void onEnable() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         this.readSpamFile();
      }
   }

   public void onLogin() {
      if ((Boolean)this.escoff.getValue() && Vonware.moduleManager.isModuleEnabled("Spammer")) {
         this.disable();
      }

   }

   public void onLogout() {
      if ((Boolean)this.escoff.getValue() && Vonware.moduleManager.isModuleEnabled("Spammer")) {
         this.disable();
      }

   }

   public void onDisable() {
      spamMessages.clear();
      this.timer.reset();
   }

   public void onTick() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         if ((Boolean)this.loadFile.getValue()) {
            this.readSpamFile();
            this.loadFile.setValue(false);
         }

         if (this.timer.passedS((Double)this.delay.getValue())) {
            if (this.mode.getValue() == Spammer.Mode.MSG) {
               String msg = this.custom.getValue();
               msg = "/msg " + (String)this.msgTarget.getValue() + " " + msg;
               mc.player.sendChatMessage(msg);
            } else if (spamMessages.size() > 0) {
               String messageOut;
               if ((Boolean)this.random.getValue()) {
                  int index = rnd.nextInt(spamMessages.size());
                  messageOut = (String)spamMessages.get(index);
                  spamMessages.remove(index);
               } else {
                  messageOut = (String)spamMessages.get(0);
                  spamMessages.remove(0);
               }

               spamMessages.add(messageOut);
               if ((Boolean)this.greentext.getValue()) {
                  messageOut = "> " + messageOut;
               }

               mc.player.connection.sendPacket(new CPacketChatMessage(messageOut.replaceAll("\ufffd", "")));
            }

            this.timer.reset();
         }

      }
   }

   private void readSpamFile() {
      List<String> fileInput = FileUtil.readTextFileAllLines("Vonware/Spammer/txt");
      Iterator<String> i = fileInput.iterator();
      spamMessages.clear();

      while(i.hasNext()) {
         String s = (String)i.next();
         if (!s.replaceAll("\\s", "").isEmpty()) {
            spamMessages.add(s);
         }
      }

      if (spamMessages.size() == 0) {
         spamMessages.add("pk on top!");
      }

   }

   public static enum Mode {
      FILE,
      MSG;
   }
}
