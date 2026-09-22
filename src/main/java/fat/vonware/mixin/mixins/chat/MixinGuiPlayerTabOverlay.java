package fat.vonware.mixin.mixins.chat;

import fat.vonware.Vonware;
import fat.vonware.event.events.chat.TabListLimitEvent;
import fat.vonware.event.events.chat.TabListNameEvent;
import fat.vonware.event.events.chat.TabListSortEvent;
import fat.vonware.features.modules.client.Media;
import fat.vonware.features.modules.misc.ExtraTab;
import fat.vonware.features.modules.render.NameTags;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GuiPlayerTabOverlay.class})
public abstract class MixinGuiPlayerTabOverlay {
   @Final
   @Shadow
   private Minecraft mc;
   @Unique
   private int maxPingOffset;

   @Shadow
   public abstract String getPlayerName(NetworkPlayerInfo var1);

   @Redirect(
      method = {"renderPlayerlist"},
      at = @At(
   value = "INVOKE",
   target = "Ljava/util/List;subList(II)Ljava/util/List;",
   remap = false
)
   )
   public List<NetworkPlayerInfo> renderPlayerList(List<NetworkPlayerInfo> list, int fromIndex, int toIndex) {
      TabListLimitEvent event = new TabListLimitEvent();
      MinecraftForge.EVENT_BUS.post(event);
      return list.subList(fromIndex, event.isCanceled() ? Math.min(10000, list.size()) : toIndex);
   }

   @ModifyVariable(
      method = {"renderPlayerlist"},
      at = @At(
   value = "STORE",
   ordinal = 1
),
      ordinal = 0
   )
   public List<NetworkPlayerInfo> renderPlayerListHook(List<NetworkPlayerInfo> list) {
      TabListSortEvent event = new TabListSortEvent(list);
      MinecraftForge.EVENT_BUS.post(event);
      return event.isCanceled() ? event.getList() : list;
   }

   @Inject(
      method = {"getPlayerName"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getPlayerNameHook(NetworkPlayerInfo playerInfo, CallbackInfoReturnable<String> info) {
      String rawName = playerInfo.getGameProfile().getName();
      TabListNameEvent event = new TabListNameEvent(rawName);
      MinecraftForge.EVENT_BUS.post(event);
      String resolvedName = event.isCanceled() ? event.getName() : rawName;
      String suffix;
      if (NameTags.isDevUser(rawName)) {
         suffix = TextFormatting.DARK_GRAY + " [Dev]" + TextFormatting.RESET;
      } else if (NameTags.isAdminUser(rawName)) {
         suffix = TextFormatting.BLUE + " [Admin]" + TextFormatting.RESET;
      } else if (NameTags.isVonwareUser(rawName)) {
         suffix = TextFormatting.DARK_PURPLE + " [User]" + TextFormatting.RESET;
      } else {
         suffix = "";
      }

      if (!suffix.isEmpty() || event.isCanceled()) {
         info.cancel();
         info.setReturnValue(resolvedName + suffix);
      }

   }

   @Inject(
      method = {"renderPlayerlist"},
      at = {@At("HEAD")}
   )
   private void renderPlayerlistHeadHook(int width, Scoreboard scoreboardIn, ScoreObjective scoreObjectiveIn, CallbackInfo ci) {
      ExtraTab EXTRA_TAB = (ExtraTab)Vonware.moduleManager.getModuleByClass(ExtraTab.class);
      if (EXTRA_TAB.ping()) {
         this.maxPingOffset = (Integer)this.mc.player.connection.getPlayerInfoMap().stream().map((info) -> info.getResponseTime()).map((ping) -> String.valueOf(ping)).map((str) -> this.mc.fontRenderer.getStringWidth(str)).max(Integer::compare).orElse(0) + 1 + (EXTRA_TAB.bars() ? 12 : 0);
      }

   }

   @Redirect(
      method = {"renderPlayerlist"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/GuiPlayerTabOverlay;getPlayerName(Lnet/minecraft/client/network/NetworkPlayerInfo;)Ljava/lang/String;"
)
   )
   private String getPlayerNameWidthFix(GuiPlayerTabOverlay instance, NetworkPlayerInfo playerInfo) {
      String name = this.getPlayerName(playerInfo);
      Media media = (Media)Vonware.moduleManager.getModuleByClass(Media.class);
      if (media != null && media.isEnabled() && name.contains(this.mc.getSession().getUsername())) {
         name = name.replace(this.mc.getSession().getUsername(), (CharSequence)media.newName.getValue());
      }

      return name;
   }

   @Redirect(
      method = {"renderPlayerlist"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I",
   ordinal = 0
)
   )
   private int getStringWidthHook(FontRenderer instance, String k) {
      ExtraTab EXTRA_TAB = (ExtraTab)Vonware.moduleManager.getModuleByClass(ExtraTab.class);
      return EXTRA_TAB.ping() ? instance.getStringWidth(k) + this.maxPingOffset : instance.getStringWidth(k);
   }

   @Inject(
      method = {"drawPing"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void drawPingHook(int x, int xOff, int y, NetworkPlayerInfo networkPlayerInfoIn, CallbackInfo ci) {
      ExtraTab EXTRA_TAB = (ExtraTab)Vonware.moduleManager.getModuleByClass(ExtraTab.class);
      if (EXTRA_TAB.ping()) {
         int ping = networkPlayerInfoIn.getResponseTime();
         int color = EXTRA_TAB.getColor(ping).getRGB();
         String pingStr = ping + "ms";
         this.mc.fontRenderer.drawStringWithShadow(pingStr, (float)(x + xOff - this.mc.fontRenderer.getStringWidth(pingStr)), (float)y, color);
      }

      if (EXTRA_TAB.isEnabled() && !EXTRA_TAB.bars()) {
         ci.cancel();
      }

   }
}
