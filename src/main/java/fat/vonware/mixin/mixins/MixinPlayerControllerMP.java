package fat.vonware.mixin.mixins;

import fat.vonware.ducks.ICPacketPlayerDigging;
import fat.vonware.ducks.IPlayerControllerMP;
import fat.vonware.event.events.BreakBlockEvent;
import fat.vonware.event.events.DamageBlockEvent;
import fat.vonware.event.events.Block.ClickBlockEvent;
import fat.vonware.features.modules.player.MultiTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerControllerMP.class})
public abstract class MixinPlayerControllerMP implements IPlayerControllerMP {
   @Shadow
   public float curBlockDamageMP;
   @Shadow
   public int blockHitDelay;

   @Invoker("syncCurrentPlayItem")
   public abstract void syncItem();

   @Accessor("blockHitDelay")
   public abstract void setBlockHitDelay(int var1);

   @Accessor("isHittingBlock")
   public abstract void setIsHittingBlock(boolean var1);

   @Invoker("isHittingPosition")
   public abstract boolean getIsHittingPosition(BlockPos var1);

   @Accessor("curBlockDamageMP")
   public abstract void setCurBlockDamageMP(float var1);

   @Inject(
      method = {"onPlayerDestroyBlock"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"
)},
      cancellable = true
   )
   public void onPlayerDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
      BreakBlockEvent breakEvent = new BreakBlockEvent(pos);
      MinecraftForge.EVENT_BUS.post(breakEvent);
      if (breakEvent.isCanceled()) {
         info.setReturnValue(true);
         info.cancel();
      }

   }

   @Redirect(
      method = {"onPlayerDamageBlock"},
      at = @At(
   value = "NEW",
   target = "net/minecraft/network/play/client/CPacketPlayerDigging"
)
   )
   private CPacketPlayerDigging cPacketPlayerDiggingInitHook(CPacketPlayerDigging.Action actionIn, BlockPos posIn, EnumFacing facingIn) {
      CPacketPlayerDigging packet = new CPacketPlayerDigging(actionIn, posIn, facingIn);
      switch (actionIn) {
         case START_DESTROY_BLOCK:
         case STOP_DESTROY_BLOCK:
         case ABORT_DESTROY_BLOCK:
            ((ICPacketPlayerDigging)packet).setNormalDigging(true);
         default:
            if (actionIn == Action.STOP_DESTROY_BLOCK) {
               ((ICPacketPlayerDigging)packet).setClientSideBreaking(true);
            }

            return packet;
      }
   }

   @Inject(
      method = {"clickBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void clickBlockHook(BlockPos pos, EnumFacing facing, CallbackInfoReturnable<Boolean> info) {
      ClickBlockEvent event = new ClickBlockEvent(pos, facing);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         info.setReturnValue(true);
         info.cancel();
      }

   }

   @Inject(
      method = {"processRightClickBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void clickBlockHook(EntityPlayerSP player, WorldClient worldIn, BlockPos pos, EnumFacing direction, Vec3d vec, EnumHand hand, CallbackInfoReturnable<EnumActionResult> info) {
      ClickBlockEvent.Right event = new ClickBlockEvent.Right(pos, direction, vec, hand);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.isCanceled()) {
         info.setReturnValue(EnumActionResult.PASS);
         info.cancel();
      }

   }

   @Inject(
      method = {"onStoppedUsingItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onStopItem(EntityPlayer playerIn, CallbackInfo ci) {
      if (MultiTask.INSTANCE.isEnabled() && playerIn.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.GOLDEN_APPLE && Minecraft.getMinecraft().gameSettings.keyBindUseItem.isKeyDown()) {
         ci.cancel();
      }

   }

   @Inject(
      method = {"onPlayerDamageBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onPlayerDamageBlock(BlockPos pos, EnumFacing facing, CallbackInfoReturnable<Boolean> cir) {
      DamageBlockEvent event = new DamageBlockEvent(pos, facing, this.curBlockDamageMP, this.blockHitDelay);
      MinecraftForge.EVENT_BUS.post(event);
      this.curBlockDamageMP = event.getDamage();
      this.blockHitDelay = event.getDelay();
      if (event.isCanceled()) {
         cir.setReturnValue(true);
         cir.cancel();
      }

   }
}
