package fat.vonware.util.Animations.draw;

import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;

public class ReZeroAnimationUtil {
   private static final Minecraft mc = Minecraft.getMinecraft();
   private static final Random random = new Random();
   private long deathTime = -1L;
   private long respawnClickTime = -1L;
   private boolean deathSoundPlayed = false;
   private float shakeOffsetX = 0.0F;
   private float shakeOffsetY = 0.0F;
   public static final int EFFECT_DURATION = 500;

   public void reset() {
      this.deathTime = System.currentTimeMillis();
      this.respawnClickTime = -1L;
      this.deathSoundPlayed = false;
      this.shakeOffsetX = 0.0F;
      this.shakeOffsetY = 0.0F;
   }

   public long getTimeSinceDeath() {
      return System.currentTimeMillis() - this.deathTime;
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

   public float getShakeOffsetX() {
      return this.shakeOffsetX;
   }

   public float getShakeOffsetY() {
      return this.shakeOffsetY;
   }

   public void render(ScaledResolution sr, long timeSinceDeath) {
      int w = sr.getScaledWidth();
      int h = sr.getScaledHeight();
      if (!this.deathSoundPlayed) {
         SoundUtil.playReZeroDeath();
         this.deathSoundPlayed = true;
      }

      this.updateShake(timeSinceDeath);
      if (timeSinceDeath < 200L) {
         float progress = (float)timeSinceDeath / 200.0F;
         float intensity = progress * progress;
         int alpha = (int)(intensity * 255.0F);
         Gui.drawRect(0, 0, w, h, alpha << 24 | 16777215);
      } else {
         float invertProgress = (float)(timeSinceDeath - 200L) / 300.0F;
         invertProgress = Math.min(1.0F, invertProgress);
         float whiteAlpha = Math.max(0.0F, 1.0F - invertProgress * 2.5F);
         float blackAlpha = Math.min(1.0F, invertProgress * 1.5F);
         if (whiteAlpha > 0.0F) {
            int wa = (int)(whiteAlpha * 255.0F);
            Gui.drawRect(0, 0, w, h, wa << 24 | 16777215);
         }

         int ba = (int)(blackAlpha * 255.0F);
         Gui.drawRect(0, 0, w, h, ba << 24);
      }

      if (this.respawnClickTime != -1L) {
         long timeSinceClick = System.currentTimeMillis() - this.respawnClickTime;
         if (timeSinceClick < 400L) {
            float flashAlpha = 1.0F - (float)timeSinceClick / 400.0F;
            flashAlpha *= flashAlpha;
            int fa = (int)(flashAlpha * 255.0F);
            Gui.drawRect(0, 0, w, h, fa << 24 | 16777215);
         }
      }

   }

   private void updateShake(long timeSinceDeath) {
      if (timeSinceDeath >= 150L && timeSinceDeath < 500L) {
         float shakeIntensity = 1.0F - (float)(timeSinceDeath - 150L) / 350.0F;
         shakeIntensity = Math.max(0.0F, shakeIntensity);
         float maxShake = 4.0F * shakeIntensity;
         this.shakeOffsetX = (random.nextFloat() - 0.5F) * 2.0F * maxShake;
         this.shakeOffsetY = (random.nextFloat() - 0.5F) * 2.0F * maxShake;
      } else if (this.respawnClickTime != -1L) {
         long timeSinceClick = System.currentTimeMillis() - this.respawnClickTime;
         if (timeSinceClick < 300L) {
            float clickShake = 1.0F - (float)timeSinceClick / 300.0F;
            float maxShake = 3.0F * clickShake;
            this.shakeOffsetX = (random.nextFloat() - 0.5F) * 2.0F * maxShake;
            this.shakeOffsetY = (random.nextFloat() - 0.5F) * 2.0F * maxShake;
         } else {
            this.shakeOffsetX = 0.0F;
            this.shakeOffsetY = 0.0F;
         }
      } else {
         this.shakeOffsetX = 0.0F;
         this.shakeOffsetY = 0.0F;
      }

   }
}
