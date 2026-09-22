package fat.vonware.features.modules.elements;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewComponent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ServerResponse extends HudModule {
   public Setting<TimeMode> timeMode;
   public Setting<Boolean> animate;
   public Setting<Float> animationSpeed;
   public Setting<Boolean> futureColor;
   public Setting<Boolean> whiteTime;
   private long lastPacketTime;
   private long previewStartTime;
   private boolean wasVisible;
   private float animationProgress;

   public ServerResponse() {
      super("ServerResponse", "Displays how long the server hasn't been responding", Module.Category.Elements, true, false, true, 100.0F, 50.0F);
      this.timeMode = new Setting<TimeMode>("TimeMode", ServerResponse.TimeMode.MS);
      this.animate = new Setting<Boolean>("Animate", true);
      this.animationSpeed = new Setting<Float>("AnimationSpeed", 2.5F, 1.0F, 5.0F, (v) -> (Boolean)this.animate.getValue());
      this.futureColor = new Setting<Boolean>("Future Color", false);
      this.whiteTime = new Setting<Boolean>("White Time", false, (v) -> (Boolean)this.futureColor.getValue());
      this.lastPacketTime = System.currentTimeMillis();
      this.previewStartTime = System.currentTimeMillis();
      this.wasVisible = false;
      this.animationProgress = 0.0F;
      this.register(this.timeMode);
      this.register(this.animate);
      this.register(this.animationSpeed);
      this.register(this.futureColor);
      this.register(this.whiteTime);
   }

   public boolean isAutoPositioned() {
      return true;
   }

   public void onEnable() {
      this.lastPacketTime = System.currentTimeMillis();
      this.previewStartTime = System.currentTimeMillis();
      this.animationProgress = 0.0F;
      this.wasVisible = false;
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      this.lastPacketTime = System.currentTimeMillis();
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      boolean hudEditor = mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
      if (!hudEditor) {
         if (mc.player == null || mc.world == null) {
            return;
         }

         if (mc.isSingleplayer()) {
            return;
         }

         if (mc.getConnection() == null) {
            return;
         }
      }

      Global.updatePulseProgress();
      int color;
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
            color = ColorUtil.rainbow(0).getRGB();
         } else {
            color = ColorUtil.rainbow((int)((float)NewComponent.counter1[0] * (Float)Colors.getInstance().rainbowHue.getValue())).getRGB();
         }
      } else if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
         color = Global.getPulseColor(Global.getGlobalColor(), 0).getRGB();
      } else {
         color = Global.getGlobalColor().getRGB();
      }

      String timeString = "";
      boolean shouldBeVisible;
      if (hudEditor) {
         long previewElapsed = System.currentTimeMillis() - this.previewStartTime;
         if (this.timeMode.getValue() == ServerResponse.TimeMode.MS) {
            long responseTime = previewElapsed % 1000L;
            timeString = responseTime + "ms";
         } else {
            long responseTime = previewElapsed % 3000L;
            timeString = String.format("%.1fs", (double)responseTime / (double)1000.0F);
         }

         shouldBeVisible = true;
      } else {
         long responseTime = System.currentTimeMillis() - this.lastPacketTime;
         if (responseTime <= 500L) {
            shouldBeVisible = false;
         } else {
            shouldBeVisible = true;
            if (this.timeMode.getValue() == ServerResponse.TimeMode.MS) {
               timeString = responseTime + "ms";
            } else {
               timeString = String.format("%.1fs", (double)responseTime / (double)1000.0F);
            }
         }
      }

      if ((Boolean)this.animate.getValue()) {
         if (shouldBeVisible) {
            if (this.animationProgress < 1.0F) {
               this.animationProgress += (Float)this.animationSpeed.getValue() * 0.01F;
               if (this.animationProgress > 1.0F) {
                  this.animationProgress = 1.0F;
               }
            }
         } else if (this.animationProgress > 0.0F) {
            this.animationProgress -= (Float)this.animationSpeed.getValue() * 0.01F;
            if (this.animationProgress < 0.0F) {
               this.animationProgress = 0.0F;
            }
         }
      } else {
         this.animationProgress = shouldBeVisible ? 1.0F : 0.0F;
      }

      if (this.animationProgress <= 0.0F) {
         this.wasVisible = false;
      } else {
         this.wasVisible = true;
         String text;
         if ((Boolean)this.futureColor.getValue()) {
            ChatFormatting timeColor = (Boolean)this.whiteTime.getValue() ? ChatFormatting.WHITE : ChatFormatting.GRAY;
            text = ChatFormatting.GRAY + "Server has not responded for " + timeColor + timeString;
         } else {
            text = "Server has not responded for " + timeString;
         }

         ScaledResolution sr = new ScaledResolution(mc);
         int screenWidth = sr.getScaledWidth();
         int textWidth = Vonware.textManager.getStringWidth(text);
         int x = screenWidth / 2 - textWidth / 2;
         Greeter greeterModule = (Greeter)Vonware.moduleManager.getModuleByClass(Greeter.class);
         int targetY = 5;
         if (greeterModule != null && greeterModule.isEnabled()) {
            targetY = 2 + Vonware.textManager.getFontHeight() + 3;
         }

         int slideDistance = 20;
         float currentY = (float)targetY - (float)slideDistance * (1.0F - this.animationProgress);
         Vonware.textManager.drawStringWithShadow(text, (float)x, (float)((int)currentY), color);
      }
   }

   public static enum TimeMode {
      MS,
      Seconds;
   }
}
