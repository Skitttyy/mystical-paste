package fat.vonware.event.events.player;

import fat.vonware.event.EventStage;
import net.minecraft.util.MovementInputFromOptions;

public class InputUpdateEvent extends EventStage {
   private final MovementInputFromOptions movementInput;

   public InputUpdateEvent(MovementInputFromOptions movementInput) {
      this.movementInput = movementInput;
   }

   public MovementInputFromOptions getMovementInput() {
      return this.movementInput;
   }
}
