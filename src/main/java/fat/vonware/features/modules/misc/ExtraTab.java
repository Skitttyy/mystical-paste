package fat.vonware.features.modules.misc;

import fat.vonware.Vonware;
import fat.vonware.event.events.chat.TabListLimitEvent;
import fat.vonware.event.events.chat.TabListNameEvent;
import fat.vonware.event.events.chat.TabListSortEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Chat.ColorEnum;
import java.awt.Color;
import java.util.function.Function;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ExtraTab extends Module {
   private final Setting<ExtraTabSorting> sorting;
   private final Setting<Boolean> noLimit;
   private final Setting<Boolean> pooled;
   private final Setting<Boolean> friends;
   private final Setting<Boolean> colorSelf;
   private final Setting<ColorEnum> friendColor;
   private final Setting<PingMode> ping;

   public ExtraTab() {
      super("ExtraTab", "", Module.Category.MISC, true, false, true);
      this.sorting = (new Setting("Sorting", ExtraTab.ExtraTabSorting.VANILLA)).withDescription((Function<ExtraTabSorting, String>)((mode) -> {
         switch (mode) {
            case VANILLA:
               return "VANILLA: Keeps the normal Minecraft tab-list ordering.";
            case TOP_FRIENDS:
               return "TOP_FRIENDS: Moves friends toward the top of the tab list.";
            case ALPHABETICAL:
               return "ALPHABETICAL: Sorts players alphabetically by name.";
            case PING:
               return "PING: Sorts players using their displayed connection latency.";
            case LENGTH:
               return "LENGTH: Sorts players based on the length of their displayed name.";
            default:
               return "";
         }
      }));
      this.noLimit = new Setting<Boolean>("NoTabLimit", true, "Removes the vanilla player limit from the tab list.");
      this.pooled = new Setting<Boolean>("Pooled", false, "Uses the module's pooled tab-list layout.");
      this.friends = new Setting<Boolean>("Friends", true, "Highlights friends in the tab list.");
      this.colorSelf = new Setting<Boolean>("ColorSelf", true, "Highlights your own name in the tab list.");
      this.friendColor = new Setting<ColorEnum>("FriendColor", ColorEnum.AQUA, (v) -> (Boolean)this.friends.getValue(), "Selects the formatting color used for highlighted friends.");
      this.ping = (new Setting("PingMode", ExtraTab.PingMode.BARS)).withDescription((Function<PingMode, String>)((mode) -> {
         switch (mode) {
            case TEXT:
               return "TEXT: Displays each player's ping as a numeric text value.";
            case BARS:
               return "BARS: Uses the normal connection-strength bars for ping.";
            default:
               return "";
         }
      }));
      this.register(this.sorting);
      this.register(this.noLimit);
      this.register(this.pooled);
      this.register(this.friends);
      this.register(this.colorSelf);
      this.register(this.friendColor);
      this.register(this.ping);
   }

   @SubscribeEvent
   public void TabListLimit(TabListLimitEvent event) {
      if (this.noLimit()) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void TabListName(TabListNameEvent event) {
      String name = event.getName();
      if (this.colorSelf() && mc.player != null && name.equals(mc.player.getName())) {
         event.setName(String.format("%s%s", this.friendColor(), name));
         event.setCanceled(true);
      } else {
         if (this.friends() && Vonware.friendManager.isFriend(name)) {
            event.setName(String.format("%s%s", this.friendColor(), name));
            event.setCanceled(true);
         }

      }
   }

   @SubscribeEvent
   public void TabListSort(TabListSortEvent event) {
      if (this.sorting() != ExtraTab.ExtraTabSorting.VANILLA) {
         event.sort(this.sorting(), this.noLimit());
         event.setCanceled(true);
      }

   }

   public boolean noLimit() {
      return this.isEnabled() && (Boolean)this.noLimit.getValue();
   }

   public boolean ping() {
      return this.isEnabled() && this.ping.getValue() == ExtraTab.PingMode.TEXT;
   }

   public boolean bars() {
      return this.isEnabled() && this.ping.getValue() == ExtraTab.PingMode.BARS;
   }

   public boolean friends() {
      return this.isEnabled() && (Boolean)this.friends.getValue();
   }

   public boolean colorSelf() {
      return this.isEnabled() && (Boolean)this.colorSelf.getValue();
   }

   public boolean pooled() {
      return this.isEnabled() && (Boolean)this.pooled.getValue();
   }

   public String friendColor() {
      return ((ColorEnum)this.friendColor.getValue()).getColor();
   }

   public ExtraTabSorting sorting() {
      return this.sorting.getValue();
   }

   public Color getColor(int ping) {
      if (ping < 100) {
         return new Color(5635925);
      } else {
         return ping < 200 ? new Color(16777045) : new Color(16733525);
      }
   }

   public static enum ExtraTabSorting {
      VANILLA,
      TOP_FRIENDS,
      ALPHABETICAL,
      PING,
      LENGTH;
   }

   public static enum PingMode {
      TEXT,
      BARS;
   }
}
