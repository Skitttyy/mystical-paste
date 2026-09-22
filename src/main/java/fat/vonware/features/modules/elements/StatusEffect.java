package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class StatusEffect extends HudModule {
   public Setting<Boolean> speed = new Setting<Boolean>("Speed Status", true);
   public Setting<Boolean> strength = new Setting<Boolean>("Strength Status", true);
   public Setting<Boolean> mcFont = new Setting<Boolean>("Mc Font", true);
   public Setting<Boolean> weakness = new Setting<Boolean>("Weakness Status", true);
   public Setting<Integer> potionThreshold = new Setting<Integer>("Potion-Threshold", 12, 0, 60);
   public Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPos", true);
   public Setting<Integer> redSpeed = new Setting<Integer>("Flash red", 5, 0, 60, (v) -> (Boolean)this.speed.getValue());
   private final Color[] speedColors = new Color[]{Color.decode("#c0f5fc"), Color.decode("#b3f6ff"), Color.decode("#a1f4ff"), Color.decode("#90e9f5"), Color.decode("#81deeb"), Color.decode("#6bcedb")};
   private final Color[] strengthColors = new Color[]{Color.decode("#ff6b6b"), Color.decode("#ff5252"), Color.decode("#ff3838"), Color.decode("#ff1f1f"), Color.decode("#ff0505"), Color.decode("#eb0000")};
   private final Color[] weaknessColors = new Color[]{Color.decode("#895bc9"), Color.decode("#7e50bf"), Color.decode("#7145b0"), Color.decode("#5c3594"), Color.decode("#4c297d"), Color.decode("#3d1e69")};
   private final Color[] weaknessWornOffColors = new Color[]{Color.decode("#d64f4f"), Color.decode("#ff3333"), Color.decode("#ff0000"), Color.decode("#cc0000"), Color.decode("#990000")};
   private final Color[] redSpeedColors = new Color[]{Color.decode("#d64f4f"), Color.decode("#ff3333"), Color.decode("#ff0000"), Color.decode("#cc0000"), Color.decode("#990000")};
   private long weaknessEndTime = 0L;

   public StatusEffect() {
      super("StatusEffect", "StatusEffect", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.speed);
      this.register(this.strength);
      this.register(this.weakness);
      this.register(this.potionThreshold);
      this.register(this.autoPosition);
      this.register(this.redSpeed);
      this.register(this.mcFont);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   protected void disableAutoPosition() {
      this.autoPosition.setValue(false);
   }

   private boolean isPreview() {
      return mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && mc.world != null) {
         if ((Boolean)this.autoPosition.getValue()) {
            this.rect.setX(1.0F);
            this.rect.setY(254.0F);
         } else {
            this.rect.setX((Float)this.x.getValue());
            this.rect.setY((Float)this.y.getValue());
         }

         boolean preview = this.isPreview();
         if (preview || !mc.player.isCreative() && !mc.player.isSpectator()) {
            float currentY = this.rect.getY();
            int renderedLines = 0;
            int maxWidth = 0;
            if ((Boolean)this.speed.getValue()) {
               int remaining = preview ? Math.min(8, (Integer)this.potionThreshold.getValue()) : this.getRemainingSpeedTime();
               String text = null;
               Color[] colors = this.speedColors;
               if (preview || remaining <= (Integer)this.potionThreshold.getValue()) {
                  if (remaining > 0) {
                     text = "You have speed for " + remaining + " seconds";
                     if (remaining <= (Integer)this.redSpeed.getValue()) {
                        colors = this.redSpeedColors;
                     }
                  } else {
                     text = "No Speed effect on.";
                  }
               }

               if (text != null) {
                  this.renderText(text, colors, this.rect.getX(), currentY);
                  maxWidth = Math.max(maxWidth, this.getTextWidth(text));
                  ++renderedLines;
                  currentY += (float)this.getLineHeight();
               }
            }

            if ((Boolean)this.strength.getValue()) {
               int remaining = preview ? Math.min(10, (Integer)this.potionThreshold.getValue()) : this.getRemainingStrengthTime();
               String text = null;
               if (preview || remaining <= (Integer)this.potionThreshold.getValue()) {
                  text = remaining > 0 ? "You have strength for " + remaining + " seconds" : "No Strength effect on.";
               }

               if (text != null) {
                  this.renderText(text, this.strengthColors, this.rect.getX(), currentY);
                  maxWidth = Math.max(maxWidth, this.getTextWidth(text));
                  ++renderedLines;
                  currentY += (float)this.getLineHeight();
               }
            }

            if ((Boolean)this.weakness.getValue()) {
               int remaining = preview ? 12 : this.getRemainingWeaknessTime();
               String text = null;
               float alpha = 1.0F;
               Color[] colors = this.weaknessColors;
               if (remaining > 0) {
                  text = "You have weakness for " + remaining + " seconds";
                  this.weaknessEndTime = 0L;
               } else {
                  if (this.weaknessEndTime == 0L) {
                     this.weaknessEndTime = System.currentTimeMillis();
                  }

                  long elapsed = System.currentTimeMillis() - this.weaknessEndTime;
                  if (elapsed < 3000L) {
                     text = "Weakness has worn off.";
                     colors = this.weaknessWornOffColors;
                     alpha = 1.0F - (float)elapsed / 3000.0F;
                  }
               }

               if (text != null) {
                  this.renderText(text, colors, this.rect.getX(), currentY, alpha);
                  maxWidth = Math.max(maxWidth, this.getTextWidth(text));
                  ++renderedLines;
               }
            }

            this.applyMeasuredBounds(maxWidth, renderedLines);
         } else {
            this.updateStatusBounds(false);
         }
      }
   }

   public void updateBounds() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(1.0F);
         this.rect.setY(254.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      this.updateStatusBounds(this.isPreview());
   }

   private void updateStatusBounds(boolean preview) {
      int maxWidth = 0;
      int lines = 0;
      if ((Boolean)this.speed.getValue()) {
         int remaining = preview ? Math.min(8, (Integer)this.potionThreshold.getValue()) : this.getRemainingSpeedTime();
         if (preview || remaining <= (Integer)this.potionThreshold.getValue()) {
            String text = remaining > 0 ? "You have speed for " + remaining + " seconds" : "No Speed effect on.";
            maxWidth = Math.max(maxWidth, this.getTextWidth(text));
            ++lines;
         }
      }

      if ((Boolean)this.strength.getValue()) {
         int remaining = preview ? Math.min(10, (Integer)this.potionThreshold.getValue()) : this.getRemainingStrengthTime();
         if (preview || remaining <= (Integer)this.potionThreshold.getValue()) {
            String text = remaining > 0 ? "You have strength for " + remaining + " seconds" : "No Strength effect on.";
            maxWidth = Math.max(maxWidth, this.getTextWidth(text));
            ++lines;
         }
      }

      if ((Boolean)this.weakness.getValue()) {
         int remaining = preview ? 12 : this.getRemainingWeaknessTime();
         String text = null;
         if (remaining > 0) {
            text = "You have weakness for " + remaining + " seconds";
         } else if (this.weaknessEndTime != 0L && System.currentTimeMillis() - this.weaknessEndTime < 3000L) {
            text = "Weakness has worn off.";
         }

         if (text != null) {
            maxWidth = Math.max(maxWidth, this.getTextWidth(text));
            ++lines;
         }
      }

      this.applyMeasuredBounds(maxWidth, lines);
   }

   private int getTextWidth(String text) {
      return (Boolean)this.mcFont.getValue() ? Minecraft.getMinecraft().fontRenderer.getStringWidth(text) : Vonware.textManager.getStringWidth(text);
   }

   private int getFontHeight() {
      return (Boolean)this.mcFont.getValue() ? Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT : Vonware.textManager.getFontHeight();
   }

   private int getLineHeight() {
      return Math.max(10, this.getFontHeight() + 1);
   }

   private void applyMeasuredBounds(int maxWidth, int lines) {
      this.rect.setWidth((float)Math.max(1, maxWidth));
      this.rect.setHeight((float)Math.max(1, lines * this.getLineHeight()));
   }

   private int getRemainingSpeedTime() {
      if (Minecraft.getMinecraft().player == null) {
         return 0;
      } else {
         PotionEffect speedEffect = Minecraft.getMinecraft().player.getActivePotionEffect(Potion.getPotionById(1));
         return speedEffect != null ? speedEffect.getDuration() / 20 : 0;
      }
   }

   private int getRemainingStrengthTime() {
      if (Minecraft.getMinecraft().player == null) {
         return 0;
      } else {
         PotionEffect strengthEffect = Minecraft.getMinecraft().player.getActivePotionEffect(Potion.getPotionById(5));
         return strengthEffect != null ? strengthEffect.getDuration() / 20 : 0;
      }
   }

   private int getRemainingWeaknessTime() {
      if (Minecraft.getMinecraft().player == null) {
         return 0;
      } else {
         PotionEffect weaknessEffect = Minecraft.getMinecraft().player.getActivePotionEffect(Potion.getPotionById(18));
         return weaknessEffect != null ? weaknessEffect.getDuration() / 20 : 0;
      }
   }

   private void renderText(String text, Color[] colors, float x, float y) {
      int color = this.getGradientColor(colors);
      if ((Boolean)this.mcFont.getValue()) {
         Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, color);
      } else {
         Vonware.textManager.drawStringWithShadow(text, x, y, color);
      }

   }

   private void renderText(String text, Color[] colors, float x, float y, float alpha) {
      int color = this.getGradientColor(colors);
      color = color & 16777215 | (int)(alpha * 255.0F) << 24;
      if ((Boolean)this.mcFont.getValue()) {
         Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, x, y, color);
      } else {
         Vonware.textManager.drawStringWithShadow(text, x, y, color);
      }

   }

   private int getGradientColor(Color[] colors) {
      long time = System.currentTimeMillis();
      int index = (int)(time / 1000L % (long)colors.length);
      int nextIndex = (index + 1) % colors.length;
      float ratio = (float)(time % 1000L) / 1000.0F;
      return this.interpolateColor(colors[index], colors[nextIndex], ratio);
   }

   private int interpolateColor(Color color1, Color color2, float ratio) {
      int red = (int)((float)color1.getRed() * (1.0F - ratio) + (float)color2.getRed() * ratio);
      int green = (int)((float)color1.getGreen() * (1.0F - ratio) + (float)color2.getGreen() * ratio);
      int blue = (int)((float)color1.getBlue() * (1.0F - ratio) + (float)color2.getBlue() * ratio);
      return (new Color(red, green, blue)).getRGB();
   }
}
