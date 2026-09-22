package fat.vonware.features.modules.player;

import fat.vonware.Vonware;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoEntityTrace extends Module {
   private static NoEntityTrace INSTANCE = new NoEntityTrace();
   public Setting<Boolean> noFriendAttack = new Setting<Boolean>("NoFriendAttack", true, "Prevents NoEntityTrace from causing interactions that would attack friends.");
   public Setting<Boolean> pick = new Setting<Boolean>("Pick", true, "Allows entity tracing to be ignored while holding a pickaxe.");
   public Setting<Boolean> gap = new Setting<Boolean>("Gap", false, "Allows entity tracing to be ignored while holding a golden apple.");
   public Setting<Boolean> crystal = new Setting<Boolean>("Crystal", false, "Allows entity tracing to be ignored while holding an end crystal.");
   public Setting<Boolean> experienceBottle = new Setting<Boolean>("EXP", false, "Allows entity tracing to be ignored while holding an experience bottle.");
   public Setting<Boolean> obby = new Setting<Boolean>("Obby", false, "Allows entity tracing to be ignored while holding obsidian.");
   public boolean noTrace;

   public NoEntityTrace() {
      super("NoEntityTrace", "Mine through entities.", Module.Category.PLAYER, true, false, false);
      this.register(this.noFriendAttack);
      this.register(this.pick);
      this.register(this.gap);
      this.register(this.crystal);
      this.register(this.experienceBottle);
      this.register(this.obby);
      this.setInstance();
   }

   public static NoEntityTrace getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new NoEntityTrace();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public void onUpdate() {
      Item item = mc.player.getHeldItemMainhand().getItem();
      if (item instanceof ItemPickaxe && (Boolean)this.pick.getValue()) {
         this.noTrace = true;
      } else if (item == Items.GOLDEN_APPLE && (Boolean)this.gap.getValue()) {
         this.noTrace = true;
      } else if (item == Items.END_CRYSTAL && (Boolean)this.crystal.getValue()) {
         this.noTrace = true;
      } else if (item == Items.EXPERIENCE_BOTTLE && (Boolean)this.experienceBottle.getValue()) {
         this.noTrace = true;
      } else if (!(item instanceof ItemBlock)) {
         this.noTrace = false;
      } else {
         this.noTrace = ((ItemBlock)item).getBlock() == Blocks.OBSIDIAN && (Boolean)this.obby.getValue();
      }
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (!fullNullCheck()) {
         Entity entity;
         if ((Boolean)this.noFriendAttack.getValue() && event.getPacket() instanceof CPacketUseEntity && (entity = ((CPacketUseEntity)event.getPacket()).getEntityFromWorld(mc.world)) != null && Vonware.friendManager.isFriend(entity.getName())) {
            event.setCanceled(true);
         }

      }
   }
}
