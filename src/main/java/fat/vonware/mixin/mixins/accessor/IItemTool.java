package fat.vonware.mixin.mixins.accessor;

import net.minecraft.item.Item;
import net.minecraft.item.ItemTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ItemTool.class})
public interface IItemTool {
   @Accessor("attackDamage")
   float getAttackDamage();

   @Accessor("toolMaterial")
   Item.ToolMaterial getToolMaterial();
}
