package fat.vonware.event.events.chat;

import fat.vonware.event.EventStage;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class TabListNameEvent extends EventStage {
   private String name;

   public TabListNameEvent(String name) {
      this.name = name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }
}
