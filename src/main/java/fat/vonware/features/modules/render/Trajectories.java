package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.gui.components.items.buttons.render.ColorPickerConfig;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.ColorSetting;
import fat.vonware.features.setting.Setting;
import fat.vonware.util.Interpolation;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemLingeringPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemSplashPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Cylinder;
import org.lwjgl.util.glu.Disk;

public class Trajectories extends Module {
   protected final Setting<RenderMode> renderMode;
   public Setting<Boolean> landing;
   public Setting<Float> lineWidth;
   public Setting<Float> diskRadius;
   public Setting<Integer> diskAlpha;
   private final ColorSetting color;
   private final Setting<Integer> alpha;

   public Trajectories() {
      super("Trajectories", "Shows a Render on projectiles", Module.Category.RENDER, true, false, false);
      this.renderMode = new Setting<RenderMode>("RenderMode", Trajectories.RenderMode.New);
      this.landing = new Setting<Boolean>("Landing", true);
      this.lineWidth = new Setting<Float>("LineWidth", 2.0F, 0.1F, 5.0F);
      this.diskRadius = new Setting<Float>("DiskRadius", 0.3F, 0.1F, 1.0F, (v) -> this.renderMode.getValue() == Trajectories.RenderMode.New);
      this.diskAlpha = new Setting<Integer>("DiskAlpha", 100, 0, 255, (v) -> this.renderMode.getValue() == Trajectories.RenderMode.New && (Boolean)this.landing.getValue());
      this.color = new ColorSetting("Color", 16777215, true);
      this.alpha = new Setting<Integer>("Alpha", 255, 0, 255);
      this.register(this.renderMode);
      this.register(this.landing);
      this.register(this.lineWidth);
      this.register(this.diskRadius);
      this.register(this.diskAlpha);
      this.register(this.color);
      this.register(this.alpha);
   }

   private Color getColor() {
      if (ColorPickerConfig.getState(this.color).useGlobalColor) {
         return ColorUtil.changeAlpha(Global.getGlobalColorRainbowS(), (Integer)this.alpha.getValue());
      } else {
         int colorInt = (Integer)this.color.getValue();
         return new Color(colorInt >> 16 & 255, colorInt >> 8 & 255, colorInt & 255, (Integer)this.alpha.getValue());
      }
   }

   public void onRender3D(Render3DEvent event) {
      if (this.renderMode.getValue() == Trajectories.RenderMode.New) {
         this.renderNewMode(event);
      } else {
         this.renderOldMode(event);
      }

   }

   private void renderNewMode(Render3DEvent event) {
      if (mc.player != null && mc.world != null) {
         Item item = null;
         if (mc.player.getHeldItemMainhand() == ItemStack.EMPTY || !(mc.player.getHeldItemMainhand().getItem() instanceof ItemBow) && !this.isThrowable(mc.player.getHeldItemMainhand().getItem())) {
            if (mc.player.getHeldItemOffhand() != ItemStack.EMPTY && this.isThrowable(mc.player.getHeldItemOffhand().getItem())) {
               item = mc.player.getHeldItemOffhand().getItem();
            }
         } else {
            item = mc.player.getHeldItemMainhand().getItem();
         }

         if (item != null) {
            if (!(item instanceof ItemBow) || mc.player.isHandActive()) {
               double renderPosX = Interpolation.getRenderPosX();
               double renderPosY = Interpolation.getRenderPosY();
               double renderPosZ = Interpolation.getRenderPosZ();
               double posX = renderPosX - (double)(MathHelper.cos(mc.player.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
               double posY = renderPosY + (double)mc.player.getEyeHeight() - 0.1000000014901161;
               double posZ = renderPosZ - (double)(MathHelper.sin(mc.player.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
               float maxDist = this.getDistance(item);
               double motionX = (double)(-MathHelper.sin(mc.player.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(mc.player.rotationPitch / 180.0F * (float)Math.PI) * maxDist);
               double motionY = (double)(-MathHelper.sin((mc.player.rotationPitch - (float)this.getThrowPitch(item)) / 180.0F * 3.141593F) * maxDist);
               double motionZ = (double)(MathHelper.cos(mc.player.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(mc.player.rotationPitch / 180.0F * (float)Math.PI) * maxDist);
               int var6 = 72000 - mc.player.getItemInUseCount();
               float power = (float)var6 / 20.0F;
               power = (power * power + power * 2.0F) / 3.0F;
               if (power > 1.0F) {
                  power = 1.0F;
               }

               float distance = MathHelper.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
               motionX /= (double)distance;
               motionY /= (double)distance;
               motionZ /= (double)distance;
               float pow = (item instanceof ItemBow ? power * 2.0F : 1.0F) * this.getThrowVelocity(item);
               motionX *= (double)pow;
               motionY *= (double)pow;
               motionZ *= (double)pow;
               Color color = this.getColor();
               float size = item instanceof ItemBow ? 0.3F : 0.25F;
               boolean hasLanded = false;
               Entity landingOnEntity = null;
               RayTraceResult landingPosition = null;
               RenderMethods.startRender();
               GL11.glLineWidth((Float)this.lineWidth.getValue());
               GL11.glEnable(2848);
               GlStateManager.color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
               GL11.glBegin(3);

               while(!hasLanded && posY > (double)0.0F) {
                  Vec3d present = new Vec3d(posX, posY, posZ);
                  Vec3d future = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
                  RayTraceResult possibleLandingStrip = mc.world.rayTraceBlocks(present, future, false, true, false);
                  if (possibleLandingStrip != null && possibleLandingStrip.typeOfHit != Type.MISS) {
                     landingPosition = possibleLandingStrip;
                     hasLanded = true;
                  }

                  AxisAlignedBB arrowBox = new AxisAlignedBB(posX - (double)size, posY - (double)size, posZ - (double)size, posX + (double)size, posY + (double)size, posZ + (double)size);

                  for(Entity entity : this.getEntitiesWithinAABB(arrowBox.offset(motionX, motionY, motionZ).expand((double)1.0F, (double)1.0F, (double)1.0F))) {
                     if (entity.canBeCollidedWith() && entity != mc.player) {
                        AxisAlignedBB var8 = entity.getEntityBoundingBox().expand(0.3, 0.3, 0.3);
                        RayTraceResult possibleEntityLanding = var8.calculateIntercept(present, future);
                        if (possibleEntityLanding != null) {
                           hasLanded = true;
                           landingOnEntity = entity;
                           landingPosition = possibleEntityLanding;
                        }
                     }
                  }

                  if (landingOnEntity != null) {
                     GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
                  }

                  posX += motionX;
                  posY += motionY;
                  posZ += motionZ;
                  motionX *= (double)0.99F;
                  motionY *= (double)0.99F;
                  motionZ *= (double)0.99F;
                  motionY -= (double)this.getGravity(item);
                  GL11.glVertex3d(posX - renderPosX, posY - renderPosY, posZ - renderPosZ);
               }

               GL11.glEnd();
               RenderMethods.endRender();
               if ((Boolean)this.landing.getValue() && landingPosition != null && landingPosition.typeOfHit != Type.MISS && landingPosition.hitVec != null) {
                  Vec3d hitVec = landingPosition.hitVec;
                  Color diskColor = landingOnEntity != null ? Color.RED : color;
                  float radius = (Float)this.diskRadius.getValue();
                  int segments = 100;
                  RenderMethods.startRender();
                  GL11.glDisable(2884);
                  GlStateManager.translate(hitVec.x - renderPosX, hitVec.y - renderPosY, hitVec.z - renderPosZ);
                  if (landingPosition.sideHit != null) {
                     switch (landingPosition.sideHit) {
                        case DOWN:
                           GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                        case UP:
                        default:
                           break;
                        case NORTH:
                           GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                           break;
                        case SOUTH:
                           GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                           break;
                        case WEST:
                           GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                           break;
                        case EAST:
                           GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                     }
                  }

                  GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                  Disk disk = new Disk();
                  disk.setDrawStyle(100012);
                  GL11.glColor4f((float)diskColor.getRed() / 255.0F, (float)diskColor.getGreen() / 255.0F, (float)diskColor.getBlue() / 255.0F, (float)(Integer)this.diskAlpha.getValue() / 255.0F);
                  disk.draw(0.0F, radius, 100, 1);
                  disk.setDrawStyle(100013);
                  GL11.glColor4f((float)diskColor.getRed() / 255.0F, (float)diskColor.getGreen() / 255.0F, (float)diskColor.getBlue() / 255.0F, 1.0F);
                  disk.draw(radius, radius, 100, 1);
                  GL11.glEnable(2884);
                  GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                  RenderMethods.endRender();
               }

            }
         }
      }
   }

   private void renderOldMode(Render3DEvent event) {
      if (mc.player != null && mc.world != null && mc.gameSettings.thirdPersonView == 0) {
         if (mc.player.getHeldItemMainhand() != ItemStack.EMPTY && mc.player.getHeldItemMainhand().getItem() instanceof ItemBow || mc.player.getHeldItemMainhand() != ItemStack.EMPTY && this.isThrowable(mc.player.getHeldItemMainhand().getItem()) || mc.player.getHeldItemOffhand() != ItemStack.EMPTY && this.isThrowable(mc.player.getHeldItemOffhand().getItem())) {
            double renderPosX = Interpolation.getRenderPosX();
            double renderPosY = Interpolation.getRenderPosY();
            double renderPosZ = Interpolation.getRenderPosZ();
            Item item = null;
            if (mc.player.getHeldItemMainhand() == ItemStack.EMPTY || !(mc.player.getHeldItemMainhand().getItem() instanceof ItemBow) && !this.isThrowable(mc.player.getHeldItemMainhand().getItem())) {
               if (mc.player.getHeldItemOffhand() != ItemStack.EMPTY && this.isThrowable(mc.player.getHeldItemOffhand().getItem())) {
                  item = mc.player.getHeldItemOffhand().getItem();
               }
            } else {
               item = mc.player.getHeldItemMainhand().getItem();
            }

            if (item != null) {
               RenderMethods.startRender();
               GL11.glLineWidth((Float)this.lineWidth.getValue());
               double posX = renderPosX - (double)(MathHelper.cos(mc.player.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
               double posY = renderPosY + (double)mc.player.getEyeHeight() - 0.1000000014901161;
               double posZ = renderPosZ - (double)(MathHelper.sin(mc.player.rotationYaw / 180.0F * (float)Math.PI) * 0.16F);
               float maxDist = this.getDistance(item);
               double motionX = (double)(-MathHelper.sin(mc.player.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(mc.player.rotationPitch / 180.0F * (float)Math.PI) * maxDist);
               double motionY = (double)(-MathHelper.sin((mc.player.rotationPitch - (float)this.getThrowPitch(item)) / 180.0F * 3.141593F) * maxDist);
               double motionZ = (double)(MathHelper.cos(mc.player.rotationYaw / 180.0F * (float)Math.PI) * MathHelper.cos(mc.player.rotationPitch / 180.0F * (float)Math.PI) * maxDist);
               int var6 = 72000 - mc.player.getItemInUseCount();
               float power = (float)var6 / 20.0F;
               power = (power * power + power * 2.0F) / 3.0F;
               if (power > 1.0F) {
                  power = 1.0F;
               }

               float distance = MathHelper.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
               motionX /= (double)distance;
               motionY /= (double)distance;
               motionZ /= (double)distance;
               float pow = (item instanceof ItemBow ? power * 2.0F : 1.0F) * this.getThrowVelocity(item);
               motionX *= (double)pow;
               motionY *= (double)pow;
               motionZ *= (double)pow;
               Color color = this.getColor();
               GlStateManager.color((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
               GL11.glEnable(2848);
               float size = (float)(item instanceof ItemBow ? 0.3 : (double)0.25F);
               boolean hasLanded = false;
               Entity landingOnEntity = null;
               RayTraceResult landingPosition = null;
               GL11.glBegin(3);

               while(!hasLanded && posY > (double)0.0F) {
                  Vec3d present = new Vec3d(posX, posY, posZ);
                  Vec3d future = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
                  RayTraceResult possibleLandingStrip = mc.world.rayTraceBlocks(present, future, false, true, false);
                  if (possibleLandingStrip != null && possibleLandingStrip.typeOfHit != Type.MISS) {
                     landingPosition = possibleLandingStrip;
                     hasLanded = true;
                  }

                  AxisAlignedBB arrowBox = new AxisAlignedBB(posX - (double)size, posY - (double)size, posZ - (double)size, posX + (double)size, posY + (double)size, posZ + (double)size);

                  for(Entity entity : this.getEntitiesWithinAABB(arrowBox.offset(motionX, motionY, motionZ).expand((double)1.0F, (double)1.0F, (double)1.0F))) {
                     if (entity.canBeCollidedWith() && entity != mc.player) {
                        float var7 = 0.3F;
                        AxisAlignedBB var8 = entity.getEntityBoundingBox().expand((double)0.3F, (double)0.3F, (double)0.3F);
                        RayTraceResult possibleEntityLanding = var8.calculateIntercept(present, future);
                        if (possibleEntityLanding != null) {
                           hasLanded = true;
                           landingOnEntity = entity;
                           landingPosition = possibleEntityLanding;
                        }
                     }
                  }

                  if (landingOnEntity != null) {
                     GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
                  }

                  posX += motionX;
                  posY += motionY;
                  posZ += motionZ;
                  motionX *= (double)0.99F;
                  motionY *= (double)0.99F;
                  motionZ *= (double)0.99F;
                  motionY -= (double)this.getGravity(item);
                  this.drawLine3D(posX - renderPosX, posY - renderPosY, posZ - renderPosZ);
               }

               GL11.glEnd();
               if ((Boolean)this.landing.getValue() && landingPosition != null && landingPosition.typeOfHit == Type.BLOCK) {
                  GlStateManager.translate(posX - renderPosX, posY - renderPosY, posZ - renderPosZ);
                  int side = landingPosition.sideHit.getIndex();
                  if (side == 2) {
                     GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                  } else if (side == 3) {
                     GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                  } else if (side == 4) {
                     GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                  } else if (side == 5) {
                     GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                  }

                  Cylinder c = new Cylinder();
                  GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                  c.setDrawStyle(100013);
                  if (landingOnEntity != null) {
                     GlStateManager.color(0.0F, 0.0F, 0.0F, 1.0F);
                     GL11.glLineWidth(2.5F);
                     c.draw(0.5F, 0.15F, 0.0F, 8, 1);
                     GL11.glLineWidth(0.1F);
                     GlStateManager.color(1.0F, 0.0F, 0.0F, 1.0F);
                  }

                  c.draw(0.5F, 0.15F, 0.0F, 8, 1);
               }

               RenderMethods.endRender();
            }
         }
      }
   }

   private boolean isHolding(Item item) {
      return mc.player.getHeldItem(EnumHand.MAIN_HAND).getItem() == item || mc.player.getHeldItem(EnumHand.OFF_HAND).getItem() == item;
   }

   protected boolean isThrowable(Item item) {
      return item instanceof ItemEnderPearl || item instanceof ItemExpBottle || item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemSplashPotion || item instanceof ItemLingeringPotion;
   }

   public void drawLine3D(double var1, double var2, double var3) {
      GL11.glVertex3d(var1, var2, var3);
   }

   protected int getThrowPitch(Item item) {
      return !(item instanceof ItemSplashPotion) && !(item instanceof ItemLingeringPotion) && !(item instanceof ItemExpBottle) ? 0 : 20;
   }

   protected float getGravity(Item item) {
      return !(item instanceof ItemBow) && !(item instanceof ItemSplashPotion) && !(item instanceof ItemLingeringPotion) && !(item instanceof ItemExpBottle) ? 0.03F : 0.05F;
   }

   protected float getThrowVelocity(Item item) {
      if (!(item instanceof ItemSplashPotion) && !(item instanceof ItemLingeringPotion)) {
         return item instanceof ItemExpBottle ? 0.59F : 1.5F;
      } else {
         return 0.5F;
      }
   }

   protected float getDistance(Item item) {
      return item instanceof ItemBow ? 1.0F : 0.4F;
   }

   protected ArrayList<Entity> getEntitiesWithinAABB(AxisAlignedBB bb) {
      ArrayList<Entity> list = new ArrayList();
      int chunkMinX = MathHelper.floor((bb.minX - (double)2.0F) / (double)16.0F);
      int chunkMaxX = MathHelper.floor((bb.maxX + (double)2.0F) / (double)16.0F);
      int chunkMinZ = MathHelper.floor((bb.minZ - (double)2.0F) / (double)16.0F);
      int chunkMaxZ = MathHelper.floor((bb.maxZ + (double)2.0F) / (double)16.0F);

      for(int x = chunkMinX; x <= chunkMaxX; ++x) {
         for(int z = chunkMinZ; z <= chunkMaxZ; ++z) {
            if (mc.world.getChunkProvider().getLoadedChunk(x, z) != null) {
               mc.world.getChunk(x, z).getEntitiesWithinAABBForEntity(mc.player, bb, list, EntitySelectors.NOT_SPECTATING);
            }
         }
      }

      return list;
   }

   public static enum RenderMode {
      New,
      Old;
   }
}
