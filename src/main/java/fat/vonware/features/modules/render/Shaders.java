package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render2DEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.mixin.mixins.accessor.IEntityRenderer;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.shader.CustomShader;
import fat.vonware.util.shader.FrameBufferShader;
import fat.vonware.util.shader.Shader;
import java.awt.Color;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.GlStateManager.CullFace;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.item.EntityExpBottle;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL20;

public class Shaders extends Module {
   private final Setting<Boolean> entities = new Setting<Boolean>("Entity", true);
   private final Setting<Boolean> players = new Setting<Boolean>("Players", true);
   private final Setting<Boolean> crystals = new Setting<Boolean>("Crystals", true);
   private final Setting<Boolean> items = new Setting<Boolean>("Items", false);
   private final Setting<Boolean> pearls = new Setting<Boolean>("Pearls", false);
   private final Setting<Boolean> xp = new Setting<Boolean>("XP", false);
   private final Setting<Boolean> hand = new Setting<Boolean>("Hand", true);
   private final Setting<Boolean> occlusion = new Setting<Boolean>("Occlusion", false);
   private final Setting<Boolean> useRange = new Setting<Boolean>("Optimize Render", false);
   private final Setting<Float> range = new Setting<Float>("Render Range", 50.0F, 0.0F, 200.0F, (v) -> (Boolean)this.useRange.getValue());
   private final Setting<Float> outlineWidth = new Setting<Float>("LineWidth", 1.0F, 0.0F, 7.0F);
   private final Setting<Integer> insideAlpha = new Setting<Integer>("InsideAlpha", 45, 0, 255);
   private final Setting<Boolean> glow = new Setting<Boolean>("Glow", false);
   private final Setting<Mode> shaderMode;
   private final Setting<ShaderTypeNew> insideMode;
   private final Setting<ShaderTypeNew> outsideMode;
   private final Setting<ShaderTypeOld> oldMode;
   private final Setting<Boolean> rainbow;
   private final Setting<RainbowType> rainbowType;
   private final Setting<Float> rainbowSpread;
   private final Setting<Boolean> gradient;
   private final ColorSetting gradientColor1;
   private final ColorSetting gradientColor2;
   private final Setting<Float> gradientSpread;
   private final Setting<Float> gradientSpeed;
   private final Setting<Boolean> bloom;
   private final Setting<Float> bloomIntensity;
   private final Setting<Float> bloomRadius;

   public boolean isOcclusionEnabled() {
      return (Boolean)this.occlusion.getValue();
   }

   public Shaders() {
      super("ShaderChams", "Draws beautiful ShaderChams", Module.Category.RENDER, true, false, false);
      this.shaderMode = new Setting<Mode>("Mode", Shaders.Mode.New);
      this.insideMode = new Setting<ShaderTypeNew>("Inside", Shaders.ShaderTypeNew.Color, (v) -> this.shaderMode.getValue() == Shaders.Mode.New);
      this.outsideMode = new Setting<ShaderTypeNew>("Outside", Shaders.ShaderTypeNew.Color, (v) -> this.shaderMode.getValue() == Shaders.Mode.New);
      this.oldMode = new Setting<ShaderTypeOld>("OldMode", Shaders.ShaderTypeOld.Color, (v) -> this.shaderMode.getValue() == Shaders.Mode.Old);
      this.rainbow = new Setting<Boolean>("Rainbow", false);
      this.rainbowType = new Setting<RainbowType>("RainbowType", Shaders.RainbowType.Normal, (v) -> (Boolean)this.rainbow.getValue());
      this.rainbowSpread = new Setting<Float>("RainbowSpread", 1.0F, 0.1F, 5.0F, (v) -> (Boolean)this.rainbow.getValue() && this.rainbowType.getValue() == Shaders.RainbowType.Gradient);
      this.gradient = new Setting<Boolean>("Gradient", false);
      this.gradientColor1 = new ColorSetting("GradColor1", 16711935, false, (v) -> (Boolean)this.gradient.getValue());
      this.gradientColor2 = new ColorSetting("GradColor2", 65535, false, (v) -> (Boolean)this.gradient.getValue());
      this.gradientSpread = new Setting<Float>("GradSpread", 2.0F, 0.1F, 5.0F, (v) -> (Boolean)this.gradient.getValue());
      this.gradientSpeed = new Setting<Float>("GradSpeed", 0.35F, 0.0F, 3.0F, (v) -> (Boolean)this.gradient.getValue());
      this.bloom = new Setting<Boolean>("Bloom", true);
      this.bloomIntensity = new Setting<Float>("BloomIntensity", 0.8F, 0.1F, 5.0F, (v) -> (Boolean)this.bloom.getValue());
      this.bloomRadius = new Setting<Float>("BloomRadius", 25.0F, 2.0F, 25.0F, (v) -> (Boolean)this.bloom.getValue());
      this.register(this.entities);
      this.register(this.players);
      this.register(this.crystals);
      this.register(this.items);
      this.register(this.pearls);
      this.register(this.xp);
      this.register(this.hand);
      this.register(this.occlusion);
      this.register(this.useRange);
      this.register(this.range);
      this.register(this.outlineWidth);
      this.register(this.insideAlpha);
      this.register(this.glow);
      this.register(this.bloom);
      this.register(this.bloomIntensity);
      this.register(this.bloomRadius);
      this.register(this.shaderMode);
      this.register(this.insideMode);
      this.register(this.outsideMode);
      this.register(this.oldMode);
      this.register(this.rainbow);
      this.register(this.rainbowType);
      this.register(this.rainbowSpread);
      this.register(this.gradient);
      this.register(this.gradientColor1);
      this.register(this.gradientColor2);
      this.register(this.gradientSpread);
      this.register(this.gradientSpeed);
   }

   private void setupRainbowUniforms(Shader shader) {
      if ((Boolean)this.rainbow.getValue()) {
         Colors colors = Colors.getInstance();
         shader.setRainbowEnabled(true);
         shader.setRainbowType(this.rainbowType.getValue() == Shaders.RainbowType.Normal ? 0 : 1);
         shader.setRainbowSpeed((Float)colors.rainbowSpeed.getValue());
         shader.setRainbowSpread((Float)this.rainbowSpread.getValue());
         shader.setRainbowSaturation((float)(Integer)colors.rainbowSaturation.getValue() / 100.0F);
         shader.setRainbowLightness((float)(Integer)colors.rainbowLightness.getValue() / 100.0F);
         shader.setRainbowHueOffset((Float)colors.rainbowHue.getValue());
      } else {
         shader.setRainbowEnabled(false);
      }

   }

   private void setupGradientUniforms(Shader shader) {
      if ((Boolean)this.gradient.getValue()) {
         Color c1 = ColorUtil.getResolvedColor(this.gradientColor1);
         Color c2 = ColorUtil.getResolvedColor(this.gradientColor2);
         shader.setGradientEnabled(true);
         shader.setGradientColor1((float)c1.getRed() / 255.0F, (float)c1.getGreen() / 255.0F, (float)c1.getBlue() / 255.0F, (float)c1.getAlpha() / 255.0F);
         shader.setGradientColor2((float)c2.getRed() / 255.0F, (float)c2.getGreen() / 255.0F, (float)c2.getBlue() / 255.0F, (float)c2.getAlpha() / 255.0F);
         shader.setGradientSpread((Float)this.gradientSpread.getValue());
         shader.setGradientSpeed((Float)this.gradientSpeed.getValue());
      } else {
         shader.setGradientEnabled(false);
      }

   }

   private void setupBloomUniforms(Shader shader) {
      if ((Boolean)this.bloom.getValue()) {
         shader.setBloomEnabled(true);
         shader.setBloomIntensity((Float)this.bloomIntensity.getValue());
         shader.setBloomRadius((Float)this.bloomRadius.getValue());
         shader.setBloomQuality(2);
      } else {
         shader.setBloomEnabled(false);
      }

   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (mc.player == null || !mc.player.isInsideOfMaterial(Material.WATER)) {
         if ((Boolean)this.hand.getValue()) {
            FrameBufferShader shader;
            if (this.shaderMode.getValue() == Shaders.Mode.New) {
               shader = Shader.INSTANCE;
               this.setupRainbowUniforms(Shader.INSTANCE);
               this.setupBloomUniforms(Shader.INSTANCE);
               this.setupGradientUniforms(Shader.INSTANCE);
            } else {
               shader = (this.oldMode.getValue()).shader;
            }

            GlStateManager.matrixMode(5889);
            GlStateManager.pushMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.pushMatrix();
            shader.startDraw(event.getPartialTicks());
            ((IEntityRenderer)mc.entityRenderer).invokeRenderHand(event.getPartialTicks(), 1);
            Color insideColor = new Color(Global.getGlobalColor().getRed(), Global.getGlobalColor().getGreen(), Global.getGlobalColor().getBlue(), (Integer)this.insideAlpha.getValue());
            Color outlineColor = new Color(Global.getGlobalColor().getRed(), Global.getGlobalColor().getGreen(), Global.getGlobalColor().getBlue());
            shader.stopDraw(insideColor, outlineColor, (Float)this.outlineWidth.getValue(), 1.0F, (this.insideMode.getValue()).mode, (this.outsideMode.getValue()).mode, (Boolean)this.glow.getValue());
            GL20.glUseProgram(0);
            mc.getFramebuffer().bindFramebuffer(true);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit + 2);
            GlStateManager.bindTexture(0);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(5888);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.bindTexture(0);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(5888);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.bindTexture(0);
            GlStateManager.matrixMode(5890);
            GlStateManager.loadIdentity();
            GlStateManager.matrixMode(5888);
            GlStateManager.matrixMode(5889);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(515);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            GlStateManager.enableTexture2D();
            GlStateManager.disableLighting();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.resetColor();
            mc.entityRenderer.setupOverlayRendering();
         }

      }
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if (mc.player == null || !mc.player.isInsideOfMaterial(Material.WATER)) {
         if (mc != null && mc.world != null && mc.getRenderViewEntity() != null && event != null) {
            if ((Boolean)this.entities.getValue()) {
               FrameBufferShader shader;
               if (this.shaderMode.getValue() == Shaders.Mode.New) {
                  shader = Shader.INSTANCE;
                  this.setupRainbowUniforms(Shader.INSTANCE);
                  this.setupBloomUniforms(Shader.INSTANCE);
                  this.setupGradientUniforms(Shader.INSTANCE);
               } else {
                  shader = (this.oldMode.getValue()).shader;
               }

               GlStateManager.matrixMode(5889);
               GlStateManager.pushMatrix();
               GlStateManager.matrixMode(5888);
               GlStateManager.pushMatrix();

               try {
                  Entity[] entityList = (Entity[])mc.world.loadedEntityList.toArray(new Entity[0]);
                  if ((Boolean)this.occlusion.getValue()) {
                     this.renderOccludedBatch(shader, entityList, event.getPartialTicks(), false);
                     if ((Boolean)this.players.getValue()) {
                        this.renderOccludedBatch(shader, entityList, event.getPartialTicks(), true);
                     }
                  } else {
                     this.renderNormalBatch(shader, entityList, event.getPartialTicks(), false);
                     if ((Boolean)this.players.getValue()) {
                        this.renderNormalBatch(shader, entityList, event.getPartialTicks(), true);
                     }
                  }
               } finally {
                  GlStateManager.alphaFunc(516, 0.1F);
                  GlStateManager.matrixMode(5889);
                  GlStateManager.popMatrix();
                  GlStateManager.matrixMode(5888);
                  GlStateManager.popMatrix();
               }

            }
         }
      }
   }

   private void renderOccludedBatch(FrameBufferShader shader, Entity[] entityList, float partialTicks, boolean friendPass) {
      shader.beginOcclusionBatch();
      boolean renderedAnyLayer = false;
      renderedAnyLayer |= this.renderOcclusionLayer(shader, entityList, partialTicks, friendPass, Shaders.OcclusionLayer.CRYSTALS);
      renderedAnyLayer |= this.renderOcclusionLayer(shader, entityList, partialTicks, friendPass, Shaders.OcclusionLayer.PLAYERS);
      renderedAnyLayer |= this.renderOcclusionLayer(shader, entityList, partialTicks, friendPass, Shaders.OcclusionLayer.OTHER);
      if (!renderedAnyLayer) {
         shader.cancelDraw();
      } else {
         shader.endOcclusionBatch();
         this.finishShaderPass(shader, friendPass);
      }
   }

   private boolean renderOcclusionLayer(FrameBufferShader shader, Entity[] entityList, float partialTicks, boolean friendPass, OcclusionLayer layer) {
      shader.startOcclusionLayer(partialTicks);
      boolean rendered = false;
      boolean playerLayer = layer == Shaders.OcclusionLayer.PLAYERS;
      if (playerLayer) {
         shader.beginPlayerOcclusionCapture();
         shader.useTerrainDepthForCurrentLayer();
      }

      boolean var15;
      try {
         for(Entity entity : entityList) {
            if (this.shouldRenderEntity(entity, friendPass) && layer.matches(entity)) {
               shader.prepareHiddenFragmentPass();
               rendered |= this.renderEntityToCapture(entity, partialTicks);
            }
         }

         if (rendered) {
            for(Entity entity : entityList) {
               if (this.shouldRenderEntity(entity, friendPass) && layer.matches(entity)) {
                  shader.beginVisibleFragmentErase(playerLayer);
                  this.renderEntityToCapture(entity, partialTicks);
               }
            }

            shader.endVisibleFragmentErase();
            shader.mergeOcclusionLayer();
            var15 = true;
            return var15;
         }

         var15 = false;
      } finally {
         if (playerLayer) {
            shader.endPlayerOcclusionCapture();
         }

      }

      return var15;
   }

   private void renderNormalBatch(FrameBufferShader shader, Entity[] entityList, float partialTicks, boolean friendPass) {
      shader.startDraw(partialTicks, false);
      boolean rendered = false;

      for(Entity entity : entityList) {
         if (this.shouldRenderEntity(entity, friendPass)) {
            rendered |= this.renderEntityToCapture(entity, partialTicks);
         }
      }

      if (!rendered) {
         shader.cancelDraw();
      } else {
         this.finishShaderPass(shader, friendPass);
      }
   }

   private void finishShaderPass(FrameBufferShader shader, boolean friendPass) {
      Color base = friendPass ? Colors.getInstance().getFriendColor() : Global.getGlobalColor();
      Color insideColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), (Integer)this.insideAlpha.getValue());
      Color outlineColor = new Color(base.getRed(), base.getGreen(), base.getBlue());
      shader.stopDraw(insideColor, outlineColor, (Float)this.outlineWidth.getValue(), 1.0F, (this.insideMode.getValue()).mode, (this.outsideMode.getValue()).mode, (Boolean)this.glow.getValue());
   }

   private boolean shouldRenderEntity(Entity entity, boolean friendPass) {
      if (entity != null && !entity.isDead && entity.addedToChunk) {
         if (entity == mc.getRenderViewEntity()) {
            return false;
         } else if ((Boolean)this.useRange.getValue() && mc.getRenderViewEntity().getDistance(entity) >= (Float)this.range.getValue()) {
            return false;
         } else {
            boolean friend = entity instanceof EntityPlayer && Vonware.friendManager.isFriend((EntityPlayer)entity);
            if (friendPass != friend) {
               return false;
            } else {
               return entity instanceof EntityPlayer && (Boolean)this.players.getValue() || entity instanceof EntityEnderCrystal && (Boolean)this.crystals.getValue() || entity instanceof EntityExpBottle && (Boolean)this.xp.getValue() || entity instanceof EntityXPOrb && (Boolean)this.xp.getValue() || entity instanceof EntityEnderPearl && (Boolean)this.pearls.getValue() || entity instanceof EntityItem && (Boolean)this.items.getValue();
            }
         }
      } else {
         return false;
      }
   }

   private boolean renderEntityToCapture(Entity entity, float partialTicks) {
      Render renderer = mc.getRenderManager().getEntityRenderObject(entity);
      if (renderer == null) {
         return false;
      } else {
         Vec3d pos = EntityUtil.getInterpolatedRenderPos(entity, partialTicks);
         if (pos == null) {
            return false;
         } else {
            try {
               GlStateManager.enableDepth();
               GlStateManager.depthMask(false);
               GlStateManager.enableCull();
               GlStateManager.cullFace(CullFace.BACK);
               GlStateManager.enableTexture2D();
               GlStateManager.enableAlpha();
               GlStateManager.alphaFunc(516, 0.003921569F);
               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               renderer.doRender(entity, pos.x, pos.y, pos.z, entity.rotationYaw, partialTicks);
               GlStateManager.bindTexture(0);
               GlStateManager.matrixMode(5890);
               GlStateManager.loadIdentity();
               GlStateManager.matrixMode(5888);
               return true;
            } catch (Exception var6) {
               return false;
            }
         }
      }
   }

   public static enum RainbowType {
      Normal,
      Gradient;
   }

   private static enum OcclusionLayer {
      CRYSTALS {
         boolean matches(Entity entity) {
            return entity instanceof EntityEnderCrystal;
         }
      },
      PLAYERS {
         boolean matches(Entity entity) {
            return entity instanceof EntityPlayer;
         }
      },
      OTHER {
         boolean matches(Entity entity) {
            return !(entity instanceof EntityEnderCrystal) && !(entity instanceof EntityPlayer);
         }
      };

      private OcclusionLayer() {
      }

      abstract boolean matches(Entity var1);
   }

   private static enum Mode {
      Old,
      New;
   }

   public static enum ShaderTypeNew {
      Color(0),
      Matter(1),
      PinkGalaxy(2),
      DeepSpace(3),
      PinkWarp(4),
      Watery(5),
      PurpleGalaxy(6);

      public int mode;

      private ShaderTypeNew(int mode) {
         this.mode = mode;
      }
   }

   public static enum ShaderTypeOld {
      Galaxy(CustomShader.GALAXY),
      Water(CustomShader.WATER),
      Lava(CustomShader.LAVA),
      Smooth1(CustomShader.SMOOTH1),
      Smooth2(CustomShader.SMOOTH2),
      Smooth3(CustomShader.SMOOTH3),
      PinkGalaxy(CustomShader.PINK_GALACTIC),
      Matter(CustomShader.MATTER),
      BARS(CustomShader.BARS),
      CATTYN(CustomShader.CATTYN),
      HACKER(CustomShader.HACKER),
      Color(CustomShader.OUTLINE);

      public CustomShader shader;

      private ShaderTypeOld(CustomShader shader) {
         this.shader = shader;
      }
   }
}
