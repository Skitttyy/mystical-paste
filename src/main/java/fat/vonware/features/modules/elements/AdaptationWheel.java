package fat.vonware.features.modules.elements;

import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Timer;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AdaptationWheel extends HudModule {
   private final Setting<Float> scale = new Setting<Float>("Scale", 1.0F, 0.1F, 2.0F);
   private final Setting<Boolean> alwaysVisible = new Setting<Boolean>("AlwaysVisible", false);
   private static final ResourceLocation WHEEL_TEXTURE = new ResourceLocation("textures/wheel.png");
   private static final float BASE_SIZE = 64.0F;
   private static final float FADE_IN_MS = 1000.0F;
   private static final float SPIN_MS = 650.0F;
   private static final float HOLD_MS = 700.0F;
   private static final float FADE_OUT_MS = 650.0F;
   private static final float TOTAL_MS = 3000.0F;
   private static final float SPIN_DEGREES = 45.0F;
   private static final float POP_DEBOUNCE_MS = 250.0F;
   private final Timer animTimer = new Timer();
   private final Timer lastPopTimer = new Timer();
   private boolean isAnimating = false;
   private float timerOffset = 0.0F;
   private int queuedSpins = 0;
   private boolean spinSoundPlayed = false;
   private float currentRotation = 0.0F;
   private float spinStartRotation = 0.0F;

   public AdaptationWheel() {
      super("AdaptationWheel", "Mahoraga's wheel appears on totem pop", Module.Category.Elements, true, false, false, 100.0F, 50.0F);
      this.register(this.scale);
      this.register(this.alwaysVisible);
   }

   @SubscribeEvent
   public void onTotemPop(TotemPopEvent event) {
      if (mc.player != null) {
         if (event.getEntity() == mc.player) {
            if (!((float)this.lastPopTimer.getPassedTimeMs() < 250.0F)) {
               this.lastPopTimer.reset();
               if ((Boolean)this.alwaysVisible.getValue()) {
                  this.startSpin(false);
               } else if (this.isAnimating) {
                  ++this.queuedSpins;
               } else {
                  this.queuedSpins = 0;
                  this.startSpin(false);
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && mc.world != null) {
         float xPos = (Float)this.x.getValue();
         float yPos = (Float)this.y.getValue();
         boolean inHudEditor = mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
         if (inHudEditor) {
            this.renderWheel(xPos, yPos, 0.45F, 20.0F);
         } else if (this.isAnimating || (Boolean)this.alwaysVisible.getValue()) {
            float elapsed = this.isAnimating ? (float)this.animTimer.getPassedTimeMs() + this.timerOffset : 0.0F;
            if (this.isAnimating && elapsed >= 3000.0F) {
               this.currentRotation = (this.currentRotation + 45.0F) % 360.0F;
               this.isAnimating = false;
            } else {
               float alpha;
               float rotation;
               if ((Boolean)this.alwaysVisible.getValue()) {
                  if (!this.isAnimating) {
                     alpha = 1.0F;
                     rotation = this.currentRotation;
                  } else if (elapsed < 1000.0F) {
                     alpha = 1.0F;
                     rotation = this.currentRotation;
                  } else if (elapsed < 1650.0F) {
                     float t = (elapsed - 1000.0F) / 650.0F;
                     alpha = 1.0F;
                     rotation = this.currentRotation + this.physicsSnap(t) * 45.0F;
                     if (!this.spinSoundPlayed && t >= 0.2F) {
                        SoundUtil.playAdaptationWheelSpin();
                        this.spinSoundPlayed = true;
                     }
                  } else {
                     alpha = 1.0F;
                     rotation = (this.currentRotation + 45.0F) % 360.0F;
                  }
               } else if (elapsed < 1000.0F) {
                  float t = elapsed / 1000.0F;
                  alpha = this.easeOutQuad(t);
                  rotation = this.spinStartRotation;
               } else if (elapsed < 1650.0F) {
                  float t = (elapsed - 1000.0F) / 650.0F;
                  alpha = 1.0F;
                  rotation = this.spinStartRotation + this.physicsSnap(t) * 45.0F;
                  if (!this.spinSoundPlayed && t >= 0.2F) {
                     SoundUtil.playAdaptationWheelSpin();
                     this.spinSoundPlayed = true;
                  }
               } else if (elapsed < 2350.0F) {
                  alpha = 1.0F;
                  rotation = this.spinStartRotation + 45.0F;
               } else if (this.queuedSpins > 0) {
                  --this.queuedSpins;
                  this.currentRotation = (this.currentRotation + 45.0F) % 360.0F;
                  this.startSpin(true);
                  elapsed = (float)this.animTimer.getPassedTimeMs() + this.timerOffset;
                  float t = Math.max(0.0F, (elapsed - 1000.0F) / 650.0F);
                  alpha = 1.0F;
                  rotation = this.spinStartRotation + this.physicsSnap(t) * 45.0F;
               } else {
                  float t = (elapsed - 1000.0F - 650.0F - 700.0F) / 650.0F;
                  alpha = 1.0F - this.easeInQuad(t);
                  rotation = this.spinStartRotation + 45.0F;
               }

               this.renderWheel(xPos, yPos, alpha, rotation);
            }
         }
      }
   }

   private void startSpin(boolean skipFadeIn) {
      this.animTimer.reset();
      this.timerOffset = skipFadeIn ? 1000.0F : 0.0F;
      this.spinStartRotation = this.currentRotation;
      this.isAnimating = true;
      this.spinSoundPlayed = false;
   }

   private float physicsSnap(float t) {
      if (t < 0.65F) {
         return this.easeInExpo(t / 0.65F) * 0.7F;
      } else {
         float s = (t - 0.65F) / 0.35F;
         float spring = 1.0F - (float)(Math.exp((double)-5.5F * (double)s) * Math.cos((double)14.0F * (double)s));
         return 0.7F + 0.3F * spring;
      }
   }

   private float easeInExpo(float t) {
      return t == 0.0F ? 0.0F : (float)Math.pow((double)2.0F, (double)(10.0F * (t - 1.0F)));
   }

   private void renderWheel(float x, float y, float alpha, float rotationDeg) {
      float size = 64.0F * (Float)this.scale.getValue();
      float half = size * 0.5F;
      float cx = x + half;
      float cy = y + half;
      GlStateManager.pushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
      GlStateManager.translate(cx, cy, 0.0F);
      GlStateManager.rotate(rotationDeg, 0.0F, 0.0F, 1.0F);
      mc.getTextureManager().bindTexture(WHEEL_TEXTURE);
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();
      buf.begin(7, DefaultVertexFormats.POSITION_TEX);
      buf.pos((double)(-half), (double)half, (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
      buf.pos((double)half, (double)half, (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
      buf.pos((double)half, (double)(-half), (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
      buf.pos((double)(-half), (double)(-half), (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
      tess.draw();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }

   public void updateBounds() {
      this.rect.setX((Float)this.x.getValue());
      this.rect.setY((Float)this.y.getValue());
      float size = 64.0F * (Float)this.scale.getValue();
      this.rect.setWidth((float)((int)size));
      this.rect.setHeight((float)((int)size));
   }

   private float easeOutQuad(float t) {
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   private float easeInQuad(float t) {
      return t * t;
   }
}
