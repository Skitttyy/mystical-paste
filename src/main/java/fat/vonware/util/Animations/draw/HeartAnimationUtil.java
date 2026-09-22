package fat.vonware.util.Animations.draw;

import fat.vonware.Vonware;
import fat.vonware.features.modules.client.Fonts;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;

public class HeartAnimationUtil {
   private static final String DEATH_TEXT = "You Died";
   private static final Random random = new Random();
   private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
   private static final Minecraft mc = Minecraft.getMinecraft();
   private long deathTime = -1L;
   private long respawnClickTime = -1L;
   private int typedChars = 0;
   private boolean heartBreakSoundPlayed = false;
   private int lastRemainingChars = "You Died".length();
   private boolean respawnButtonHovered = false;
   private boolean wasHoveringRespawn = false;
   private long hoverOffTime = -1L;
   private long hoverStartTime = -1L;

   public void reset() {
      this.deathTime = System.currentTimeMillis();
      this.respawnClickTime = -1L;
      this.typedChars = 0;
      this.heartBreakSoundPlayed = false;
      this.lastRemainingChars = "You Died".length();
      this.respawnButtonHovered = false;
      this.wasHoveringRespawn = false;
      this.hoverOffTime = -1L;
      this.hoverStartTime = -1L;
   }

   public long getDeathTime() {
      return this.deathTime;
   }

   public long getRespawnClickTime() {
      return this.respawnClickTime;
   }

   public void setRespawnClickTime(long time) {
      this.respawnClickTime = time;
   }

   public void clearRespawnClick() {
      this.respawnClickTime = -1L;
   }

   public long getTimeSinceDeath() {
      return System.currentTimeMillis() - this.deathTime;
   }

   public boolean isRespawnButtonHovered() {
      return this.respawnButtonHovered;
   }

   public void renderBreakingHeart(ScaledResolution sr, long timeSinceDeath) {
      float breakProgress;
      if (this.respawnClickTime != -1L) {
         long timeSinceRespawn = System.currentTimeMillis() - this.respawnClickTime;
         float snapProgress = Math.min(1.0F, (float)timeSinceRespawn / 500.0F);
         breakProgress = 1.0F - snapProgress;
         breakProgress *= breakProgress;
      } else if (this.respawnButtonHovered && timeSinceDeath >= 1200L && this.hoverStartTime != -1L) {
         long timeSinceHoverStart = System.currentTimeMillis() - this.hoverStartTime;
         float healProgress = Math.min(1.0F, (float)timeSinceHoverStart / 400.0F);
         breakProgress = 1.0F - healProgress;
      } else if (this.wasHoveringRespawn && this.hoverOffTime != -1L) {
         long timeSinceHoverOff = System.currentTimeMillis() - this.hoverOffTime;
         if (timeSinceHoverOff < 400L) {
            breakProgress = (float)timeSinceHoverOff / 400.0F;
         } else {
            this.wasHoveringRespawn = false;
            this.hoverOffTime = -1L;
            breakProgress = 1.0F;
         }
      } else {
         breakProgress = Math.min(1.0F, Math.max(0.0F, (float)(timeSinceDeath - 200L) / 1000.0F));
         if (timeSinceDeath >= 200L && !this.heartBreakSoundPlayed && mc.player != null) {
            mc.player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.4F, 0.1F);
            this.heartBreakSoundPlayed = true;
         }
      }

      if (timeSinceDeath >= 200L || this.respawnClickTime != -1L) {
         mc.getTextureManager().bindTexture(ICONS);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         float heartScale = 2.5F;
         float centerX = (float)sr.getScaledWidth() / 2.0F;
         float centerY = (float)sr.getScaledHeight() / 2.0F - 20.0F;
         GlStateManager.color(1.0F - breakProgress * 0.5F, 1.0F - breakProgress, 1.0F - breakProgress, 1.0F);
         float separation = breakProgress * 12.0F;
         if (breakProgress < 0.01F) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX, centerY, 0.0F);
            GlStateManager.scale(heartScale, heartScale, 1.0F);
            (new Gui()).drawTexturedModalRect(-4, -4, 52, 0, 9, 9);
            GlStateManager.popMatrix();
         } else {
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX - separation, centerY + breakProgress * 2.0F, 0.0F);
            GlStateManager.scale(heartScale, heartScale, 1.0F);
            GlStateManager.rotate(breakProgress * -15.0F, 0.0F, 0.0F, 1.0F);
            (new Gui()).drawTexturedModalRect(-5, -4, 52, 0, 5, 9);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX + separation, centerY + breakProgress * 2.0F, 0.0F);
            GlStateManager.scale(heartScale, heartScale, 1.0F);
            GlStateManager.rotate(breakProgress * 15.0F, 0.0F, 0.0F, 1.0F);
            (new Gui()).drawTexturedModalRect(0, -4, 57, 0, 4, 9);
            GlStateManager.popMatrix();
         }

         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.disableBlend();
      }
   }

   public void renderGlitchyTypingText(ScaledResolution sr, long phaseTime) {
      boolean customFontEnabled = Fonts.getInstance().isEnabled();
      int currentChars = (int)(phaseTime / 200L);
      currentChars = Math.min(currentChars, "You Died".length());
      if (currentChars > this.typedChars && mc.player != null) {
         mc.player.playSound(SoundEvents.UI_BUTTON_CLICK, 0.2F, 1.5F);
         this.typedChars = currentChars;
      }

      if (currentChars != 0) {
         String text = "You Died".substring(0, currentChars);
         if (!(random.nextFloat() < 0.15F)) {
            int textWidth = customFontEnabled ? Vonware.textManager.getStringWidth(text) : mc.fontRenderer.getStringWidth(text);
            float jitterX = (random.nextFloat() - 0.5F) * 3.0F;
            float jitterY = (random.nextFloat() - 0.5F) * 3.0F;
            float centerX = (float)(sr.getScaledWidth() - textWidth * 2) / 2.0F + jitterX;
            float centerY = (float)sr.getScaledHeight() / 2.0F - 60.0F + jitterY;
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX + (float)textWidth, centerY, 0.0F);
            GlStateManager.scale(2.0F, 2.0F, 1.0F);
            GlStateManager.translate((float)(-textWidth) / 2.0F, 0.0F, 0.0F);
            this.drawGlowingText(text, 0.0F, 0.0F, -1, 1.0F, customFontEnabled);
            GlStateManager.popMatrix();
         }
      }
   }

   public void renderGlitchyFullText(ScaledResolution sr) {
      boolean customFontEnabled = Fonts.getInstance().isEnabled();
      if (!(random.nextFloat() < 0.1F)) {
         String text = "You Died";
         int textWidth = customFontEnabled ? Vonware.textManager.getStringWidth(text) : mc.fontRenderer.getStringWidth(text);
         float jitterX = (random.nextFloat() - 0.5F) * 1.5F;
         float jitterY = (random.nextFloat() - 0.5F) * 1.5F;
         float centerX = (float)(sr.getScaledWidth() - textWidth * 2) / 2.0F + jitterX;
         float centerY = (float)sr.getScaledHeight() / 2.0F - 60.0F + jitterY;
         GlStateManager.pushMatrix();
         GlStateManager.translate(centerX + (float)textWidth, centerY, 0.0F);
         GlStateManager.scale(2.0F, 2.0F, 1.0F);
         GlStateManager.translate((float)(-textWidth) / 2.0F, 0.0F, 0.0F);
         this.drawGlowingText(text, 0.0F, 0.0F, -1, 1.2F, customFontEnabled);
         GlStateManager.popMatrix();
      }
   }

   public void renderGlitchyDeletingText(ScaledResolution sr, long phaseTime) {
      boolean customFontEnabled = Fonts.getInstance().isEnabled();
      int charsToDelete = (int)(phaseTime / 200L);
      int remainingChars = "You Died".length() - charsToDelete;
      remainingChars = Math.max(0, remainingChars);
      if (remainingChars < this.lastRemainingChars && remainingChars > 0 && mc.player != null) {
         mc.player.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, 1.0F, 0.5F);
         this.lastRemainingChars = remainingChars;
      }

      if (remainingChars == 0) {
         this.typedChars = 0;
         this.lastRemainingChars = "You Died".length();
      } else if (!(random.nextFloat() < 0.15F)) {
         String text = "You Died".substring(0, remainingChars);
         int textWidth = customFontEnabled ? Vonware.textManager.getStringWidth(text) : mc.fontRenderer.getStringWidth(text);
         float glitchIntensity = 1.0F + (1.0F - (float)remainingChars / (float)"You Died".length());
         float jitterX = (random.nextFloat() - 0.5F) * 3.0F * glitchIntensity;
         float jitterY = (random.nextFloat() - 0.5F) * 3.0F * glitchIntensity;
         float centerX = (float)(sr.getScaledWidth() - textWidth * 2) / 2.0F + jitterX;
         float centerY = (float)sr.getScaledHeight() / 2.0F - 60.0F + jitterY;
         float alpha = (float)remainingChars / (float)"You Died".length();
         GlStateManager.pushMatrix();
         GlStateManager.translate(centerX + (float)textWidth, centerY, 0.0F);
         GlStateManager.scale(2.0F, 2.0F, 1.0F);
         GlStateManager.translate((float)(-textWidth) / 2.0F, 0.0F, 0.0F);
         int textAlpha = (int)(alpha * 255.0F) << 24;
         int color = textAlpha | -65536;
         this.drawGlowingText(text, 0.0F, 0.0F, color, alpha, customFontEnabled);
         GlStateManager.popMatrix();
      }
   }

   public void updateHoverState(boolean currentlyHoveringRespawn) {
      if (currentlyHoveringRespawn && !this.respawnButtonHovered) {
         this.hoverStartTime = System.currentTimeMillis();
         this.wasHoveringRespawn = false;
      } else if (!currentlyHoveringRespawn && this.respawnButtonHovered) {
         this.wasHoveringRespawn = true;
         this.hoverOffTime = System.currentTimeMillis();
         this.hoverStartTime = -1L;
      }

      this.respawnButtonHovered = currentlyHoveringRespawn;
   }

   private void drawGlowingText(String text, float x, float y, int color, float glowIntensity, boolean customFontEnabled) {
      if (customFontEnabled) {
         for(int i = 3; i > 0; --i) {
            float offset = (float)i * 0.8F;
            int glowAlpha = (int)(glowIntensity * 52.0F / (float)i);
            int glowColor = glowAlpha << 24 | 16777215;
            Vonware.textManager.drawStringWithShadow(text, x - offset, y, glowColor);
            Vonware.textManager.drawStringWithShadow(text, x + offset, y, glowColor);
            Vonware.textManager.drawStringWithShadow(text, x, y - offset, glowColor);
            Vonware.textManager.drawStringWithShadow(text, x, y + offset, glowColor);
         }

         Vonware.textManager.drawStringWithShadow(text, x, y, color);
      } else {
         mc.fontRenderer.drawStringWithShadow(text, x, y, color);
      }

   }
}
