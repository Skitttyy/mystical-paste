package fat.vonware.mixin.mixins.accessor;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {ItemRenderer.class},
   priority = Integer.MAX_VALUE
)
public interface IItemRenderer {
   @Accessor("equippedProgressMainHand")
   void settEquippedProgressMainHand(float var1);

   @Accessor("equippedProgressMainHand")
   float gettEquippedProgressMainHand();

   @Accessor("equippedProgressOffHand")
   void settEquippedProgressOffHand(float var1);

   @Accessor("equippedProgressOffHand")
   float gettEquippedProgressOffHand();

   @Accessor("prevEquippedProgressMainHand")
   void settPrevEquippedProgressMainHand(float var1);

   @Accessor("prevEquippedProgressMainHand")
   float gettPrevEquippedProgressMainHand();

   @Accessor("prevEquippedProgressOffHand")
   void settPrevEquippedProgressOffHand(float var1);

   @Accessor("prevEquippedProgressOffHand")
   float gettPrevEquippedProgressOffHand();

   @Accessor("itemStackMainHand")
   void settItemStackMainHand(ItemStack var1);

   @Accessor("itemStackMainHand")
   ItemStack gettItemStackMainHand();

   @Accessor("itemStackOffHand")
   void settItemStackOffHand(ItemStack var1);

   @Accessor("itemStackOffHand")
   ItemStack gettItemStackOffHand();
}
