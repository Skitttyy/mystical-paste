package fat.vonware.mixin.mixins;

import fat.vonware.features.modules.client.Manager;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiConnecting.class})
public class MixinGuiConnecting extends GuiScreen {
   @Inject(
      method = {"drawScreen(IIF)V"},
      at = {@At("RETURN")}
   )
   private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      if (Manager.getInstance().isEnabled() && (Boolean)Manager.getInstance().connectionInfo.getValue() && this.mc.getCurrentServerData() != null) {
         String ConnectMessage = "Connecting to... [" + this.mc.getCurrentServerData().serverIP + "]";
         this.mc.fontRenderer.drawStringWithShadow(ConnectMessage, (float)this.width / 2.0F - (float)this.mc.fontRenderer.getStringWidth(ConnectMessage) / 2.0F, (float)this.height / 2.0F - (float)(this.mc.fontRenderer.FONT_HEIGHT * 2), -1);
      }

   }
}
