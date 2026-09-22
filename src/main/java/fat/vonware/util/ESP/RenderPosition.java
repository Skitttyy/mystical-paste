package fat.vonware.util.ESP;

import net.minecraft.util.math.BlockPos;

public class RenderPosition {
   private final BlockPos pos;
   private final long time;

   public RenderPosition(BlockPos pos) {
      this.pos = pos;
      this.time = System.currentTimeMillis();
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public long getTime() {
      return this.time;
   }
}
