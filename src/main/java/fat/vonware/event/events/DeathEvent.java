package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraft.entity.player.EntityPlayer;

public class DeathEvent extends EventStage {
   public EntityPlayer player;

   public DeathEvent(EntityPlayer player) {
      this.player = player;
   }

   public EntityPlayer getEntity() {
      return this.player;
   }
}
