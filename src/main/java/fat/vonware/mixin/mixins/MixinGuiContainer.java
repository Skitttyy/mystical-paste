package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.Animations;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiContainer.class})
public class MixinGuiContainer {
   @Inject(
      method = {"drawScreen"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerBackgroundLayer(FII)V",
   shift = At.Shift.BEFORE
)}
   )
   public void onDrawContainerPre(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      Animations animations = (Animations)Vonware.moduleManager.getModuleByClass(Animations.class);
      if (animations != null && animations.isEnabled() && (Boolean)animations.guiAnimations.getValue()) {
         float scale = animations.getScale();
         if (scale != 1.0F) {
            GuiContainer container = (GuiContainer)(Object)this;
            ScaledResolution sr = new ScaledResolution(container.mc);
            int centerX = sr.getScaledWidth() / 2;
            int centerY = sr.getScaledHeight() / 2;
            GL11.glPushMatrix();
            GL11.glTranslatef((float)centerX, (float)centerY, 0.0F);
            GL11.glScalef(scale, scale, scale);
            GL11.glTranslatef((float)(-centerX), (float)(-centerY), 0.0F);
         }
      }

   }

   @Inject(
      method = {"drawScreen"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V",
   shift = At.Shift.AFTER
)}
   )
   public void onDrawContainerPost(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      Animations animations = (Animations)Vonware.moduleManager.getModuleByClass(Animations.class);
      if (animations != null && animations.isEnabled() && (Boolean)animations.guiAnimations.getValue()) {
         float scale = animations.getScale();
         if (scale != 1.0F) {
            GL11.glPopMatrix();
         }
      }

   }
}
