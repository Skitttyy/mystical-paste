package fat.vonware.mixin.mixins.accessor;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Gui.class})
public interface IGuiAccessor {
   @Invoker("drawTexturedModalRect")
   void vonware$drawTexturedModalRect(int var1, int var2, int var3, int var4, int var5, int var6);
}
