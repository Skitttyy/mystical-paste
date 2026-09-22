package fat.vonware.util.shader;

import fat.vonware.mixin.mixins.accessor.IEntityRenderer;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public abstract class FrameBufferShader extends GLSLShader {
   private static boolean playerOcclusionCapture;
   private static boolean playerOcclusionCleanup;
   private static boolean heldItemOcclusionLayer;
   private static boolean offhandOcclusionLayer;
   private static boolean heldItemContextActive;
   private static Framebuffer terrainDepthFramebuffer;
   private static int terrainDepthWidth = -1;
   private static int terrainDepthHeight = -1;
   protected final Minecraft mc = Minecraft.getMinecraft();
   protected Framebuffer framebuffer;
   protected Color inside;
   protected Color outside;
   protected float radius = 2.0F;
   protected float quality = 1.0F;
   protected long startTime = System.currentTimeMillis();
   protected int insideMode;
   protected int outsideMode;
   protected int glow;
   private boolean entityShadows;
   private boolean depthOcclusion;
   private Framebuffer captureFramebuffer;
   private boolean occlusionBatchActive;

   public static boolean isPlayerOcclusionCapture() {
      return playerOcclusionCapture;
   }

   public static boolean isPlayerOcclusionCleanup() {
      return playerOcclusionCleanup;
   }

   public static void setHeldItemOcclusionContext(boolean offhand) {
      if (playerOcclusionCapture) {
         heldItemContextActive = true;
         heldItemOcclusionLayer = true;
         offhandOcclusionLayer = offhand;
         applyPlayerLayerOcclusionState();
      }
   }

   public static void clearHeldItemOcclusionContext() {
      heldItemContextActive = false;
      heldItemOcclusionLayer = false;
      offhandOcclusionLayer = false;
      heldItemContextActive = false;
      applyPlayerLayerOcclusionState();
   }

   public static void applyCurrentHeldItemOcclusionState() {
      if (playerOcclusionCapture && heldItemContextActive) {
         applyPlayerLayerOcclusionState();
      }
   }

   public static void beginHeldItemOcclusionLayer() {
      heldItemOcclusionLayer = true;
      applyPlayerLayerOcclusionState();
   }

   public static void applyHeldItemOcclusionState(boolean leftHanded) {
      if (playerOcclusionCapture) {
         heldItemOcclusionLayer = true;
         offhandOcclusionLayer = leftHanded;
         applyPlayerLayerOcclusionState();
      }
   }

   public static void endHeldItemOcclusionLayer() {
      heldItemOcclusionLayer = false;
      offhandOcclusionLayer = false;
      applyPlayerLayerOcclusionState();
   }

   public void beginPlayerOcclusionCapture() {
      playerOcclusionCapture = true;
   }

   public void endPlayerOcclusionCapture() {
      playerOcclusionCapture = false;
      playerOcclusionCleanup = false;
      heldItemOcclusionLayer = false;
      offhandOcclusionLayer = false;
      GL11.glDisable(32823);
      GL11.glPolygonOffset(0.0F, 0.0F);
   }

   public static void applyPlayerLayerOcclusionState() {
      if (playerOcclusionCapture) {
         GL11.glDisable(32823);
         GL11.glPolygonOffset(0.0F, 0.0F);
         double depthFar = (double)1.0F;
         if (playerOcclusionCleanup && heldItemOcclusionLayer) {
            depthFar = offhandOcclusionLayer ? 0.99875 : 0.9999;
         }

         GL11.glDepthRange((double)0.0F, depthFar);
         GlStateManager.enableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.003921569F);
         GlStateManager.colorMask(true, true, true, true);
         GL14.glBlendEquation(32774);
         GlStateManager.enableBlend();
         if (playerOcclusionCleanup) {
            GlStateManager.depthFunc(515);
            GL11.glBlendFunc(0, 0);
         } else {
            GlStateManager.depthFunc(516);
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         }

      }
   }

   public static void beginPlayerLayerCleanup() {
      applyPlayerLayerOcclusionState();
   }

   public static void endPlayerLayerCleanup() {
      applyPlayerLayerOcclusionState();
   }

   public static void captureTerrainDepth() {
      Minecraft minecraft = Minecraft.getMinecraft();
      if (OpenGlHelper.isFramebufferEnabled() && minecraft.getFramebuffer() != null) {
         if (terrainDepthFramebuffer == null || terrainDepthWidth != minecraft.displayWidth || terrainDepthHeight != minecraft.displayHeight) {
            if (terrainDepthFramebuffer != null) {
               terrainDepthFramebuffer.deleteFramebuffer();
            }

            terrainDepthFramebuffer = new Framebuffer(minecraft.displayWidth, minecraft.displayHeight, true);
            terrainDepthWidth = minecraft.displayWidth;
            terrainDepthHeight = minecraft.displayHeight;
         }

         GL30.glBindFramebuffer(36008, minecraft.getFramebuffer().framebufferObject);
         GL30.glBindFramebuffer(36009, terrainDepthFramebuffer.framebufferObject);
         GL30.glBlitFramebuffer(0, 0, minecraft.displayWidth, minecraft.displayHeight, 0, 0, terrainDepthFramebuffer.framebufferWidth, terrainDepthFramebuffer.framebufferHeight, 256, 9728);
         GL30.glBindFramebuffer(36160, minecraft.getFramebuffer().framebufferObject);
      }
   }

   public void useTerrainDepthForCurrentLayer() {
      if (this.captureFramebuffer != null && terrainDepthFramebuffer != null) {
         GL30.glBindFramebuffer(36008, terrainDepthFramebuffer.framebufferObject);
         GL30.glBindFramebuffer(36009, this.captureFramebuffer.framebufferObject);
         GL30.glBlitFramebuffer(0, 0, terrainDepthFramebuffer.framebufferWidth, terrainDepthFramebuffer.framebufferHeight, 0, 0, this.captureFramebuffer.framebufferWidth, this.captureFramebuffer.framebufferHeight, 256, 9728);
         GL30.glBindFramebuffer(36160, this.captureFramebuffer.framebufferObject);
      }
   }

   public FrameBufferShader(String fragmentShader) {
      super(fragmentShader);
   }

   public void beginOcclusionBatch() {
      this.depthOcclusion = OpenGlHelper.isFramebufferEnabled();
      this.occlusionBatchActive = this.depthOcclusion;
      this.framebuffer = this.setupFrameBuffer(this.framebuffer);
      this.framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.framebuffer.framebufferClear();
      this.entityShadows = this.mc.gameSettings.entityShadows;
      this.mc.gameSettings.entityShadows = false;
   }

   public void startOcclusionLayer(float partialTicks) {
      if (!this.occlusionBatchActive) {
         this.startDraw(partialTicks, true);
      } else {
         this.captureFramebuffer = this.setupFrameBuffer(this.captureFramebuffer);
         this.captureFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
         this.captureFramebuffer.framebufferClear();
         this.copyWorldDepthBuffer(this.captureFramebuffer);
         this.captureFramebuffer.bindFramebuffer(true);
         GlStateManager.matrixMode(5890);
         GlStateManager.loadIdentity();
         GlStateManager.matrixMode(5888);
         this.prepareHiddenFragmentPass();
         GlStateManager.disableLighting();
         GlStateManager.enableTexture2D();
         ((IEntityRenderer)this.mc.entityRenderer).invokeSetupCameraTransform(partialTicks, 0);
      }
   }

   public void mergeOcclusionLayer() {
      if (this.occlusionBatchActive && this.captureFramebuffer != null) {
         this.framebuffer.bindFramebuffer(true);
         GlStateManager.viewport(0, 0, this.framebuffer.framebufferWidth, this.framebuffer.framebufferHeight);
         GlStateManager.matrixMode(5889);
         GlStateManager.pushMatrix();
         GlStateManager.loadIdentity();
         GlStateManager.ortho((double)0.0F, (double)this.mc.displayWidth, (double)this.mc.displayHeight, (double)0.0F, (double)-1.0F, (double)1.0F);
         GlStateManager.matrixMode(5888);
         GlStateManager.pushMatrix();
         GlStateManager.loadIdentity();
         GlStateManager.disableLighting();
         GlStateManager.disableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.enableTexture2D();
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.001F);
         GlStateManager.enableBlend();
         GL14.glBlendEquation(32774);
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GL11.glBindTexture(3553, this.captureFramebuffer.framebufferTexture);
         GL11.glBegin(7);
         GL11.glTexCoord2f(0.0F, 1.0F);
         GL11.glVertex2f(0.0F, 0.0F);
         GL11.glTexCoord2f(0.0F, 0.0F);
         GL11.glVertex2f(0.0F, (float)this.mc.displayHeight);
         GL11.glTexCoord2f(1.0F, 0.0F);
         GL11.glVertex2f((float)this.mc.displayWidth, (float)this.mc.displayHeight);
         GL11.glTexCoord2f(1.0F, 1.0F);
         GL11.glVertex2f((float)this.mc.displayWidth, 0.0F);
         GL11.glEnd();
         GlStateManager.matrixMode(5888);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5889);
         GlStateManager.popMatrix();
         GlStateManager.matrixMode(5888);
         GlStateManager.depthMask(false);
         GlStateManager.enableDepth();
         GlStateManager.depthFunc(516);
         GlStateManager.alphaFunc(516, 0.003921569F);
      }
   }

   public void endOcclusionBatch() {
      this.occlusionBatchActive = false;
      this.depthOcclusion = true;
   }

   public void startDraw(float partialTicks) {
      this.startDraw(partialTicks, false);
   }

   public void startDraw(float partialTicks, boolean depthOcclusion) {
      this.depthOcclusion = depthOcclusion && OpenGlHelper.isFramebufferEnabled();
      (this.framebuffer = this.setupFrameBuffer(this.framebuffer)).setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
      this.framebuffer.framebufferClear();
      if (this.depthOcclusion) {
         this.copyWorldDepthBuffer();
      }

      this.framebuffer.bindFramebuffer(true);
      this.entityShadows = this.mc.gameSettings.entityShadows;
      this.mc.gameSettings.entityShadows = false;
      GlStateManager.matrixMode(5890);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.003921569F);
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      GlStateManager.disableLighting();
      GlStateManager.enableTexture2D();
      GlStateManager.enableDepth();
      GlStateManager.depthMask(!this.depthOcclusion);
      GlStateManager.depthFunc(this.depthOcclusion ? 516 : 515);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      ((IEntityRenderer)this.mc.entityRenderer).invokeSetupCameraTransform(partialTicks, 0);
   }

   private void copyWorldDepthBuffer() {
      this.copyWorldDepthBuffer(this.framebuffer);
   }

   private void copyWorldDepthBuffer(Framebuffer target) {
      int mainFramebuffer = this.mc.getFramebuffer().framebufferObject;
      int shaderFramebuffer = target.framebufferObject;
      GL30.glBindFramebuffer(36008, mainFramebuffer);
      GL30.glBindFramebuffer(36009, shaderFramebuffer);
      GL30.glBlitFramebuffer(0, 0, this.mc.displayWidth, this.mc.displayHeight, 0, 0, target.framebufferWidth, target.framebufferHeight, 256, 9728);
      GL30.glBindFramebuffer(36160, shaderFramebuffer);
   }

   public void prepareHiddenFragmentPass() {
      if (this.depthOcclusion) {
         GL14.glBlendEquation(32774);
         GlStateManager.colorMask(true, true, true, true);
         GlStateManager.enableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.depthFunc(516);
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.003921569F);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void beginVisibleFragmentErase() {
      this.beginVisibleFragmentErase(false);
   }

   public void beginVisibleFragmentErase(boolean playerLayer) {
      if (this.depthOcclusion) {
         GL11.glDepthRange((double)0.0F, playerLayer ? 0.9999 : (double)1.0F);
         playerOcclusionCleanup = playerLayer;
         GL11.glDisable(32823);
         GL11.glPolygonOffset(0.0F, 0.0F);
         GlStateManager.enableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.depthFunc(515);
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.003921569F);
         GlStateManager.enableBlend();
         GL14.glBlendEquation(32774);
         GL11.glBlendFunc(0, 0);
         GlStateManager.colorMask(true, true, true, true);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void endVisibleFragmentErase() {
      if (this.depthOcclusion) {
         playerOcclusionCleanup = false;
         GL11.glDepthRange((double)0.0F, (double)1.0F);
         GL11.glDisable(32823);
         GL11.glPolygonOffset(0.0F, 0.0F);
         GL14.glBlendEquation(32774);
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.enableDepth();
         GlStateManager.depthMask(false);
         GlStateManager.depthFunc(516);
         GlStateManager.enableAlpha();
         GlStateManager.alphaFunc(516, 0.003921569F);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public void cancelDraw() {
      GL11.glDepthRange((double)0.0F, (double)1.0F);
      this.mc.gameSettings.entityShadows = this.entityShadows;
      GL20.glUseProgram(0);
      this.mc.getFramebuffer().bindFramebuffer(true);
      GL14.glBlendEquation(32774);
      GlStateManager.colorMask(true, true, true, true);
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.depthFunc(515);
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      this.depthOcclusion = false;
      this.occlusionBatchActive = false;
      playerOcclusionCapture = false;
      heldItemOcclusionLayer = false;
      offhandOcclusionLayer = false;
      GL11.glDisable(32823);
      GL11.glPolygonOffset(0.0F, 0.0F);
   }

   public void stopDraw(Color inside, float radius, float quality) {
   }

   public void stopDraw(Color inside, Color outside, float radius, float quality, int insideMode, int outsideMode, boolean glow) {
      GL11.glDepthRange((double)0.0F, (double)1.0F);
      this.mc.gameSettings.entityShadows = this.entityShadows;
      GlStateManager.bindTexture(0);
      GlStateManager.matrixMode(5890);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.enableLighting();
      GlStateManager.depthMask(true);
      this.mc.getFramebuffer().bindFramebuffer(true);
      this.inside = inside;
      this.outside = outside;
      this.radius = radius;
      this.quality = quality;
      this.insideMode = insideMode;
      this.outsideMode = outsideMode;
      this.glow = glow ? 1 : 0;
      this.mc.entityRenderer.disableLightmap();
      RenderHelper.disableStandardItemLighting();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.disableAlpha();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      this.startShader();
      this.mc.entityRenderer.setupOverlayRendering();
      this.drawFramebuffer(this.framebuffer);
      this.stopShader();
      this.mc.entityRenderer.disableLightmap();
      GlStateManager.enableTexture2D();
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.depthFunc(515);
      this.depthOcclusion = false;
      this.occlusionBatchActive = false;
      playerOcclusionCapture = false;
      heldItemOcclusionLayer = false;
      offhandOcclusionLayer = false;
      GL11.glDisable(32823);
      GL11.glPolygonOffset(0.0F, 0.0F);
      GlStateManager.enableCull();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.matrixMode(5890);
      GlStateManager.loadIdentity();
      GlStateManager.matrixMode(5888);
      GL11.glBindTexture(3553, 0);
   }

   public void setRadius(float radius) {
      this.radius = radius;
   }

   public Framebuffer setupFrameBuffer(Framebuffer frameBuffer) {
      if (frameBuffer == null || frameBuffer.framebufferWidth != this.mc.displayWidth || frameBuffer.framebufferHeight != this.mc.displayHeight) {
         if (frameBuffer != null) {
            frameBuffer.deleteFramebuffer();
         }

         frameBuffer = new Framebuffer(this.mc.displayWidth, this.mc.displayHeight, true);
      }

      return frameBuffer;
   }

   public void drawFramebuffer(Framebuffer framebuffer) {
      ScaledResolution scaledResolution = new ScaledResolution(this.mc);
      GL11.glBindTexture(3553, framebuffer.framebufferTexture);
      GL11.glBegin(7);
      GL11.glTexCoord2d((double)0.0F, (double)1.0F);
      GL11.glVertex2d((double)0.0F, (double)0.0F);
      GL11.glTexCoord2d((double)0.0F, (double)0.0F);
      GL11.glVertex2d((double)0.0F, (double)scaledResolution.getScaledHeight());
      GL11.glTexCoord2d((double)1.0F, (double)0.0F);
      GL11.glVertex2d((double)scaledResolution.getScaledWidth(), (double)scaledResolution.getScaledHeight());
      GL11.glTexCoord2d((double)1.0F, (double)1.0F);
      GL11.glVertex2d((double)scaledResolution.getScaledWidth(), (double)0.0F);
      GL11.glEnd();
      GL20.glUseProgram(0);
   }
}
