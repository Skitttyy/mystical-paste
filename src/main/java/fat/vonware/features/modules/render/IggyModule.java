package fat.vonware.features.modules.render;

import fat.vonware.Global;
import fat.vonware.event.events.PacketEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.setting.Setting;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.util.EnumParticleTypes;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.lwjgl.opengl.GL11;

public class IggyModule extends Module {
   private final Map<Integer, EntityEnderCrystal> trackedCrystals = new ConcurrentHashMap();
   private final Setting<Boolean> spawns = new Setting<Boolean>("Spawns", false);
   private final Setting<SpawnMode> spawnMode;
   private final Setting<Float> spawnRange;
   public static final Map<UUID, Thingering> thingers = new HashMap();

   public IggyModule() {
      super("IggyModule", "draws a Crit particle on a crystal when its broken, and a spawn indicator when it spawns", Module.Category.RENDER, true, false, false);
      this.spawnMode = new Setting<SpawnMode>("SpawnMode", IggyModule.SpawnMode.Spawn, (v) -> (Boolean)this.spawns.getValue());
      this.spawnRange = new Setting<Float>("SpawnRange", 5.0F, 1.0F, 10.0F, (v) -> (Boolean)this.spawns.getValue());
      this.register(this.spawns);
      this.register(this.spawnMode);
      this.register(this.spawnRange);
   }

   @SubscribeEvent
   public void onClientTick(TickEvent.ClientTickEvent event) {
      if (!fullNullCheck()) {
         if (event.phase == Phase.START) {
            for(Entity entity : mc.world.loadedEntityList) {
               if (entity instanceof EntityEnderCrystal) {
                  this.trackedCrystals.put(entity.getEntityId(), (EntityEnderCrystal)entity);
               }
            }

            if ((Boolean)this.spawns.getValue()) {
               if (this.spawnMode.getValue() == IggyModule.SpawnMode.JellyTest) {
                  thingers.entrySet().removeIf((entry) -> {
                     Entity crystalEntity = ((Thingering)entry.getValue()).entity;
                     return crystalEntity.isDead || !mc.world.loadedEntityList.contains(crystalEntity);
                  });
               } else {
                  thingers.entrySet().removeIf((entry) -> {
                     Entity crystalEntity = ((Thingering)entry.getValue()).entity;
                     long duration = System.currentTimeMillis() - ((Thingering)entry.getValue()).starTime;
                     boolean animationFinished = duration >= 1500L;
                     return (crystalEntity.isDead || !mc.world.loadedEntityList.contains(crystalEntity)) && animationFinished;
                  });
               }

               for(Entity entity : mc.world.loadedEntityList) {
                  if (entity instanceof EntityEnderCrystal) {
                     if (thingers.containsKey(entity.getUniqueID())) {
                        Thingering thinger = (Thingering)thingers.get(entity.getUniqueID());
                        thinger.lastX = entity.posX;
                        thinger.lastY = entity.posY;
                        thinger.lastZ = entity.posZ;
                     } else if (!(mc.player.getDistance(entity) > (Float)this.spawnRange.getValue())) {
                        thingers.put(entity.getUniqueID(), new Thingering(this, entity));
                        ((Thingering)thingers.get(entity.getUniqueID())).starTime = System.currentTimeMillis();
                     }
                  }
               }

            }
         }
      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck()) {
         if (event.getPacket() instanceof SPacketDestroyEntities) {
            SPacketDestroyEntities packet = (SPacketDestroyEntities)event.getPacket();

            for(int entityId : packet.getEntityIDs()) {
               EntityEnderCrystal crystal = (EntityEnderCrystal)this.trackedCrystals.remove(entityId);
               if (crystal != null) {
                  mc.addScheduledTask(() -> mc.effectRenderer.emitParticleAtEntity(crystal, EnumParticleTypes.CRIT));
               }
            }
         }

      }
   }

   public void onRender3D(Render3DEvent eventRender3D) {
      if (!fullNullCheck() && (Boolean)this.spawns.getValue()) {
         for(Map.Entry<UUID, Thingering> entry : thingers.entrySet()) {
            Thingering thinger = (Thingering)entry.getValue();
            Entity entity = thinger.entity;
            long duration = System.currentTimeMillis() - thinger.starTime;
            if (this.spawnMode.getValue() == IggyModule.SpawnMode.JellyTest) {
               if (!entity.isDead && !(mc.player.getDistance(entity) > (Float)this.spawnRange.getValue())) {
                  double baseRadius = Double.longBitsToDouble(Double.doubleToLongBits(205.3116845075892) ^ 9190063402910341247L);
                  double pulse = Math.sin((double)duration / (double)200.0F) * 0.15 + (double)1.0F;
                  double jellyRadius = baseRadius * pulse;
                  long worldTime = mc.world.getTotalWorldTime();
                  float crystalHeight = 2.0F;
                  float yProgress = (float)((Math.sin((double)worldTime * 0.3) + (double)1.0F) / (double)2.0F);
                  float yOffset = yProgress * crystalHeight;
                  float opacity = 1.0F;
                  this.drawCircle(entity, eventRender3D.getPartialTicks(), jellyRadius, yOffset, opacity, false);
               }
            } else if (duration < 1500L) {
               float opacity = Float.intBitsToFloat(Float.floatToIntBits(1.2886874E38F) ^ 2126636655);
               if (duration < 1500L) {
                  opacity = Float.intBitsToFloat(Float.floatToIntBits(13.7902155F) ^ 2128389305) - (float)duration / Float.intBitsToFloat(Float.floatToIntBits(6.1687006E-4F) ^ 2124035443);
               }

               Entity renderEntity = entity.isDead ? null : entity;
               this.drawCircle(renderEntity, thinger, eventRender3D.getPartialTicks(), Double.longBitsToDouble(Double.doubleToLongBits(205.3116845075892) ^ 9190063402910341247L), (float)duration / Float.intBitsToFloat(Float.floatToIntBits(0.025765074F) ^ 2115703111), opacity, true);
            }
         }

      }
   }

   public void drawCircle(Entity entity, float partialTicks, double rad, float plusY, float alpha, boolean useOriginalDivision) {
      GL11.glPushMatrix();
      GL11.glDisable(3553);
      startSmooth();
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glLineWidth(Float.intBitsToFloat(Float.floatToIntBits(0.8191538F) ^ 2131866640));
      GL11.glBegin(3);
      double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks - mc.getRenderManager().viewerPosX;
      double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks - mc.getRenderManager().viewerPosY;
      double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks - mc.getRenderManager().viewerPosZ;
      double finalY = useOriginalDivision ? y + (double)(plusY / Float.intBitsToFloat(Float.floatToIntBits(0.13022153F) ^ 2133153995)) : y + (double)plusY;

      for(int i = 0; i <= 90; ++i) {
         GL11.glColor4f((float)Global.getRed() / 255.0F, (float)Global.getGreen() / 255.0F, (float)Global.getBlue() / 255.0F, alpha);
         GL11.glVertex3d(x + rad * Math.cos((double)i * Double.longBitsToDouble(Double.doubleToLongBits(0.038923223119235344) ^ 9203893389252104647L) / Double.longBitsToDouble(Double.doubleToLongBits(0.010043755046771538) ^ 9205940181866359915L)), finalY, z + rad * Math.sin((double)i * Double.longBitsToDouble(Double.doubleToLongBits(0.012655047216797511) ^ 9192070147616231359L) / Double.longBitsToDouble(Double.doubleToLongBits(0.00992417958121009) ^ 9206152726280235987L)));
      }

      GL11.glEnd();
      GL11.glDepthMask(true);
      GL11.glEnable(2929);
      endSmooth();
      GL11.glEnable(3553);
      GL11.glPopMatrix();
   }

   public void drawCircle(Entity entity, Thingering thinger, float partialTicks, double rad, float plusY, float alpha, boolean useOriginalDivision) {
      GL11.glPushMatrix();
      GL11.glDisable(3553);
      startSmooth();
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glLineWidth(Float.intBitsToFloat(Float.floatToIntBits(0.8191538F) ^ 2131866640));
      GL11.glBegin(3);
      double x;
      double y;
      double z;
      if (entity != null && !entity.isDead) {
         x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double)partialTicks - mc.getRenderManager().viewerPosX;
         y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double)partialTicks - mc.getRenderManager().viewerPosY;
         z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double)partialTicks - mc.getRenderManager().viewerPosZ;
      } else {
         x = thinger.lastX - mc.getRenderManager().viewerPosX;
         y = thinger.lastY - mc.getRenderManager().viewerPosY;
         z = thinger.lastZ - mc.getRenderManager().viewerPosZ;
      }

      double finalY = useOriginalDivision ? y + (double)(plusY / Float.intBitsToFloat(Float.floatToIntBits(0.13022153F) ^ 2133153995)) : y + (double)plusY;

      for(int i = 0; i <= 90; ++i) {
         GL11.glColor4f((float)Global.getRed() / 255.0F, (float)Global.getGreen() / 255.0F, (float)Global.getBlue() / 255.0F, alpha);
         GL11.glVertex3d(x + rad * Math.cos((double)i * Double.longBitsToDouble(Double.doubleToLongBits(0.038923223119235344) ^ 9203893389252104647L) / Double.longBitsToDouble(Double.doubleToLongBits(0.010043755046771538) ^ 9205940181866359915L)), finalY, z + rad * Math.sin((double)i * Double.longBitsToDouble(Double.doubleToLongBits(0.012655047216797511) ^ 9192070147616231359L) / Double.longBitsToDouble(Double.doubleToLongBits(0.00992417958121009) ^ 9206152726280235987L)));
      }

      GL11.glEnd();
      GL11.glDepthMask(true);
      GL11.glEnable(2929);
      endSmooth();
      GL11.glEnable(3553);
      GL11.glPopMatrix();
   }

   public static void startSmooth() {
      GL11.glEnable(2848);
      GL11.glEnable(2881);
      GL11.glEnable(2832);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glHint(3154, 4354);
      GL11.glHint(3155, 4354);
      GL11.glHint(3153, 4354);
   }

   public static void endSmooth() {
      GL11.glDisable(2848);
      GL11.glDisable(2881);
      GL11.glEnable(2832);
   }

   public void onDisable() {
      this.trackedCrystals.clear();
      thingers.clear();
      super.onDisable();
   }

   public static enum SpawnMode {
      Spawn,
      JellyTest;
   }

   public static class Thingering {
      public Entity entity;
      public long starTime;
      public IggyModule this$0;
      public double lastX;
      public double lastY;
      public double lastZ;

      public Thingering(IggyModule this$0, Entity entity) {
         this.this$0 = this$0;
         this.entity = entity;
         this.starTime = 0L;
         this.lastX = entity.posX;
         this.lastY = entity.posY;
         this.lastZ = entity.posZ;
      }
   }
}
