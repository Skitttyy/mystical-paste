package fat.vonware.util.holeesp;

import fat.vonware.util.holeesp.Enums.SafetyEnum;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Hole {
   private BlockPos first;
   private BlockPos second;
   private BlockPos third;
   private BlockPos fourth;
   private final SafetyEnum safety;

   public Hole(BlockPos first, SafetyEnum safety) {
      this.first = first;
      this.safety = safety;
   }

   public Hole(BlockPos first, BlockPos second, SafetyEnum safety) {
      this.first = first;
      this.second = second;
      this.safety = safety;
   }

   public Hole(BlockPos first, BlockPos second, BlockPos third, BlockPos fourth, SafetyEnum safety) {
      this.first = first;
      this.second = second;
      this.third = third;
      this.fourth = fourth;
      this.safety = safety;
   }

   public void setFirst(BlockPos first) {
      this.first = first;
   }

   public BlockPos getFirst() {
      return this.first;
   }

   public void setSecond(BlockPos second) {
      this.second = second;
   }

   public BlockPos getSecond() {
      return this.second;
   }

   public boolean isDoubleHole() {
      return this.second != null;
   }

   public void setThird(BlockPos third) {
      this.third = third;
   }

   public BlockPos getThird() {
      return this.third;
   }

   public void setFourth(BlockPos fourth) {
      this.fourth = fourth;
   }

   public BlockPos getFourth() {
      return this.fourth;
   }

   public boolean is2x2() {
      return this.third != null && this.fourth != null;
   }

   public SafetyEnum getSafety() {
      return this.safety;
   }

   public String toString() {
      return this.is2x2() ? "Hole(" + this.first + ", " + this.second + ", " + this.third + ", " + this.fourth + ", " + this.safety + ")" : "Hole(" + this.first + ", " + this.second + ", " + this.safety + ")";
   }

   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (!(obj instanceof Hole)) {
         return false;
      } else {
         Hole hole = (Hole)obj;
         return this.first == hole.first && this.second == hole.second && this.third == hole.third && this.fourth == hole.fourth && this.safety == hole.safety;
      }
   }

   public int hashCode() {
      return (new HashCodeBuilder(17, 31)).append(this.first).append(this.second).append(this.third).append(this.fourth).append(this.safety).toHashCode();
   }
}
