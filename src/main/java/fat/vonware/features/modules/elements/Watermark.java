package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.ColorUtil;
import java.awt.Color;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Watermark extends HudModule {
   private final Setting<WatermarkMode> watermarkMode;
   private final Setting<String> spoofwatermark;
   public final Setting<Boolean> whiteVersion;
   public Setting<Boolean> autoPosition;
   public Setting<Boolean> FuturePos;

   public Watermark() {
      super("Watermark", "Displays a watermark", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.watermarkMode = new Setting<WatermarkMode>("Mode", Watermark.WatermarkMode.Mystic);
      this.spoofwatermark = new Setting<String>("GreeterName", "Mystic", (v) -> this.watermarkMode.getValue() == Watermark.WatermarkMode.Custom);
      this.whiteVersion = new Setting<Boolean>("White Version", true, (v) -> this.watermarkMode.getValue() != Watermark.WatermarkMode.Custom);
      this.autoPosition = new Setting<Boolean>("AutoPos", true);
      this.FuturePos = new Setting<Boolean>("FuturePos", false);
      this.register(this.watermarkMode);
      this.register(this.spoofwatermark);
      this.register(this.whiteVersion);
      this.register(this.autoPosition);
      this.register(this.FuturePos);
      this.enabled.setValue(true);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue() || (Boolean)this.FuturePos.getValue();
   }

   public void updateBounds() {
      float xPos = (Boolean)this.autoPosition.getValue() ? 2.0F : ((Boolean)this.FuturePos.getValue() ? 2.0F : (Float)this.x.getValue());
      float yPos = (Boolean)this.autoPosition.getValue() ? 3.0F : ((Boolean)this.FuturePos.getValue() ? 12.0F : (Float)this.y.getValue());
      this.rect.setX(xPos);
      this.rect.setY(yPos);
      this.rect.setWidth((float)this.renderer.getStringWidth(this.getFullText()));
      this.rect.setHeight((float)(this.renderer.getFontHeight() + 4));
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      super.onRender2D(event);
      Global.updatePulseProgress();
      float xPos = (Boolean)this.autoPosition.getValue() ? 2.0F : ((Boolean)this.FuturePos.getValue() ? 2.0F : this.rect.getX());
      float yPos = (Boolean)this.autoPosition.getValue() ? 3.0F : ((Boolean)this.FuturePos.getValue() ? 12.0F : this.rect.getY());
      this.rect.setX(xPos);
      this.rect.setY(yPos);
      if (this.watermarkMode.getValue() == Watermark.WatermarkMode.Custom) {
         this.drawColoredText(this.spoofwatermark.getValue(), xPos, yPos, 0);
      } else {
         String clientName = "Mystic";
         String suffix = this.getSuffix();
         float nextX = this.drawColoredText(clientName, xPos, yPos, 0);
         if (!suffix.isEmpty()) {
            if ((Boolean)this.whiteVersion.getValue()) {
               this.renderer.drawString(suffix, nextX, yPos, Color.WHITE.getRGB(), true);
            } else {
               this.drawColoredText(suffix, nextX, yPos, clientName.length());
            }
         }

      }
   }

   private float drawColoredText(String text, float x, float y, int rainbowIndexOffset) {
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         float offset = 0.0F;
         int index = rainbowIndexOffset;

         for(char c : text.toCharArray()) {
            int delay = Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Flow ? (int)((float)index * (Float)Colors.getInstance().rainbowHue.getValue()) : 0;
            this.renderer.drawString(String.valueOf(c), x + offset, y, ColorUtil.rainbow(delay).getRGB(), true);
            offset += (float)this.renderer.getStringWidth(String.valueOf(c));
            ++index;
         }

         return x + offset;
      } else {
         Color currentColor = Colors.getInstance().getCurrentColor();
         int color = ColorUtil.toRGBA(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
         if ((Boolean)ClickGui.getInstance().pulseColor.getValue()) {
            color = Global.getPulseColor(Global.getGlobalColor(), 0).getRGB();
         }

         this.renderer.drawString(text, x, y, color, true);
         return x + (float)this.renderer.getStringWidth(text);
      }
   }

   private String getSuffix() {
      return " " + Global.clientVersion();
   }

   private String getFullText() {
      switch ((WatermarkMode)this.watermarkMode.getValue()) {
         case Custom:
            return this.spoofwatermark.getValue();
         case Mystic:
         default:
            return "Mystic" + this.getSuffix();
      }
   }

   public static enum WatermarkMode {
      Mystic,
      Custom;
   }
}
