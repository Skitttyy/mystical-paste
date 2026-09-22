package fat.vonware.features.modules.misc;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.HexColorText;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PvPInfo extends Module {
   private final Setting<Boolean> self = new Setting<Boolean>("Self", true, "Includes your own potion and pearl actions in PvPInfo messages.");
   private final Setting<Double> range = new Setting<Double>("Range", (double)30.0F, (double)10.0F, (double)100.0F, "Maximum distance from you for player actions to be announced.");
   private final Setting<Boolean> levelTwoPots = new Setting<Boolean>("LevelTwoPots", false, "Only announces Strength and Speed potions that are level II or higher.");
   private final Setting<Boolean> strength = new Setting<Boolean>("Strength", true, "Announces when a nearby player uses a Strength potion.");
   private final Setting<Boolean> speed = new Setting<Boolean>("Speed", true, "Announces when a nearby player uses a Speed potion.");
   private final Setting<Boolean> pearl = new Setting<Boolean>("Pearl", true, "Announces when a nearby player throws an ender pearl.");
   private final Setting<Boolean> pearlDirection = new Setting<Boolean>("PearlDirection", true, "Includes the direction the pearl thrower was facing in the announcement.");
   private final Map<UUID, ItemStack> lastHeldItem = new HashMap();
   private final Map<EntityEnderPearl, String> pearlThrowers = new HashMap();

   public PvPInfo() {
      super("PvPInfo", "Announces when players drink potions & pearl", Module.Category.MISC, true, false, false);
      this.register(this.self);
      this.register(this.range);
      this.register(this.levelTwoPots);
      this.register(this.strength);
      this.register(this.speed);
      this.register(this.pearl);
      this.register(this.pearlDirection);
   }

   private String c1() {
      return TextFormatting.WHITE.toString();
   }

   private String c2() {
      return HexColorText.color(Global.getGlobalColor().getRGB());
   }

   private String prefix() {
      return Global.getClientName();
   }

   public void onUpdate() {
      if (mc.world != null && mc.player != null) {
         for(EntityPlayer player : mc.world.playerEntities) {
            if (((Boolean)this.self.getValue() || player != mc.player) && !((double)mc.player.getDistance(player) > (Double)this.range.getValue())) {
               UUID uuid = player.getUniqueID();
               ItemStack currentItem = player.getHeldItemMainhand();
               ItemStack previousItem = (ItemStack)this.lastHeldItem.get(uuid);
               if (previousItem != null && !previousItem.isEmpty() && previousItem.getItem() == Items.POTIONITEM) {
                  boolean potionConsumed = false;
                  if (!currentItem.isEmpty() && currentItem.getItem() == Items.POTIONITEM) {
                     if (currentItem.getItem() == Items.POTIONITEM && currentItem.getCount() < previousItem.getCount()) {
                        potionConsumed = true;
                     }
                  } else {
                     potionConsumed = true;
                  }

                  if (potionConsumed) {
                     this.checkPotionEffects(player, previousItem);
                  }
               }

               this.lastHeldItem.put(uuid, currentItem.copy());
            }
         }

         this.pearlThrowers.keySet().removeIf((pearl) -> pearl.isDead);
      }
   }

   @SubscribeEvent
   public void onEntityJoinWorld(EntityJoinWorldEvent event) {
      if (event.getEntity() instanceof EntityEnderPearl) {
         if (mc.world != null && mc.player != null) {
            EntityEnderPearl pearl = (EntityEnderPearl)event.getEntity();
            EntityPlayer thrower = null;
            double closestDistance = (double)2.5F;

            for(EntityPlayer player : mc.world.playerEntities) {
               double distance = pearl.getDistance(player.posX, player.posY + (double)player.getEyeHeight(), player.posZ);
               if (distance < closestDistance) {
                  closestDistance = distance;
                  thrower = player;
               }
            }

            if (thrower != null) {
               if ((Boolean)this.self.getValue() || thrower != mc.player) {
                  if (!((double)mc.player.getDistance(thrower) > (Double)this.range.getValue())) {
                     if ((Boolean)this.pearl.getValue()) {
                        this.pearlThrowers.put(pearl, thrower.getName());
                        String message;
                        if (thrower == mc.player) {
                           message = this.prefix() + this.c2() + "You \u00a7fhave thrown a pearl";
                           if ((Boolean)this.pearlDirection.getValue()) {
                              message = message + " \u00a7ffacing " + this.c1() + "[" + this.c2() + this.getThrowerFacingDirection(thrower) + this.c1() + "]";
                           }
                        } else {
                           String playerName = thrower.getName();
                           String nameColor = this.getPlayerColor(thrower);
                           message = this.prefix() + nameColor + playerName + " \u00a7fhas thrown a pearl";
                           if ((Boolean)this.pearlDirection.getValue()) {
                              message = message + " \u00a7ffacing " + this.c1() + "[" + this.c2() + this.getThrowerFacingDirection(thrower) + this.c1() + "]";
                           }
                        }

                        Command.sendSilentMessage(message);
                     }
                  }
               }
            }
         }
      }
   }

   private String getThrowerFacingDirection(EntityPlayer player) {
      float yaw = player.rotationYaw % 360.0F;
      if (yaw < 0.0F) {
         yaw += 360.0F;
      }

      if (!(yaw >= 315.0F) && !(yaw < 45.0F)) {
         if (yaw >= 45.0F && yaw < 135.0F) {
            return "West -X";
         } else {
            return yaw >= 135.0F && yaw < 225.0F ? "North -Z" : "East +X";
         }
      } else {
         return "South +Z";
      }
   }

   private void checkPotionEffects(EntityPlayer player, ItemStack potionStack) {
      for(PotionEffect stackEffect : PotionUtils.getEffectsFromStack(potionStack)) {
         if ((Boolean)this.strength.getValue() && stackEffect.getPotion() == MobEffects.STRENGTH) {
            int level = stackEffect.getAmplifier() + 1;
            if (!(Boolean)this.levelTwoPots.getValue() || level >= 2) {
               String levelText = level == 1 ? "" : " " + level;
               String playerName = this.getPlayerDisplayName(player);
               String nameColor = this.getPlayerColor(player);
               Command.sendSilentMessage(this.prefix() + nameColor + playerName + " \u00a7fdrank \u00a7cStrength" + levelText);
            }
         }

         if ((Boolean)this.speed.getValue() && stackEffect.getPotion() == MobEffects.SPEED) {
            int level = stackEffect.getAmplifier() + 1;
            if (!(Boolean)this.levelTwoPots.getValue() || level >= 2) {
               String levelText = level == 1 ? "" : " " + level;
               String playerName = this.getPlayerDisplayName(player);
               String nameColor = this.getPlayerColor(player);
               Command.sendSilentMessage(this.prefix() + nameColor + playerName + " \u00a7fdrank \u00a7bSpeed" + levelText);
            }
         }
      }

   }

   private String getPlayerDisplayName(EntityPlayer player) {
      return player == mc.player ? this.c2() + "You" : player.getName();
   }

   private String getPlayerColor(EntityPlayer player) {
      return Vonware.friendManager.isFriend(player.getName()) ? this.c2() : "\u00a7c";
   }

   public void onDisable() {
      this.lastHeldItem.clear();
      this.pearlThrowers.clear();
   }
}
