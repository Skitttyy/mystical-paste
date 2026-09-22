package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.event.events.player.FreecamEvent;
import fat.vonware.features.modules.render.EntityTweaks;
import fat.vonware.features.modules.render.NoRender;
import fat.vonware.manager.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderPlayer.class})
public abstract class MixinRenderPlayer extends RenderLivingBase<AbstractClientPlayer> {
   private int savedHurtTime = 0;

   @Shadow
   public abstract ModelPlayer getMainModel();

   public MixinRenderPlayer(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn) {
      super(renderManagerIn, modelBaseIn, shadowSizeIn);
   }

   @Inject(
      method = {"preRenderCallback"},
      at = {@At("RETURN")}
   )
   protected void preRenderCallback(AbstractClientPlayer entitylivingbaseIn, float partialTickTime, CallbackInfo ci) {
      EntityTweaks playerTweaks = (EntityTweaks)Vonware.moduleManager.getModuleByClass(EntityTweaks.class);
      if (playerTweaks != null && playerTweaks.isEnabled() && (Boolean)playerTweaks.changePlayerSize.getValue() && entitylivingbaseIn != Minecraft.getMinecraft().player) {
         float size = (Float)playerTweaks.playerSize.getValue();
         GlStateManager.scale(size, size, size);
      }

   }

   @Redirect(
      method = {"doRender"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isUser()Z"
)
   )
   private boolean isUserRedirect(AbstractClientPlayer abstractClientPlayer) {
      Minecraft mc = Minecraft.getMinecraft();
      FreecamEvent event = new FreecamEvent();
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         return abstractClientPlayer.isUser();
      } else {
         return abstractClientPlayer.isUser() && abstractClientPlayer == mc.getRenderViewEntity();
      }
   }

   @Inject(
      method = {"doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"},
      at = {@At("HEAD")}
   )
   private void preDoRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      if (entity instanceof EntityPlayerSP && entity == Minecraft.getMinecraft().player) {
         RotationManager.INSTANCE.savePlayerRotations();
         RotationManager.INSTANCE.applyPlayerRotations(partialTicks);
      }

   }

   @Inject(
      method = {"doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"},
      at = {@At("RETURN")}
   )
   private void postDoRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      if (entity instanceof EntityPlayerSP && entity == Minecraft.getMinecraft().player) {
         RotationManager.INSTANCE.restorePlayerRotations();
      }

   }

   @Inject(
      method = {"doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"},
      at = {@At("HEAD")}
   )
   private void onDoRenderPre(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      float alpha = NoRender.getInstance().getClusterAlpha(entity);
      if (alpha < 1.0F) {
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
         GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
         if (NoRender.getInstance().shouldDisableHurtForCluster(entity)) {
            this.savedHurtTime = entity.hurtTime;
            entity.hurtTime = 0;
         }
      }

   }

   @Inject(
      method = {"doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onDoRenderFOVCheck(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      if (!NoRender.getInstance().shouldRenderPlayerEntity(entity)) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"},
      at = {@At("RETURN")}
   )
   private void onDoRenderPost(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      float alpha = NoRender.getInstance().getClusterAlpha(entity);
      if (alpha < 1.0F) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.disableBlend();
         if (NoRender.getInstance().shouldDisableHurtForCluster(entity)) {
            entity.hurtTime = this.savedHurtTime;
            this.savedHurtTime = 0;
         }
      }

   }

   @Inject(
      method = {"setModelVisibilities"},
      at = {@At("TAIL")}
   )
   private void setModelVisibilitiesI(AbstractClientPlayer clientPlayer, CallbackInfo ci) {
      EntityTweaks module = (EntityTweaks)Vonware.moduleManager.getModuleByClass(EntityTweaks.class);
      if (module != null && module.isEnabled() && (Boolean)module.selfSneak.getValue() && clientPlayer == Minecraft.getMinecraft().player) {
         ModelPlayer model = this.getMainModel();
         model.isSneak = true;
      }

   }

   @Inject(
      method = {"doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/entity/RenderPlayer;setModelVisibilities(Lnet/minecraft/client/entity/AbstractClientPlayer;)V",
   shift = At.Shift.BEFORE
)}
   )
   private void applySelfSneakOffset(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
      EntityTweaks module = (EntityTweaks)Vonware.moduleManager.getModuleByClass(EntityTweaks.class);
      if (module != null && module.isEnabled() && (Boolean)module.selfSneak.getValue() && entity == Minecraft.getMinecraft().player && !entity.isSneaking()) {
         GlStateManager.translate((double)0.0F, (double)-0.125F, (double)0.0F);
      }

   }
}
