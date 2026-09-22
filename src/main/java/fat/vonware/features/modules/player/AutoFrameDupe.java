package fat.vonware.features.modules.player;

import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.util.EnumHand;

public class AutoFrameDupe extends Module {
   private final Setting<Boolean> shulkersonly = new Setting<Boolean>("Shulkers-Only", true, "Only uses shulker boxes as valid items for the item-frame dupe routine.");
   private final Setting<Integer> range = new Setting<Integer>("Range", 5, 0, 6, "Maximum interaction range used while searching for item frames.");
   private final Setting<Integer> turns = new Setting<Integer>("Turns", 1, 0, 5, "Number of interaction turns performed by the dupe sequence.");
   private final Setting<Integer> ticks = new Setting<Integer>("Ticks", 10, 1, 20, "Tick interval used by the AutoFrameDupe sequence.");
   private int timeoutTicks = 0;

   public AutoFrameDupe() {
      super("AutoFrameDupe", "Frame dupe", Module.Category.PLAYER, true, false, false);
      this.register(this.shulkersonly);
      this.register(this.range);
      this.register(this.turns);
      this.register(this.ticks);
   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null) {
         if ((Boolean)this.shulkersonly.getValue()) {
            int shulkerSlot = this.getShulkerSlot();
            if (shulkerSlot != -1) {
               mc.player.inventory.currentItem = shulkerSlot;
            }
         }

         for(Entity frame : mc.world.loadedEntityList) {
            if (frame instanceof EntityItemFrame && mc.player.getDistance(frame) <= (float)(Integer)this.range.getValue()) {
               if (this.timeoutTicks >= (Integer)this.ticks.getValue()) {
                  if (((EntityItemFrame)frame).getDisplayedItem().getItem() == Items.AIR && !mc.player.getHeldItemMainhand().isEmpty) {
                     mc.playerController.interactWithEntity(mc.player, frame, EnumHand.MAIN_HAND);
                  }

                  if (((EntityItemFrame)frame).getDisplayedItem().getItem() != Items.AIR) {
                     for(int i = 0; i < (Integer)this.turns.getValue(); ++i) {
                        mc.playerController.interactWithEntity(mc.player, frame, EnumHand.MAIN_HAND);
                     }

                     mc.playerController.attackEntity(mc.player, frame);
                     this.timeoutTicks = 0;
                  }
               }

               ++this.timeoutTicks;
            }
         }
      }

   }

   private int getShulkerSlot() {
      int shulkerSlot = -1;

      for(int i = 0; i < 9; ++i) {
         Item item = mc.player.inventory.getStackInSlot(i).getItem();
         if (item instanceof ItemShulkerBox) {
            shulkerSlot = i;
         }
      }

      return shulkerSlot;
   }
}
