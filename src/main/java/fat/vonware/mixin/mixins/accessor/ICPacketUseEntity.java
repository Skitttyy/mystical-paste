package fat.vonware.mixin.mixins.accessor;

import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({CPacketUseEntity.class})
public interface ICPacketUseEntity {
   @Accessor("entityId")
   void setEntityId(int var1);

   @Accessor("entityId")
   int getEntityId();

   @Accessor("action")
   void setAction(CPacketUseEntity.Action var1);

   @Accessor("hand")
   void setHand(EnumHand var1);
}
