package fat.vonware.mixin.mixins.block;

import fat.vonware.ducks.ICPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({CPacketPlayerDigging.class})
public abstract class MixinCPacketPlayerDigging implements ICPacketPlayerDigging {
   @Unique
   private boolean clientSideBreaking;
   @Unique
   private boolean normalDigging;

   @Unique
   public void setClientSideBreaking(boolean breaking) {
      this.clientSideBreaking = breaking;
   }

   @Unique
   public boolean isClientSideBreaking() {
      return this.clientSideBreaking;
   }

   @Unique
   public void setNormalDigging(boolean normalDigging) {
      this.normalDigging = normalDigging;
   }

   @Unique
   public boolean isNormalDigging() {
      return this.normalDigging;
   }
}
