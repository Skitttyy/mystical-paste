package fat.vonware.event.events;

import fat.vonware.event.EventStage;

public class UpdateEquippedItemEvent extends EventStage {
   public boolean isCancelable() {
      return true;
   }
}
