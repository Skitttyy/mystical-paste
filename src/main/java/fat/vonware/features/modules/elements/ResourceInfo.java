package fat.vonware.features.modules.elements;

import fat.vonware.event.events.Render2DEvent;
import fat.vonware.features.gui.newgui.newcomponents.NewVonwareGui;
import fat.vonware.features.modules.HudModule;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Animations.render.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ResourceInfo extends HudModule {
   public Setting<Boolean> autoPosition = new Setting<Boolean>("AutoPos", false);
   public Setting<RenderMode> renderMode;
   public Setting<Boolean> crystals;
   public Setting<Boolean> gapples;
   public Setting<Boolean> exp;
   public Setting<Boolean> obsidian;
   public Setting<Boolean> animate;
   private int crystalCount;
   private int gappleCount;
   private int expCount;
   private int obsidianCount;
   private static final int ITEM_SIZE = 16;
   private static final int ITEM_GAP = 4;

   public ResourceInfo() {
      super("ResourceInfo", "Displays resource counts", Module.Category.Elements, true, false, true, 80.0F, 90.0F);
      this.renderMode = new Setting<RenderMode>("Render Mode", ResourceInfo.RenderMode.Long);
      this.crystals = new Setting<Boolean>("Crystals", true);
      this.gapples = new Setting<Boolean>("Gapples", true);
      this.exp = new Setting<Boolean>("EXP", true);
      this.obsidian = new Setting<Boolean>("Obsidian", false);
      this.animate = new Setting<Boolean>("Animate", true);
      this.crystalCount = 0;
      this.gappleCount = 0;
      this.expCount = 0;
      this.obsidianCount = 0;
      this.register(this.autoPosition);
      this.register(this.renderMode);
      this.register(this.crystals);
      this.register(this.gapples);
      this.register(this.exp);
      this.register(this.obsidian);
      this.register(this.animate);
   }

   public boolean isAutoPositioned() {
      return (Boolean)this.autoPosition.getValue();
   }

   public void onUpdate() {
      if (mc.player != null) {
         this.crystalCount = 0;
         this.gappleCount = 0;
         this.expCount = 0;
         this.obsidianCount = 0;

         for(int i = 0; i < 45; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
               if (stack.getItem() == Items.END_CRYSTAL) {
                  this.crystalCount += stack.getCount();
               }

               if (stack.getItem() == Items.GOLDEN_APPLE && stack.getMetadata() == 1) {
                  this.gappleCount += stack.getCount();
               }

               if (stack.getItem() == Items.EXPERIENCE_BOTTLE) {
                  this.expCount += stack.getCount();
               }

               if (stack.getItem() == Item.getItemFromBlock(Blocks.OBSIDIAN)) {
                  this.obsidianCount += stack.getCount();
               }
            }
         }

      }
   }

   @SubscribeEvent
   public void onRender2D(Render2DEvent event) {
      if (mc.player != null && this.isEnabled()) {
         if ((Boolean)this.autoPosition.getValue()) {
            this.rect.setX(522.0F);
            this.rect.setY(477.0F);
         } else {
            this.rect.setX((Float)this.x.getValue());
            this.rect.setY((Float)this.y.getValue());
         }

         boolean preview = mc.currentScreen instanceof NewVonwareGui && ((NewVonwareGui)mc.currentScreen).isHudEditor();
         int previewCrystals = preview ? 64 : this.crystalCount;
         int previewGapples = preview ? 64 : this.gappleCount;
         int previewExp = preview ? 64 : this.expCount;
         int previewObsidian = preview ? 64 : this.obsidianCount;
         int x = (int)this.rect.getX();
         int y = (int)this.rect.getY();
         if (this.renderMode.getValue() == ResourceInfo.RenderMode.Tall) {
            this.renderTall(x, y, previewCrystals, previewGapples, previewExp, previewObsidian);
         } else if (this.renderMode.getValue() == ResourceInfo.RenderMode.Square) {
            this.renderSquare(x, y, previewCrystals, previewGapples, previewExp, previewObsidian);
         } else {
            this.renderLong(x, y, previewCrystals, previewGapples, previewExp, previewObsidian);
         }

      }
   }

   private void renderSquare(int xPos, int yPos, int c, int g, int e, int o) {
      ItemStack[] stacks = new ItemStack[4];
      int[] counts = new int[4];
      String[] ids = new String[4];
      int slot = 0;
      if ((Boolean)this.crystals.getValue()) {
         stacks[slot] = new ItemStack(Items.END_CRYSTAL);
         counts[slot] = c;
         ids[slot] = "resource_crystal";
         ++slot;
      }

      if ((Boolean)this.gapples.getValue()) {
         stacks[slot] = new ItemStack(Items.GOLDEN_APPLE, 1, 1);
         counts[slot] = g;
         ids[slot] = "resource_gapple";
         ++slot;
      }

      if ((Boolean)this.exp.getValue()) {
         stacks[slot] = new ItemStack(Items.EXPERIENCE_BOTTLE);
         counts[slot] = e;
         ids[slot] = "resource_exp";
         ++slot;
      }

      if ((Boolean)this.obsidian.getValue()) {
         stacks[slot] = new ItemStack(Blocks.OBSIDIAN);
         counts[slot] = o;
         ids[slot] = "resource_obsidian";
         ++slot;
      }

      int total = slot;

      for(int i = 0; i < total; ++i) {
         int col = i % 2;
         int row = i / 2;
         int ix = xPos + col * 20;
         int iy = yPos + row * 20;
         this.drawItemWithCount(stacks[i], counts[i], ix, iy, ids[i]);
      }

      int cols = Math.min(total, 2);
      int rows = (total + 1) / 2;
      int totalW = cols > 0 ? cols * 16 + (cols - 1) * 4 : 0;
      int totalH = rows > 0 ? rows * 16 + (rows - 1) * 4 : 0;
      this.rect.setWidth((float)totalW);
      this.rect.setHeight((float)totalH);
   }

   private void renderTall(int xPos, int yPos, int c, int g, int e, int o) {
      int yOffset = 0;
      if ((Boolean)this.crystals.getValue()) {
         this.drawItemWithCount(new ItemStack(Items.END_CRYSTAL), c, xPos, yPos + yOffset, "resource_crystal");
         yOffset += 20;
      }

      if ((Boolean)this.gapples.getValue()) {
         this.drawItemWithCount(new ItemStack(Items.GOLDEN_APPLE, 1, 1), g, xPos, yPos + yOffset, "resource_gapple");
         yOffset += 20;
      }

      if ((Boolean)this.exp.getValue()) {
         this.drawItemWithCount(new ItemStack(Items.EXPERIENCE_BOTTLE), e, xPos, yPos + yOffset, "resource_exp");
         yOffset += 20;
      }

      if ((Boolean)this.obsidian.getValue()) {
         this.drawItemWithCount(new ItemStack(Blocks.OBSIDIAN), o, xPos, yPos + yOffset, "resource_obsidian");
         yOffset += 20;
      }

      int totalH = yOffset > 0 ? yOffset - 4 : 0;
      this.rect.setWidth(16.0F);
      this.rect.setHeight((float)totalH);
   }

   private void renderLong(int xPos, int yPos, int c, int g, int e, int o) {
      int xOffset = 0;
      if ((Boolean)this.crystals.getValue()) {
         this.drawItemWithCount(new ItemStack(Items.END_CRYSTAL), c, xPos + xOffset, yPos, "resource_crystal");
         xOffset += 20;
      }

      if ((Boolean)this.gapples.getValue()) {
         this.drawItemWithCount(new ItemStack(Items.GOLDEN_APPLE, 1, 1), g, xPos + xOffset, yPos, "resource_gapple");
         xOffset += 20;
      }

      if ((Boolean)this.exp.getValue()) {
         this.drawItemWithCount(new ItemStack(Items.EXPERIENCE_BOTTLE), e, xPos + xOffset, yPos, "resource_exp");
         xOffset += 20;
      }

      if ((Boolean)this.obsidian.getValue()) {
         this.drawItemWithCount(new ItemStack(Blocks.OBSIDIAN), o, xPos + xOffset, yPos, "resource_obsidian");
         xOffset += 20;
      }

      int totalW = xOffset > 0 ? xOffset - 4 : 0;
      this.rect.setWidth((float)totalW);
      this.rect.setHeight(16.0F);
   }

   private void drawItemWithCount(ItemStack stack, int count, int x, int y, String animId) {
      boolean wasDepth = GL11.glIsEnabled(2929);
      boolean wasBlend = GL11.glIsEnabled(3042);
      boolean wasLighting = GL11.glIsEnabled(2896);
      boolean wasRescaleNormal = GL11.glIsEnabled(32826);
      boolean wasAlpha = GL11.glIsEnabled(3008);
      GlStateManager.pushMatrix();
      GlStateManager.enableDepth();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, 0.1F);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      RenderHelper.enableGUIStandardItemLighting();
      stack.setCount(Math.max(1, count));
      mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
      mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, x, y, "");
      RenderHelper.disableStandardItemLighting();
      GlStateManager.disableDepth();
      GlStateManager.disableRescaleNormal();
      GlStateManager.disableAlpha();
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      float tx = (float)(x + 17 - mc.fontRenderer.getStringWidth(String.valueOf(count)));
      float ty = (float)(y + 9);
      if ((Boolean)this.animate.getValue()) {
         RenderUtil.drawAnimatedInt(animId, count, tx, ty, "", -1, true, 1.0F);
      } else {
         mc.fontRenderer.drawStringWithShadow(String.valueOf(count), tx, ty, -1);
      }

      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      if (wasDepth) {
         GlStateManager.enableDepth();
      } else {
         GlStateManager.disableDepth();
      }

      if (wasBlend) {
         GlStateManager.enableBlend();
      } else {
         GlStateManager.disableBlend();
      }

      if (wasLighting) {
         GlStateManager.enableLighting();
      } else {
         GlStateManager.disableLighting();
      }

      if (wasRescaleNormal) {
         GlStateManager.enableRescaleNormal();
      } else {
         GlStateManager.disableRescaleNormal();
      }

      if (wasAlpha) {
         GlStateManager.enableAlpha();
      } else {
         GlStateManager.disableAlpha();
      }

      GlStateManager.popMatrix();
   }

   public void updateBounds() {
      if ((Boolean)this.autoPosition.getValue()) {
         this.rect.setX(522.0F);
         this.rect.setY(477.0F);
      } else {
         this.rect.setX((Float)this.x.getValue());
         this.rect.setY((Float)this.y.getValue());
      }

      int enabledCount = 0;
      if ((Boolean)this.crystals.getValue()) {
         ++enabledCount;
      }

      if ((Boolean)this.gapples.getValue()) {
         ++enabledCount;
      }

      if ((Boolean)this.exp.getValue()) {
         ++enabledCount;
      }

      if ((Boolean)this.obsidian.getValue()) {
         ++enabledCount;
      }

      if (this.renderMode.getValue() == ResourceInfo.RenderMode.Long) {
         int totalW = enabledCount > 0 ? enabledCount * 16 + (enabledCount - 1) * 4 : 0;
         this.rect.setWidth((float)totalW);
         this.rect.setHeight(16.0F);
      } else if (this.renderMode.getValue() == ResourceInfo.RenderMode.Square) {
         int cols = Math.min(enabledCount, 2);
         int rows = (enabledCount + 1) / 2;
         int totalW = cols > 0 ? cols * 16 + (cols - 1) * 4 : 0;
         int totalH = rows > 0 ? rows * 16 + (rows - 1) * 4 : 0;
         this.rect.setWidth((float)totalW);
         this.rect.setHeight((float)totalH);
      } else {
         int totalH = enabledCount > 0 ? enabledCount * 16 + (enabledCount - 1) * 4 : 0;
         this.rect.setWidth(16.0F);
         this.rect.setHeight((float)totalH);
      }

   }

   public static enum RenderMode {
      Tall,
      Long,
      Square;
   }
}
