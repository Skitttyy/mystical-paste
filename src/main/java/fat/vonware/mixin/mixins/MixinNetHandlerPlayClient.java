package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.ducks.INetHandlerPlayClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NetHandlerPlayClient.class})
public abstract class MixinNetHandlerPlayClient implements INetHandlerPlayClient {
   @Accessor("doneLoadingTerrain")
   public abstract boolean isDoneLoadingTerrain();

   @Accessor("doneLoadingTerrain")
   public abstract void setDoneLoadingTerrain(boolean var1);

   @Inject(
      method = {"handlePlayerPosLook"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/Packet;)V",
   ordinal = 1,
   shift = At.Shift.BEFORE
)}
   )
   public void handlePlayerPosLookHook(SPacketPlayerPosLook packetIn, CallbackInfo ci) {
      Vonware.rotationManager.setBlocking(true);
   }

   @Inject(
      method = {"handlePlayerPosLook"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/Packet;)V",
   ordinal = 1,
   shift = At.Shift.AFTER
)}
   )
   public void handlePlayerPosLookHookPost(SPacketPlayerPosLook packetIn, CallbackInfo ci) {
      Vonware.rotationManager.setBlocking(false);
   }
}
