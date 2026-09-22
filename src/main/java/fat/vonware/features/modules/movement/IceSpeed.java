package fat.vonware.features.modules.movement;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class IceSpeed extends Module {
   private final Setting<Float> traction = new Setting<Float>("Traction", 0.4F, 0.2F, 2.0F, "Sets the slipperiness/traction value applied to ice blocks while IceSpeed is enabled.");

   public IceSpeed() {
      super("IceSpeed", "Faster on ice", Module.Category.MOVEMENT, true, false, false);
      this.register(this.traction);
   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
      if (!fullNullCheck()) {
         if (Blocks.ICE != null && Blocks.PACKED_ICE != null && Blocks.FROSTED_ICE != null) {
            Blocks.ICE.slipperiness = 0.98F;
            Blocks.PACKED_ICE.slipperiness = 0.98F;
            Blocks.FROSTED_ICE.slipperiness = 0.98F;
         }
      }
   }

   @SubscribeEvent
   public void onTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if (Blocks.ICE != null && Blocks.PACKED_ICE != null && Blocks.FROSTED_ICE != null) {
            Blocks.ICE.slipperiness = ((Number)this.traction.getValue()).floatValue();
            Blocks.PACKED_ICE.slipperiness = ((Number)this.traction.getValue()).floatValue();
            Blocks.FROSTED_ICE.slipperiness = ((Number)this.traction.getValue()).floatValue();
         }
      }
   }
}
