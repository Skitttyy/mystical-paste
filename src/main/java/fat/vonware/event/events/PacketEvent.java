package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

public class PacketEvent extends EventStage {
   private final Packet<?> packet;

   public PacketEvent(int stage, Packet<?> packet) {
      super(stage);
      this.packet = packet;
   }

   public <T extends Packet<?>> T getPacket() {
      return (T)this.packet;
   }

   @Cancelable
   public static class Send extends PacketEvent {
      public Send(int stage, Packet<?> packet) {
         super(stage, packet);
      }
   }

   public static class NoEvent extends PacketEvent {
      public NoEvent(Packet<?> packet) {
         super(0, packet);
      }
   }

   @Cancelable
   public static class Post extends PacketEvent {
      public Post(Packet<?> packet) {
         super(0, packet);
      }
   }

   @Cancelable
   public static class Receive extends PacketEvent {
      public Receive(int stage, Packet<?> packet) {
         super(stage, packet);
      }
   }
}
