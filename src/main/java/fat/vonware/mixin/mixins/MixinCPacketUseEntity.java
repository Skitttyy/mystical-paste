package fat.vonware.mixin.mixins;

import fat.vonware.ducks.ICPacketUseEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketUseEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({CPacketUseEntity.class})
public class MixinCPacketUseEntity implements ICPacketUseEntity {
   private Entity entity;

   public Entity getAttackedEntity() {
      return this.entity;
   }
}
