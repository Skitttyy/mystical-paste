package fat.vonware.event.events;

import fat.vonware.event.EventStage;

public class KeyEvent extends EventStage {
   private final int key;

   public KeyEvent(int key) {
      this.key = key;
   }

   public int getKey() {
      return this.key;
   }
}
