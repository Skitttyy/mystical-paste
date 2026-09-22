package fat.vonware.util.OtherRenderShit.ca;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class Pair<K, V> {
   private final K key;
   private final V value;

   public Pair(K key, V value) {
      this.key = key;
      this.value = value;
   }

   public K key() {
      return this.key;
   }

   public V value() {
      return this.value;
   }

   public static class BlockPair extends Pair<BlockPos, EnumFacing> {
      public BlockPair(BlockPos pos, EnumFacing face) {
         super(pos, face);
      }
   }

   public static class BlockPairTime extends Pair<BlockPos, Long> {
      public BlockPairTime(BlockPos pos) {
         super(pos, System.currentTimeMillis());
      }
   }
}
