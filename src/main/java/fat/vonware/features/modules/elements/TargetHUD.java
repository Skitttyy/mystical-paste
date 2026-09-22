package fat.vonware.features.modules.elements;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderUtil;
import fat.vonware.util.shape.StencilUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class TargetHUD extends HudModule {
   private final Setting<Float> range = new Setting<Float>("Range", 500.0F, 0.0F, 600.0F);
   private final Setting<Boolean> showName = new Setting<Boolean>("Show Name", true);
   private final Setting<Boolean> showNumberHP = new Setting<Boolean>("Show NumberHP", true);
   private final Setting<Boolean> showPops = new Setting<Boolean>("Show Pops", false);
   private final Setting<Boolean> showDistance = new Setting<Boolean>("Show Distance", true);
   private final Setting<Boolean> showArmor = new Setting<Boolean>("Show Armor", false);
   private final Setting<Boolean> roundedHead = new Setting<Boolean>("Rounded Head", true);
   private final Setting<Boolean> globalBackground = new Setting<Boolean>("Global Background", true);
   private final Setting<Float> textSize = new Setting<Float>("Text Size", 1.0F, 0.1F, 1.0F);
   private final Setting<Integer> alpha = new Setting<Integer>("Alpha", 255, 0, 255);
   private final Setting<Boolean> animate = new Setting<Boolean>("Animate", true);
   private final Setting<Boolean> phaseMeter = new Setting<Boolean>("Phase Meter", false);
   private final Setting<Boolean> damageTilt = new Setting<Boolean>("Damage Tilt", true);
   private final Setting<Float> tiltAmount = new Setting<Float>("Tilt Amount", 10.0F, 0.0F, 45.0F);
   public float health;
   public long damageTime = 0L;
   public float animationProgress = 0.0F;
   public EntityPlayer lastTarget = null;
   public boolean shouldRender = false;
   public long lastUpdateTime = System.currentTimeMillis();
   public float lastHealthValue = 0.0F;
   public float phaseProgress = 0.0F;

   public TargetHUD() {
      super("TargetHUD", "Displays a HUD for enemies", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.range);
      this.register(this.showName);
      this.register(this.showNumberHP);
      this.register(this.showPops);
      this.register(this.showDistance);
      this.register(this.showArmor);
      this.register(this.roundedHead);
      this.register(this.globalBackground);
      this.register(this.textSize);
      this.register(this.alpha);
      this.register(this.animate);
      this.register(this.phaseMeter);
      this.register(this.damageTilt);
      this.register(this.tiltAmount);
   }

   public void updateBounds() {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      this.rect.setWidth(150.0F);
      this.rect.setHeight(54.0F);
   }

   @SubscribeEvent
   public void onRenderGameOverlay(RenderGameOverlayEvent.Text event) {
      if (mc.player != null && mc.world != null) {
         EntityPlayer target = mc.world.playerEntities.stream().filter((entity) -> !Vonware.friendManager.isFriend(entity) && !entity.equals(mc.player) && !entity.isDead && entity.getDistance(mc.player) <= (Float)this.range.getValue()).min(Comparator.comparingDouble((entity) -> (double)entity.getDistance(mc.player))).orElse(null);
         long currentTime = System.currentTimeMillis();
         float deltaTime = Math.min((float)(currentTime - this.lastUpdateTime) / 16.67F, 3.0F);
         this.lastUpdateTime = currentTime;
         if (target == null) {
            if ((Boolean)this.animate.getValue() && this.animationProgress > 0.0F) {
               this.shouldRender = true;
               GL11.glPushAttrib(24581);
               float speed = 0.2F * deltaTime;
               this.animationProgress -= this.animationProgress * speed;
               if (this.animationProgress < 0.05F) {
                  this.animationProgress = 0.0F;
                  this.lastTarget = null;
                  this.shouldRender = false;
               }

               GL11.glPopAttrib();
            } else {
               this.shouldRender = false;
               this.animationProgress = 0.0F;
               this.lastTarget = null;
               this.phaseProgress = 0.0F;
            }
         } else {
            this.shouldRender = true;
            float currentHealth = target.getHealth() + target.getAbsorptionAmount();
            if (this.lastTarget == target && this.lastHealthValue > currentHealth && this.lastHealthValue - currentHealth > 1.0F) {
               this.damageTime = System.currentTimeMillis();
            }

            this.lastHealthValue = currentHealth;
            if ((Boolean)this.animate.getValue()) {
               if (this.lastTarget != target) {
                  this.animationProgress = 0.0F;
                  this.lastTarget = target;
               }

               float speed = 0.15F * deltaTime;
               this.animationProgress += (1.0F - this.animationProgress) * speed;
               if (this.animationProgress > 0.98F) {
                  this.animationProgress = 1.0F;
               }
            } else {
               this.animationProgress = 1.0F;
            }

            this.rect.setX(this.rect.getX());
            this.rect.setY(this.rect.getY());
            GL11.glPushAttrib(24581);
            GlStateManager.pushMatrix();
            float centerX = this.rect.getX() + 75.0F;
            float centerY = this.rect.getY() + 27.0F;
            GlStateManager.translate(centerX, centerY, 0.0F);
            GlStateManager.scale(this.animationProgress, this.animationProgress, 1.0F);
            if ((Boolean)this.damageTilt.getValue()) {
               float tilt = (float)Math.max(0L, 500L - System.currentTimeMillis() + this.damageTime) / 500.0F * (Float)this.tiltAmount.getValue();
               GlStateManager.rotate(tilt, 0.0F, 0.0F, 1.0F);
            }

            GlStateManager.translate(-centerX, -centerY, 0.0F);
            Color backgroundColor;
            if ((Boolean)this.globalBackground.getValue()) {
               if ((Boolean)Colors.getInstance().rainbow.getValue()) {
                  if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
                     Color rainbowColor = ColorUtil.rainbow(0);
                     backgroundColor = new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), 65);
                  } else {
                     Color rainbowColor = ColorUtil.rainbow(0);
                     backgroundColor = new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), 65);
                  }
               } else {
                  backgroundColor = new Color(Global.getRed(), Global.getGreen(), Global.getBlue(), 65);
               }
            } else {
               backgroundColor = new Color(20, 20, 20, 65);
            }

            this.drawRoundedRect((double)this.rect.getX(), (double)this.rect.getY(), (double)(this.rect.getX() + 150.0F), (double)(this.rect.getY() + 54.0F), (double)6.0F, backgroundColor.hashCode());
            int outlineColor;
            if ((Boolean)Colors.getInstance().rainbow.getValue()) {
               if (Colors.getInstance().rainbowMode.getValue() == Colors.RainbowMode.Static) {
                  outlineColor = ColorUtil.rainbow(0).getRGB();
               } else {
                  outlineColor = ColorUtil.rainbow(0).getRGB();
               }

               Color rainbowColor = new Color(outlineColor);
               outlineColor = (new Color(rainbowColor.getRed(), rainbowColor.getGreen(), rainbowColor.getBlue(), (Integer)this.alpha.getValue())).hashCode();
            } else {
               outlineColor = (new Color(Global.getRed(), Global.getGreen(), Global.getBlue(), (Integer)this.alpha.getValue())).hashCode();
            }

            this.drawOutlinedRoundedRect((double)this.rect.getX(), (double)this.rect.getY(), (double)(this.rect.getX() + 150.0F), (double)(this.rect.getY() + 54.0F), (double)6.0F, 3.0F, outlineColor);
            if ((Boolean)this.roundedHead.getValue()) {
               StencilUtil.write(false);
               RenderUtil.drawCircleHead(this.rect.getX() + 21.0F, this.rect.getY() + 21.0F, 16.0F, -1);
               StencilUtil.erase(true);
               RenderUtil.drawHead(target, this.rect.getX() + 5.0F, this.rect.getY() + 5.0F, 32.0F, 32.0F);
               StencilUtil.dispose();
            } else {
               RenderUtil.drawHead(target, this.rect.getX() + 5.0F, this.rect.getY() + 5.0F, 32.0F, 32.0F);
            }

            float currentY = this.rect.getY() + 5.5F;
            float rightEdgeX = this.rect.getX() + 145.0F;
            List<TextItem> textItems = new ArrayList();
            if ((Boolean)this.showName.getValue()) {
               String nameText = target.getName();
               textItems.add(new TextItem(nameText, mc.fontRenderer.getStringWidth(nameText)));
            }

            if ((Boolean)this.showPops.getValue()) {
               String popsText = "Pops " + Vonware.totemPopManager.getPopCount(target.getName());
               textItems.add(new TextItem(popsText, mc.fontRenderer.getStringWidth(popsText)));
            }

            if ((Boolean)this.showDistance.getValue()) {
               String distanceText = "Distance " + MathUtil.round((double)target.getDistance(mc.player), 1);
               textItems.add(new TextItem(distanceText, mc.fontRenderer.getStringWidth(distanceText)));
            }

            if ((Boolean)this.showName.getValue() && textItems.size() > 1) {
               TextItem name = (TextItem)textItems.remove(0);
               textItems.sort(Comparator.comparingInt((itemx) -> -itemx.width));
               textItems.add(0, name);
            } else if (!(Boolean)this.showName.getValue()) {
               textItems.sort(Comparator.comparingInt((itemx) -> -itemx.width));
            }

            for(TextItem item : textItems) {
               GlStateManager.pushMatrix();
               GlStateManager.scale((Float)this.textSize.getValue(), (Float)this.textSize.getValue(), 1.0F);
               float xPos = (rightEdgeX - (float)item.width * (Float)this.textSize.getValue()) / (Float)this.textSize.getValue();
               Vonware.textManager.drawString(item.text, xPos, currentY / (Float)this.textSize.getValue(), -1, true);
               GlStateManager.popMatrix();
               currentY += 8.5F;
            }

            if ((Boolean)this.showArmor.getValue()) {
               GlStateManager.pushMatrix();
               float xPos = this.rect.getX() + 85.0F;

               for(ItemStack armor : target.inventory.armorInventory) {
                  if (!armor.isEmpty()) {
                     GlStateManager.enableDepth();
                     mc.getItemRenderer().itemRenderer.renderItemAndEffectIntoGUI(armor, (int)xPos, (int)(this.rect.getY() + 23.0F));
                     mc.getItemRenderer().itemRenderer.renderItemOverlayIntoGUI(mc.fontRenderer, armor, (int)xPos, (int)(this.rect.getY() + 23.0F), "");
                     GlStateManager.enableTexture2D();
                     GlStateManager.disableLighting();
                     GlStateManager.disableDepth();
                  }

                  xPos -= 15.0F;
               }

               GlStateManager.popMatrix();
            }

            float health = target.getHealth() + target.getAbsorptionAmount();
            float healthMultiplier = (Boolean)this.showNumberHP.getValue() ? 3.5F : 4.3F;
            this.health += (health * healthMultiplier - this.health) * 0.075F;
            float healthBarY = (Boolean)this.phaseMeter.getValue() ? this.rect.getY() + 40.5F : this.rect.getY() + 43.5F;
            float healthBarEndY = (Boolean)this.phaseMeter.getValue() ? this.rect.getY() + 44.0F : this.rect.getY() + 47.0F;
            this.drawGradientHealthBar(this.rect.getX() + 5.0F, healthBarY, this.rect.getX() + this.health, healthBarEndY);
            if ((Boolean)this.showNumberHP.getValue()) {
               float healthTextY = (Boolean)this.phaseMeter.getValue() ? this.rect.getY() + 39.0F : this.rect.getY() + 42.0F;
               Vonware.textManager.drawString(MathUtil.round((double)health, 1) + "", this.rect.getX() + 6.0F + this.health * 0.91F + 4.0F + 3.0F, healthTextY, -1, true);
            }

            if ((Boolean)this.phaseMeter.getValue()) {
               float targetPhasePercentage = 0.0F;
               if (EntityUtil.isPartialPhasing(target)) {
                  targetPhasePercentage = 33.33F;
               } else if (EntityUtil.isBurrowPhasing(target)) {
                  targetPhasePercentage = 66.66F;
               } else if (EntityUtil.isFullPhasing(target)) {
                  targetPhasePercentage = 100.0F;
               }

               this.phaseProgress += (targetPhasePercentage - this.phaseProgress) * 0.075F;
               float phaseBarWidth = 1.4F * this.phaseProgress;
               this.drawGradientPhaseBar(this.rect.getX() + 5.0F, this.rect.getY() + 46.5F, this.rect.getX() + 5.0F + phaseBarWidth, this.rect.getY() + 50.0F);
            }

            GlStateManager.popMatrix();
            GL11.glPopAttrib();
         }

         if (this.shouldRender && this.animationProgress <= 0.0F) {
         }
      }

   }

   public Color getPhaseGradientColor(float percentage) {
      int red;
      int green;
      int blue;
      if (percentage < 0.7F) {
         float localPercentage = percentage / 0.7F;
         red = (int)(138.0F + 39.0F * localPercentage);
         green = (int)(43.0F + 113.0F * localPercentage);
         blue = (int)(226.0F + -9.0F * localPercentage);
      } else {
         float localPercentage = (percentage - 0.7F) / 0.3F;
         red = (int)(177.0F - 77.0F * localPercentage);
         green = (int)(156.0F + 99.0F * localPercentage);
         blue = (int)(217.0F + 38.0F * localPercentage);
      }

      return new Color(red, green, blue, 255);
   }

   public void drawGradientPhaseBar(float x1, float y1, float x2, float y2) {
      float maxBarWidth = 140.0F;
      int segments = 100;
      float segmentWidth = maxBarWidth / (float)segments;
      StencilUtil.write(false);
      this.drawRoundedRect((double)x1, (double)y1, (double)x2, (double)y2, (double)2.0F, -1);
      StencilUtil.erase(true);

      for(int i = 0; i < segments; ++i) {
         float segmentX1 = x1 + (float)i * segmentWidth;
         float segmentX2 = segmentX1 + segmentWidth;
         if (segmentX1 < x2) {
            float percentage = (float)i / (float)segments;
            Color color = this.getPhaseGradientColor(percentage);
            float clippedX2 = Math.min(segmentX2, x2);
            RenderUtil.drawRect(segmentX1, y1, clippedX2, y2, color.hashCode());
         }
      }

      StencilUtil.dispose();
   }

   public void drawGradientHealthBar(float x1, float y1, float x2, float y2) {
      float maxBarWidth = 140.0F;
      int segments = 150;
      float segmentWidth = maxBarWidth / (float)segments;
      StencilUtil.write(false);
      this.drawRoundedRect((double)x1, (double)y1, (double)(x1 + maxBarWidth), (double)y2, (double)(y2 - y1) / (double)2.0F, -1);
      StencilUtil.erase(true);

      for(int i = 0; i < segments; ++i) {
         float segX1 = x1 + (float)i * segmentWidth;
         float segX2 = segX1 + segmentWidth;
         if (segX1 >= x2) {
            break;
         }

         float percentage = (float)i / (float)segments;
         Color color = this.getGradientColor(percentage);
         float clippedX2 = Math.min(segX2, x2);
         RenderUtil.drawRect(segX1, y1, clippedX2, y2, color.hashCode());
      }

      StencilUtil.dispose();
   }

   public Color getGradientColor(float percentage) {
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

   public void drawOutlinedRoundedRect(double x, double y, double width, double height, double radius, float lineWidth, int color) {
      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      GL11.glPushAttrib(0);
      GL11.glScaled((double)0.5F, (double)0.5F, (double)0.5F);
      x *= (double)2.0F;
      y *= (double)2.0F;
      width *= (double)2.0F;
      height *= (double)2.0F;
      GL11.glLineWidth(lineWidth);
      GL11.glDisable(3553);
      GL11.glColor4f(r, g, b, a);
      GL11.glEnable(2848);
      GL11.glBegin(2);

      for(int i = 0; i <= 90; i += 3) {
         GL11.glVertex2d(x + radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F, y + radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F);
      }

      for(int i = 90; i <= 180; i += 3) {
         GL11.glVertex2d(x + radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F, height - radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F);
      }

      for(int i = 0; i <= 90; i += 3) {
         GL11.glVertex2d(width - radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius, height - radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius);
      }

      for(int i = 90; i <= 180; i += 3) {
         GL11.glVertex2d(width - radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius, y + radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius);
      }

      GL11.glEnd();
      GL11.glEnable(3553);
      GL11.glDisable(2848);
      GL11.glEnable(3553);
      GL11.glScaled((double)2.0F, (double)2.0F, (double)2.0F);
      GL11.glPopAttrib();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
      GlStateManager.resetColor();
   }

   public void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
      GlStateManager.pushMatrix();
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      float a = (float)(color >> 24 & 255) / 255.0F;
      float r = (float)(color >> 16 & 255) / 255.0F;
      float g = (float)(color >> 8 & 255) / 255.0F;
      float b = (float)(color & 255) / 255.0F;
      GL11.glPushAttrib(0);
      GL11.glScaled((double)0.5F, (double)0.5F, (double)0.5F);
      x *= (double)2.0F;
      y *= (double)2.0F;
      width *= (double)2.0F;
      height *= (double)2.0F;
      GL11.glColor4f(r, g, b, a);
      GL11.glEnable(2848);
      GL11.glBegin(9);

      for(int i = 0; i <= 90; i += 3) {
         GL11.glVertex2d(x + radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F, y + radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F);
      }

      for(int i = 90; i <= 180; i += 3) {
         GL11.glVertex2d(x + radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F, height - radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius * (double)-1.0F);
      }

      for(int i = 0; i <= 90; i += 3) {
         GL11.glVertex2d(width - radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius, height - radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius);
      }

      for(int i = 90; i <= 180; i += 3) {
         GL11.glVertex2d(width - radius + Math.sin((double)i * Math.PI / (double)180.0F) * radius, y + radius + Math.cos((double)i * Math.PI / (double)180.0F) * radius);
      }

      GL11.glEnd();
      GL11.glEnable(3553);
      GL11.glDisable(3042);
      GL11.glDisable(2848);
      GL11.glScaled((double)2.0F, (double)2.0F, (double)2.0F);
      GL11.glPopAttrib();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
      GlStateManager.resetColor();
   }

   private static class TextItem {
      String text;
      int width;

      TextItem(String text, int width) {
         this.text = text;
         this.width = width;
      }
   }
}
