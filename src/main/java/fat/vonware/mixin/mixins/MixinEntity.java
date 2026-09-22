package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.event.events.PushEvent;
import fat.vonware.event.events.PushOutOfBlocksEvent;
import fat.vonware.event.events.StepEvent;
import fat.vonware.event.events.TurnEvent;
import fat.vonware.features.modules.render.EntityTweaks;
import fat.vonware.util.Util;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class MixinEntity implements Util {
   @Shadow
   public float stepHeight;

   @Shadow
   public abstract AxisAlignedBB getEntityBoundingBox();

   @Shadow
   public abstract boolean isSneaking();

   @Redirect(
      method = {"applyEntityCollision"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"
)
   )
   public void addVelocityHook(Entity entity, double x, double y, double z) {
      PushEvent event = new PushEvent(entity, x, y, z, true, 2);
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         entity.motionX += event.x;
         entity.motionY += event.y;
         entity.motionZ += event.z;
         entity.isAirBorne = event.airbone;
      }

   }

   @Inject(
      method = {"turn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onTurnHook(float yaw, float pitch, CallbackInfo info) {
      TurnEvent event = new TurnEvent(yaw, pitch);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"isSneaking"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onIsSneaking(CallbackInfoReturnable<Boolean> cir) {
      EntityTweaks module = (EntityTweaks)Vonware.moduleManager.getModuleByClass(EntityTweaks.class);
      if (module != null && module.isEnabled() && (Boolean)module.alwaysSneak.getValue()) {
         Entity entity = (Entity)(Object)this;
         if (entity instanceof AbstractClientPlayer) {
            cir.setReturnValue(true);
         }
      }

   }

   @Inject(
      method = {"pushOutOfBlocks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pushOutOfBlocks(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      PushOutOfBlocksEvent event = new PushOutOfBlocksEvent();
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         cir.cancel();
      }

   }

   @Inject(
      method = {"move"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/profiler/Profiler;endSection()V",
   shift = At.Shift.BEFORE,
   ordinal = 0
)}
   )
   public void onMove(MoverType type, double x, double y, double z, CallbackInfo info) {
      if (this.equals(mc.player)) {
         StepEvent event = new StepEvent(this.getEntityBoundingBox(), this.stepHeight);
         MinecraftForge.EVENT_BUS.post(event);
         if (event.isCanceled()) {
            this.stepHeight = event.getHeight();
         }
      }

   }
}
