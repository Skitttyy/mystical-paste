package fat.vonware.mixin.mixins;

import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.MoveEvent;
import fat.vonware.event.events.PushOutOfBlocksEvent;
import fat.vonware.event.events.player.FreecamEvent;
import fat.vonware.features.modules.movement.Sprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.MoverType;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {EntityPlayerSP.class},
   priority = 2147483637
)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
   @Shadow
   protected Minecraft mc;
   private MotionUpdateEvent motionEvent = new MotionUpdateEvent();

   public MixinEntityPlayerSP(Minecraft p_i47378_1_, World p_i47378_2_, NetHandlerPlayClient p_i47378_3_, StatisticsManager p_i47378_4_, RecipeBook p_i47378_5_) {
      super(p_i47378_2_, p_i47378_3_.getGameProfile());
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
      method = {"onUpdateWalkingPlayer"},
      at = {@At("RETURN")}
   )
   private void onUpdateWalkingPlayer_Return(CallbackInfo callbackInfo) {
      MotionUpdateEvent event = new MotionUpdateEvent(1, this.motionEvent);
      event.setCanceled(this.motionEvent.isCanceled());
      MinecraftForge.EVENT_BUS.post(event);
   }

   @Inject(
      method = {"onUpdate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V",
   shift = At.Shift.BEFORE
)}
   )
   private void onUpdateWalkingPlayerPre(CallbackInfo ci) {
      this.motionEvent = new MotionUpdateEvent(0, this.posX, this.getEntityBoundingBox().minY, this.posZ, this.rotationYaw, this.rotationPitch, this.onGround);
      MinecraftForge.EVENT_BUS.post(this.motionEvent);
      this.posX = this.motionEvent.getX();
      this.posY = this.motionEvent.getY();
      this.posZ = this.motionEvent.getZ();
      this.rotationYaw = this.motionEvent.getRotationYaw();
      this.rotationPitch = this.motionEvent.getRotationPitch();
      this.onGround = this.motionEvent.isOnGround();
   }

   @Inject(
      method = {"onUpdate"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V",
   shift = At.Shift.AFTER
)}
   )
   private void onUpdateWalkingPlayerPost(CallbackInfo ci) {
      if (this.posX == this.motionEvent.getX()) {
         this.posX = this.motionEvent.getInitialX();
      }

      if (this.posY == this.motionEvent.getY()) {
         this.posY = this.motionEvent.getInitialY();
      }

      if (this.posZ == this.motionEvent.getZ()) {
         this.posZ = this.motionEvent.getInitialZ();
      }

      if (this.rotationYaw == this.motionEvent.getRotationYaw()) {
         this.rotationYaw = this.motionEvent.getInitialYaw();
      }

      if (this.rotationPitch == this.motionEvent.getRotationPitch()) {
         this.rotationPitch = this.motionEvent.getInitialPitch();
      }

      if (this.onGround == this.motionEvent.isOnGround()) {
         this.onGround = this.motionEvent.isInitialOnGround();
      }

   }

   @Redirect(
      method = {"onUpdateWalkingPlayer"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;isCurrentViewEntity()Z"
)
   )
   private boolean redirectIsCurrentViewEntity(EntityPlayerSP entityPlayerSP) {
      FreecamEvent event = new FreecamEvent();
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         return entityPlayerSP == this.mc.player;
      } else {
         return this.mc.getRenderViewEntity() == entityPlayerSP;
      }
   }

   @Redirect(
      method = {"updateEntityActionState"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;isCurrentViewEntity()Z"
)
   )
   private boolean redirectIsCurrentViewEntity2(EntityPlayerSP entityPlayerSP) {
      Minecraft mc = Minecraft.getMinecraft();
      FreecamEvent event = new FreecamEvent();
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         return entityPlayerSP == mc.player;
      } else {
         return mc.getRenderViewEntity() == entityPlayerSP;
      }
   }

   @Redirect(
      method = {"onLivingUpdate"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/EntityPlayerSP;setSprinting(Z)V",
   ordinal = 2
)
   )
   public void onLivingUpdate(EntityPlayerSP entityPlayerSP, boolean sprintUpdate) {
      if (Sprint.INSTANCE != null && Sprint.INSTANCE.isEnabled() && Sprint.INSTANCE.mode != null && Sprint.INSTANCE.mode.getValue() != null && entityPlayerSP != null) {
         if (Sprint.INSTANCE.mode.getValue() == Sprint.SprintMode.Rage && (entityPlayerSP.moveForward != 0.0F || entityPlayerSP.moveStrafing != 0.0F) && Sprint.canSprint()) {
            entityPlayerSP.setSprinting(!entityPlayerSP.collidedHorizontally && !entityPlayerSP.isSneaking());
         } else {
            entityPlayerSP.setSprinting(sprintUpdate);
         }

      } else {
         entityPlayerSP.setSprinting(sprintUpdate);
      }
   }

   @Redirect(
      method = {"move"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/entity/AbstractClientPlayer;move(Lnet/minecraft/entity/MoverType;DDD)V"
)
   )
   public void move(AbstractClientPlayer player, MoverType moverType, double x, double y, double z) {
      MoveEvent event = new MoveEvent(0, moverType, x, y, z);
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         super.move(event.getType(), event.getX(), event.getY(), event.getZ());
      }

   }
}
