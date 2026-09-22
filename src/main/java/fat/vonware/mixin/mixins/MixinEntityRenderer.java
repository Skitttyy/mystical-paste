package fat.vonware.mixin.mixins;

import com.google.common.base.Predicate;
import fat.vonware.Vonware;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.client.ClickGui;
import fat.vonware.features.modules.client.Optimizer;
import fat.vonware.features.modules.player.Freecam;
import fat.vonware.features.modules.player.NoEntityTrace;
import fat.vonware.features.modules.render.CameraTweaks;
import fat.vonware.features.modules.render.Environment;
import fat.vonware.features.modules.render.NameTags;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.features.modules.render.Shaders;
import fat.vonware.mixin.mixins.accessor.IEntityRenderer;
import fat.vonware.util.shader.FrameBufferShader;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {EntityRenderer.class},
   priority = 10000
)
public abstract class MixinEntityRenderer implements IEntityRenderer {
   public Minecraft mc = Minecraft.getMinecraft();
   @Shadow
   private int[] lightmapColors;
   @Shadow
   private ItemStack itemActivationItem;
   @Shadow
   private ShaderGroup shaderGroup;
   private boolean wasInGui = false;
   private int lastBlurStrength = -1;
   private static Field cachedListShadersField;
   @Shadow
   private boolean lightmapUpdateNeeded;

   @Shadow
   public abstract boolean isShaderActive();

   @Shadow
   protected abstract void renderHand(float var1, int var2);

   private int[] toRGBAArray(int colorBuffer) {
      return new int[]{colorBuffer >> 16 & 255, colorBuffer >> 8 & 255, colorBuffer & 255};
   }

   @Shadow
   protected abstract void setupCameraTransform(float var1, int var2);

   public void setLightmapUpdateNeeded(boolean val) {
      this.lightmapUpdateNeeded = val;
   }

   @Redirect(
      method = {"renderWorldPass"},
      at = @At(
   value = "INVOKE",
   target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
   remap = false
)
   )
   private void onRenderWorldPass(float fovy, float aspect, float zNear, float zFar) {
      float ratio = (float)this.mc.displayWidth / (float)this.mc.displayHeight;
      if (CameraTweaks.INSTANCE != null && CameraTweaks.INSTANCE.isEnabled() && (Boolean)CameraTweaks.INSTANCE.aspectRatio.getValue()) {
         ratio = (Float)CameraTweaks.INSTANCE.aspectRatioAmount.getValue();
      }

      Project.gluPerspective(fovy, ratio, zNear, zFar);
   }

   @Redirect(
      method = {"renderCloudsCheck"},
      at = @At(
   value = "INVOKE",
   target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
   remap = false
)
   )
   private void onRenderCloudsCheck(float fovy, float aspect, float zNear, float zFar) {
      float ratio = (float)this.mc.displayWidth / (float)this.mc.displayHeight;
      if (CameraTweaks.INSTANCE != null && CameraTweaks.INSTANCE.isEnabled() && (Boolean)CameraTweaks.INSTANCE.aspectRatio.getValue()) {
         ratio = (Float)CameraTweaks.INSTANCE.aspectRatioAmount.getValue();
      }

      Project.gluPerspective(fovy, ratio, zNear, zFar);
   }

   @Redirect(
      method = {"setupCameraTransform"},
      at = @At(
   value = "INVOKE",
   target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
   remap = false
)
   )
   private void onSetupCameraTransform(float fovy, float aspect, float zNear, float zFar) {
      float ratio = (float)this.mc.displayWidth / (float)this.mc.displayHeight;
      if (CameraTweaks.INSTANCE != null && CameraTweaks.INSTANCE.isEnabled() && (Boolean)CameraTweaks.INSTANCE.aspectRatio.getValue()) {
         ratio = (Float)CameraTweaks.INSTANCE.aspectRatioAmount.getValue();
      }

      Project.gluPerspective(fovy, ratio, zNear, zFar);
   }

   @Inject(
      method = {"updateCameraAndRender"},
      at = {@At("HEAD")}
   )
   private void onUpdateCameraAndRender(float partialTicks, long nanoTime, CallbackInfo ci) {
      GuiScreen screen = this.mc.currentScreen;
      boolean isNewGui = screen instanceof NewVonwareGui;
      ClickGui.GuiBlur newBlurMode = ClickGui.getInstance().guiBlur.getValue();
      boolean blurEnabled = isNewGui && newBlurMode == ClickGui.GuiBlur.All;
      int blurStrength = (Integer)ClickGui.getInstance().blurStrength.getValue();
      if (isNewGui && blurEnabled && this.mc.world != null) {
         if (!this.isShaderActive()) {
            this.mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
            this.lastBlurStrength = -1;
         }

         if (blurStrength != this.lastBlurStrength) {
            this.updateBlurStrength(blurStrength);
            this.lastBlurStrength = blurStrength;
         }

         this.wasInGui = true;
      } else {
         if (this.wasInGui && this.isShaderActive()) {
            this.mc.entityRenderer.stopUseShader();
            this.lastBlurStrength = -1;
         }

         this.wasInGui = false;
      }

   }

   @Inject(
      method = {"renderWorldPass"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"
)}
   )
   private void vonware$captureTerrainDepthBeforeEntities(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
      Shaders shaders = (Shaders)Vonware.moduleManager.getModuleByClass(Shaders.class);
      if (shaders != null && shaders.isOn() && shaders.isOcclusionEnabled()) {
         FrameBufferShader.captureTerrainDepth();
      }

   }

   @Inject(
      method = {"renderWorldPass"},
      at = {@At("HEAD")}
   )
   public void onRenderWorldPass(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
      if (this.mc.player != null && this.mc.world != null) {
         double px = this.mc.getRenderManager().viewerPosX;
         double py = this.mc.getRenderManager().viewerPosY;
         double pz = this.mc.getRenderManager().viewerPosZ;
         Optimizer.updateFrustum(px, py, pz);
      }

   }

   private static Field getListShadersField() {
      if (cachedListShadersField != null) {
         return cachedListShadersField;
      } else {
         try {
            cachedListShadersField = ShaderGroup.class.getDeclaredField("listShaders");
            cachedListShadersField.setAccessible(true);
            return cachedListShadersField;
         } catch (NoSuchFieldException var2) {
            try {
               cachedListShadersField = ShaderGroup.class.getDeclaredField("listShaders");
               cachedListShadersField.setAccessible(true);
               return cachedListShadersField;
            } catch (NoSuchFieldException var1) {
               return null;
            }
         }
      }
   }

   private void updateBlurStrength(int strength) {
      if (this.shaderGroup != null) {
         try {
            Field field = getListShadersField();
            if (field == null) {
               return;
            }

            for(Shader shader : (List<Shader>)field.get(this.shaderGroup)) {
               ShaderUniform radiusUniform = shader.getShaderManager().getShaderUniform("Radius");
               if (radiusUniform != null) {
                  radiusUniform.set((float)strength);
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }

      }
   }

   public void invokeSetupCameraTransform(float partialTicks, int pass) {
      this.setupCameraTransform(partialTicks, pass);
   }

   public void invokeRenderHand(float partialTicks, int pass) {
      this.renderHand(partialTicks, pass);
   }

   private Vector3f mix(Vector3f first, Vector3f second, float factor) {
      return new Vector3f(first.x * (1.0F - factor) + second.x * factor, first.y * (1.0F - factor) + second.y * factor, first.z * (1.0F - factor) + first.z * factor);
   }

   @Redirect(
      method = {"updateCameraAndRender"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"
)
   )
   private void redirectTurn(EntityPlayerSP entityPlayerSP, float yaw, float pitch) {
      if (Freecam.INSTANCE != null && Freecam.INSTANCE.isEnabled()) {
         Minecraft.getMinecraft().getRenderViewEntity().turn(yaw, pitch);
      } else {
         entityPlayerSP.turn(yaw, pitch);
      }
   }

   @Redirect(
      method = {"renderWorldPass"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSpectator()Z"
)
   )
   public boolean redirectIsSpectator(EntityPlayerSP entityPlayerSP) {
      if (Freecam.INSTANCE != null && Freecam.INSTANCE.isEnabled()) {
         return true;
      } else {
         return entityPlayerSP != null && entityPlayerSP.isSpectator();
      }
   }

   @Redirect(
      method = {"getMouseOver"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/multiplayer/WorldClient;getEntitiesInAABBexcluding(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;"
)
   )
   public List<Entity> getEntitiesInAABBexcludingHook(WorldClient worldClient, @Nullable Entity entityIn, AxisAlignedBB boundingBox, @Nullable Predicate<? super Entity> predicate) {
      return (List<Entity>)(NoEntityTrace.getInstance().isEnabled() && NoEntityTrace.getInstance().noTrace ? new ArrayList() : worldClient.getEntitiesInAABBexcluding(entityIn, boundingBox, predicate));
   }

   @Inject(
      method = {"renderWorldPass"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/EntityRenderer;renderRainSnow(F)V"
)}
   )
   public void weatherHook(int pass, float partialTicks, long finishTimeNano, CallbackInfo ci) {
      if (((Environment)Vonware.moduleManager.getModuleByClass(Environment.class)).isOn()) {
         ((Environment)Vonware.moduleManager.getModuleByClass(Environment.class)).renderWeather(partialTicks);
      }

   }

   @Inject(
      method = {"drawNameplate"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void renderName(FontRenderer fontRendererIn, String str, float x, float y, float z, int verticalShift, float viewerYaw, float viewerPitch, boolean isThirdPersonFrontal, boolean isSneaking, CallbackInfo ci) {
      if (((NameTags)Vonware.moduleManager.getModuleByClass(NameTags.class)).isEnabled()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"setupFog"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setupFogHook(int startCoords, float partialTicks, CallbackInfo info) {
      if (NoRender.getInstance().isOn() && NoRender.getInstance().fog.getValue() == NoRender.Fog.NOFOG) {
         GL11.glDisable(2912);
         info.cancel();
      }

   }

   @Inject(
      method = {"setupFog"},
      at = {@At("RETURN")}
   )
   public void setupFogHookPost(int startCoords, float partialTicks, CallbackInfo info) {
      if (NoRender.getInstance().isOn() && NoRender.getInstance().fog.getValue() == NoRender.Fog.NOFOG) {
         GL11.glDisable(2912);
      }

   }

   @Redirect(
      method = {"setupFog"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getBlockStateAtEntityViewpoint(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;F)Lnet/minecraft/block/state/IBlockState;"
)
   )
   public IBlockState getBlockStateAtEntityViewpointHook(World worldIn, Entity entityIn, float p_186703_2_) {
      return NoRender.getInstance().isOn() && NoRender.getInstance().fog.getValue() == NoRender.Fog.AIR ? Blocks.AIR.defaultBlockState : ActiveRenderInfo.getBlockStateAtEntityViewpoint(worldIn, entityIn, p_186703_2_);
   }

   @Inject(
      method = {"updateLightmap"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V",
   shift = At.Shift.BEFORE
)}
   )
   private void updateTextureHook(float partialTicks, CallbackInfo ci) {
      try {
         Environment environment = (Environment)Vonware.moduleManager.getModuleByClass(Environment.class);
         if (environment != null && environment.isEnabled() && (Boolean)environment.fullBright.getValue() && (Boolean)environment.lightmapBoolean.getValue() && environment.fullBrightMode.getValue() == Environment.FullBrightMode.Gamma) {
            Color color = environment.getLightmapColor();
            float redMod = (float)color.getRed() / 255.0F;
            float greenMod = (float)color.getGreen() / 255.0F;
            float blueMod = (float)color.getBlue() / 255.0F;

            for(int i = 0; i < this.lightmapColors.length; ++i) {
               int c = this.lightmapColors[i];
               int[] bgr = this.toRGBAArray(c);
               int red = (int)((float)bgr[2] * redMod);
               int green = (int)((float)bgr[1] * greenMod);
               int blue = (int)((float)bgr[0] * blueMod);
               this.lightmapColors[i] = -16777216 | red << 16 | green << 8 | blue;
            }
         }
      } catch (Exception var14) {
      }

   }

   @Redirect(
      method = {"setupCameraTransform"},
      at = @At(
   value = "FIELD",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;prevTimeInPortal:F"
)
   )
   public float prevTimeInPortalHook(EntityPlayerSP entityPlayerSP) {
      NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
      return noRender != null && noRender.noNausea() ? -Float.MAX_VALUE : entityPlayerSP.prevTimeInPortal;
   }

   @Inject(
      method = {"renderItemActivation"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderItemActivationHook(CallbackInfo info) {
      if (this.itemActivationItem != null) {
         NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
         if (noRender != null && noRender.noTotems() && this.itemActivationItem.getItem() == Items.TOTEM_OF_UNDYING) {
            info.cancel();
         }
      }

   }

   @Inject(
      method = {"hurtCameraEffect"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hurtCameraEffectHook(float ticks, CallbackInfo info) {
      NoRender noRender = (NoRender)Vonware.moduleManager.getModuleByClass(NoRender.class);
      if (noRender != null && noRender.noHurtCam()) {
         info.cancel();
      }

   }

   @ModifyVariable(
      method = {"orientCamera"},
      ordinal = 3,
      at = @At("STORE"),
      require = 1
   )
   private double changeCameraDistanceHook(double range) {
      return CameraTweaks.INSTANCE.isEnabled() && (Boolean)CameraTweaks.INSTANCE.viewClip.getValue() ? ((Float)CameraTweaks.INSTANCE.distance.getValue()).doubleValue() : range;
   }

   @ModifyVariable(
      method = {"orientCamera"},
      ordinal = 7,
      at = @At("STORE"),
      require = 1
   )
   private double orientCameraHook(double range) {
      return CameraTweaks.INSTANCE.isEnabled() && (Boolean)CameraTweaks.INSTANCE.viewClip.getValue() ? ((Float)CameraTweaks.INSTANCE.distance.getValue()).doubleValue() : range;
   }
}
