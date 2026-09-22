package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.modules.client.Manager;
import fat.vonware.features.modules.client.Media;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.DamageUtil;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Interpolation;
import fat.vonware.util.MathUtil;
import fat.vonware.util.PositionUtil;
import fat.vonware.util.TextUtil;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.Render2DMethods;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.Animations.render.RenderUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;

public class NameTags extends Module {
   public static NameTags INSTANCE;
   private static final ResourceLocation USER_ICON = new ResourceLocation("textures/user1.png");
   private static final ResourceLocation DEV_ICON = new ResourceLocation("textures/dev.png");
   private final Setting<Integer> horizontalPadding = new Setting<Integer>("HorizontalPadding", 2, 0, 10);
   private final Setting<Integer> verticalPadding = new Setting<Integer>("VerticalPadding", 0, 0, 10);
   private final Setting<Boolean> armor = new Setting<Boolean>("Armor", true);
   private final Setting<Boolean> enchants = new Setting<Boolean>("Enchants", true);
   private final Setting<Boolean> itemStack = new Setting<Boolean>("ItemStack", true);
   private final Setting<Boolean> durability = new Setting<Boolean>("Durability", true);
   private static final int[] NETHERITE_MAX_DAMAGE = new int[]{481, 555, 592, 407};
   private final Setting<Boolean> ping = new Setting<Boolean>("Ping", true);
   private final Setting<Boolean> decimal = new Setting<Boolean>("DecimalHealth", true);
   private final Setting<Boolean> totemPops = new Setting<Boolean>("TotemPops", true);
   private final Setting<PopColorMode> popColorMode;
   private final ColorSetting pop1Color;
   private final ColorSetting pop2Color;
   private final ColorSetting pop3Color;
   private final ColorSetting pop4Color;
   private final ColorSetting pop5Color;
   private final ColorSetting pop6PlusColor;
   private final Setting<Float> scaling;
   private final Setting<Float> bgOpacity;
   private final Setting<Boolean> outline;
   private final ColorSetting outlineColor;
   private final Setting<Boolean> extraShadow;
   public final Setting<Float> shadowOpacity;
   private final Setting<Boolean> globalColor;
   private final Setting<Boolean> phase;
   private final ColorSetting phaseColor;
   private final Setting<Boolean> optimized;
   protected final List<Block> phaseList;
   public static final Set<String> ADMIN_USERS = new HashSet<String>() {
      {
         this.add("IgnIted_7");
         this.add("CRYSTALPVPSUKUNA");
         this.add("B5m");
         this.add("PuIzarYT");
         this.add("ninethousand");
         this.add("8ma");
         this.add("Neo2008");
      }
   };
   public static final Set<String> DEV_USERS = new HashSet<String>() {
      {
         this.add("BlackRuntz");
         this.add("KingVonsGhost");
         this.add("Tenzinqq");
         this.add("JEWSTOMP3R");
         this.add("CRYSTALPVPNURSE");
         this.add("Kira1986");
         this.add("CRYSTALPVPGOJO");
      }
   };
   public static final Set<String> VONWARE_USERS = new HashSet<String>() {
      {
         this.add("IgnIted_7");
         this.add("donkaypvp");
         this.add("Gh0st_Mafia");
         this.add("ninethousand");
         this.add("KingHolecamp");
         this.add("Misa1984");
         this.add("fazebootypounder");
         this.add("FelixHvH");
         this.add("PuIzarYT");
         this.add("fundzbandz");
         this.add("RegearWarrior");
         this.add("8ma");
         this.add("B5m");
         this.add("1nxanity");
         this.add("CRYSTALPVPGENIUS");
         this.add("wirup_____");
         this.add("Neo2008");
         this.add("bitcoinprize");
         this.add("URGETTINGNERVOUS");
         this.add("SaleemAhmadAyub");
         this.add("seatb4lt");
         this.add("Xormioses");
         this.add("CRYSTALPVPSUKUNA");
         this.add("sosa120");
         this.add("IHackedWalmart");
         this.add("kourako");
         this.add("SHR7");
         this.add("spewki_");
         this.add("173fps");
         this.add("hollowtiphazel");
         this.add("h3artch3rades");
         this.add("xX_BombDog_Xx");
         this.add("EyeTrim");
         this.add("SternGrain");
         this.add("AutoRekt");
      }
   };

   public NameTags() {
      super("NameTags", "player nametags", Module.Category.RENDER, true, false, false);
      this.popColorMode = new Setting<PopColorMode>("PopColor", NameTags.PopColorMode.Default, (v) -> (Boolean)this.totemPops.getValue());
      this.pop1Color = new ColorSetting("Pop1Color", 5635925, false, (v) -> (Boolean)this.totemPops.getValue() && this.popColorMode.getValue() == NameTags.PopColorMode.Custom);
      this.pop2Color = new ColorSetting("Pop2Color", 43520, false, (v) -> (Boolean)this.totemPops.getValue() && this.popColorMode.getValue() == NameTags.PopColorMode.Custom);
      this.pop3Color = new ColorSetting("Pop3Color", 16777045, false, (v) -> (Boolean)this.totemPops.getValue() && this.popColorMode.getValue() == NameTags.PopColorMode.Custom);
      this.pop4Color = new ColorSetting("Pop4Color", 16755200, false, (v) -> (Boolean)this.totemPops.getValue() && this.popColorMode.getValue() == NameTags.PopColorMode.Custom);
      this.pop5Color = new ColorSetting("Pop5Color", 16733525, false, (v) -> (Boolean)this.totemPops.getValue() && this.popColorMode.getValue() == NameTags.PopColorMode.Custom);
      this.pop6PlusColor = new ColorSetting("Pop6+Color", 11141120, false, (v) -> (Boolean)this.totemPops.getValue() && this.popColorMode.getValue() == NameTags.PopColorMode.Custom);
      this.scaling = new Setting<Float>("Scaling", 0.3F, 0.1F, 1.0F);
      this.bgOpacity = new Setting<Float>("BgOpacity", 0.3F, 0.0F, 1.0F);
      this.outline = new Setting<Boolean>("Outline", false);
      this.outlineColor = new ColorSetting("OutlineColor", 16777215, true, (v) -> (Boolean)this.outline.getValue());
      this.extraShadow = new Setting<Boolean>("ExtraShadow", false);
      this.shadowOpacity = new Setting<Float>("ShadowOpacity", 0.5F, 0.0F, 1.0F, (v) -> (Boolean)this.extraShadow.getValue());
      this.globalColor = new Setting<Boolean>("GlobalColor", false);
      this.phase = new Setting<Boolean>("Phase", true);
      this.phaseColor = new ColorSetting("PhaseColor", 7422404, false, (v) -> (Boolean)this.phase.getValue());
      this.optimized = new Setting<Boolean>("OptimizeNametags", false);
      this.phaseList = Arrays.asList(Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BEACON, Blocks.PISTON, Blocks.REDSTONE_BLOCK, Blocks.ENCHANTING_TABLE, Blocks.ANVIL);
      this.register(this.horizontalPadding);
      this.register(this.verticalPadding);
      this.register(this.armor);
      this.register(this.enchants);
      this.register(this.durability);
      this.register(this.itemStack);
      this.register(this.ping);
      this.register(this.decimal);
      this.register(this.outline);
      this.register(this.outlineColor);
      this.register(this.phase);
      this.register(this.phaseColor);
      this.register(this.totemPops);
      this.register(this.popColorMode);
      this.register(this.pop1Color);
      this.register(this.pop2Color);
      this.register(this.pop3Color);
      this.register(this.pop4Color);
      this.register(this.pop5Color);
      this.register(this.pop6PlusColor);
      this.register(this.scaling);
      this.register(this.bgOpacity);
      this.register(this.extraShadow);
      this.register(this.shadowOpacity);
      this.register(this.globalColor);
      this.register(this.optimized);
      INSTANCE = this;
   }

   public void onRender3D(Render3DEvent event) {
      Entity renderEntity = RenderMethods.getEntity();
      Frustum frustum = Interpolation.createFrustum(renderEntity);
      Vec3d interp = Interpolation.interpolateEntity(renderEntity);
      List<EntityPlayer> playerList = new ArrayList(mc.world.playerEntities);
      playerList.sort(Comparator.comparing((playerx) -> mc.player.getDistance(playerx)));

      for(EntityPlayer player : playerList) {
         if (player != renderEntity && !EntityUtil.isDead(player)) {
            AxisAlignedBB bb = player.getEntityBoundingBox();
            boolean inFrustum = frustum.isBoundingBoxInFrustum(bb.expand((double)0.75F, (double)0.75F, (double)0.75F));
            if ((inFrustum || !(Boolean)this.optimized.getValue()) && inFrustum) {
               Vec3d vec = Interpolation.interpolateEntity(player);
               this.renderNameTag(player, vec.x, vec.y, vec.z, interp);
            }
         }
      }

   }

   private void renderNameTag(EntityPlayer player, double x, double y, double z, Vec3d mcPlayerInterpolation) {
      double tempY = y + (player.isSneaking() ? (double)0.5F : 0.7);
      double xDist = mcPlayerInterpolation.x - x;
      double yDist = mcPlayerInterpolation.y - y;
      double zDist = mcPlayerInterpolation.z - z;
      y = (double)MathHelper.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
      String displayName = player.getDisplayName().getFormattedText();
      if (Media.INSTANCE != null && Media.INSTANCE.isEnabled() && player == mc.player) {
         displayName = Media.INSTANCE.newName.getValue();
      }

      double health = (double)EntityUtil.getHealth(player);
      String pingStr = "";
      if ((Boolean)this.ping.getValue()) {
         try {
            int responseTime = ((NetHandlerPlayClient)Objects.requireNonNull(mc.getConnection())).getPlayerInfo(player.getUniqueID()).getResponseTime();
            pingStr = " " + responseTime + "ms";
         } catch (Exception var60) {
         }
      }

      String healthColor;
      if (health > (double)18.0F) {
         healthColor = TextFormatting.GREEN.toString();
      } else if (health > (double)16.0F) {
         healthColor = TextFormatting.DARK_GREEN.toString();
      } else if (health > (double)12.0F) {
         healthColor = TextFormatting.YELLOW.toString();
      } else if (health > (double)8.0F) {
         healthColor = TextFormatting.GOLD.toString();
      } else if (health > (double)5.0F) {
         healthColor = TextFormatting.RED.toString();
      } else {
         healthColor = TextFormatting.DARK_RED.toString();
      }

      String healthStr;
      if ((Boolean)this.decimal.getValue()) {
         healthStr = String.format("%.1f", health).replaceAll("\\.0$", "");
      } else {
         int roundedHealth = (int)Math.ceil(health);
         if (roundedHealth == 0 && health > (double)0.0F) {
            roundedHealth = 1;
         }

         healthStr = String.valueOf(roundedHealth);
      }

      int popCount = 0;
      String popStr = "";
      if ((Boolean)this.totemPops.getValue()) {
         popCount = Vonware.totemPopManager.getPopCount(player.getName());
         if (popCount > 0) {
            popStr = " -" + popCount;
            if (this.popColorMode.getValue() != NameTags.PopColorMode.Custom) {
               popStr = this.getPopFormatting(popCount) + popStr;
            }
         }
      }

      float iconSize = (float)(mc.fontRenderer.FONT_HEIGHT - 1);
      int iconWidth = 0;
      String nameAndPing = displayName + pingStr + " ";
      String healthText = healthColor + healthStr;
      boolean isVonwareUser = isVonwareUser(player.getName());
      boolean isDevUser = isDevUser(player.getName());
      int devIconWidth = isDevUser ? (int)iconSize + 2 : 0;
      int userIconWidth = isVonwareUser ? (int)iconSize + 2 : 0;
      int nameAndPingWidth = Vonware.textManager.getStringWidth(nameAndPing);
      int healthWidth = Vonware.textManager.getStringWidth(healthText);
      int popWidth = Vonware.textManager.getStringWidth(popStr);
      int totalTextWidth = devIconWidth + userIconWidth + nameAndPingWidth + iconWidth + healthWidth + popWidth;
      int totalWidth = totalTextWidth / 2;
      double scale = 0.0018 + (double)MathUtil.fixedNametagScaling((Float)this.scaling.getValue()) * y;
      if (y <= (double)8.0F) {
         scale = 0.0018 + (double)MathUtil.fixedNametagScaling((Float)this.scaling.getValue()) * (double)8.0F;
      }

      GlStateManager.pushMatrix();
      RenderHelper.enableStandardItemLighting();
      GlStateManager.enablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, -1500000.0F);
      GlStateManager.disableLighting();
      GlStateManager.translate((float)x, (float)tempY + 1.4F, (float)z);
      GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      float xRot = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
      GlStateManager.rotate(mc.getRenderManager().playerViewX, xRot, 0.0F, 0.0F);
      GlStateManager.scale(-scale, -scale, scale);
      GlStateManager.disableDepth();
      GlStateManager.enableBlend();
      int nameX = -totalWidth;
      int userIconX = nameX + devIconWidth;
      int actualNameX = userIconX + userIconWidth;
      int healthX = actualNameX + nameAndPingWidth;
      int popX = healthX + healthWidth;
      int padding = (Integer)this.horizontalPadding.getValue();
      int verticalPadding = (Integer)this.verticalPadding.getValue();
      int alpha = (int)((Float)this.bgOpacity.getValue() * 255.0F);
      int bgColor;
      if ((Boolean)this.globalColor.getValue()) {
         Color globalCol = Global.getGlobalColor();
         bgColor = alpha << 24 | globalCol.getRed() << 16 | globalCol.getGreen() << 8 | globalCol.getBlue();
      } else {
         bgColor = alpha << 24 | 0;
      }

      if ((Boolean)this.extraShadow.getValue()) {
         RenderUtil.drawBlurredRect(-totalWidth - padding, -(mc.fontRenderer.FONT_HEIGHT + verticalPadding), totalWidth + padding, 1 + verticalPadding, bgColor, (Boolean)this.globalColor.getValue());
      } else {
         Render2DMethods.drawRect((float)(-totalWidth - padding), (float)(-(mc.fontRenderer.FONT_HEIGHT + verticalPadding)), (float)(totalWidth + padding), (float)(1 + verticalPadding), bgColor);
      }

      if ((Boolean)this.outline.getValue()) {
         int x1 = -totalWidth - padding;
         int y1 = -(mc.fontRenderer.FONT_HEIGHT + verticalPadding);
         int x2 = totalWidth + padding;
         int y2 = 1 + verticalPadding;
         int oColor;
         if (Vonware.friendManager.isFriend(player.getName())) {
            oColor = Colors.getInstance().getFriendColorARGB();
         } else {
            oColor = ColorUtil.getResolvedColor(this.outlineColor).getRGB() | -16777216;
         }

         GL11.glLineWidth(1.0F);
         Render2DMethods.quickDrawRect((float)x1, (float)y1, (float)x2, (float)y2, oColor, true);
         GL11.glLineWidth(1.0F);
      }

      if (isVonwareUser) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         mc.getTextureManager().bindTexture(USER_ICON);
         GlStateManager.enableBlend();
         GlStateManager.enableAlpha();
         float iconY = (float)(-(mc.fontRenderer.FONT_HEIGHT - 1));
         Tessellator userTessellator = Tessellator.getInstance();
         BufferBuilder userBuffer = userTessellator.getBuffer();
         userBuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
         userBuffer.pos((double)userIconX, (double)(iconY + iconSize), (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
         userBuffer.pos((double)((float)userIconX + iconSize), (double)(iconY + iconSize), (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
         userBuffer.pos((double)((float)userIconX + iconSize), (double)iconY, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
         userBuffer.pos((double)userIconX, (double)iconY, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
         userTessellator.draw();
      }

      if (isDevUser) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         mc.getTextureManager().bindTexture(DEV_ICON);
         GlStateManager.enableBlend();
         GlStateManager.enableAlpha();
         float iconY = (float)(-(mc.fontRenderer.FONT_HEIGHT - 1));
         Tessellator devTessellator = Tessellator.getInstance();
         BufferBuilder devBuffer = devTessellator.getBuffer();
         devBuffer.begin(7, DefaultVertexFormats.POSITION_TEX);
         devBuffer.pos((double)nameX, (double)(iconY + iconSize), (double)0.0F).tex((double)0.0F, (double)1.0F).endVertex();
         devBuffer.pos((double)((float)nameX + iconSize), (double)(iconY + iconSize), (double)0.0F).tex((double)1.0F, (double)1.0F).endVertex();
         devBuffer.pos((double)((float)nameX + iconSize), (double)iconY, (double)0.0F).tex((double)1.0F, (double)0.0F).endVertex();
         devBuffer.pos((double)nameX, (double)iconY, (double)0.0F).tex((double)0.0F, (double)0.0F).endVertex();
         devTessellator.draw();
      }

      int nameColor = this.getNameColor(player);
      Vonware.textManager.drawStringWithShadow(nameAndPing, (float)actualNameX, (float)(-(mc.fontRenderer.FONT_HEIGHT - 1)), nameColor);
      Vonware.textManager.drawStringWithShadow(healthText, (float)healthX, (float)(-(mc.fontRenderer.FONT_HEIGHT - 1)), -1);
      if (!popStr.isEmpty()) {
         int popColor = this.popColorMode.getValue() == NameTags.PopColorMode.Custom ? this.getCustomPopColor(popCount) : -1;
         Vonware.textManager.drawStringWithShadow(popStr, (float)popX, (float)(-(mc.fontRenderer.FONT_HEIGHT - 1)), popColor);
      }

      if ((Boolean)this.armor.getValue()) {
         GlStateManager.pushMatrix();
         ItemStack heldItemMainhand = player.getHeldItemMainhand();
         ItemStack heldItemOffhand = player.getHeldItemOffhand();
         int xOffset = 0;
         int enchantOffset = 0;

         for(int i = 3; i >= 0; --i) {
            ItemStack itemStack = (ItemStack)player.inventory.armorInventory.get(i);
            if (!itemStack.isEmpty()) {
               xOffset -= 8;
               int size = EnchantmentHelper.getEnchantments(itemStack).size();
               if ((Boolean)this.enchants.getValue() && size > enchantOffset) {
                  enchantOffset = size;
               }
            }
         }

         if (!heldItemOffhand.isEmpty()) {
            xOffset -= 8;
            int size = EnchantmentHelper.getEnchantments(heldItemOffhand).size();
            if ((Boolean)this.enchants.getValue() && size > enchantOffset) {
               enchantOffset = size;
            }
         }

         if (!heldItemMainhand.isEmpty()) {
            xOffset -= 8;
            int size = EnchantmentHelper.getEnchantments(heldItemMainhand).size();
            if ((Boolean)this.enchants.getValue() && size > enchantOffset) {
               enchantOffset = size;
            }
         }

         int armorOffset = this.getOffset(enchantOffset);
         int durabilityOffset = armorOffset - 32;
         if (!heldItemMainhand.isEmpty()) {
            this.renderStack(heldItemMainhand, xOffset, armorOffset, enchantOffset);
            if ((Boolean)this.durability.getValue() && heldItemMainhand.isItemStackDamageable()) {
               this.renderDurability(heldItemMainhand, (float)xOffset, (float)durabilityOffset, -1);
            }

            if ((Boolean)this.itemStack.getValue()) {
               this.renderText(heldItemMainhand, (float)(durabilityOffset - ((Boolean)this.durability.getValue() ? 10 : 2)));
            }

            xOffset += 16;
         }

         for(int i = 3; i >= 0; --i) {
            ItemStack itemStack = (ItemStack)player.inventory.armorInventory.get(i);
            if (!itemStack.isEmpty()) {
               this.renderStack(itemStack, xOffset, armorOffset, enchantOffset);
               if ((Boolean)this.durability.getValue() && itemStack.isItemStackDamageable()) {
                  this.renderDurability(itemStack, (float)xOffset, (float)durabilityOffset, i);
               }

               xOffset += 16;
            }
         }

         if (!heldItemOffhand.isEmpty()) {
            this.renderStack(heldItemOffhand, xOffset, armorOffset, enchantOffset);
            if ((Boolean)this.durability.getValue() && heldItemOffhand.isItemStackDamageable()) {
               this.renderDurability(heldItemOffhand, (float)xOffset, (float)durabilityOffset, -1);
            }
         }

         GlStateManager.popMatrix();
      }

      GlStateManager.enableDepth();
      GlStateManager.disableBlend();
      GlStateManager.disablePolygonOffset();
      GlStateManager.doPolygonOffset(1.0F, 1500000.0F);
      GlStateManager.popMatrix();
   }

   private int getItemNameColor(ItemStack stack) {
      if (stack.getItem() != Items.DIAMOND_SWORD && stack.getItem() != Items.DIAMOND_PICKAXE) {
         return stack.getItem() == Items.GOLDEN_APPLE && stack.getMetadata() == 1 ? -16711681 : -1;
      } else {
         return -16711681;
      }
   }

   private void renderText(ItemStack stack, float y) {
      GlStateManager.scale(0.5F, 0.5F, 0.5F);
      GlStateManager.disableDepth();
      String name = stack.getDisplayName();
      Vonware.textManager.drawStringWithShadow(name, (float)(-Vonware.textManager.getStringWidth(name) >> 1), y, this.getItemNameColor(stack));
      GlStateManager.enableDepth();
      GlStateManager.scale(2.0F, 2.0F, 2.0F);
   }

   private String getPopFormatting(int pops) {
      switch ((PopColorMode)this.popColorMode.getValue()) {
         case Cartel:
            return this.getCartelPop(pops);
         case Default:
            return this.getDefaultPop(pops);
         case White:
         case Custom:
         default:
            return TextFormatting.WHITE.toString();
      }
   }

   private int getCustomPopColor(int pops) {
      Color color;
      if (pops <= 1) {
         color = ColorUtil.getResolvedColor(this.pop1Color);
      } else if (pops == 2) {
         color = ColorUtil.getResolvedColor(this.pop2Color);
      } else if (pops == 3) {
         color = ColorUtil.getResolvedColor(this.pop3Color);
      } else if (pops == 4) {
         color = ColorUtil.getResolvedColor(this.pop4Color);
      } else if (pops == 5) {
         color = ColorUtil.getResolvedColor(this.pop5Color);
      } else {
         color = ColorUtil.getResolvedColor(this.pop6PlusColor);
      }

      return color.getRGB() | -16777216;
   }

   private String getCartelPop(int pops) {
      if (pops == 1) {
         return TextFormatting.AQUA.toString();
      } else if (pops == 2) {
         return TextFormatting.BLUE.toString();
      } else if (pops == 3) {
         return TextFormatting.DARK_AQUA.toString();
      } else {
         return pops >= 4 ? TextFormatting.DARK_BLUE.toString() : TextFormatting.DARK_BLUE.toString();
      }
   }

   private String getDefaultPop(int pops) {
      if (pops == 1) {
         return TextFormatting.GREEN.toString();
      } else if (pops == 2) {
         return TextFormatting.DARK_GREEN.toString();
      } else if (pops == 3) {
         return TextFormatting.YELLOW.toString();
      } else if (pops == 4) {
         return TextFormatting.GOLD.toString();
      } else {
         return pops == 5 ? TextFormatting.RED.toString() : TextFormatting.DARK_RED.toString();
      }
   }

   private int getOffset(int offset) {
      int fixedOffset = -26;
      if (offset > 4) {
         fixedOffset -= (offset - 4) * 8;
      }

      return fixedOffset;
   }

   protected void renderStack(ItemStack stack, int x, int y, int enchHeight) {
      GlStateManager.pushMatrix();
      GlStateManager.depthMask(true);
      GlStateManager.clear(256);
      RenderHelper.enableStandardItemLighting();
      mc.getRenderItem().zLevel = -150.0F;
      GlStateManager.disableAlpha();
      GlStateManager.enableDepth();
      GlStateManager.disableCull();
      int height = enchHeight > 4 ? (enchHeight - 4) * 8 / 2 : 0;
      mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y + height);
      mc.getRenderItem().renderItemOverlays(mc.fontRenderer, stack, x, y + height);
      mc.getRenderItem().zLevel = 0.0F;
      RenderHelper.disableStandardItemLighting();
      GlStateManager.enableCull();
      GlStateManager.enableAlpha();
      if ((Boolean)this.enchants.getValue()) {
         GlStateManager.scale(0.5F, 0.5F, 0.5F);
         GlStateManager.disableDepth();
         this.renderEnchants(stack, x, y - 24);
         GlStateManager.enableDepth();
         GlStateManager.scale(2.0F, 2.0F, 2.0F);
      }

      GlStateManager.popMatrix();
   }

   private void renderEnchants(ItemStack stack, int xOffset, int yOffset) {
      Set<Enchantment> e = EnchantmentHelper.getEnchantments(stack).keySet();
      List<String> enchantTexts = new ArrayList(e.size());

      for(Enchantment enchantment : e) {
         enchantTexts.add(this.getEnchantText(enchantment, EnchantmentHelper.getEnchantmentLevel(enchantment, stack)));
      }

      for(String enchantment2 : enchantTexts) {
         if (enchantment2 != null) {
            Vonware.textManager.drawStringWithShadow(TextUtil.capitalize(enchantment2), (float)xOffset * 2.0F, (float)yOffset, -1);
            yOffset += 8;
         }
      }

      if (stack.getItem().equals(Items.GOLDEN_APPLE) && stack.hasEffect()) {
         Vonware.textManager.drawStringWithShadow("God", (float)xOffset * 2.0F, (float)yOffset, (new Color(195, 73, 74)).getRGB());
      }

   }

   private String getEnchantText(Enchantment ench, int lvl) {
      ResourceLocation resource = (ResourceLocation)Enchantment.REGISTRY.getNameForObject(ench);
      String name = resource == null ? ench.getName() : resource.toString();
      int lvlOffset = lvl > 1 ? 12 : 13;
      if (name.length() > lvlOffset) {
         name = name.substring(10, lvlOffset);
      }

      if (lvl > 1) {
         name = name + lvl;
      }

      return name.length() < 2 ? name : TextUtil.getFixedName(name);
   }

   private int getNameColor(EntityPlayer player) {
      if (player == mc.player) {
         return Colors.getInstance().getFriendColorARGB();
      } else if (Vonware.friendManager.isFriend(player.getName())) {
         return Colors.getInstance().getFriendColorARGB();
      } else {
         if ((Boolean)this.phase.getValue()) {
            BlockPos pos = PositionUtil.getPosition(player);
            IBlockState state = mc.world.getBlockState(pos);
            if (this.phaseList.contains(state.getBlock()) && state.getBoundingBox(mc.world, pos).offset(pos).maxY > player.posY) {
               return this.getBurrowColor().getRGB();
            }
         }

         return -1;
      }
   }

   protected Color getBurrowColor() {
      return ColorUtil.getResolvedColor(this.phaseColor);
   }

   protected void renderDurability(ItemStack stack, float x, float y, int slot) {
      boolean useNetherite = (Boolean)Manager.getInstance().calcNetherite.getValue() && slot >= 0 && slot <= 3;
      int percent = (int)(useNetherite ? this.getNetheriteDamageInPercent(stack, slot) : DamageUtil.getDamageInPercent(stack));
      int color = useNetherite ? this.getNetheriteColor(stack, slot) : stack.getItem().getRGBDurabilityForDisplay(stack);
      GlStateManager.scale(0.5F, 0.5F, 0.5F);
      GlStateManager.disableDepth();
      Vonware.textManager.drawStringWithShadow(percent + "%", x * 2.0F, y, color);
      GlStateManager.enableDepth();
      GlStateManager.scale(2.0F, 2.0F, 2.0F);
   }

   private double getNetheriteDamageInPercent(ItemStack stack, int slot) {
      int maxDamage = NETHERITE_MAX_DAMAGE[MathHelper.clamp(slot, 0, 3)];
      double percent = (double)stack.getItemDamage() / (double)maxDamage;
      if (percent <= (double)0.0F) {
         return (double)100.0F;
      } else {
         return percent >= (double)1.0F ? (double)0.0F : (double)100.0F - percent * (double)100.0F;
      }
   }

   private int getNetheriteColor(ItemStack stack, int slot) {
      int maxDamage = NETHERITE_MAX_DAMAGE[MathHelper.clamp(slot, 0, 3)];
      float fraction = MathHelper.clamp((float)stack.getItemDamage() / (float)maxDamage, 0.0F, 1.0F);
      return MathHelper.hsvToRGB(Math.max(0.0F, 1.0F - fraction) / 3.0F, 1.0F, 1.0F);
   }

   public static boolean isAdminUser(String username) {
      return ADMIN_USERS.contains(username);
   }

   public static boolean isVonwareUser(String username) {
      return VONWARE_USERS.contains(username);
   }

   public static boolean isDevUser(String username) {
      return DEV_USERS.contains(username);
   }

   public static enum PopColorMode {
      White,
      Cartel,
      Default,
      Custom;
   }
}
