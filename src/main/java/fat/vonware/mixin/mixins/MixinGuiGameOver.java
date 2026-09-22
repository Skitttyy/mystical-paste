package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Fonts;
import fat.vonware.features.modules.render.CustomDeathUI;
import fat.vonware.util.Animations.draw.HeartAnimationUtil;
import fat.vonware.util.Animations.draw.ReZeroAnimationUtil;
import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiGameOver;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiGameOver.class})
public abstract class MixinGuiGameOver extends GuiScreen {
   private final HeartAnimationUtil heartAnim = new HeartAnimationUtil();
   private final ReZeroAnimationUtil reZeroAnim = new ReZeroAnimationUtil();

   @Inject(
      method = {"initGui"},
      at = {@At("HEAD")}
   )
   public void onInitGui(CallbackInfo ci) {
      if (CustomDeathUI.getInstance().isEnabled()) {
         CustomDeathUI.Animation mode = CustomDeathUI.getInstance().animation.getValue();
         if (mode == CustomDeathUI.Animation.Heart) {
            this.heartAnim.reset();
         } else if (mode == CustomDeathUI.Animation.ReZero) {
            this.reZeroAnim.reset();
         }

      }
   }

   @Inject(
      method = {"drawScreen"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      if (CustomDeathUI.getInstance().isEnabled()) {
         ci.cancel();
         ScaledResolution sr = new ScaledResolution(this.mc);
         CustomDeathUI.Animation mode = CustomDeathUI.getInstance().animation.getValue();
         if (mode == CustomDeathUI.Animation.Heart) {
            long timeSinceDeath = this.heartAnim.getTimeSinceDeath();
            float blackAlpha = Math.min(1.0F, (float)timeSinceDeath / 100.0F);
            float gradientAlpha = Math.min(1.0F, (float)timeSinceDeath / 1000.0F);
            int redIntensity = (int)(gradientAlpha * 26.0F);
            this.drawGradientRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), (int)(blackAlpha * 255.0F) << 24 | redIntensity << 16, (int)(blackAlpha * 255.0F) << 24);
            this.heartAnim.renderBreakingHeart(sr, timeSinceDeath);
            if (timeSinceDeath > 1200L) {
               long textTime = timeSinceDeath - 1200L;
               long cycleTime = textTime % 6000L;
               if (cycleTime < 2000L) {
                  this.heartAnim.renderGlitchyTypingText(sr, cycleTime);
               } else if (cycleTime < 3000L) {
                  this.heartAnim.renderGlitchyFullText(sr);
               } else if (cycleTime < 5000L) {
                  this.heartAnim.renderGlitchyDeletingText(sr, cycleTime - 3000L);
               }
            }

            if (timeSinceDeath > 3200L) {
               this.renderCustomButtons(sr, mouseX, mouseY, timeSinceDeath);
            }
         } else if (mode == CustomDeathUI.Animation.ReZero) {
            long timeSinceDeath = this.reZeroAnim.getTimeSinceDeath();
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.reZeroAnim.getShakeOffsetX(), this.reZeroAnim.getShakeOffsetY(), 0.0F);
            this.reZeroAnim.render(sr, timeSinceDeath);
            if (!this.isAutoRespawnActive() && timeSinceDeath > 500L) {
               this.renderCustomButtons(sr, mouseX, mouseY, timeSinceDeath);
            }

            GlStateManager.popMatrix();
         }

      }
   }

   private void renderCustomButtons(ScaledResolution sr, int mouseX, int mouseY, long timeSinceDeath) {
      int centerX = sr.getScaledWidth() / 2;
      int startY = sr.getScaledHeight() / 2 + 60;
      CustomDeathUI.Animation mode = CustomDeathUI.getInstance().animation.getValue();
      long buttonAppearTime = mode == CustomDeathUI.Animation.ReZero ? 500L : 3200L;
      float fadeProgress = Math.min(1.0F, (float)(timeSinceDeath - buttonAppearTime) / 500.0F);
      int alpha = (int)(fadeProgress * 255.0F);
      boolean currentlyHoveringRespawn = false;
      int buttonIndex = 0;

      for(GuiButton button : this.buttonList) {
         if (button.id != 1) {
            int buttonY = startY + buttonIndex * 30;
            button.x = centerX - 100;
            button.y = buttonY;
            button.width = 200;
            button.height = 24;
            boolean isHovered = mouseX >= centerX - 100 && mouseX <= centerX + 100 && mouseY >= buttonY && mouseY <= buttonY + 24;
            if (button.id == 0 && isHovered) {
               currentlyHoveringRespawn = true;
            }

            this.drawStylizedButton(button.displayString, centerX, buttonY, 200, 24, isHovered, button.id, alpha);
            ++buttonIndex;
         }
      }

      if (mode == CustomDeathUI.Animation.Heart) {
         this.heartAnim.updateHoverState(currentlyHoveringRespawn);
      }

   }

   private void drawStylizedButton(String text, int centerX, int y, int width, int height, boolean hovered, int buttonId, int alpha) {
      int x = centerX - width / 2;
      if (buttonId == 0) {
         boolean customFontEnabled = Fonts.getInstance().isEnabled();
         int baseColor = alpha * 120 / 255 << 24 | 655360;
         float scale = hovered ? 1.02F : 1.0F;
         GlStateManager.pushMatrix();
         GlStateManager.translate((float)centerX, (float)(y + height / 2), 0.0F);
         GlStateManager.scale(scale, scale, 1.0F);
         GlStateManager.translate((float)(-centerX), (float)(-(y + height / 2)), 0.0F);
         drawRect(x, y, x + width, y + height, baseColor);
         int halfWidth = width / 2;
         float lineOffset = 0.0F;
         float lineBrightness = 1.0F;
         if (hovered) {
            long time = System.currentTimeMillis();
            lineOffset = (float)((double)time / (double)10.0F % (double)width) - (float)width / 2.0F;
            lineBrightness = 1.3F;
         }

         for(int i = 0; i < halfWidth; ++i) {
            float fade = (float)i / (float)halfWidth;
            if (hovered) {
               float shimmerPos = Math.abs((float)i - (float)halfWidth / 2.0F + lineOffset);
               float shimmer = Math.max(0.0F, 1.0F - shimmerPos / 50.0F);
               fade = Math.min(1.0F, fade + shimmer * 0.5F);
            }

            int whiteAlpha = (int)(fade * (float)alpha * lineBrightness);
            whiteAlpha = Math.min(255, whiteAlpha);
            int gradientColor = whiteAlpha << 24 | 16777215;
            drawRect(x + i, y, x + i + 1, y + 1, gradientColor);
            drawRect(x + width - i - 1, y, x + width - i, y + 1, gradientColor);
         }

         int textColor = alpha << 24 | 16777215;
         int textWidth = customFontEnabled ? Vonware.textManager.getStringWidth(text) : this.mc.fontRenderer.getStringWidth(text);
         float textX = (float)centerX - (float)textWidth / 2.0F;
         float textY = (float)y + (float)(height - 8) / 2.0F;
         if (hovered && customFontEnabled) {
            for(int i = 3; i > 0; --i) {
               float offset = (float)i * 0.5F;
               int glowAlpha = alpha * 40 / i / 255;
               int textGlowColor = glowAlpha << 24 | 16777215;
               Vonware.textManager.drawStringWithShadow(text, textX - offset, textY, textGlowColor);
               Vonware.textManager.drawStringWithShadow(text, textX + offset, textY, textGlowColor);
               Vonware.textManager.drawStringWithShadow(text, textX, textY - offset, textGlowColor);
               Vonware.textManager.drawStringWithShadow(text, textX, textY + offset, textGlowColor);
            }
         }

         if (customFontEnabled) {
            Vonware.textManager.drawStringWithShadow(text, textX, textY, textColor);
         } else {
            this.mc.fontRenderer.drawStringWithShadow(text, textX, textY, textColor);
         }

         GlStateManager.popMatrix();
      }

   }

   protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      if (!CustomDeathUI.getInstance().isEnabled()) {
         super.mouseClicked(mouseX, mouseY, mouseButton);
      } else {
         CustomDeathUI.Animation mode = CustomDeathUI.getInstance().animation.getValue();
         if (mode == CustomDeathUI.Animation.ReZero) {
            if (this.isAutoRespawnActive()) {
               return;
            }

            if (this.reZeroAnim.getTimeSinceDeath() < 500L) {
               return;
            }
         } else if (this.heartAnim.getTimeSinceDeath() < 3200L) {
            return;
         }

         if (mouseButton == 0) {
            for(GuiButton button : this.buttonList) {
               if (button.mousePressed(this.mc, mouseX, mouseY)) {
                  button.playPressSound(this.mc.getSoundHandler());
                  this.actionPerformed(button);
                  return;
               }
            }
         }

      }
   }

   @Inject(
      method = {"actionPerformed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void onActionPerformed(GuiButton button, CallbackInfo ci) throws IOException {
      if (CustomDeathUI.getInstance().isEnabled()) {
         CustomDeathUI.Animation mode = CustomDeathUI.getInstance().animation.getValue();
         if (mode == CustomDeathUI.Animation.ReZero) {
            if (this.isAutoRespawnActive()) {
               ci.cancel();
            } else {
               if (this.reZeroAnim.getTimeSinceDeath() < 500L) {
                  ci.cancel();
               } else if (button.id == 0) {
                  this.reZeroAnim.setRespawnClickTime(System.currentTimeMillis());
               }

            }
         } else {
            if (this.heartAnim.getTimeSinceDeath() < 3200L) {
               ci.cancel();
            } else if (button.id == 0) {
               this.heartAnim.setRespawnClickTime(System.currentTimeMillis());
            }

         }
      }
   }

   @Inject(
      method = {"drawScreen"},
      at = {@At("RETURN")}
   )
   public void afterDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      if (CustomDeathUI.getInstance().isEnabled()) {
         CustomDeathUI.Animation mode = CustomDeathUI.getInstance().animation.getValue();
         if (mode == CustomDeathUI.Animation.Heart) {
            if (this.heartAnim.getRespawnClickTime() == -1L) {
               return;
            }

            long timeSinceClick = System.currentTimeMillis() - this.heartAnim.getRespawnClickTime();
            if (timeSinceClick >= 600L) {
               this.heartAnim.clearRespawnClick();
               if (this.mc.player != null) {
                  this.mc.player.respawnPlayer();
               }

               this.mc.displayGuiScreen((GuiScreen)null);
            }
         } else if (mode == CustomDeathUI.Animation.ReZero && !this.isAutoRespawnActive()) {
            if (this.reZeroAnim.getRespawnClickTime() == -1L) {
               return;
            }

            long timeSinceClick = System.currentTimeMillis() - this.reZeroAnim.getRespawnClickTime();
            if (timeSinceClick >= 500L) {
               this.reZeroAnim.clearRespawnClick();
               if (this.mc.player != null) {
                  this.mc.player.respawnPlayer();
               }

               this.mc.displayGuiScreen((GuiScreen)null);
            }
         }

      }
   }

   private boolean isAutoRespawnActive() {
      Module autoRespawn = Vonware.moduleManager.getModuleByName("AutoRespawn");
      return autoRespawn != null && autoRespawn.isEnabled();
   }
}
