package fat.vonware.event.events.chat;

import fat.vonware.Vonware;
import fat.vonware.event.EventStage;
import fat.vonware.features.modules.misc.ExtraTab;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class TabListSortEvent extends EventStage {
   private List<NetworkPlayerInfo> list;

   public TabListSortEvent(List<NetworkPlayerInfo> list) {
      this.list = list;
   }

   public void sort(ExtraTab.ExtraTabSorting sorting, boolean inf) {
      switch (sorting) {
         case PING:
            this.list.sort(Comparator.comparing(NetworkPlayerInfo::getResponseTime));
            break;
         case TOP_FRIENDS:
            this.list.sort(Comparator.comparing((info) -> Vonware.friendManager.isFriend(info) ? -1 : 1));
            break;
         case ALPHABETICAL:
            this.list.sort((info1, info2) -> info1.getGameProfile().getName().compareToIgnoreCase(info2.getGameProfile().getName()));
            break;
         case LENGTH:
            this.list.sort(Comparator.comparingInt((info) -> info.getGameProfile().getName().length()));
      }

      if (inf) {
         this.list = this.list.subList(0, Math.min(this.list.size(), 1000));
      }

   }

   public List<NetworkPlayerInfo> getList() {
      return this.list;
   }
}
