package fat.vonware.event.events.Block;

import fat.vonware.event.EventStage;

public class PickBlockEvent extends EventStage {
   public boolean isCancelable() {
      return true;
   }
}
