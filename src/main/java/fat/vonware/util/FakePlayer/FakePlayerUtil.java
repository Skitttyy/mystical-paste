package fat.vonware.util.FakePlayer;

import com.mojang.authlib.GameProfile;
import fat.vonware.util.Util;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.entity.EntityOtherPlayerMP;

public class FakePlayerUtil implements Util {
   private static final Map<String, EntityOtherPlayerMP> players = new HashMap();

   public static void addFakePlayerToWorld(String label, GameProfile profile, int id) {
      if (mc.player != null) {
         EntityOtherPlayerMP fake = new EntityOtherPlayerMP(mc.world, profile);
         fake.inventory.copyInventory(mc.player.inventory);
         fake.copyLocationAndAnglesFrom(mc.player);
         fake.setHealth(mc.player.getHealth());
         fake.setAbsorptionAmount(mc.player.getAbsorptionAmount());
         fake.onGround = mc.player.onGround;
         mc.world.addEntityToWorld(id, fake);
         players.put(label, fake);
      }

   }

   public static void removeFakePlayerFromWorld(String label, int id) {
      if (mc.player != null) {
         mc.world.removeEntityFromWorld(id);
         players.remove(label);
      }

   }

   public static EntityOtherPlayerMP getPlayer(String label) {
      return (EntityOtherPlayerMP)players.get(label);
   }
}
