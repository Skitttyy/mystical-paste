package fat.vonware.features.modules.elements;

import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class HealthVisualizer extends HudModule {
   private static final ResourceLocation HEART_ICON = new ResourceLocation("textures/hearticon.png");
   private final Setting<DisplayMode> displayMode;
   private final Setting<Boolean> animate;
   private final Setting<Boolean> showDecimals;
   private final Setting<Boolean> showIcon;
   private final Setting<Boolean> showValue;
   private final Setting<Boolean> showAbsorption;
   private static final int BAR_WIDTH = 100;
   private static final int segments = 32;
   private float animatedHealth;
   private float animatedAbsorption;

   public HealthVisualizer() {
      super("HealthVisualizer", "Displays your health", Module.Category.Elements, true, false, false, 100.0F, 80.0F);
      this.displayMode = new Setting<DisplayMode>("Mode", HealthVisualizer.DisplayMode.Text);
      this.animate = new Setting<Boolean>("Animate", true, (v) -> this.displayMode.getValue() == HealthVisualizer.DisplayMode.Text);
      this.showDecimals = new Setting<Boolean>("Decimals", true, (v) -> this.displayMode.getValue() == HealthVisualizer.DisplayMode.Text);
      this.showIcon = new Setting<Boolean>("Icon", true, (v) -> this.displayMode.getValue() == HealthVisualizer.DisplayMode.Text);
      this.showValue = new Setting<Boolean>("ShowValue", false, (v) -> this.displayMode.getValue() == HealthVisualizer.DisplayMode.Bar);
      this.showAbsorption = new Setting<Boolean>("Absorption", false, (v) -> this.displayMode.getValue() == HealthVisualizer.DisplayMode.Bar);
      this.animatedHealth = 0.0F;
      this.animatedAbsorption = 0.0F;
      this.register(this.displayMode);
      this.register(this.animate);
      this.register(this.showDecimals);
      this.register(this.showIcon);
      this.register(this.showValue);
      this.register(this.showAbsorption);
   }

   private float getBaseHealth() {
      return mc.player == null ? 20.0F : mc.player.getHealth();
   }

   private float getBaseMaxHealth() {
      return mc.player == null ? 20.0F : Math.max(mc.player.getMaxHealth(), 1.0F);
   }

   private float getAbsorption() {
      return mc.player == null ? 0.0F : mc.player.getAbsorptionAmount();
   }

   private float getHealth() {
      return mc.player == null ? 20.0F : mc.player.getHealth() + mc.player.getAbsorptionAmount();
   }

   private Color getGradientColor(float percentage) {
      int red;
      int green;
      if (percentage < 0.5F) {
         red = 255;
         green = (int)(255.0F * percentage * 2.0F);
      } else {
         red = (int)(255.0F * (1.0F - (percentage - 0.5F) * 2.0F));
         green = 255;
      }

      return new Color(red, green, 0, 255);
   }

   private int getTextColor(float healthPct) {
      return this.getGradientColor(healthPct).getRGB();
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && mc.world != null) {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
         float xPos = this.rect.getX();
         float yPos = this.rect.getY();
         float baseHealth = this.getBaseHealth();
         float baseMax = this.getBaseMaxHealth();
         float absorption = this.getAbsorption();
         this.animatedHealth += (baseHealth - this.animatedHealth) * 0.075F;
         this.animatedAbsorption += (absorption - this.animatedAbsorption) * 0.075F;
         float pct = Math.min(this.animatedHealth / baseMax, 1.0F);
         switch ((DisplayMode)this.displayMode.getValue()) {
            case Text:
               this.renderText(xPos, yPos, this.getHealth(), baseHealth, baseMax);
               break;
            case Bar:
               this.renderBar(xPos, yPos, pct, this.animatedAbsorption, baseMax, this.getHealth());
         }

      }
   }

   private void renderText(float x, float y, float totalHealth, float baseHealth, float baseMax) {
      float realPct = Math.min(baseHealth / baseMax, 1.0F);
      int color = this.getTextColor(realPct);
      int fontH = Vonware.textManager.getFontHeight();
      float iconSize = (float)fontH;
      float iconGap = 2.0F;
      float textX = x;
      if ((Boolean)this.showIcon.getValue()) {
         this.drawIcon(x, y, iconSize);
         textX = x + iconSize + iconGap;
      }

      this.renderHealthText(textX, y, totalHealth, color);
   }

   private void renderHealthText(float x, float y, float health, int color) {
      int whole = (int)health;
      float decimal = health - (float)whole;
      String decStr = String.format(".%d", (int)(decimal * 10.0F));
      if ((Boolean)this.animate.getValue()) {
         RenderUtil.drawAnimatedInt("healthvisualizer_hp", whole, x, y, "", color, true, 1.0F);
         if ((Boolean)this.showDecimals.getValue()) {
            float intWidth = (float)Vonware.textManager.getStringWidth(String.valueOf(whole));
            Vonware.textManager.drawStringWithShadow(decStr, x + intWidth, y, color);
         }
      } else {
         String text = (Boolean)this.showDecimals.getValue() ? String.format("%.1f", health) : String.valueOf(whole);
         Vonware.textManager.drawStringWithShadow(text, x, y, color);
      }

   }

   private void drawIcon(float x, float y, float size) {
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      mc.getTextureManager().bindTexture(HEART_ICON);
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      buf.begin(7, DefaultVertexFormats.POSITION_TEX);
      buf.pos((double)x, (double)(y + size), (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buf.pos((double)(x + size), (double)(y + size), (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buf.pos((double)(x + size), (double)y, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      buf.pos((double)x, (double)y, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      tess.draw();
      GlStateManager.disableBlend();
   }

   private void renderBar(float x, float y, float pct, float absorption, float baseMax, float displayHealth) {
      float w = 100.0F;
      float h = 1.0F;
      float r = h / 2.0F;
      float cx1 = x + r;
      float cx2 = x + w - r;
      float cy = y + r;
      float absorptionPct = Math.min(absorption / baseMax, 1.0F);
      float absorptionW = w * absorptionPct;
      Color gold = new Color(255, 200, 50, 220);
      Color bg = new Color(20, 20, 20, 160);
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      this.setGlColor(bg);
      this.drawArc((double)cx1, (double)cy, (double)r, (double)90.0F, (double)270.0F);
      this.drawArc((double)cx2, (double)cy, (double)r, (double)270.0F, (double)450.0F);
      this.drawFilledRect(cx1, y, w - h, h, bg);
      int gradientSegments = 100;
      float innerFill = Math.max(0.0F, w * pct - h);
      this.setGlColor(this.getGradientColor(0.0F));
      this.drawArc((double)cx1, (double)cy, (double)r, (double)90.0F, (double)270.0F);
      if (pct >= 1.0F) {
         this.setGlColor(this.getGradientColor(1.0F));
         this.drawArc((double)cx2, (double)cy, (double)r, (double)270.0F, (double)450.0F);
      }

      if (innerFill > 0.0F) {
         float segW = (w - h) / (float)gradientSegments;

         for(int i = 0; i < gradientSegments; ++i) {
            float segX1 = cx1 + (float)i * segW;
            float segX2 = segX1 + segW;
            if (segX1 >= cx1 + innerFill) {
               break;
            }

            float segPct = (float)i / (float)gradientSegments;
            Color segColor = this.getGradientColor(segPct);
            float clippedX2 = Math.min(segX2, cx1 + innerFill);
            this.setGlColor(segColor);
            this.drawFilledRect(segX1, y, clippedX2 - segX1, h, segColor);
         }
      }

      if ((Boolean)this.showAbsorption.getValue() && absorptionW > 0.0F) {
         float absInnerFill = Math.max(0.0F, absorptionW - h);
         this.setGlColor(gold);
         this.drawArc((double)cx1, (double)cy, (double)r, (double)90.0F, (double)270.0F);
         if (absorptionPct >= 1.0F) {
            this.drawArc((double)cx2, (double)cy, (double)r, (double)270.0F, (double)450.0F);
         }

         if (absInnerFill > 0.0F) {
            this.drawFilledRect(cx1, y, absInnerFill, h, gold);
         }
      }

      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      if ((Boolean)this.showValue.getValue()) {
         float labelPct = Math.min(this.animatedHealth / baseMax, 1.0F);
         int labelColor = this.getGradientColor(labelPct).getRGB();
         if ((Boolean)this.animate.getValue()) {
            int whole = (int)displayHealth;
            float labelX = x + w / 2.0F - (float)mc.fontRenderer.getStringWidth(String.valueOf(whole)) / 2.0F;
            float labelY = y - (float)Vonware.textManager.getFontHeight() - 1.0F;
            RenderUtil.drawAnimatedInt("healthvisualizer_barlabel", whole, labelX, labelY, "", labelColor, true, 1.0F);
            if ((Boolean)this.showDecimals.getValue()) {
               float decVal = displayHealth - (float)whole;
               String decStr = String.format(".%d", (int)(decVal * 10.0F));
               float intW = (float)Vonware.textManager.getStringWidth(String.valueOf(whole));
               Vonware.textManager.drawStringWithShadow(decStr, labelX + intW, labelY, labelColor);
            }
         } else {
            String label = (Boolean)this.showDecimals.getValue() ? String.format("%.1f", displayHealth) : String.valueOf((int)displayHealth);
            float labelX = x + w / 2.0F - (float)Vonware.textManager.getStringWidth(label) / 2.0F;
            float labelY = y - (float)Vonware.textManager.getFontHeight() - 1.0F;
            Vonware.textManager.drawStringWithShadow(label, labelX, labelY, labelColor);
         }
      }

   }

   private void drawFilledRect(float x, float y, float w, float h, Color color) {
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float a = (float)color.getAlpha() / 255.0F;
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buf.pos((double)x, (double)(y + h), (double)0.0F).color(r, g, b, a).endVertex();
      buf.pos((double)(x + w), (double)(y + h), (double)0.0F).color(r, g, b, a).endVertex();
      buf.pos((double)(x + w), (double)y, (double)0.0F).color(r, g, b, a).endVertex();
      buf.pos((double)x, (double)y, (double)0.0F).color(r, g, b, a).endVertex();
      tess.draw();
   }

   private void setGlColor(Color c) {
      GL11.glColor4f((float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F, (float)c.getAlpha() / 255.0F);
   }

   private void drawArc(double cx, double cy, double r, double start_angle, double end_angle) {
      GlStateManager.glBegin(4);
      double totalSteps = Math.abs(end_angle - start_angle);
      int steps = Math.max(1, (int)(totalSteps / (double)11.25F));
      double stepSize = (end_angle - start_angle) / (double)steps;

      for(int i = 0; i < steps; ++i) {
         double a1 = start_angle + (double)i * stepSize;
         double a2 = start_angle + (double)(i + 1) * stepSize;
         double rad1 = Math.toRadians(a1);
         double rad2 = Math.toRadians(a2);
         GL11.glVertex2d(cx, cy);
         GL11.glVertex2d(cx + Math.cos(rad2) * r, cy + Math.sin(rad2) * r);
         GL11.glVertex2d(cx + Math.cos(rad1) * r, cy + Math.sin(rad1) * r);
      }

      GlStateManager.glEnd();
   }

   public void updateBounds() {
      float health = this.getHealth();
      int fontH = Vonware.textManager.getFontHeight();
      switch ((DisplayMode)this.displayMode.getValue()) {
         case Text:
            int whole = (int)health;
            String decPart = (Boolean)this.showDecimals.getValue() ? ".0" : "";
            float textW = (float)Vonware.textManager.getStringWidth(whole + decPart);
            float iconW = (Boolean)this.showIcon.getValue() ? (float)fontH + 2.0F : 0.0F;
            this.rect.setWidth(iconW + textW);
            this.rect.setHeight((float)(fontH + 4));
            break;
         case Bar:
            int labelH = (Boolean)this.showValue.getValue() ? fontH + 2 : 0;
            this.rect.setWidth(100.0F);
            this.rect.setHeight(1.0F + (float)labelH + 4.0F);
      }

   }

   public static enum DisplayMode {
      Text,
      Bar;
   }
}
