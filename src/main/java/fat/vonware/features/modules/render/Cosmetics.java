package fat.vonware.features.modules.render;

import fat.vonware.event.events.Render3DEvent;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Animations.game.MahoragaWheelModel;
import fat.vonware.util.Cosmetics.RenderWings;
import java.awt.Color;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class Cosmetics extends Module {
   private static final MahoragaWheelModel WHEEL_MODEL = new MahoragaWheelModel();
   private static final SoundEvent WHEEL_SPIN_SOUND = new SoundEvent(new ResourceLocation("vonware", "adaptation_wheel_spin"));
   private static final long POP_PULSE_MS = 900L;
   private static final long SPIN_DURATION_MS = 650L;
   private static final long POP_DEBOUNCE_MS = 250L;
   private static final float SPIN_STEP_DEGREES = -45.0F;
   private static final int MAX_QUEUED_SPINS = 8;
   private static final float LINE_WIDTH = 2.25F;
   public final Setting<Boolean> wings = new Setting<Boolean>("Wings", true);
   private final Setting<Boolean> wheel = new Setting<Boolean>("MahoragaWheel", true);
   private final Setting<Float> wheelHeight = (new Setting<Float>("WheelHeight", 0.3F, 0.1F, 3.0F, (v) -> (Boolean)this.wheel.getValue())).setStep(0.05F);
   private final Setting<Float> wheelScaleSetting = (new Setting<Float>("WheelScale", 0.3F, 0.2F, 2.5F, (v) -> (Boolean)this.wheel.getValue())).setStep(0.1F);
   private final Setting<Float> wheelTilt = new Setting<Float>("WheelTilt", 0.0F, 0.0F, 45.0F, (v) -> (Boolean)this.wheel.getValue());
   private final Setting<Float> wheelPulseScale = (new Setting<Float>("WheelPulseScale", 0.0F, 0.0F, 0.8F, (v) -> (Boolean)this.wheel.getValue())).setStep(0.05F);
   private final Setting<Boolean> wheelSound = new Setting<Boolean>("WheelSound", true, (v) -> (Boolean)this.wheel.getValue());
   private final Setting<Float> wheelSoundVolume = new Setting<Float>("WheelSoundVolume", 1.0F, 0.0F, 2.0F, (v) -> (Boolean)this.wheel.getValue() && (Boolean)this.wheelSound.getValue());
   private WheelState wheelState = new WheelState();
   private static Cosmetics instance;

   public Cosmetics() {
      super("Cosmetics", "wings, mahoraga wheel", Module.Category.RENDER, true, false, false);
      this.register(this.wings);
      this.register(this.wheel);
      this.register(this.wheelHeight);
      this.register(this.wheelScaleSetting);
      this.register(this.wheelTilt);
      this.register(this.wheelPulseScale);
      this.register(this.wheelSound);
      this.register(this.wheelSoundVolume);
   }

   public void onDisable() {
      this.wheelState = new WheelState();
   }

   public void onLogout() {
      this.wheelState = new WheelState();
   }

   public void onUpdate() {
      if (!fullNullCheck() && (Boolean)this.wheel.getValue()) {
         this.advanceSpinState(this.wheelState, System.currentTimeMillis());
      }
   }

   @SubscribeEvent
   public void onRenderPlayer(RenderPlayerEvent.Post event) {
      EntityPlayer player = event.getEntityPlayer();
      if (player == mc.player && (Boolean)this.wings.getValue()) {
         RenderWings.getInstance().doRender(player, event.getPartialRenderTick());
      }

   }

   @SubscribeEvent
   public void onTotemPop(TotemPopEvent event) {
      if (event != null && !fullNullCheck() && (Boolean)this.wheel.getValue() && event.getEntity() == mc.player) {
         long now = System.currentTimeMillis();
         if (now - this.wheelState.lastPopTime >= 250L) {
            this.wheelState.lastPopTime = now;
            this.advanceSpinState(this.wheelState, now);
            if (this.wheelState.spinning) {
               this.wheelState.queuedSpins = Math.min(8, this.wheelState.queuedSpins + 1);
            } else {
               this.startSpin(this.wheelState, now);
            }

            if ((Boolean)this.wheelSound.getValue()) {
               this.playWheelSound();
            }

         }
      }
   }

   public void onRender3D(Render3DEvent event) {
      if (!fullNullCheck() && (Boolean)this.wheel.getValue()) {
         this.renderWheel(mc.player, this.wheelState, event.getPartialTicks(), System.currentTimeMillis());
      }
   }

   private void renderWheel(EntityPlayer player, WheelState state, float partialTicks, long now) {
      Vec3d interpolated = EntityUtil.getInterpolatedPos(player, (double)partialTicks);
      Vec3d center = new Vec3d(interpolated.x, interpolated.y + (double)player.height + (double)(Float)this.wheelHeight.getValue(), interpolated.z);
      float popProgress = this.getPopProgress(state, now);
      float renderScale = (Float)this.wheelScaleSetting.getValue() * (1.0F + popProgress * (Float)this.wheelPulseScale.getValue());
      float width = 2.25F * (1.0F + popProgress * 0.18F);
      Color primary = this.brighten(new Color(211, 156, 55, 255), popProgress * 0.22F);
      Color accent = this.brighten(new Color(255, 218, 118, 255), popProgress * 0.16F);
      float faceTilt = -90.0F + (Float)this.wheelTilt.getValue();
      double faceTiltRadians = Math.toRadians((double)faceTilt);
      double frontNormalY = -Math.sin(faceTiltRadians);
      double frontNormalZ = Math.cos(faceTiltRadians);
      boolean frontView = (mc.getRenderManager().viewerPosY - center.y) * frontNormalY + (mc.getRenderManager().viewerPosZ - center.z) * frontNormalZ >= (double)0.0F;
      GlStateManager.pushMatrix();

      try {
         GlStateManager.translate(center.x - mc.getRenderManager().viewerPosX, center.y - mc.getRenderManager().viewerPosY, center.z - mc.getRenderManager().viewerPosZ);
         GlStateManager.rotate(faceTilt, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(this.getSpinAngle(state, now), 0.0F, 0.0F, 1.0F);
         GlStateManager.scale(renderScale, renderScale, renderScale);
         GlStateManager.enableBlend();
         GlStateManager.enableDepth();
         GlStateManager.tryBlendFuncSeparate(770, 771, 0, 1);
         GlStateManager.disableTexture2D();
         GlStateManager.disableLighting();
         GlStateManager.depthMask(true);
         GlStateManager.disableCull();
         GL11.glEnable(2848);
         GL11.glHint(3154, 4354);
         GL11.glLineWidth(width);
         WHEEL_MODEL.render(primary, accent, popProgress, frontView);
      } finally {
         GL11.glDisable(2848);
         GlStateManager.depthMask(true);
         GlStateManager.enableCull();
         GlStateManager.enableDepth();
         GlStateManager.enableLighting();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.popMatrix();
      }

   }

   private Color brighten(Color color, float factor) {
      if (factor <= 0.0F) {
         return color;
      } else {
         int newRed = MathHelper.clamp(color.getRed() + Math.round((float)(255 - color.getRed()) * factor), 0, 255);
         int newGreen = MathHelper.clamp(color.getGreen() + Math.round((float)(255 - color.getGreen()) * factor), 0, 255);
         int newBlue = MathHelper.clamp(color.getBlue() + Math.round((float)(255 - color.getBlue()) * factor), 0, 255);
         int newAlpha = MathHelper.clamp(color.getAlpha() + Math.round((float)(255 - color.getAlpha()) * factor * 0.5F), 0, 255);
         return new Color(newRed, newGreen, newBlue, newAlpha);
      }
   }

   private void playWheelSound() {
      if (mc.getSoundHandler() != null && !((Float)this.wheelSoundVolume.getValue() <= 0.0F)) {
         PositionedSoundRecord record = new PositionedSoundRecord(WHEEL_SPIN_SOUND.getSoundName(), SoundCategory.MASTER, (Float)this.wheelSoundVolume.getValue(), 1.0F, false, 0, AttenuationType.NONE, 0.0F, 0.0F, 0.0F);
         mc.getSoundHandler().playSound(record);
      }
   }

   private float getPopProgress(WheelState state, long now) {
      if (state.lastPopTime <= 0L) {
         return 0.0F;
      } else {
         long elapsed = now - state.lastPopTime;
         return elapsed >= 900L ? 0.0F : 1.0F - (float)elapsed / 900.0F;
      }
   }

   private void startSpin(WheelState state, long now) {
      state.spinStartAngle = state.settledAngle;
      state.spinStartedAt = now;
      state.spinning = true;
   }

   private void advanceSpinState(WheelState state, long now) {
      while(true) {
         if (state.spinning && now - state.spinStartedAt >= 650L) {
            state.settledAngle = this.wrapDegrees(state.spinStartAngle + -45.0F);
            if (state.queuedSpins > 0) {
               state.queuedSpins--;
               state.spinStartAngle = state.settledAngle;
               state.spinStartedAt = state.spinStartedAt + 650L;
               continue;
            }

            state.spinning = false;
         }

         return;
      }
   }

   private float getSpinAngle(WheelState state, long now) {
      this.advanceSpinState(state, now);
      if (!state.spinning) {
         return state.settledAngle;
      } else {
         float progress = MathHelper.clamp((float)(now - state.spinStartedAt) / 650.0F, 0.0F, 1.0F);
         return state.spinStartAngle + this.physicsSnap(progress) * -45.0F;
      }
   }

   private float physicsSnap(float progress) {
      if (progress < 0.65F) {
         return this.easeInExpo(progress / 0.65F) * 0.7F;
      } else {
         float springProgress = (progress - 0.65F) / 0.35F;
         float spring = 1.0F - (float)(Math.exp((double)-5.5F * (double)springProgress) * Math.cos((double)14.0F * (double)springProgress));
         return 0.7F + 0.3F * spring;
      }
   }

   private float easeInExpo(float progress) {
      return progress == 0.0F ? 0.0F : (float)Math.pow((double)2.0F, (double)(10.0F * (progress - 1.0F)));
   }

   private float wrapDegrees(float angle) {
      return MathHelper.wrapDegrees(angle);
   }

   public static Cosmetics getInstance() {
      if (instance == null) {
         instance = new Cosmetics();
      }

      return instance;
   }

   private static class WheelState {
      private float settledAngle;
      private float spinStartAngle;
      private long spinStartedAt;
      private int queuedSpins;
      private boolean spinning;
      private long lastPopTime;

      private WheelState() {
      }
   }
}
