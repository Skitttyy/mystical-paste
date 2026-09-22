package fat.vonware.features.modules.render;

import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.draw.BlackFlashUtil;
import fat.vonware.util.Animations.game.TintedLightningRenderer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.LogoutSpots.GhostRenderState;
import fat.vonware.util.LogoutSpots.LogoutSpot;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLightningBolt;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import org.lwjgl.opengl.GL11;

public class KillEffects extends Module {
   public Setting<Mode> mode;
   public Setting<Integer> amount;
   public ColorSetting lightningColor;
   public Setting<Float> blackFlashSize;
   public Setting<Integer> ghostFadeTime;
   public Setting<Integer> ghostAlpha;
   public Setting<Integer> furnaceDuration;
   public Setting<Float> furnaceRadius;
   public Setting<Float> furnaceHeight;
   public Setting<Integer> furnaceEmbers;
   public Setting<Boolean> sound;
   public Setting<Integer> numberSound;
   private final ArrayList<EntityPlayer> playersDead;
   private final List<GhostEffect> ghostEffects;
   private final List<FurnaceEffect> furnaceEffects;
   private static RenderManager cachedManager;
   private static TintedLightningRenderer tintedRenderer;
   private static Render<EntityLightningBolt> vanillaRenderer;

   public KillEffects() {
      super("KillEffects", "Effects when you kill", Module.Category.RENDER, true, false, false);
      this.mode = new Setting<Mode>("Mode", KillEffects.Mode.Lightning);
      this.amount = new Setting<Integer>("Number-Lightning", 1, 1, 10, (v) -> this.mode.getValue() == KillEffects.Mode.Lightning);
      this.lightningColor = new ColorSetting("Lightning-Color", 16777215, false, (v) -> this.mode.getValue() == KillEffects.Mode.Lightning);
      this.blackFlashSize = new Setting<Float>("BlackFlash-Size", 2.2F, 1.0F, 5.0F, (v) -> this.mode.getValue() == KillEffects.Mode.BlackFlash);
      this.ghostFadeTime = new Setting<Integer>("Ghost-Fade-Time", 1500, 250, 5000, (v) -> this.mode.getValue() == KillEffects.Mode.Ghost);
      this.ghostAlpha = new Setting<Integer>("Ghost-Alpha", 180, 1, 255, (v) -> this.mode.getValue() == KillEffects.Mode.Ghost);
      this.furnaceDuration = new Setting<Integer>("Furnace-Duration", 1400, 500, 5000, (v) -> this.mode.getValue() == KillEffects.Mode.FurnaceTest);
      this.furnaceRadius = new Setting<Float>("Furnace-Radius", 2.8F, 1.0F, 6.0F, (v) -> this.mode.getValue() == KillEffects.Mode.FurnaceTest);
      this.furnaceHeight = new Setting<Float>("Furnace-Height", 5.5F, 2.0F, 10.0F, (v) -> this.mode.getValue() == KillEffects.Mode.FurnaceTest);
      this.furnaceEmbers = new Setting<Integer>("Furnace-Embers", 32, 8, 96, (v) -> this.mode.getValue() == KillEffects.Mode.FurnaceTest);
      this.sound = new Setting<Boolean>("Sound", true);
      this.numberSound = new Setting<Integer>("Number-Sound", 1, 1, 10, (v) -> (Boolean)this.sound.getValue());
      this.playersDead = new ArrayList();
      this.ghostEffects = new ArrayList();
      this.furnaceEffects = new ArrayList();
      this.register(this.mode);
      this.register(this.amount);
      this.register(this.lightningColor);
      this.register(this.blackFlashSize);
      this.register(this.ghostFadeTime);
      this.register(this.ghostAlpha);
      this.register(this.furnaceDuration);
      this.register(this.furnaceRadius);
      this.register(this.furnaceHeight);
      this.register(this.furnaceEmbers);
      this.register(this.sound);
      this.register(this.numberSound);
   }

   public void onEnable() {
      this.playersDead.clear();
      this.ghostEffects.clear();
      this.furnaceEffects.clear();
      this.updateLightningRenderer();
   }

   public void onDisable() {
      this.playersDead.clear();
      this.ghostEffects.clear();
      this.furnaceEffects.clear();
      BlackFlashUtil.clear();
      this.restoreVanillaRenderer();
   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         this.updateLightningRenderer();

         for(EntityPlayer player : new ArrayList<EntityPlayer>(mc.world.playerEntities)) {
            if (player != null && player != mc.player) {
               boolean dead = player.isDead || player.getHealth() <= 0.0F;
               if (!dead) {
                  this.playersDead.remove(player);
               } else if (!this.playersDead.contains(player)) {
                  this.spawnKillEffect(player);
                  this.playersDead.add(player);
               }
            }
         }

      } else {
         this.playersDead.clear();
         this.ghostEffects.clear();
         this.furnaceEffects.clear();
         BlackFlashUtil.clear();
      }
   }

   public void onRender3D(Render3DEvent event) {
      if (event != null) {
         if (mc.player != null && mc.world != null) {
            BlackFlashUtil.render(event.getPartialTicks());
            this.renderGhostEffects(event.getPartialTicks());
            this.renderFurnaceEffects(event.getPartialTicks());
         } else {
            BlackFlashUtil.clear();
            this.ghostEffects.clear();
            this.furnaceEffects.clear();
         }
      }
   }

   private void spawnKillEffect(EntityPlayer player) {
      switch ((Mode)this.mode.getValue()) {
         case Lightning:
            this.spawnLightning(player);
            break;
         case BlackFlash:
            this.spawnBlackFlash(player);
            break;
         case Ghost:
            this.spawnGhost(player);
            break;
         case FurnaceTest:
            this.spawnFurnace(player);
      }

      if ((Boolean)this.sound.getValue()) {
         this.playEffectSound(player);
      }

   }

   private void spawnLightning(EntityPlayer player) {
      for(int i = 0; i < (Integer)this.amount.getValue(); ++i) {
         EntityLightningBolt lightningBolt = new EntityLightningBolt(mc.world, player.posX, player.posY, player.posZ, true);
         mc.world.spawnEntity(lightningBolt);
      }

   }

   private void spawnBlackFlash(EntityPlayer player) {
      double centerY = player.posY + (double)player.height * 0.56;
      BlackFlashUtil.spawn(player.posX, centerY, player.posZ, (Float)this.blackFlashSize.getValue());
   }

   private void spawnGhost(EntityPlayer player) {
      this.ghostEffects.add(new GhostEffect(new LogoutSpot(player), System.currentTimeMillis(), (Integer)this.ghostFadeTime.getValue(), (float)(Integer)this.ghostAlpha.getValue() / 255.0F));
   }

   private void spawnFurnace(EntityPlayer player) {
      this.furnaceEffects.add(new FurnaceEffect(player.posX, player.posY, player.posZ, System.currentTimeMillis(), (Integer)this.furnaceDuration.getValue(), (Float)this.furnaceRadius.getValue(), (Float)this.furnaceHeight.getValue(), (Integer)this.furnaceEmbers.getValue(), (long)player.getEntityId() * 31L + System.nanoTime()));
   }

   private void renderGhostEffects(float partialTicks) {
      if (!this.ghostEffects.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<GhostEffect> iterator = this.ghostEffects.iterator();
         List<GhostEffect> toRender = new ArrayList(this.ghostEffects.size());

         while(iterator.hasNext()) {
            GhostEffect effect = (GhostEffect)iterator.next();
            float progress = (float)(now - effect.startTime) / (float)effect.durationMs;
            if (progress >= 1.0F) {
               iterator.remove();
            } else {
               toRender.add(effect);
            }
         }

         if (!toRender.isEmpty()) {
            RenderManager renderManager = mc.getRenderManager();
            double camX = renderManager.viewerPosX;
            double camY = renderManager.viewerPosY;
            double camZ = renderManager.viewerPosZ;
            toRender.sort((a, b) -> Double.compare(distanceSq(b.spot, camX, camY, camZ), distanceSq(a.spot, camX, camY, camZ)));

            for(GhostEffect effect : toRender) {
               float progress = (float)(now - effect.startTime) / (float)effect.durationMs;
               float fade = 1.0F - Math.max(0.0F, Math.min(1.0F, progress));
               fade *= fade;
               this.renderGhost(effect.spot, partialTicks, effect.startAlpha * fade);
            }

         }
      }
   }

   private void renderFurnaceEffects(float partialTicks) {
      if (!this.furnaceEffects.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<FurnaceEffect> iterator = this.furnaceEffects.iterator();

         while(iterator.hasNext()) {
            FurnaceEffect effect = (FurnaceEffect)iterator.next();
            float progress = (float)(now - effect.startTime) / (float)effect.durationMs;
            if (progress >= 1.0F) {
               iterator.remove();
            } else {
               this.renderFurnace(effect, progress, partialTicks);
            }
         }

      }
   }

   private void renderFurnace(FurnaceEffect effect, float progress, float partialTicks) {
      RenderManager manager = mc.getRenderManager();
      double x = effect.x - manager.viewerPosX;
      double y = effect.y - manager.viewerPosY;
      double z = effect.z - manager.viewerPosZ;
      float time = ((float)(System.currentTimeMillis() - effect.startTime) + partialTicks * 50.0F) / 1000.0F;
      float fadeOut = 1.0F - smoothStep(0.7F, 1.0F, progress);
      float ignition = 1.0F - smoothStep(0.0F, 0.105F, progress);
      float blastProgress = clamp01((progress - 0.025F) / 0.48F);
      float blastEase = 1.0F - (float)Math.pow((double)(1.0F - blastProgress), (double)3.35F);
      float blastAlpha = fadeOut * smoothStep(0.015F, 0.075F, progress);
      float maxBlastRadius = effect.radius * 5.25F;
      float blastRadius = maxBlastRadius * blastEase;
      float fireHeight = effect.height * (0.24F + 0.3F * (1.0F - blastProgress));
      GlStateManager.pushMatrix();
      GlStateManager.translate(x, y + 0.04, z);
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableLighting();
      GlStateManager.disableCull();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.shadeModel(7425);
      this.drawIgnitionFlash(effect.radius * 1.35F, ignition);
      this.drawExpandingFireCloud(effect, blastRadius, fireHeight, time, blastProgress, blastAlpha);
      this.drawExplosionShell(blastRadius * 1.07F, effect.height * (0.14F + 0.2F * (1.0F - blastProgress)), time, blastAlpha * 0.24F, blastProgress);
      this.drawGroundBlast(blastRadius * 1.24F, blastAlpha * (0.95F - blastProgress * 0.42F));
      this.drawHeatRing(blastRadius * 1.23F, 0.045F, blastAlpha * (1.0F - blastProgress * 0.72F), 96);
      this.drawHeatRing(blastRadius * 1.08F, 0.11F, blastAlpha * 0.55F * (1.0F - blastProgress), 96);
      this.drawRadialFlameStreaks(effect, blastRadius * 1.15F, effect.height, blastProgress, blastAlpha);
      float coreLife = 1.0F - smoothStep(0.18F, 0.82F, progress);
      this.drawFlameColumn(effect.radius * (0.72F + ignition * 0.35F), effect.height * (0.45F + ignition * 0.35F), time, fadeOut * coreLife * 0.92F);
      this.drawEmbers(effect, time, blastProgress, blastAlpha);
      GlStateManager.shadeModel(7424);
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.enableCull();
      GlStateManager.enableLighting();
      GlStateManager.disableBlend();
      GlStateManager.enableTexture2D();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private void drawExpandingFireCloud(FurnaceEffect effect, float blastRadius, float height, float time, float progress, float alpha) {
      if (!(blastRadius <= 0.01F) && !(alpha <= 0.001F)) {
         float centerScale = effect.radius * (1.55F - progress * 0.92F);
         this.drawFireSphere((double)0.0F, (double)height * 0.33, (double)0.0F, centerScale, alpha * (1.0F - progress * 0.72F), time, 0.0F);
         int lobes = Math.max(22, Math.min(42, effect.embers));

         for(int i = 0; i < lobes; ++i) {
            long seed = mixSeed(effect.seed + (long)i * -7046029254386353131L);
            float angleRandom = (float)(seed >>> 8 & 65535L) / 65535.0F;
            float distanceRandom = (float)(seed >>> 25 & 65535L) / 65535.0F;
            float sizeRandom = (float)(seed >>> 41 & 255L) / 255.0F;
            float liftRandom = (float)(seed >>> 49 & 255L) / 255.0F;
            double angle = (Math.PI * 2D) * ((double)i / (double)lobes) + (double)angleRandom * 0.46;
            float travel = blastRadius * (0.48F + distanceRandom * 0.53F);
            float trailing = effect.radius * (0.18F + (1.0F - progress) * 0.34F);
            float wobble = (float)Math.sin((double)(time * 7.0F + (float)i * 1.71F)) * trailing;
            float radial = Math.max(0.0F, travel + wobble);
            double px = Math.cos(angle) * (double)radial;
            double pz = Math.sin(angle) * (double)radial;
            double py = 0.1 + (double)(height * (0.13F + liftRandom * 0.62F)) + Math.sin((double)(time * 5.0F + (float)i)) * (double)effect.radius * (double)0.09F;
            float size = effect.radius * (0.42F + sizeRandom * 0.5F) * (0.72F + (1.0F - progress) * 0.54F);
            float lobeAlpha = alpha * (0.44F + sizeRandom * 0.38F);
            this.drawFireSphere(px, py, pz, size, lobeAlpha, time, (float)i * 0.61F);
            float trailRadius = Math.max(0.0F, radial - size * 0.72F);
            this.drawFireSphere(Math.cos(angle) * (double)trailRadius, py * 0.8, Math.sin(angle) * (double)trailRadius, size * 0.68F, lobeAlpha * 0.62F, time, (float)i * 0.61F + 2.0F);
         }

      }
   }

   private void drawFireSphere(double cx, double cy, double cz, float radius, float alpha, float time, float phase) {
      if (!(radius <= 0.01F) && !(alpha <= 0.001F)) {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         int slices = 12;
         int bands = 7;
         buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

         for(int band = 0; band < bands; ++band) {
            double phi0 = (-Math.PI / 2D) + Math.PI * (double)band / (double)bands;
            double phi1 = (-Math.PI / 2D) + Math.PI * (double)(band + 1) / (double)bands;

            for(int slice = 0; slice < slices; ++slice) {
               double a0 = (Math.PI * 2D) * (double)slice / (double)slices;
               double a1 = (Math.PI * 2D) * (double)(slice + 1) / (double)slices;
               this.putFireSphereVertex(buffer, cx, cy, cz, a0, phi0, radius, alpha, time, phase);
               this.putFireSphereVertex(buffer, cx, cy, cz, a1, phi0, radius, alpha, time, phase);
               this.putFireSphereVertex(buffer, cx, cy, cz, a1, phi1, radius, alpha, time, phase);
               this.putFireSphereVertex(buffer, cx, cy, cz, a0, phi1, radius, alpha, time, phase);
            }
         }

         tessellator.draw();
      }
   }

   private void putFireSphereVertex(BufferBuilder buffer, double cx, double cy, double cz, double angle, double phi, float radius, float alpha, float time, float phase) {
      float noise = 1.0F + (float)Math.sin(angle * (double)4.0F + phi * (double)5.0F - (double)time * (double)9.0F + (double)phase) * 0.13F;
      float r = radius * noise;
      double horizontal = Math.cos(phi);
      double px = cx + Math.cos(angle) * horizontal * (double)r;
      double py = cy + Math.sin(phi) * (double)r;
      double pz = cz + Math.sin(angle) * horizontal * (double)r;
      float vertical = (float)((Math.sin(phi) + (double)1.0F) * (double)0.5F);
      float green = 0.16F + (1.0F - vertical) * 0.48F;
      float vertexAlpha = alpha * (0.18F + (1.0F - vertical) * 0.28F);
      buffer.pos(px, py, pz).color(1.0F, green, 0.015F, vertexAlpha).endVertex();
   }

   private void drawIgnitionFlash(float baseRadius, float ignition) {
      if (!(ignition <= 0.001F)) {
         float radius = baseRadius * (0.35F + (1.0F - ignition) * 1.15F);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         int segments = 56;
         buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
         buffer.pos((double)0.0F, 0.1, (double)0.0F).color(1.0F, 1.0F, 0.82F, ignition * 0.95F).endVertex();

         for(int i = 0; i <= segments; ++i) {
            double angle = (Math.PI * 2D) * (double)i / (double)segments;
            buffer.pos(Math.cos(angle) * (double)radius, 0.1, Math.sin(angle) * (double)radius).color(1.0F, 0.34F, 0.02F, 0.0F).endVertex();
         }

         tessellator.draw();
      }
   }

   private void drawExplosionShell(float radius, float height, float time, float alpha, float progress) {
      if (!(radius <= 0.01F) && !(alpha <= 0.001F)) {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         int slices = 56;
         int bands = 10;
         buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

         for(int band = 0; band < bands; ++band) {
            float v0 = (float)band / (float)bands;
            float v1 = (float)(band + 1) / (float)bands;
            double phi0 = (double)v0 * Math.PI * (double)0.5F;
            double phi1 = (double)v1 * Math.PI * (double)0.5F;

            for(int slice = 0; slice < slices; ++slice) {
               double a0 = (Math.PI * 2D) * (double)slice / (double)slices;
               double a1 = (Math.PI * 2D) * (double)(slice + 1) / (double)slices;
               this.putExplosionVertex(buffer, a0, phi0, radius, height, time, alpha, progress);
               this.putExplosionVertex(buffer, a1, phi0, radius, height, time, alpha, progress);
               this.putExplosionVertex(buffer, a1, phi1, radius, height, time, alpha, progress);
               this.putExplosionVertex(buffer, a0, phi1, radius, height, time, alpha, progress);
            }
         }

         tessellator.draw();
      }
   }

   private void putExplosionVertex(BufferBuilder buffer, double angle, double phi, float radius, float height, float time, float alpha, float progress) {
      float vertical = (float)Math.sin(phi);
      float horizontal = (float)Math.cos(phi);
      float turbulence = (float)Math.sin(angle * (double)5.0F + phi * (double)8.0F - (double)time * (double)10.0F) * radius * 0.045F;
      float r = radius * horizontal + turbulence;
      double px = Math.cos(angle) * (double)r;
      double pz = Math.sin(angle) * (double)r;
      double py = 0.08 + (double)(vertical * height) + Math.sin(angle * (double)3.0F + (double)time * (double)7.0F) * 0.08 * (double)vertical;
      float hot = 1.0F - vertical * 0.55F;
      float green = 0.12F + hot * 0.52F;
      float shellFade = 0.12F + (1.0F - progress) * 0.3F;
      buffer.pos(px, py, pz).color(1.0F, green, 0.015F, alpha * shellFade).endVertex();
   }

   private void drawGroundBlast(float radius, float alpha) {
      if (!(radius <= 0.01F) && !(alpha <= 0.001F)) {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         int segments = 72;
         buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
         buffer.pos((double)0.0F, 0.025, (double)0.0F).color(1.0F, 0.58F, 0.1F, alpha * 0.26F).endVertex();

         for(int i = 0; i <= segments; ++i) {
            double angle = (Math.PI * 2D) * (double)i / (double)segments;
            buffer.pos(Math.cos(angle) * (double)radius, 0.025, Math.sin(angle) * (double)radius).color(1.0F, 0.08F, 0.0F, 0.0F).endVertex();
         }

         tessellator.draw();
      }
   }

   private void drawRadialFlameStreaks(FurnaceEffect effect, float radius, float height, float progress, float alpha) {
      if (!(progress <= 0.001F) && !(alpha <= 0.001F)) {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         buffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
         int streaks = Math.max(18, effect.embers / 2);

         for(int i = 0; i < streaks; ++i) {
            long value = mixSeed(effect.seed ^ (long)i * -7046029254386353131L);
            float random = (float)(value >>> 16 & 65535L) / 65535.0F;
            float lift = (float)(value >>> 34 & 65535L) / 65535.0F;
            double angle = (Math.PI * 2D) * (double)random + (double)i * 0.37;
            float endRadius = radius * (0.72F + random * 0.43F);
            float startRadius = endRadius * (0.36F + progress * 0.3F);
            double y0 = 0.12 + (double)(lift * height) * 0.18;
            double y1 = 0.2 + (double)(lift * height) * 0.58;
            buffer.pos(Math.cos(angle) * (double)startRadius, y0, Math.sin(angle) * (double)startRadius).color(1.0F, 0.82F, 0.26F, alpha * 0.72F).endVertex();
            buffer.pos(Math.cos(angle) * (double)endRadius, y1, Math.sin(angle) * (double)endRadius).color(1.0F, 0.1F, 0.0F, 0.0F).endVertex();
         }

         tessellator.draw();
      }
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float smoothStep(float edge0, float edge1, float value) {
      if (edge0 == edge1) {
         return value < edge0 ? 0.0F : 1.0F;
      } else {
         float t = clamp01((value - edge0) / (edge1 - edge0));
         return t * t * (3.0F - 2.0F * t);
      }
   }

   private void drawFlameColumn(float radius, float height, float time, float alpha) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      int segments = 48;
      int layers = 14;
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);

      for(int layer = 0; layer < layers; ++layer) {
         float v0 = (float)layer / (float)layers;
         float v1 = (float)(layer + 1) / (float)layers;

         for(int i = 0; i < segments; ++i) {
            double a0 = (Math.PI * 2D) * (double)i / (double)segments;
            double a1 = (Math.PI * 2D) * (double)(i + 1) / (double)segments;
            this.putFlameVertex(buffer, a0, v0, radius, height, time, alpha);
            this.putFlameVertex(buffer, a1, v0, radius, height, time, alpha);
            this.putFlameVertex(buffer, a1, v1, radius, height, time, alpha);
            this.putFlameVertex(buffer, a0, v1, radius, height, time, alpha);
         }
      }

      tessellator.draw();
      buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos((double)0.0F, 0.03, (double)0.0F).color(1.0F, 0.88F, 0.3F, alpha * 0.72F).endVertex();

      for(int i = 0; i <= segments; ++i) {
         double angle = (Math.PI * 2D) * (double)i / (double)segments;
         buffer.pos(Math.cos(angle) * (double)radius * 0.62, 0.03, Math.sin(angle) * (double)radius * 0.62).color(1.0F, 0.12F, 0.01F, 0.0F).endVertex();
      }

      tessellator.draw();
   }

   private void putFlameVertex(BufferBuilder buffer, double angle, float v, float radius, float height, float time, float alpha) {
      double py = (double)(v * height);
      float taper = (float)Math.pow((double)(1.0F - v), (double)0.62F);
      float layerRadius = radius * (0.22F + 0.78F * taper);
      float wave = (float)Math.sin((double)(time * 5.5F + v * 11.0F)) * radius * 0.1F * v;
      double wobble = Math.sin(angle * (double)3.0F + (double)time * (double)7.0F + (double)v * (double)8.0F) * (double)radius * 0.08 * (double)v;
      double r = (double)layerRadius + wobble;
      double px = Math.cos(angle) * r + Math.cos((double)(time * 3.2F + v * 6.0F)) * (double)wave;
      double pz = Math.sin(angle) * r + Math.sin((double)(time * 3.2F + v * 6.0F)) * (double)wave;
      float green = 0.12F + (1.0F - v) * 0.42F;
      float a = alpha * (0.1F + (1.0F - v) * 0.28F);
      buffer.pos(px, py, pz).color(1.0F, green, 0.015F, a).endVertex();
   }

   private void drawHeatRing(float radius, float y, float alpha, int segments) {
      if (!(alpha <= 0.001F)) {
         GL11.glLineWidth(2.0F);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         buffer.begin(2, DefaultVertexFormats.POSITION_COLOR);

         for(int i = 0; i < segments; ++i) {
            double angle = (Math.PI * 2D) * (double)i / (double)segments;
            buffer.pos(Math.cos(angle) * (double)radius, (double)y, Math.sin(angle) * (double)radius).color(1.0F, 0.24F, 0.02F, alpha * 0.8F).endVertex();
         }

         tessellator.draw();
         GL11.glLineWidth(1.0F);
      }
   }

   private void drawEmbers(FurnaceEffect effect, float time, float progress, float alpha) {
      GL11.glPointSize(3.5F);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(0, DefaultVertexFormats.POSITION_COLOR);

      for(int i = 0; i < effect.embers; ++i) {
         long value = mixSeed(effect.seed + (long)i * -7046029254386353131L);
         float phase = (float)(value >>> 8 & 65535L) / 65535.0F;
         float radial = 0.18F + (float)(value >>> 24 & 65535L) / 65535.0F * 0.92F;
         float speed = 0.35F + (float)(value >>> 40 & 255L) / 255.0F * 0.85F;
         float life = (phase + progress * speed) % 1.0F;
         double angle = (double)phase * Math.PI * (double)2.0F + (double)time * (1.8 + (double)radial * 2.2) + (double)i * 0.73;
         double r = (double)(effect.radius * radial) * (0.35 + (double)life * 0.95);
         double px = Math.cos(angle) * r;
         double pz = Math.sin(angle) * r;
         double py = 0.15 + (double)(life * effect.height) * 1.06;
         float emberAlpha = alpha * (1.0F - life) * 0.95F;
         buffer.pos(px, py, pz).color(1.0F, 0.28F + (1.0F - life) * 0.58F, 0.03F, emberAlpha).endVertex();
      }

      tessellator.draw();
      GL11.glPointSize(1.0F);
   }

   private static long mixSeed(long value) {
      value ^= value >>> 33;
      value *= -49064778989728563L;
      value ^= value >>> 33;
      value *= -4265267296055464877L;
      return value ^ value >>> 33;
   }

   private static double distanceSq(LogoutSpot spot, double x, double y, double z) {
      double dx = spot.getX() - x;
      double dy = spot.getY() - y;
      double dz = spot.getZ() - z;
      return dx * dx + dy * dy + dz * dz;
   }

   private void renderGhost(LogoutSpot spot, float partialTicks, float alpha) {
      EntityPlayer player = spot.getEntity();
      if (player != null && !(alpha <= 0.001F)) {
         player.hurtTime = 0;
         player.deathTime = 0;
         double x = spot.getX() - mc.getRenderManager().viewerPosX;
         double y = spot.getY() - mc.getRenderManager().viewerPosY;
         double z = spot.getZ() - mc.getRenderManager().viewerPosZ;
         GlStateManager.pushMatrix();
         GlStateManager.enableTexture2D();
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.disableLighting();
         GlStateManager.enableDepth();
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.003921569F);
         OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
         GhostRenderState.capeAlpha = alpha;

         try {
            GL11.glColorMask(false, false, false, false);
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            mc.getRenderManager().renderEntity(player, x, y, z, player.rotationYaw, partialTicks, false);
            GL11.glColorMask(true, true, true, true);
            GlStateManager.depthMask(false);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            mc.getRenderManager().renderEntity(player, x, y, z, player.rotationYaw, partialTicks, false);
         } finally {
            GhostRenderState.capeAlpha = 1.0F;
            GL11.glColorMask(true, true, true, true);
         }

         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         GlStateManager.depthMask(true);
         GlStateManager.enableLighting();
         GlStateManager.enableDepth();
         GlStateManager.enableTexture2D();
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.1F);
         GlStateManager.disableBlend();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.resetColor();
         GlStateManager.popMatrix();
      }
   }

   private void playEffectSound(EntityPlayer player) {
      for(int i = 0; i < (Integer)this.numberSound.getValue(); ++i) {
         switch ((Mode)this.mode.getValue()) {
            case Lightning:
               this.playLightningSound(player);
               break;
            case BlackFlash:
               this.playBlackFlashSound(player);
               break;
            case Ghost:
               mc.world.playSound(mc.player, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_DEATH, player.getSoundCategory(), 0.55F, 1.0F);
               break;
            case FurnaceTest:
               mc.world.playSound(mc.player, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_BLAZE_SHOOT, player.getSoundCategory(), 0.9F, 0.72F);
               mc.world.playSound(mc.player, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_FIRE_AMBIENT, player.getSoundCategory(), 1.0F, 0.58F);
         }
      }

   }

   private void playLightningSound(EntityPlayer player) {
      mc.world.playSound(mc.player, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_LIGHTNING_THUNDER, player.getSoundCategory(), 0.6F, 1.0F);
   }

   private void playBlackFlashSound(EntityPlayer player) {
      SoundUtil.playBlackFlash();
   }

   private void updateLightningRenderer() {
      if (mc.getRenderManager() != null) {
         if (this.mode.getValue() == KillEffects.Mode.Lightning) {
            this.applyTintedRenderer();
         } else {
            this.restoreVanillaRenderer();
         }

      }
   }

   private void applyTintedRenderer() {
      RenderManager renderManager = mc.getRenderManager();
      if (renderManager != null) {
         if (cachedManager != renderManager) {
            cachedManager = renderManager;
            tintedRenderer = null;
            vanillaRenderer = null;
         }

         Render<EntityLightningBolt> currentRenderer = (Render)renderManager.entityRenderMap.get(EntityLightningBolt.class);
         if (!(currentRenderer instanceof TintedLightningRenderer)) {
            if (currentRenderer instanceof RenderLightningBolt) {
               vanillaRenderer = currentRenderer;
            }

            if (tintedRenderer == null) {
               tintedRenderer = new TintedLightningRenderer(renderManager, () -> {
                  Color color = ColorUtil.getResolvedColor(this.lightningColor);
                  return new float[]{(float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F};
               });
            }

            renderManager.entityRenderMap.put(EntityLightningBolt.class, tintedRenderer);
         }
      }
   }

   private void restoreVanillaRenderer() {
      RenderManager renderManager = mc.getRenderManager();
      if (renderManager != null) {
         Render<EntityLightningBolt> currentRenderer = (Render)renderManager.entityRenderMap.get(EntityLightningBolt.class);
         if (currentRenderer instanceof TintedLightningRenderer && vanillaRenderer != null) {
            renderManager.entityRenderMap.put(EntityLightningBolt.class, vanillaRenderer);
         }

      }
   }

   public static enum Mode {
      Lightning,
      BlackFlash,
      Ghost,
      FurnaceTest;
   }

   private static final class FurnaceEffect {
      private final double x;
      private final double y;
      private final double z;
      private final long startTime;
      private final int durationMs;
      private final float radius;
      private final float height;
      private final int embers;
      private final long seed;

      private FurnaceEffect(double x, double y, double z, long startTime, int durationMs, float radius, float height, int embers, long seed) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.startTime = startTime;
         this.durationMs = durationMs;
         this.radius = radius;
         this.height = height;
         this.embers = embers;
         this.seed = seed;
      }
   }

   private static final class GhostEffect {
      private final LogoutSpot spot;
      private final long startTime;
      private final int durationMs;
      private final float startAlpha;

      private GhostEffect(LogoutSpot spot, long startTime, int durationMs, float startAlpha) {
         this.spot = spot;
         this.startTime = startTime;
         this.durationMs = durationMs;
         this.startAlpha = startAlpha;
      }
   }
}
