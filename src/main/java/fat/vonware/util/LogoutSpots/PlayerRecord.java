package fat.vonware.util.LogoutSpots;

import net.minecraft.entity.player.EntityPlayer;

public class PlayerRecord {
   private final EntityPlayer player;
   private final long time;

   public EntityPlayer getPlayer() {
      return this.player;
   }

   public long getTime() {
      return this.time;
   }

   public PlayerRecord(EntityPlayer player, long time) {
      this.player = player;
      this.time = time;
   }
}
