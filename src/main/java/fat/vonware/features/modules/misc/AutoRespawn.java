package fat.vonware.features.modules.misc;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.modules.render.CustomDeathUI;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.HexColorText;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoRespawn extends Module {
   private final Setting<Boolean> message = new Setting<Boolean>("Message", true, "Shows a message containing the coordinates where you died.");
   private final Setting<Boolean> waypoint = new Setting<Boolean>("WayPoint", false, (v) -> (Boolean)this.message.getValue(), "Adds a waypoint option to the death-coordinate message.");
   private final Setting<Boolean> autoKit = new Setting<Boolean>("AutoKit", false, "Automatically runs a kit command shortly after respawning.");
   private final Setting<String> kitName = new Setting<String>("KitName", "kit", (v) -> (Boolean)this.autoKit.getValue(), "Name of the kit requested after respawning.");
   private final Setting<Boolean> waitForEffect = new Setting<Boolean>("WaitForScreenEffects", false, "Waits for the configured death-screen effect to finish before respawning.");
   private static final int KIT_DELAY = 100;
   private int ticksSinceRespawn = 0;
   private boolean shouldSendKit = false;
   private long deathScreenOpenTime = -1L;
   private boolean waitingForAnimation = false;
   public boolean canSendMessage = false;

   public AutoRespawn() {
      super("AutoRespawn", "automatically respawns you", Module.Category.MISC, true, false, false);
      this.register(this.message);
      this.register(this.waypoint);
      this.register(this.autoKit);
      this.register(this.kitName);
      this.register(this.waitForEffect);
   }

   private int getScreenEffectDelay() {
      CustomDeathUI.Animation mode = CustomDeathUI.getInstance().animation.getValue();
      return mode == CustomDeathUI.Animation.ReZero ? 500 : 3200;
   }

   public void onEnable() {
      this.canSendMessage = false;
      this.shouldSendKit = false;
      this.ticksSinceRespawn = 0;
      this.deathScreenOpenTime = -1L;
      this.waitingForAnimation = false;
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if (mc.world == null) {
         this.canSendMessage = false;
         this.shouldSendKit = false;
         this.ticksSinceRespawn = 0;
         this.deathScreenOpenTime = -1L;
         this.waitingForAnimation = false;
      } else {
         if (mc.player != null && mc.player.getHealth() > 0.0F) {
            this.canSendMessage = true;
         }

         if (this.waitingForAnimation && this.deathScreenOpenTime != -1L) {
            long timeSinceDeath = System.currentTimeMillis() - this.deathScreenOpenTime;
            if (timeSinceDeath >= (long)this.getScreenEffectDelay()) {
               this.performRespawn();
               this.waitingForAnimation = false;
               this.deathScreenOpenTime = -1L;
            }
         }

         if (this.shouldSendKit) {
            ++this.ticksSinceRespawn;
            if (this.ticksSinceRespawn >= 2) {
               if (mc.player != null) {
                  mc.player.sendChatMessage("/kit " + (String)this.kitName.getValue());
               }

               this.shouldSendKit = false;
               this.ticksSinceRespawn = 0;
            }
         }

      }
   }

   @SubscribeEvent
   public void onDisplayDeathScreen(GuiOpenEvent event) {
      if (this.canSendMessage) {
         if (event.getGui() instanceof GuiGameOver) {
            if ((Boolean)this.waitForEffect.getValue() && CustomDeathUI.getInstance().isEnabled()) {
               this.deathScreenOpenTime = System.currentTimeMillis();
               this.waitingForAnimation = true;
            } else {
               event.setCanceled(true);
               this.performRespawn();
            }
         }

      }
   }

   private void performRespawn() {
      if ((Boolean)this.message.getValue() && mc.player != null) {
         int x = (int)mc.player.posX;
         int y = (int)mc.player.posY;
         int z = (int)mc.player.posZ;
         Command.sendSilentMessage(this.buildDeathMessageComponent(x, y, z));
      }

      if (mc.player != null) {
         mc.player.respawnPlayer();
      }

      if ((Boolean)this.autoKit.getValue()) {
         this.shouldSendKit = true;
         this.ticksSinceRespawn = 0;
      }

      this.canSendMessage = false;
   }

   private ITextComponent buildDeathMessageComponent(int x, int y, int z) {
      TextComponentString base = new TextComponentString(this.getFormattedDeathMessage(x, y, z));
      if ((Boolean)this.waypoint.getValue()) {
         TextComponentString showText = new TextComponentString(ChatFormatting.GRAY + " " + ChatFormatting.DARK_AQUA + "<" + ChatFormatting.AQUA + "Show" + ChatFormatting.DARK_AQUA + ">" + ChatFormatting.RESET);
         showText.getStyle().setClickEvent(new ClickEvent(Action.RUN_COMMAND, Vonware.commandManager.getPrefix() + "showwaypoint Death " + x + " " + y + " " + z));
         showText.getStyle().setHoverEvent(new HoverEvent(net.minecraft.util.text.event.HoverEvent.Action.SHOW_TEXT, new TextComponentString(ChatFormatting.YELLOW + "Show Waypoint")));
         base.appendSibling(showText);
      }

      return base;
   }

   private String getFormattedDeathMessage(int x, int y, int z) {
      if (Manager.getInstance().notifier.getValue() == Manager.Notifier.Iggyware) {
         return ChatFormatting.BLACK + "<Iggyware>" + ChatFormatting.RED + " You died at -> (X: " + x + " Y: " + y + " Z: " + z + ")";
      } else {
         String globalColor = HexColorText.color(Global.getGlobalColor().getRGB());
         return Global.getMysticName() + globalColor + "You died at " + globalColor + "X: " + ChatFormatting.WHITE + x + " " + globalColor + "Y: " + ChatFormatting.WHITE + y + " " + globalColor + "Z: " + ChatFormatting.WHITE + z + ChatFormatting.RESET;
      }
   }
}
