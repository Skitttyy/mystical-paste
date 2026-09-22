package fat.vonware.mixin.mixins;

import fat.vonware.Vonware;
import fat.vonware.features.modules.render.Animations;
import fat.vonware.features.modules.render.ContainerPreview;
import fat.vonware.util.Animations.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.world.storage.MapData;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiScreen.class})
public class MixinGuiScreen extends Gui {
   public Minecraft mc = Minecraft.getMinecraft();

   @Inject(
      method = {"drawScreen"},
      at = {@At("HEAD")},
      cancellable = false
   )
   public void onDrawScreenPre(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      Animations animations = (Animations)Vonware.moduleManager.getModuleByClass(Animations.class);
      if (animations != null && animations.isEnabled() && (Boolean)animations.guiAnimations.getValue()) {
         float scale = animations.getScale();
         if (scale != 1.0F) {
            ScaledResolution sr = new ScaledResolution(this.mc);
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
      at = {@At("RETURN")}
   )
   public void onDrawScreenPost(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
      Animations animations = (Animations)Vonware.moduleManager.getModuleByClass(Animations.class);
      if (animations != null && animations.isEnabled() && (Boolean)animations.guiAnimations.getValue()) {
         float scale = animations.getScale();
         if (scale != 1.0F) {
            GL11.glPopMatrix();
         }
      }

   }

   @Inject(
      method = {"renderToolTip"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderToolTip(ItemStack stack, int x, int y, CallbackInfo ci) {
      if (ContainerPreview.INSTANCE != null && ContainerPreview.INSTANCE.isEnabled()) {
         if (stack.getItem() instanceof ItemMap && (Boolean)ContainerPreview.INSTANCE.mapPreview.getValue()) {
            ci.cancel();
            ItemMap itemMap = (ItemMap)stack.getItem();
            if (this.mc.world != null) {
               MapData mapData = itemMap.getMapData(stack, this.mc.world);
               if (mapData != null) {
                  this.mc.entityRenderer.getMapItemRenderer().updateMapTexture(mapData);
                  GlStateManager.pushMatrix();
                  GlStateManager.disableLighting();
                  GlStateManager.disableDepth();
                  GlStateManager.disableBlend();
                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                  GlStateManager.translate((float)x, (float)y, 0.0F);
                  GlStateManager.scale(1.0F, 1.0F, 0.0F);
                  this.mc.entityRenderer.getMapItemRenderer().renderMap(mapData, false);
                  GlStateManager.enableBlend();
                  GlStateManager.enableLighting();
                  GlStateManager.enableDepth();
                  GlStateManager.popMatrix();
               }
            }

            return;
         }

         if (stack.getItem() instanceof ItemShulkerBox && (Boolean)ContainerPreview.INSTANCE.shulkerPreview.getValue()) {
            ci.cancel();
            NBTTagCompound tagCompound = stack.getTagCompound();
            if (tagCompound != null && tagCompound.hasKey("BlockEntityTag", 10) && tagCompound.getCompoundTag("BlockEntityTag").hasKey("Items", 9)) {
               NonNullList<ItemStack> itemList = NonNullList.withSize(27, ItemStack.EMPTY);
               ItemStackHelper.loadAllItems(tagCompound.getCompoundTag("BlockEntityTag"), itemList);
               GlStateManager.pushMatrix();
               GlStateManager.disableLighting();
               GlStateManager.disableDepth();
               RenderUtil.drawRect((float)x - 2.0F, (float)y, (float)x + 156.0F, (float)y + 54.0F, ContainerPreview.INSTANCE.getColor().hashCode());
               this.mc.fontRenderer.drawString(stack.getDisplayName(), (float)x + 2.0F, (float)(y - this.mc.fontRenderer.FONT_HEIGHT) - 2.0F, -1, true);
               GlStateManager.enableDepth();
               this.mc.getRenderItem().zLevel = 150.0F;
               RenderHelper.enableGUIStandardItemLighting();
               int iX = x;
               int iY = y + 2;
               int j = 0;

               for(ItemStack itemStack : itemList) {
                  this.mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, iX, iY);
                  this.mc.getRenderItem().renderItemOverlayIntoGUI(this.mc.fontRenderer, itemStack, iX, iY, (String)null);
                  iX += 17;
                  ++j;
                  if (j >= 9) {
                     iY += 17;
                     iX = x;
                     j = 0;
                  }
               }

               RenderHelper.disableStandardItemLighting();
               this.mc.getRenderItem().zLevel = 0.0F;
               GlStateManager.enableLighting();
               GlStateManager.enableDepth();
               GlStateManager.popMatrix();
            }
         }
      }

   }
}
