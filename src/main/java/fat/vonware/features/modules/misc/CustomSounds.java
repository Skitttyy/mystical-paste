package fat.vonware.features.modules.misc;

import fat.vonware.event.events.DeathEvent;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.TotemPopEvent;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.OtherRenderShit.gui.SoundUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CustomSounds extends Module {
   private static final ResourceLocation TOTEM_USE;
   public final Setting<Boolean> killSounds = new Setting<Boolean>("Kill Sounds", true, "Plays the selected custom sound when another player is killed.");
   public final Setting<KillSoundMode> mode;
   public final Setting<Boolean> totemSounds;
   public final Setting<TotemSoundMode> totemMode;
   public final Setting<Float> range;
   private final Set<Integer> recentlyDead;

   public CustomSounds() {
      super("CustomSounds", "Plays custom sounds for nearby deaths and totem pops", Module.Category.MISC, true, false, false);
      this.mode = (new Setting("Kill Mode", CustomSounds.KillSoundMode.Valorant)).withDescription((Function<KillSoundMode, String>)((mode) -> {
         switch (mode) {
            case Valorant:
               return "Valorant: Plays the built-in Valorant kill sound.";
            case Murasaki:
               return "Murasaki: Plays the Hollow Purple sound from Jujutsu Kaisen.";
            case Custom:
               return "Custom: Plays the custom kill sound loaded by SoundUtil.";
            default:
               return "";
         }
      }));
      this.totemSounds = new Setting<Boolean>("Totem Sounds", true, "Replaces or plays a custom sound when a totem pops.");
      this.totemMode = (new Setting("Totem Mode", CustomSounds.TotemSoundMode.CutePop)).withDescription((Function<TotemSoundMode, String>)((mode) -> {
         switch (mode) {
            case CutePop:
               return "CutePop: Plays the built-in CutePop totem sound.";
            case Custom:
               return "Custom: Plays the custom totem sound loaded by SoundUtil.";
            default:
               return "";
         }
      }));
      this.range = new Setting<Float>("Range", 6.0F, 1.0F, 20.0F, "Maximum distance used by the module's custom-sound handling.");
      this.recentlyDead = new HashSet();
      this.register(this.killSounds);
      this.register(this.mode);
      this.register(this.totemSounds);
      this.register(this.totemMode);
      this.register(this.range);
   }

   public void onEnable() {
      Command.sendSilentMessage("");
      if ((Boolean)this.killSounds.getValue() && this.mode.getValue() == CustomSounds.KillSoundMode.Custom && !SoundUtil.hasCustomKillSound()) {
         Command.sendSilentMessage("");
      }

      if ((Boolean)this.totemSounds.getValue() && this.totemMode.getValue() == CustomSounds.TotemSoundMode.Custom && !SoundUtil.hasCustomTotemSound()) {
         Command.sendSilentMessage("");
      }

   }

   public void onDisable() {
      this.recentlyDead.clear();
      Command.sendSilentMessage("");
   }

   @SubscribeEvent
   public void onPlaySound(PlaySoundEvent event) {
      if (this.isOn()) {
         if ((Boolean)this.totemSounds.getValue()) {
            if (event.getSound() != null) {
               ResourceLocation soundLocation = event.getSound().getSoundLocation();
               if (TOTEM_USE.equals(soundLocation)) {
                  event.setResultSound((ISound)null);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onDeath(DeathEvent event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.killSounds.getValue()) {
            if (event.getEntity() != mc.player) {
               if (!(mc.player.getDistance(event.getEntity()) > (Float)this.range.getValue())) {
                  int id = event.getEntity().getEntityId();
                  if (this.recentlyDead.add(id)) {
                     this.playKillSound();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onTotemPop(TotemPopEvent event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.totemSounds.getValue()) {
            if (event.getEntity() != mc.player) {
               if (!(mc.player.getDistance(event.getEntity()) > (Float)this.range.getValue())) {
                  this.playTotemSound();
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onUpdate(MotionUpdateEvent event) {
      if (!fullNullCheck()) {
         this.recentlyDead.removeIf((id) -> {
            Entity entity = mc.world.getEntityByID(id);
            if (entity == null) {
               return true;
            } else if (entity instanceof EntityLivingBase) {
               return ((EntityLivingBase)entity).getHealth() > 0.0F;
            } else {
               return true;
            }
         });
      }
   }

   private void playKillSound() {
      switch ((KillSoundMode)this.mode.getValue()) {
         case Valorant:
            SoundUtil.playValorantKill();
            break;
         case Murasaki:
            SoundUtil.playMurasakiKill();
            break;
         case Custom:
            SoundUtil.playCustomKill();
      }

   }

   private void playTotemSound() {
      switch ((TotemSoundMode)this.totemMode.getValue()) {
         case CutePop:
            SoundUtil.playCutePopTotem();
            break;
         case Custom:
            SoundUtil.playCustomTotem();
      }

   }

   static {
      TOTEM_USE = SoundEvents.ITEM_TOTEM_USE.getSoundName();
   }

   public static enum KillSoundMode {
      Valorant,
      Murasaki,
      Custom;
   }

   public static enum TotemSoundMode {
      CutePop,
      Custom;
   }
}
