package fat.vonware.mixin.mixins;

import fat.vonware.ducks.INetworkManager;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.util.Phase.PacketUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({NetworkManager.class})
public abstract class MixinNetworkManager implements INetworkManager {
   @Shadow
   private Channel channel;

   @Shadow
   public abstract boolean isChannelOpen();

   @Shadow
   protected abstract void flushOutboundQueue();

   @Shadow
   public abstract void setConnectionState(EnumConnectionState var1);

   @Shadow
   protected abstract void dispatchPacket(Packet<?> var1, @Nullable GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>>[] var2);

   @Inject(
      method = {"sendPacket(Lnet/minecraft/network/Packet;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSendPacketPre(Packet<?> packet, CallbackInfo info) {
      if (PacketUtil.noEventPackets.contains(packet)) {
         PacketUtil.noEventPackets.remove(packet);
      } else {
         PacketEvent.Send event = new PacketEvent.Send(0, packet);
         MinecraftForge.EVENT_BUS.post(event);
         if (event.isCanceled()) {
            info.cancel();
         }

      }
   }

   @Unique
   public void sendPacketNoEvent(Packet<?> packet) {
      if (packet instanceof CPacketPlayer.Position) {
         if (this.isChannelOpen()) {
            this.flushOutboundQueue();
            this.dispatchSilently(packet);
         }

      } else {
         PacketEvent.NoEvent event = new PacketEvent.NoEvent(packet);
         MinecraftForge.EVENT_BUS.post(event);
         if (!event.isCanceled()) {
            if (this.isChannelOpen()) {
               this.flushOutboundQueue();
               this.dispatchSilently(packet);
            }

         }
      }
   }

   @Unique
   private void dispatchSilently(Packet<?> inPacket) {
      EnumConnectionState enumconnectionstate = EnumConnectionState.getFromPacket(inPacket);
      EnumConnectionState protocolConnectionState = (EnumConnectionState)this.channel.attr(NetworkManager.PROTOCOL_ATTRIBUTE_KEY).get();
      if (protocolConnectionState != enumconnectionstate) {
         this.channel.config().setAutoRead(false);
      }

      if (this.channel.eventLoop().inEventLoop()) {
         if (enumconnectionstate != protocolConnectionState) {
            this.setConnectionState(enumconnectionstate);
         }

         ChannelFuture channelfuture = this.channel.writeAndFlush(inPacket);
         channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
      } else {
         this.channel.eventLoop().execute(() -> {
            if (enumconnectionstate != protocolConnectionState) {
               this.setConnectionState(enumconnectionstate);
            }

            ChannelFuture channelfuture2 = this.channel.writeAndFlush(inPacket);
            channelfuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
         });
      }

   }

   @Inject(
      method = {"channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/network/Packet;processPacket(Lnet/minecraft/network/INetHandler;)V",
   shift = At.Shift.BEFORE
)},
      cancellable = true
   )
   private void channelRead0Hook(ChannelHandlerContext context, Packet<?> packet, CallbackInfo info) {
      if (this.channel.isOpen()) {
         PacketEvent.Receive event = new PacketEvent.Receive(0, packet);

         try {
            MinecraftForge.EVENT_BUS.post(event);
         } catch (Throwable t) {
            t.printStackTrace();
         }

         if (event.isCanceled()) {
            info.cancel();
         }
      }

   }

   @Inject(
      method = {"exceptionCaught"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void exceptionCaughtHook(ChannelHandlerContext p_exceptionCaught_1_, Throwable p_exceptionCaught_2_, CallbackInfo ci) {
      p_exceptionCaught_2_.printStackTrace();
      System.out.println("----------------------------------------------");
      Thread.dumpStack();
      ci.cancel();
   }

   @Inject(
      method = {"dispatchPacket"},
      at = {@At("RETURN")}
   )
   private void dispatchPacketHook(Packet<?> packetIn, @Nullable GenericFutureListener<? extends Future<? super Void>>[] futureListeners, CallbackInfo info) {
      PacketEvent.Post event = new PacketEvent.Post(packetIn);
      MinecraftForge.EVENT_BUS.post(event);
   }

   public void sendFast(Packet<?> packet, @Nullable GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>>[] futureListeners) {
      this.dispatchPacket(packet, futureListeners);
   }
}
