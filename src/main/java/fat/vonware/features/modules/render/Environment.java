package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.IEntityRenderer;
import fat.vonware.util.Util;
import fat.vonware.util.Animations.game.FallingParticleRenderer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.shader.SkyShaderRenderer;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.FogMode;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Environment extends Module {
   private static final Random RANDOM = new Random();
   private static final ResourceLocation RAIN_TEXTURES = new ResourceLocation("textures/environment/rain.png");
   private static final ResourceLocation SNOW_TEXTURES = new ResourceLocation("textures/environment/snow.png");
   private static final ResourceLocation GLINT_TEXTURE = new ResourceLocation("textures/misc/enchanted_item_glint.png");
   public Setting<Boolean> clientsideTime = new Setting<Boolean>("ClientsideTime", false);
   public Setting<Integer> time = new Setting<Integer>("Time", 1000, 0, 23000, (v) -> (Boolean)this.clientsideTime.getValue());
   public Setting<Boolean> sky = new Setting<Boolean>("Sky", false);
   public Setting<Boolean> skyGlint = new Setting<Boolean>("SkyGlint", false);
   public Setting<Integer> skyGlintColor = new ColorSetting("SkyGlintColor", 11141375, false, (v) -> (Boolean)this.skyGlint.getValue());
   public Setting<Boolean> noSkybox = new Setting<Boolean>("NoSkyBox", false);
   public Setting<Boolean> skyShader = new Setting<Boolean>("SkyShader", false);
   public Setting<Integer> skyShaderColor1 = new ColorSetting("NoiseColor1", 657966, false, (v) -> (Boolean)this.skyShader.getValue());
   public Setting<Integer> skyShaderColor2 = new ColorSetting("NoiseColor2", 7016360, false, (v) -> (Boolean)this.skyShader.getValue());
   public Setting<Float> skyShaderSpeed = (new Setting<Float>("NoiseSpeed", 0.4F, 0.0F, 3.0F, (v) -> (Boolean)this.skyShader.getValue())).setStep(0.05F);
   public Setting<Float> skyShaderScale = (new Setting<Float>("NoiseScale", 2.5F, 0.5F, 8.0F, (v) -> (Boolean)this.skyShader.getValue())).setStep(0.1F);
   public Setting<Integer> skyShaderOctaves = new Setting<Integer>("NoiseOctaves", 5, 1, 8, (v) -> (Boolean)this.skyShader.getValue());
   public Setting<Boolean> skyShaderCameraSync = new Setting<Boolean>("CameraSync", true, (v) -> (Boolean)this.skyShader.getValue());
   private final Setting<Boolean> fog = new Setting<Boolean>("Fog", true);
   private final Setting<Float> fogDensity = (new Setting<Float>("Density", 0.1F, 0.01F, 1.0F, (v) -> (Boolean)this.fog.getValue())).setStep(0.01F);
   public Setting<Boolean> screenTint = new Setting<Boolean>("ScreenTint", false);
   public Setting<Integer> screenTintColor = new ColorSetting("TintColor", 16711680, false, (v) -> (Boolean)this.screenTint.getValue());
   public Setting<Float> screenTintAlpha = (new Setting<Float>("TintAlpha", 0.3F, 0.0F, 1.0F, (v) -> (Boolean)this.screenTint.getValue())).setStep(0.01F);
   public Setting<Boolean> weather = new Setting<Boolean>("WeatherEditor", false);
   private final Setting<Float> strength = new Setting<Float>("Strength", 0.8F, 0.1F, 1.0F, (v) -> (Boolean)this.weather.getValue());
   public Setting<Boolean> snow = new Setting<Boolean>("Snow", true, (v) -> (Boolean)this.weather.getValue());
   public Setting<Boolean> fullBright = new Setting<Boolean>("FullBright", false);
   public Setting<FullBrightMode> fullBrightMode;
   public Setting<Boolean> lightmapBoolean;
   private final ColorSetting customColor;
   private final ColorSetting weatherColorPicker;
   private final ColorSetting lightmapColor;
   public Setting<Boolean> particles;
   public Setting<FallingParticleRenderer.ParticleType> particleType;
   public Setting<Integer> particleCount;
   public Setting<Float> particleDensity;
   public Setting<Float> particleWind;
   public Setting<Float> particleSize;
   public Setting<Float> particleOpacity;
   private final ColorSetting particleColor;
   public static Environment INSTANCE;

   public Environment() {
      super("Environment", "changes world visuals", Module.Category.RENDER, true, false, false);
      this.fullBrightMode = new Setting<FullBrightMode>("FBMode", Environment.FullBrightMode.Gamma, (v) -> (Boolean)this.fullBright.getValue());
      this.lightmapBoolean = new Setting<Boolean>("ColoredLightMap", false, (v) -> (Boolean)this.fullBright.getValue());
      this.customColor = new ColorSetting("Color", 16777215, true);
      this.weatherColorPicker = new ColorSetting("WeatherColor", 6592255, false, (v) -> (Boolean)this.weather.getValue());
      this.lightmapColor = new ColorSetting("LightMapColor", 16777215, true, (v) -> (Boolean)this.lightmapBoolean.getValue() && (Boolean)this.fullBright.getValue());
      this.particles = new Setting<Boolean>("Particles", false);
      this.particleType = new Setting<FallingParticleRenderer.ParticleType>("ParticleType", FallingParticleRenderer.ParticleType.Ambient, (v) -> (Boolean)this.particles.getValue());
      this.particleCount = new Setting<Integer>("ParticleCount", 150, 10, 1000, (v) -> (Boolean)this.particles.getValue());
      this.particleDensity = (new Setting<Float>("ParticleDensity", 1.0F, 0.1F, 5.0F, (v) -> (Boolean)this.particles.getValue())).setStep(0.1F);
      this.particleWind = (new Setting<Float>("ParticleWind", 1.0F, 0.1F, 3.0F, (v) -> (Boolean)this.particles.getValue())).setStep(0.05F);
      this.particleSize = (new Setting<Float>("ParticleSize", 1.0F, 0.25F, 2.5F, (v) -> (Boolean)this.particles.getValue())).setStep(0.05F);
      this.particleOpacity = (new Setting<Float>("ParticleOpacity", 0.85F, 0.1F, 1.0F, (v) -> (Boolean)this.particles.getValue())).setStep(0.05F);
      this.particleColor = new ColorSetting("ParticleColor", 16758725, false, (v) -> (Boolean)this.particles.getValue());
      this.register(this.clientsideTime);
      this.register(this.time);
      this.register(this.sky);
      this.register(this.skyGlint);
      this.register(this.skyGlintColor);
      this.register(this.noSkybox);
      this.register(this.skyShader);
      this.register(this.skyShaderColor1);
      this.register(this.skyShaderColor2);
      this.register(this.skyShaderSpeed);
      this.register(this.skyShaderScale);
      this.register(this.skyShaderOctaves);
      this.register(this.skyShaderCameraSync);
      this.register(this.fog);
      this.register(this.fogDensity);
      this.register(this.customColor);
      this.register(this.screenTint);
      this.register(this.screenTintColor);
      this.register(this.screenTintAlpha);
      this.register(this.weather);
      this.register(this.strength);
      this.register(this.snow);
      this.register(this.weatherColorPicker);
      this.register(this.fullBright);
      this.register(this.fullBrightMode);
      this.register(this.lightmapBoolean);
      this.register(this.lightmapColor);
      this.register(this.particles);
      this.register(this.particleType);
      this.register(this.particleCount);
      this.register(this.particleDensity);
      this.register(this.particleWind);
      this.register(this.particleSize);
      this.register(this.particleOpacity);
      this.register(this.particleColor);
      INSTANCE = this;
   }

   public void onUpdate() {
      if (this.isEnabled() && !fullNullCheck()) {
         if ((Boolean)this.fullBright.getValue()) {
            if (this.fullBrightMode.getValue() == Environment.FullBrightMode.Gamma) {
               mc.gameSettings.gammaSetting = 1000.0F;
            } else {
               mc.gameSettings.gammaSetting = 1.0F;
               PotionEffect nightVision = new PotionEffect(Potion.getPotionById(16), Integer.MAX_VALUE, 0);
               nightVision.setPotionDurationMax(true);
               mc.player.addPotionEffect(nightVision);
            }
         } else if (this.fullBrightMode.getValue() == Environment.FullBrightMode.Gamma) {
            mc.gameSettings.gammaSetting = 1.0F;
         } else {
            mc.player.removePotionEffect(MobEffects.NIGHT_VISION);
         }

         if ((Boolean)this.particles.getValue()) {
            FallingParticleRenderer.INSTANCE.update(this.particleType.getValue(), (Integer)this.particleCount.getValue(), (Float)this.particleDensity.getValue(), (Float)this.particleWind.getValue(), (Float)this.particleSize.getValue());
         } else {
            FallingParticleRenderer.INSTANCE.clear();
         }

      }
   }

   public void onEnable() {
      super.onEnable();
      SkyShaderRenderer.INSTANCE.destroy();
   }

   public void onDisable() {
      super.onDisable();
      SkyShaderRenderer.INSTANCE.destroy();
      FallingParticleRenderer.INSTANCE.clear();
      if (!fullNullCheck()) {
         if (this.fullBrightMode.getValue() == Environment.FullBrightMode.Gamma) {
            mc.gameSettings.gammaSetting = 1.0F;
         } else {
            mc.player.removePotionEffect(MobEffects.NIGHT_VISION);
         }

      }
   }

   @SubscribeEvent
   public void onRenderGameOverlay(RenderGameOverlayEvent.Post event) {
      if (this.isEnabled() && (Boolean)this.screenTint.getValue()) {
         if (event.getType() == ElementType.ALL) {
            int sw = mc.displayWidth;
            int sh = mc.displayHeight;
            float a = (Float)this.screenTintAlpha.getValue();
            GlStateManager.pushMatrix();
            GlStateManager.disableTexture2D();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.disableDepth();
            Colors colors = Colors.getInstance();
            boolean diagonalFlow = ColorPickerConfig.getState(this.screenTintColor).useGlobalColor && (Boolean)colors.rainbow.getValue() && colors.rainbowMode.getValue() == Colors.RainbowMode.Flow;
            if (diagonalFlow) {
               this.renderDiagonalRainbowTint(sw, sh, a);
            } else {
               Color color = this.getScreenTintColor();
               float r = (float)color.getRed() / 255.0F;
               float g = (float)color.getGreen() / 255.0F;
               float b = (float)color.getBlue() / 255.0F;
               Tessellator tess = Tessellator.getInstance();
               BufferBuilder buf = tess.getBuffer();
               buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
               buf.pos((double)0.0F, (double)sh, (double)0.0F).color(r, g, b, a).endVertex();
               buf.pos((double)sw, (double)sh, (double)0.0F).color(r, g, b, a).endVertex();
               buf.pos((double)sw, (double)0.0F, (double)0.0F).color(r, g, b, a).endVertex();
               buf.pos((double)0.0F, (double)0.0F, (double)0.0F).color(r, g, b, a).endVertex();
               tess.draw();
            }

            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
         }
      }
   }

   private void renderDiagonalRainbowTint(int sw, int sh, float alpha) {
      Colors colors = Colors.getInstance();
      float hueOffset = (Float)colors.rainbowHue.getValue();
      int bandWidth = Math.max(20, sw / 16);
      int totalSpan = sw + sh;
      int bandCount = totalSpan / bandWidth + 2;
      GlStateManager.disableCull();
      Tessellator tess = Tessellator.getInstance();
      BufferBuilder buf = tess.getBuffer();

      for(int i = 0; i < bandCount; ++i) {
         float x0 = (float)(-sh + i * bandWidth);
         float x1 = x0 + (float)bandWidth;
         Color bandColor = ColorUtil.rainbow((int)((float)i * hueOffset));
         float r = (float)bandColor.getRed() / 255.0F;
         float g = (float)bandColor.getGreen() / 255.0F;
         float b = (float)bandColor.getBlue() / 255.0F;
         buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
         buf.pos((double)x0, (double)0.0F, (double)0.0F).color(r, g, b, alpha).endVertex();
         buf.pos((double)x1, (double)0.0F, (double)0.0F).color(r, g, b, alpha).endVertex();
         buf.pos((double)(x1 + (float)sh), (double)sh, (double)0.0F).color(r, g, b, alpha).endVertex();
         buf.pos((double)(x0 + (float)sh), (double)sh, (double)0.0F).color(r, g, b, alpha).endVertex();
         tess.draw();
      }

      GlStateManager.enableCull();
   }

   @SubscribeEvent
   public void onRenderWorldLast(RenderWorldLastEvent event) {
      if (this.isEnabled() && (Boolean)this.particles.getValue()) {
         Color color = this.getParticleColor();
         FallingParticleRenderer.INSTANCE.render(event.getPartialTicks(), this.particleType.getValue(), (float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (Float)this.particleOpacity.getValue(), 1.0F);
      }
   }

   private Color getParticleColor() {
      if (ColorPickerConfig.getState(this.particleColor).useGlobalColor) {
         return new Color(Global.getRed(), Global.getGreen(), Global.getBlue());
      } else {
         int c = (Integer)this.particleColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   private Color getScreenTintColor() {
      if (ColorPickerConfig.getState(this.screenTintColor).useGlobalColor) {
         Colors colors = Colors.getInstance();
         return (Boolean)colors.rainbow.getValue() && colors.rainbowMode.getValue() == Colors.RainbowMode.Static ? ColorUtil.rainbow(0) : new Color(Global.getRed(), Global.getGreen(), Global.getBlue());
      } else {
         int c = (Integer)this.screenTintColor.getValue();
         return new Color(c >> 16 & 255, c >> 8 & 255, c & 255);
      }
   }

   public Color getLightmapColor() {
      if (ColorPickerConfig.getState(this.lightmapColor).useGlobalColor) {
         return Global.getGlobalColor();
      } else {
         int colorInt = (Integer)this.lightmapColor.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   @SubscribeEvent
   public void onFogColors(EntityViewRenderEvent.FogColors event) {
      if ((Boolean)this.fog.getValue()) {
         Color color = this.getCustomColor();
         event.setRed((float)color.getRed() / 255.0F);
         event.setGreen((float)color.getGreen() / 255.0F);
         event.setBlue((float)color.getBlue() / 255.0F);
      }
   }

   @SubscribeEvent
   public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
      if ((Boolean)this.fog.getValue()) {
         GlStateManager.setFog(FogMode.EXP);
         event.setDensity((Float)this.fogDensity.getValue() * 0.3F);
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
      if ((Boolean)this.fog.getValue()) {
         float renderDist = (float)mc.gameSettings.renderDistanceChunks * 16.0F;
         float d = (Float)this.fogDensity.getValue();
         float fogEnd = renderDist * (2.0F - d * 1.8F);
         float fogStart = fogEnd * 0.75F;
         GlStateManager.setFogStart(fogStart);
         GlStateManager.setFogEnd(fogEnd);
         event.setCanceled(true);
      }
   }

   private Color getCustomColor() {
      if (ColorPickerConfig.getState(this.customColor).useGlobalColor) {
         return new Color(Global.getRed(), Global.getGreen(), Global.getBlue());
      } else {
         int colorInt = (Integer)this.customColor.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   private Color getWeatherColor() {
      if (ColorPickerConfig.getState(this.weatherColorPicker).useGlobalColor) {
         return new Color(Global.getRed(), Global.getGreen(), Global.getBlue());
      } else {
         int colorInt = (Integer)this.weatherColorPicker.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255);
      }
   }

   public Color getSkyColor() {
      return (Boolean)this.sky.getValue() ? this.getCustomColor() : null;
   }

   public void renderSkyGlint(float partialTicks) {
      if ((Boolean)this.skyGlint.getValue()) {
         if (mc.world.provider.isSurfaceWorld()) {
            Color color = new Color((Integer)this.skyGlintColor.getValue());
            float r = (float)color.getRed() / 255.0F;
            float g = (float)color.getGreen() / 255.0F;
            float b = (float)color.getBlue() / 255.0F;
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();
            mc.getTextureManager().bindTexture(GLINT_TEXTURE);
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(SourceFactor.SRC_COLOR, DestFactor.ONE);
            float time = (float)mc.world.getTotalWorldTime() + partialTicks;
            float radius = 300.0F;

            for(int pass = 0; pass < 2; ++pass) {
               GlStateManager.pushMatrix();
               float rotation = pass == 0 ? 45.0F : -45.0F;
               float speed = pass == 0 ? 7.0E-4F : -9.0E-4F;
               float scroll = time * speed % 1.0F;
               GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);

               for(int i = 0; i < 10; ++i) {
                  float height = (float)i / 10.0F * radius;
                  float alpha = 0.5F * (1.0F - (float)i / 10.0F * 0.7F);
                  buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
                  buffer.pos((double)(-radius), (double)height, (double)(-radius)).tex((double)(0.0F + scroll), (double)0.0F).color(r, g, b, alpha).endVertex();
                  buffer.pos((double)radius, (double)height, (double)(-radius)).tex((double)(1.0F + scroll), (double)0.0F).color(r, g, b, alpha).endVertex();
                  buffer.pos((double)radius, (double)height, (double)radius).tex((double)(1.0F + scroll), (double)1.0F).color(r, g, b, alpha).endVertex();
                  buffer.pos((double)(-radius), (double)height, (double)radius).tex((double)(0.0F + scroll), (double)1.0F).color(r, g, b, alpha).endVertex();
                  tess.draw();
               }

               GlStateManager.popMatrix();
            }

            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
         }
      }
   }

   public void renderWeather(float partialTicks) {
      if ((Boolean)this.weather.getValue()) {
         float f = (Float)this.strength.getValue();
         EntityRenderer renderer = mc.entityRenderer;
         renderer.enableLightmap();
         Entity entity = Util.mc.getRenderViewEntity();
         if (entity != null) {
            World world = Util.mc.world;
            int i = MathHelper.floor(entity.posX);
            int j = MathHelper.floor(entity.posY);
            int k = MathHelper.floor(entity.posZ);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            GlStateManager.disableCull();
            GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.alphaFunc(516, 0.1F);
            double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks;
            double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks;
            double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks;
            int l = MathHelper.floor(d1);
            int i1 = Util.mc.gameSettings.fancyGraphics ? 10 : 5;
            int j1 = -1;
            float f1 = (float)((IEntityRenderer)renderer).getRendererUpdateCount() + partialTicks;
            bufferbuilder.setTranslation(-d0, -d1, -d2);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            Color weatherColor = this.getWeatherColor();
            float weatherR = (float)weatherColor.getRed() / 255.0F;
            float weatherG = (float)weatherColor.getGreen() / 255.0F;
            float weatherB = (float)weatherColor.getBlue() / 255.0F;

            for(int k1 = k - i1; k1 <= k + i1; ++k1) {
               for(int l1 = i - i1; l1 <= i + i1; ++l1) {
                  int i2 = (k1 - k + 16) * 32 + l1 - i + 16;
                  double d3 = (double)((IEntityRenderer)renderer).getRainXCoords()[i2] * (double)0.5F;
                  double d4 = (double)((IEntityRenderer)renderer).getRainYCoords()[i2] * (double)0.5F;
                  blockpos$mutableblockpos.setPos(l1, 0, k1);
                  world.getBiome(blockpos$mutableblockpos);
                  int j2 = 0;
                  int k2 = j - i1;
                  int l2 = j + i1;
                  if (k2 < j2) {
                     k2 = j2;
                  }

                  if (l2 < j2) {
                     l2 = j2;
                  }

                  int i3 = Math.max(j2, l);
                  if (k2 != l2) {
                     RANDOM.setSeed((long)l1 * (long)l1 * 3121L + (long)l1 * 45238971L ^ (long)k1 * (long)k1 * 418711L + (long)k1 * 13761L);
                     blockpos$mutableblockpos.setPos(l1, k2, k1);
                     if (!(Boolean)this.snow.getValue()) {
                        if (j1 != 0) {
                           if (j1 >= 0) {
                              tessellator.draw();
                           }

                           j1 = 0;
                           Util.mc.getTextureManager().bindTexture(RAIN_TEXTURES);
                           bufferbuilder.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                        }

                        double d5 = -((double)(((IEntityRenderer)renderer).getRendererUpdateCount() + l1 * l1 * 3121 + l1 * 45238971 + k1 * k1 * 418711 + k1 * 13761 & 31) + (double)partialTicks) / (double)32.0F * ((double)3.0F + RANDOM.nextDouble());
                        double d6 = (double)((float)l1 + 0.5F) - entity.posX;
                        double d7 = (double)((float)k1 + 0.5F) - entity.posZ;
                        float f3 = MathHelper.sqrt(d6 * d6 + d7 * d7) / (float)i1;
                        float f4 = ((1.0F - f3 * f3) * 0.5F + 0.5F) * f;
                        blockpos$mutableblockpos.setPos(l1, i3, k1);
                        int j3 = world.getCombinedLight(blockpos$mutableblockpos, 0);
                        int k3 = j3 >> 16 & '\uffff';
                        int l3 = j3 & '\uffff';
                        bufferbuilder.pos((double)l1 - d3 + (double)0.5F, (double)l2, (double)k1 - d4 + (double)0.5F).tex((double)0.0F, (double)k2 * (double)0.25F + d5).color(weatherR, weatherG, weatherB, f4).lightmap(k3, l3).endVertex();
                        bufferbuilder.pos((double)l1 + d3 + (double)0.5F, (double)l2, (double)k1 + d4 + (double)0.5F).tex((double)1.0F, (double)k2 * (double)0.25F + d5).color(weatherR, weatherG, weatherB, f4).lightmap(k3, l3).endVertex();
                        bufferbuilder.pos((double)l1 + d3 + (double)0.5F, (double)k2, (double)k1 + d4 + (double)0.5F).tex((double)1.0F, (double)l2 * (double)0.25F + d5).color(weatherR, weatherG, weatherB, f4).lightmap(k3, l3).endVertex();
                        bufferbuilder.pos((double)l1 - d3 + (double)0.5F, (double)k2, (double)k1 - d4 + (double)0.5F).tex((double)0.0F, (double)l2 * (double)0.25F + d5).color(weatherR, weatherG, weatherB, f4).lightmap(k3, l3).endVertex();
                     } else {
                        if (j1 != 1) {
                           if (j1 == 0) {
                              tessellator.draw();
                           }

                           j1 = 1;
                           Util.mc.getTextureManager().bindTexture(SNOW_TEXTURES);
                           bufferbuilder.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
                        }

                        double d8 = (double)(-((float)(((IEntityRenderer)renderer).getRendererUpdateCount() & 511) + partialTicks) / 512.0F);
                        double d9 = RANDOM.nextDouble() + (double)f1 * 0.01 * RANDOM.nextGaussian();
                        double d10 = RANDOM.nextDouble() + (double)(f1 * (float)RANDOM.nextGaussian()) * 0.001;
                        double d11 = (double)((float)l1 + 0.5F) - entity.posX;
                        double d12 = (double)((float)k1 + 0.5F) - entity.posZ;
                        float f6 = MathHelper.sqrt(d11 * d11 + d12 * d12) / (float)i1;
                        float f5 = ((1.0F - f6 * f6) * 0.3F + 0.5F) * f;
                        blockpos$mutableblockpos.setPos(l1, i3, k1);
                        int i4 = (world.getCombinedLight(blockpos$mutableblockpos, 0) * 3 + 15728880) / 4;
                        int j4 = i4 >> 16 & '\uffff';
                        int k4 = i4 & '\uffff';
                        bufferbuilder.pos((double)l1 - d3 + (double)0.5F, (double)l2, (double)k1 - d4 + (double)0.5F).tex((double)0.0F + d9, (double)k2 * (double)0.25F + d8 + d10).color(weatherR, weatherG, weatherB, f5).lightmap(j4, k4).endVertex();
                        bufferbuilder.pos((double)l1 + d3 + (double)0.5F, (double)l2, (double)k1 + d4 + (double)0.5F).tex((double)1.0F + d9, (double)k2 * (double)0.25F + d8 + d10).color(weatherR, weatherG, weatherB, f5).lightmap(j4, k4).endVertex();
                        bufferbuilder.pos((double)l1 + d3 + (double)0.5F, (double)k2, (double)k1 + d4 + (double)0.5F).tex((double)1.0F + d9, (double)l2 * (double)0.25F + d8 + d10).color(weatherR, weatherG, weatherB, f5).lightmap(j4, k4).endVertex();
                        bufferbuilder.pos((double)l1 - d3 + (double)0.5F, (double)k2, (double)k1 - d4 + (double)0.5F).tex((double)0.0F + d9, (double)l2 * (double)0.25F + d8 + d10).color(weatherR, weatherG, weatherB, f5).lightmap(j4, k4).endVertex();
                     }
                  }
               }
            }

            if (j1 >= 0) {
               tessellator.draw();
            }

            bufferbuilder.setTranslation((double)0.0F, (double)0.0F, (double)0.0F);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
            renderer.disableLightmap();
         }
      }
   }

   public static enum FullBrightMode {
      Gamma,
      Potion;
   }
}
