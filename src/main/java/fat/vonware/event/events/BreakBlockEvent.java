package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class BreakBlockEvent extends EventStage {
   private final BlockPos pos;

   public BreakBlockEvent(BlockPos pos) {
      this.pos = pos;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
