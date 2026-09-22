package fat.vonware.util.Cosmetics;

import fat.vonware.util.EntityUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.CullFace;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.Loader;

public class RenderWings extends ModelBase {
   private final Minecraft mc = Minecraft.getMinecraft();
   private final ResourceLocation location = new ResourceLocation("textures/wings.png");
   private final ModelRenderer wing;
   private final ModelRenderer wingTip;
   private final boolean playerUsesFullHeight = Loader.isModLoaded("animations");
   private static RenderWings instance;

   public RenderWings() {
      this.setTextureOffset("wing.bone", 0, 0);
      this.setTextureOffset("wing.skin", -10, 8);
      this.setTextureOffset("wingtip.bone", 0, 5);
      this.setTextureOffset("wingtip.skin", -10, 18);
      (this.wing = new ModelRenderer(this, "wing")).setTextureSize(30, 30);
      this.wing.setRotationPoint(-2.0F, 0.0F, 0.0F);
      this.wing.addBox("bone", -10.0F, -1.0F, -1.0F, 10, 2, 2);
      this.wing.addBox("skin", -10.0F, 0.0F, 0.5F, 10, 0, 10);
      (this.wingTip = new ModelRenderer(this, "wingtip")).setTextureSize(30, 30);
      this.wingTip.setRotationPoint(-10.0F, 0.0F, 0.0F);
      this.wingTip.addBox("bone", -10.0F, -0.5F, -0.5F, 10, 1, 1);
      this.wingTip.addBox("skin", -10.0F, 0.0F, 0.5F, 10, 0, 10);
      this.wing.addChild(this.wingTip);
   }

   public void doRender(EntityPlayer player, float partialTicks) {
      if (player == this.mc.player) {
         if (this.mc.currentScreen == null) {
            GlStateManager.pushMatrix();
            GlStateManager.pushAttrib();

            try {
               GlStateManager.disableLighting();
               GlStateManager.disableCull();
               GlStateManager.enableBlend();
               GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
               double scale = (double)0.75F;
               double rotate = (double)this.interpolate(player.prevRenderYawOffset, player.renderYawOffset, partialTicks);
               Vec3d interpolatedPosition = EntityUtil.getInterpolatedRenderPos(player, partialTicks);
               GlStateManager.translate(interpolatedPosition.x, interpolatedPosition.y, interpolatedPosition.z);
               GlStateManager.scale((double)-0.75F, (double)-0.75F, (double)0.75F);
               GlStateManager.rotate((float)((double)180.0F + rotate), 0.0F, 1.0F, 0.0F);
               GlStateManager.translate((double)0.0F, -(this.playerUsesFullHeight ? 1.45 : (double)1.25F) / (double)0.75F, (double)0.0F);
               GlStateManager.translate((double)0.0F, (double)0.0F, 0.26666666666666666);
               if (player.isSneaking()) {
                  GlStateManager.translate((double)0.0F, 0.16666666666666666, (double)0.0F);
               }

               GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
               this.mc.getTextureManager().bindTexture(this.location);

               for(int j = 0; j < 2; ++j) {
                  GlStateManager.enableCull();
                  float f11 = (float)(System.currentTimeMillis() % 1000L) / 1000.0F * (float)Math.PI * 2.0F;
                  this.wing.rotateAngleX = (float)Math.toRadians((double)-80.0F) - (float)Math.cos((double)f11) * 0.2F;
                  this.wing.rotateAngleY = (float)Math.toRadians((double)20.0F) + (float)Math.sin((double)f11) * 0.4F;
                  this.wing.rotateAngleZ = (float)Math.toRadians((double)20.0F);
                  this.wingTip.rotateAngleZ = -((float)(Math.sin((double)(f11 + 2.0F)) + (double)0.5F)) * 0.75F;
                  this.wing.render(0.0625F);
                  GlStateManager.scale(-1.0F, 1.0F, 1.0F);
                  if (j == 0) {
                     GlStateManager.cullFace(CullFace.FRONT);
                  }
               }

               GlStateManager.cullFace(CullFace.BACK);
               GlStateManager.disableCull();
            } finally {
               GlStateManager.enableLighting();
               GlStateManager.disableBlend();
               GlStateManager.popAttrib();
               GlStateManager.popMatrix();
            }

         }
      }
   }

   private float interpolate(float yaw1, float yaw2, float percent) {
      float f = (yaw1 + (yaw2 - yaw1) * percent) % 360.0F;
      if (f < 0.0F) {
         f += 360.0F;
      }

      return f;
   }

   public static RenderWings getInstance() {
      if (instance == null) {
         instance = new RenderWings();
      }

      return instance;
   }
}
