package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class SuffocationEvent extends EventStage {
   private final EntityPlayer player;

   public SuffocationEvent(EntityPlayer player) {
      this.player = player;
   }

   public EntityPlayer getPlayer() {
      return this.player;
   }
}
