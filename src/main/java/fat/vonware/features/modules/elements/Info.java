package fat.vonware.features.modules.elements;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.FastLatency;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.HUD.HudElements.FPS;
import fat.vonware.util.OtherRenderShit.Network.NetworkUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

public class Info extends HudModule {
   private static final ResourceLocation INVENTORY_BACKGROUND = new ResourceLocation("textures/gui/container/inventory.png");
   private final Setting<Boolean> speed = new Setting<Boolean>("Speed", true, "Displays your current speed.");
   private final Setting<Boolean> tps = new Setting<Boolean>("TPS", false, "Displays the server's current ticks per second.");
   private final Setting<Boolean> fps = new Setting<Boolean>("FPS", true, "Displays your frames per second.");
   private final Setting<Potions> potions;
   private final Setting<Boolean> potionIcons;
   private final Setting<Boolean> grayEnabled;
   private final Setting<GrayMode> grayMode;
   private final Setting<Render> render;
   private final Setting<Boolean> ping;
   private final Setting<UptimeMode> uptime;
   private final Timer sessionTimer;
   private final Timer serverTimer;
   private boolean serverConnected;
   private final FPS[] timers;
   protected int fpsCount;
   private float chatOffset;

   public Info() {
      super("Info", "Renders info elements on your screen", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.potions = new Setting<Potions>("Potions", Info.Potions.GRAY, "Controls how active potion effects are displayed.");
      this.potionIcons = new Setting<Boolean>("PotionIcons", true, (v) -> this.potions.getValue() != Info.Potions.OFF, "Displays the potion icon next to each active potion effect.");
      this.grayEnabled = new Setting<Boolean>("Gray", false, "Enables gray coloring for selected parts of the info text.");
      this.grayMode = new Setting<GrayMode>("Gray Mode", Info.GrayMode.Labels, (v) -> (Boolean)this.grayEnabled.getValue(), "Chooses whether labels, values, or both are rendered gray.");
      this.render = new Setting<Render>("Render", Info.Render.Down, "Controls whether info elements render upward or downward.");
      this.ping = new Setting<Boolean>("Ping", true, "Displays your current latency to the server.");
      this.uptime = new Setting<UptimeMode>("Uptime", Info.UptimeMode.None, "Displays uptime. Server tracks time since joining the current server / Session tracks time since launching the client.");
      this.sessionTimer = new Timer();
      this.serverTimer = new Timer();
      this.serverConnected = false;
      this.timers = new FPS[100];
      this.fpsCount = 0;
      this.chatOffset = 0.0F;
      long startTime = System.nanoTime();

      for(int i = 0; i < this.timers.length; ++i) {
         long plus = startTime + (long)i * 1000000000L / (long)this.timers.length;
         this.timers[i] = new FPS(plus);
      }

      this.register(this.speed);
      this.register(this.ping);
      this.register(this.tps);
      this.register(this.fps);
      this.register(this.potions);
      this.register(this.potionIcons);
      this.register(this.grayEnabled);
      this.register(this.grayMode);
      this.register(this.render);
      this.register(this.uptime);
      this.enabled.setValue(true);
   }

   @SubscribeEvent
   public void onServerConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
      this.serverTimer.resetDelay();
      this.serverConnected = true;
   }

   @SubscribeEvent
   public void onServerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
      this.serverConnected = false;
   }

   public boolean isAutoPositioned() {
      return true;
   }

   @SubscribeEvent
   public void onRenderGameOverlay(RenderGameOverlayEvent.Pre event) {
      if (event.getType() == ElementType.POTION_ICONS && this.potions.getValue() != Info.Potions.OFF) {
         event.setCanceled(true);
      }

   }

   private String formatUptime(long millis) {
      long totalSeconds = millis / 1000L;
      long hours = totalSeconds / 3600L;
      long minutes = totalSeconds % 3600L / 60L;
      long seconds = totalSeconds % 60L;
      return hours > 0L ? String.format("%d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
   }

   public void onRender2D(Render2DEvent event) {
      for(FPS fps : this.timers) {
         fps.updateFPSCount();
         if (fps.isOver()) {
            this.fpsCount = fps.getFpsCount();
            fps.updateStartTime();
         }
      }

      if (!fullNullCheck()) {
         Global.updatePulseProgress();
         int width = this.renderer.scaledWidth;
         int height = this.renderer.scaledHeight;
         List<TextElement> textElements = new ArrayList();
         this.addTextElement(textElements, "Speed ", String.format("%.2f", Vonware.speedManager.getSpeedKmh()) + " km/h", (Boolean)this.speed.getValue());
         String tpsValue = String.format("%.1f", Vonware.serverManager.getTPS()) + ChatFormatting.GRAY + " (" + (!(Boolean)this.grayEnabled.getValue() || this.grayMode.getValue() != Info.GrayMode.Numbers && this.grayMode.getValue() != Info.GrayMode.Both ? ChatFormatting.RESET : ChatFormatting.GRAY) + String.format("%.2f", Vonware.serverManager.getCurrentTps()) + ChatFormatting.GRAY + ")";
         this.addTextElement(textElements, "TPS ", tpsValue, (Boolean)this.tps.getValue());
         String pingValue = ((FastLatency)Vonware.moduleManager.getModuleByClass(FastLatency.class)).isEnabled() ? NetworkUtil.getLatency() + "ms" : Vonware.serverManager.getPing() + "ms";
         this.addTextElement(textElements, "Ping ", pingValue, (Boolean)this.ping.getValue());
         this.addTextElement(textElements, "FPS ", String.valueOf(this.fpsCount), (Boolean)this.fps.getValue());
         switch ((UptimeMode)this.uptime.getValue()) {
            case Server:
               if (this.serverConnected) {
                  String uptimeStr = this.formatUptime(System.currentTimeMillis() - this.serverTimer.getStartTime());
                  this.addTextElement(textElements, "Uptime ", uptimeStr, true);
               }
               break;
            case Session:
               String uptimeStr = this.formatUptime(System.currentTimeMillis() - this.sessionTimer.getStartTime());
               this.addTextElement(textElements, "Uptime ", uptimeStr, true);
            case None:
         }

         textElements.sort(Comparator.comparingInt((e) -> -e.width));
         int lineHeight = 10;
         int xPadding = 2;
         int yPadding = 0;
         int yPosition;
         if (this.render.getValue() == Info.Render.Up) {
            yPosition = 2;
         } else {
            yPosition = height - 0;
            float targetOffset = mc.ingameGUI.getChatGUI().getChatOpen() ? 13.0F : 0.0F;
            this.chatOffset += (targetOffset - this.chatOffset) * 0.05F;
            yPosition -= (int)this.chatOffset;
         }

         if (this.potions.getValue() != Info.Potions.OFF) {
            String color = this.potions.getValue() == Info.Potions.GRAY ? TextFormatting.GRAY.toString() : TextFormatting.WHITE.toString();
            boolean brackets = this.potions.getValue() == Info.Potions.BRACKETED;
            int iconWidth = (Boolean)this.potionIcons.getValue() ? 12 : 0;

            for(PotionEffect effect : mc.player.getActivePotionEffects()) {
               String potStr = I18n.format(effect.getEffectName(), new Object[0]) + (effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "") + " " + color + (brackets ? TextFormatting.GRAY + "[" + TextFormatting.WHITE : "") + Potion.getPotionDurationString(effect, 1.0F) + (brackets ? TextFormatting.GRAY + "]" : "");
               int textWidth = this.renderer.getStringWidth(potStr);
               int totalWidth = textWidth + iconWidth;
               int xPosition = Math.max(width - totalWidth - 2, 2);
               int potionColor = this.potions.getValue() != Info.Potions.NONE ? effect.getPotion().getLiquidColor() : Global.getGlobalColor().getRGB();
               if (this.render.getValue() == Info.Render.Up) {
                  if ((Boolean)this.potionIcons.getValue()) {
                     int iconY = yPosition - 1;
                     this.drawPotionIcon(effect.getPotion(), xPosition, iconY);
                  }

                  this.renderer.drawString(potStr, (float)(xPosition + iconWidth), (float)yPosition, potionColor, true);
                  yPosition += lineHeight;
               } else {
                  int textY = yPosition - lineHeight;
                  if ((Boolean)this.potionIcons.getValue()) {
                     int iconY = textY - 1;
                     this.drawPotionIcon(effect.getPotion(), xPosition, iconY);
                  }

                  this.renderer.drawString(potStr, (float)(xPosition + iconWidth), (float)textY, potionColor, true);
                  yPosition -= lineHeight;
               }
            }
         }

         int counter = 0;

         for(TextElement textElement : textElements) {
            int xPosition = Math.max(width - textElement.width - 2, 2);
            String prefixColor = !(Boolean)this.grayEnabled.getValue() || this.grayMode.getValue() != Info.GrayMode.Labels && this.grayMode.getValue() != Info.GrayMode.Both ? "" : TextFormatting.GRAY.toString();
            int elementColor;
            if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
               elementColor = Global.getPulseColor(Global.getGlobalColor(), 0).getRGB();
            } else if ((Boolean)Colors.getInstance().rainbow.getValue()) {
               int delay = Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Flow ? (int)((float)counter * (Float)Colors.getInstance().rainbowHue.getValue()) : 0;
               elementColor = ColorUtil.rainbow(delay).getRGB();
            } else {
               Color currentColor = Colors.getInstance().getCurrentColor();
               elementColor = ColorUtil.toRGBA(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
            }

            if (this.render.getValue() == Info.Render.Up) {
               this.renderer.drawString(prefixColor + textElement.prefix, (float)xPosition, (float)yPosition, elementColor, true);
               String valueColor = !(Boolean)this.grayEnabled.getValue() || this.grayMode.getValue() != Info.GrayMode.Numbers && this.grayMode.getValue() != Info.GrayMode.Both ? TextFormatting.WHITE.toString() : TextFormatting.GRAY.toString();
               this.renderer.drawString(valueColor + textElement.value, (float)(xPosition + this.renderer.getStringWidth(textElement.prefix)), (float)yPosition, 16777215, true);
               yPosition += lineHeight;
            } else {
               this.renderer.drawString(prefixColor + textElement.prefix, (float)xPosition, (float)(yPosition - lineHeight), elementColor, true);
               String valueColor = !(Boolean)this.grayEnabled.getValue() || this.grayMode.getValue() != Info.GrayMode.Numbers && this.grayMode.getValue() != Info.GrayMode.Both ? TextFormatting.WHITE.toString() : TextFormatting.GRAY.toString();
               this.renderer.drawString(valueColor + textElement.value, (float)(xPosition + this.renderer.getStringWidth(textElement.prefix)), (float)(yPosition - lineHeight), 16777215, true);
               yPosition -= lineHeight;
            }

            ++counter;
         }

      }
   }

   private void drawPotionIcon(Potion potion, int x, int y) {
      if (potion.hasStatusIcon()) {
         GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
         int iconIndex = potion.getStatusIconIndex();
         int iconX = iconIndex % 8 * 18;
         int iconY = 198 + iconIndex / 8 * 18;
         float scale = 0.55F;
         GlStateManager.translate((float)x, (float)y, 0.0F);
         GlStateManager.scale(scale, scale, 1.0F);
         mc.ingameGUI.drawTexturedModalRect(0, 0, iconX, iconY, 18, 18);
         GlStateManager.popMatrix();
      }

   }

   private void addTextElement(List<TextElement> textElements, String prefix, String value, boolean condition) {
      if (condition) {
         String fullText = prefix + value;
         textElements.add(new TextElement(prefix, value, this.renderer.getStringWidth(fullText)));
      }

   }

   private static class TextElement {
      String prefix;
      String value;
      int width;

      TextElement(String prefix, String value, int width) {
         this.prefix = prefix;
         this.value = value;
         this.width = width;
      }
   }

   public static enum Potions {
      OFF,
      NONE,
      WHITE,
      GRAY,
      BRACKETED;
   }

   public static enum GrayMode {
      Labels,
      Numbers,
      Both;
   }

   public static enum Render {
      Up,
      Down;
   }

   public static enum UptimeMode {
      None,
      Server,
      Session;
   }
}
