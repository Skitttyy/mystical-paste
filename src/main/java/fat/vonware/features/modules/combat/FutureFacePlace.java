package fat.vonware.features.modules.combat;

import fat.vonware.Vonware;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.function.Function;
import net.minecraft.entity.player.EntityPlayer;

public class FutureFacePlace extends Module {
   public final Setting<Mode> mode;
   public final Setting<Float> base;
   public final Setting<Float> enableDamage;
   public final Setting<Float> faceplaceHealth;
   public final Setting<Float> normalHealth;
   public final Setting<Float> maxRange;
   private boolean isFaceplacing;

   public FutureFacePlace() {
      super("FutureMinDamage", "for Future Client (no macro needed)", Module.Category.COMBAT, true, false, false);
      this.mode = (new Setting("Mode", FutureFacePlace.Mode.Smart)).withDescription((Function<Mode, String>)((mode) -> {
         switch (mode) {
            case Toggle:
               return "Toggle: Sets AutoCrystal MinDamage to Faceplace when enabled, then restores BaseDamage when disabled.";
            case Smart:
               return "Smart: Automatically switches between Faceplace and BaseDamage based on the closest target's health.";
            default:
               return "";
         }
      }));
      this.base = new Setting<Float>("BaseDamage", 6.0F, 0.0F, 20.0F, "Normal AutoCrystal MinDamage value used when face-place is not active.");
      this.enableDamage = new Setting<Float>("Faceplace", 1.0F, 0.0F, 6.0F, "AutoCrystal MinDamage value used while face-place is active.");
      this.faceplaceHealth = new Setting<Float>("FaceplaceHP", 10.0F, 0.0F, 36.0F, (v) -> this.mode.getValue() == FutureFacePlace.Mode.Smart, "Target health at or below which Smart mode switches AutoCrystal to the Faceplace damage value.");
      this.normalHealth = new Setting<Float>("NormalHP", 14.0F, 0.0F, 36.0F, (v) -> this.mode.getValue() == FutureFacePlace.Mode.Smart, "Target health at or above which Smart mode restores AutoCrystal to the BaseDamage value.");
      this.maxRange = new Setting<Float>("MaxRange", 12.0F, 1.0F, 30.0F, (v) -> this.mode.getValue() == FutureFacePlace.Mode.Smart, "Maximum distance at which Smart mode searches for the closest non-friend target.");
      this.isFaceplacing = false;
      this.register(this.mode);
      this.register(this.base);
      this.register(this.enableDamage);
      this.register(this.faceplaceHealth);
      this.register(this.normalHealth);
      this.register(this.maxRange);
   }

   public String getDisplayInfo() {
      return ((Mode)this.mode.getValue()).name();
   }

   public void onUpdate() {
      if (mc.player != null && mc.world != null && this.mode.getValue() == FutureFacePlace.Mode.Smart) {
         if (!mc.player.isSpectator()) {
            EntityPlayer target = this.findClosestTarget();
            if (target != null) {
               float targetHealth = target.getHealth() + target.getAbsorptionAmount();
               if (!this.isFaceplacing && targetHealth <= (Float)this.faceplaceHealth.getValue()) {
                  this.isFaceplacing = true;
                  mc.player.sendChatMessage(".Autocrystal mindamage " + this.enableDamage.getValue());
               } else if (this.isFaceplacing && targetHealth >= (Float)this.normalHealth.getValue()) {
                  this.isFaceplacing = false;
                  mc.player.sendChatMessage(".Autocrystal mindamage " + this.base.getValue());
               }

            }
         }
      }
   }

   private EntityPlayer findClosestTarget() {
      EntityPlayer closest = null;
      double closestDistance = Double.MAX_VALUE;

      for(EntityPlayer player : mc.world.playerEntities) {
         if (player != mc.player && !player.isDead && !Vonware.friendManager.isFriend(player)) {
            double distance = (double)mc.player.getDistance(player);
            if (!(distance > (double)(Float)this.maxRange.getValue()) && distance < closestDistance) {
               closestDistance = distance;
               closest = player;
            }
         }
      }

      return closest;
   }

   public void onDisable() {
      if (mc.player != null && mc.world != null) {
         if (this.mode.getValue() == FutureFacePlace.Mode.Toggle) {
            mc.player.sendChatMessage(".Autocrystal mindamage " + this.base.getValue());
         } else {
            mc.player.sendChatMessage(".Autocrystal mindamage " + this.base.getValue());
            this.isFaceplacing = false;
         }

      }
   }

   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         if (this.mode.getValue() == FutureFacePlace.Mode.Toggle) {
            mc.player.sendChatMessage(".Autocrystal mindamage " + this.enableDamage.getValue());
         } else {
            mc.player.sendChatMessage(".Autocrystal mindamage " + this.base.getValue());
            this.isFaceplacing = false;
         }

      }
   }

   public static enum Mode {
      Toggle,
      Smart;
   }
}
