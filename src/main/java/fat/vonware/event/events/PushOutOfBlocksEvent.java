package fat.vonware.event.events;

import net.minecraftforge.fml.common.eventhandler.Event;

public class PushOutOfBlocksEvent extends Event {
   public boolean isCancelable() {
      return true;
   }
}
