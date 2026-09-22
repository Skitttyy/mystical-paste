package fat.vonware.features.modules.misc;

import com.mojang.realmsclient.gui.ChatFormatting;
import fat.vonware.Vonware;
import fat.vonware.features.command.Command;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.player.FastProjectile;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.Timer;
import java.awt.Color;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class MiddleClick extends Module {
   public Setting<Boolean> pearl = new Setting<Boolean>("Pearl", true, "Throws an ender pearl when using the middle-click action.");
   public Setting<Boolean> entityPass = new Setting<Boolean>("EntityPass", false, (v) -> (Boolean)this.pearl.getValue(), "Allows pearl targeting to pass through selected entities that would normally block the click.");
   public Setting<Boolean> crystalPass = new Setting<Boolean>("Crystal", true, (v) -> (Boolean)this.pearl.getValue() && (Boolean)this.entityPass.getValue(), "Allows pearl targeting to pass through end crystals.");
   public Setting<Boolean> playerPass = new Setting<Boolean>("Player", true, (v) -> (Boolean)this.pearl.getValue() && (Boolean)this.entityPass.getValue(), "Allows pearl targeting to pass through players.");
   public Setting<Boolean> charge = new Setting<Boolean>("Charge", true, (v) -> (Boolean)this.pearl.getValue(), "Requires the pearl action to be held for a short charge time before throwing.");
   public Setting<Float> chargeTime = new Setting<Float>("Charge Time", 1.0F, 0.6F, 3.0F, (v) -> (Boolean)this.charge.getValue(), "Amount of time the pearl action must be held before the pearl is thrown.");
   public Setting<Boolean> friend = new Setting<Boolean>("Friend", true, "Adds or removes the player under your crosshair as a friend with middle click.");
   public Setting<Boolean> xp = new Setting<Boolean>("XP", false, "Throws experience bottles with the middle-click action.");
   public Setting<Integer> xpDelay = new Setting<Integer>("XP Delay", 1, 0, 5, (v) -> (Boolean)this.xp.getValue(), "Delay between experience-bottle uses.");
   public Setting<Boolean> obsidian = new Setting<Boolean>("Obsidian", false, "Places obsidian with the middle-click action while holding left shift.");
   private final Timer xpTimer = new Timer();
   private boolean clicked = false;
   private boolean xpFlag = false;
   public boolean chargeStarted = false;
   public long chargeStart = 0L;
   private boolean obsidianClicked = false;
   private static final long PHASE1_MS = 500L;
   private static final long DOT_INTERVAL_MS = 400L;
   private static final long FADE_MS = 600L;
   private int dotCount = 0;
   private long lastDotTick = 0L;
   private boolean wasReady = false;
   private long readyStart = 0L;

   public MiddleClick() {
      super("MiddleClick", "Middle click", Module.Category.MISC, true, false, false);
      this.register(this.pearl);
      this.register(this.entityPass);
      this.register(this.crystalPass);
      this.register(this.playerPass);
      this.register(this.charge);
      this.register(this.chargeTime);
      this.register(this.friend);
      this.register(this.xp);
      this.register(this.xpDelay);
      this.register(this.obsidian);
   }

   private long getChargeTotalMs() {
      return (long)((Float)this.chargeTime.getValue() * 1000.0F);
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if (mc.currentScreen == null) {
            if ((Boolean)this.obsidian.getValue() && this.isShiftDown() && Mouse.isButtonDown(0)) {
               if (!this.obsidianClicked) {
                  this.handleObsidianPlace();
                  this.obsidianClicked = true;
               }
            } else if (!Mouse.isButtonDown(0)) {
               this.obsidianClicked = false;
            }

            if (Mouse.isButtonDown(2)) {
               if ((Boolean)this.pearl.getValue() && (Boolean)this.charge.getValue() && !this.xpFlag && !this.chargeStarted) {
                  this.chargeStart = System.currentTimeMillis();
                  this.chargeStarted = true;
                  this.dotCount = 0;
                  this.lastDotTick = System.currentTimeMillis();
                  this.wasReady = false;
               }

               if (!this.clicked) {
                  if (this.chargeStarted) {
                     return;
                  }

                  if ((Boolean)this.xp.getValue()) {
                     this.throwExp();
                     this.xpFlag = true;
                  }

                  if ((Boolean)this.friend.getValue()) {
                     this.handleFriend();
                  }

                  if ((Boolean)this.pearl.getValue() && !(Boolean)this.charge.getValue() && this.allowPearl()) {
                     this.handlePearl(false, false);
                  }

                  this.clicked = true;
               }
            } else {
               if ((Boolean)this.pearl.getValue() && (Boolean)this.charge.getValue() && this.chargeStarted) {
                  long elapsed = System.currentTimeMillis() - this.chargeStart;
                  if (elapsed < 500L) {
                     this.handlePearl(false, false);
                  } else if (elapsed < this.getChargeTotalMs()) {
                     this.handlePearl(false, true);
                  } else {
                     this.handlePearl(true, false);
                  }
               }

               this.clicked = false;
               this.xpFlag = false;
               this.chargeStarted = false;
            }

         }
      }
   }

   @SubscribeEvent
   public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
      if (event.getType() == ElementType.CROSSHAIRS) {
         if (this.chargeStarted) {
            if (!fullNullCheck()) {
               ScaledResolution sr = new ScaledResolution(mc);
               float cx = (float)sr.getScaledWidth() / 2.0F;
               float cy = (float)sr.getScaledHeight() / 2.0F;
               long elapsed = System.currentTimeMillis() - this.chargeStart;
               long total = this.getChargeTotalMs();
               Math.min(1.0F, (float)(elapsed - 500L) / (float)(total - 500L));
               boolean pastPhase1 = elapsed >= 500L;
               boolean fullyCharged = elapsed >= total;
               this.renderTextMode(cx, cy, pastPhase1, fullyCharged);
            }
         }
      }
   }

   private void renderTextMode(float cx, float cy, boolean pastPhase1, boolean fullyCharged) {
      if (pastPhase1) {
         float textY = cy + 16.0F;
         if (fullyCharged) {
            if (!this.wasReady) {
               this.wasReady = true;
               this.readyStart = System.currentTimeMillis();
            }

            float t = Math.min(1.0F, (float)(System.currentTimeMillis() - this.readyStart) / 600.0F);
            int r = (int)(255.0F * (1.0F - t));
            int g = (int)(200.0F * t);
            this.drawCenteredText("Ready!", cx, textY, (new Color(r, g, 0, 255)).getRGB());
         } else {
            this.wasReady = false;
            long now = System.currentTimeMillis();
            if (now - this.lastDotTick >= 400L) {
               this.dotCount = (this.dotCount + 1) % 4;
               this.lastDotTick = now;
            }

            StringBuilder dots = new StringBuilder();

            for(int i = 0; i < this.dotCount; ++i) {
               dots.append(".");
            }

            this.drawCenteredText("Charging" + dots, cx, textY, (new Color(220, 50, 50, 255)).getRGB());
         }

      }
   }

   private void drawCenteredText(String text, float cx, float y, int color) {
      GlStateManager.disableBlend();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      int w = mc.fontRenderer.getStringWidth(text);
      mc.fontRenderer.drawStringWithShadow(text, cx - (float)w / 2.0F, y, color);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void handlePearl(boolean useFastProjectile, boolean failed) {
      if (failed) {
         Command.sendMessage(ChatFormatting.RED + "Failed! Release when Ready.");
      } else {
         int pearlSlot = InventoryUtil.getItemFromHotbar(Items.ENDER_PEARL);
         if (pearlSlot != -1) {
            FastProjectile fp = (FastProjectile)Vonware.moduleManager.getModuleByClass(FastProjectile.class);
            int lastSlot = mc.player.inventory.currentItem;
            if (useFastProjectile) {
               mc.player.connection.sendPacket(new CPacketHeldItemChange(pearlSlot));
               if (fp != null && fp.isEnabled()) {
                  fp.suppressEvent = true;
                  fp.doTheWork();
                  fp.suppressEvent = false;
               }

               mc.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
               mc.player.connection.sendPacket(new CPacketHeldItemChange(lastSlot));
            } else {
               mc.player.inventory.currentItem = pearlSlot;
               mc.player.connection.sendPacket(new CPacketHeldItemChange(pearlSlot));
               if (fp != null && fp.isEnabled() && (Boolean)this.charge.getValue()) {
                  fp.suppressEvent = true;
               }

               mc.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
               if (fp != null) {
                  fp.suppressEvent = false;
               }

               mc.player.connection.sendPacket(new CPacketHeldItemChange(lastSlot));
               mc.player.inventory.currentItem = lastSlot;
            }

         }
      }
   }

   private void throwExp() {
      int oldSlot = mc.player.inventory.currentItem;
      int newSlot = InventoryUtil.findHotbarBlock(ItemExpBottle.class);
      if (newSlot != -1) {
         if (this.xpTimer.passedMs((long)(Integer)this.xpDelay.getValue() * 1000L)) {
            mc.player.connection.sendPacket(new CPacketHeldItemChange(newSlot));
            mc.player.connection.sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
            mc.player.connection.sendPacket(new CPacketHeldItemChange(oldSlot));
            this.xpTimer.reset();
         }
      }
   }

   private void handleFriend() {
      RayTraceResult rtr = mc.objectMouseOver;
      if (rtr != null && rtr.typeOfHit == Type.ENTITY) {
         if (rtr.entityHit instanceof EntityPlayer) {
            String name = rtr.entityHit.getName();
            if (Vonware.friendManager.isFriend(name)) {
               Vonware.friendManager.removeFriend(name);
               Command.sendMessage(ChatFormatting.RED + name + " has been unfriended.");
            } else {
               Vonware.friendManager.addFriend(name);
               Command.sendMessage(ChatFormatting.AQUA + name + " has been friended.");
            }

         }
      }
   }

   private boolean allowPearl() {
      RayTraceResult result = mc.objectMouseOver;
      if (result != null && result.typeOfHit != Type.MISS) {
         if ((Boolean)this.entityPass.getValue() && result.typeOfHit == Type.ENTITY && result.entityHit != null) {
            if (result.entityHit instanceof EntityEnderCrystal) {
               return (Boolean)this.crystalPass.getValue();
            } else {
               return result.entityHit instanceof EntityPlayer ? (Boolean)this.playerPass.getValue() : false;
            }
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   private boolean isShiftDown() {
      return Keyboard.isKeyDown(42) || Keyboard.isKeyDown(54);
   }

   private void handleObsidianPlace() {
      RayTraceResult rtr = mc.objectMouseOver;
      if (rtr != null && rtr.typeOfHit == Type.BLOCK) {
         Item obsidianItem = Item.getItemFromBlock(Blocks.OBSIDIAN);
         int obsSlot = InventoryUtil.getItemFromHotbar(obsidianItem);
         if (obsSlot != -1) {
            int lastSlot = mc.player.inventory.currentItem;
            BlockPos pos = rtr.getBlockPos();
            EnumFacing side = rtr.sideHit;
            float hitX = (float)(rtr.hitVec.x - (double)pos.getX());
            float hitY = (float)(rtr.hitVec.y - (double)pos.getY());
            float hitZ = (float)(rtr.hitVec.z - (double)pos.getZ());
            mc.player.inventory.currentItem = obsSlot;
            mc.player.connection.sendPacket(new CPacketHeldItemChange(obsSlot));
            mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(pos, side, EnumHand.MAIN_HAND, hitX, hitY, hitZ));
            mc.player.connection.sendPacket(new CPacketHeldItemChange(lastSlot));
            mc.player.inventory.currentItem = lastSlot;
         }
      }
   }

   public static enum RenderMode {
      CIRCLE,
      TEXT;
   }
}
