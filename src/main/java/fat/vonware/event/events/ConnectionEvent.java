package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayer;

public class ConnectionEvent extends EventStage {
   private final UUID uuid;
   private final EntityPlayer entity;
   private final String name;

   public ConnectionEvent(int stage, UUID uuid, String name) {
      super(stage);
      this.uuid = uuid;
      this.name = name;
      this.entity = null;
   }

   public ConnectionEvent(int stage, EntityPlayer entity, UUID uuid, String name) {
      super(stage);
      this.entity = entity;
      this.uuid = uuid;
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public EntityPlayer getEntity() {
      return this.entity;
   }

   public static class Leave extends ConnectionEvent {
      public Leave(int name, UUID uuid, EntityPlayer player) {
         super(name, player, uuid, (String)null);
      }
   }
}
