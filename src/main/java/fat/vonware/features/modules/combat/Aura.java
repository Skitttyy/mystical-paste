package fat.vonware.features.modules.combat;

import fat.vonware.Global;
import fat.vonware.Vonware;
import fat.vonware.event.events.MotionUpdateEvent;
import fat.vonware.event.events.Render3DEvent;
import fat.vonware.features.modules.Module;
import fat.vonware.features.modules.client.Colors;
import fat.vonware.features.setting.Setting;
import fat.vonware.manager.RotationManager;
import fat.vonware.util.EntityUtil;
import fat.vonware.util.Interpolation;
import fat.vonware.util.InventoryUtil;
import fat.vonware.util.MathUtil;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Timer;
import fat.vonware.util.Animations.render.ColorUtil;
import fat.vonware.util.Animations.render.RenderMethods;
import fat.vonware.util.OtherRenderShit.Render.FreecamEntity;
import fat.vonware.util.Phase.PacketUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Aura extends Module {
   protected final Setting<Float> range = new Setting<Float>("Range", 4.5F, 1.0F, 6.0F, "Maximum distance at which Aura can attack targets.");
   protected final Setting<Float> wallRange = new Setting<Float>("Wall-Range", 3.0F, 1.0F, 6.0F, "Maximum attack distance when the target is behind a wall.");
   protected final Setting<Targeting> targeting;
   protected final Setting<AuraSorting> sortMode;
   protected final Setting<Boolean> smartHealth;
   protected final Setting<SwordMode> sword;
   protected final Setting<TpsSync> tpsSync;
   protected final Setting<Location> bone;
   protected final Setting<Boolean> rotate;
   protected final Setting<Boolean> rotateHold;
   protected final Setting<Boolean> silentRotate;
   protected final Setting<Boolean> pick;
   protected final Setting<Boolean> stopSneak;
   protected final Setting<Boolean> stopSprint;
   protected final Setting<Boolean> stopShield;
   protected final Setting<Boolean> invis;
   protected final Setting<Boolean> monsters;
   protected final Setting<Boolean> render;
   protected final Setting<RenderMode> renderMode;
   protected final Setting<Integer> boxAlpha;
   protected final Setting<Integer> lineAlpha;
   protected EntityLivingBase target;
   protected List<EntityLivingBase> targets;
   protected final Timer timer;
   protected float[] rotations;

   public Aura() {
      super("Aura", "Automatically attacks targets", Module.Category.COMBAT, true, false, false);
      this.targeting = (new Setting("Targeting", Aura.Targeting.Single)).withDescription((Function<Targeting, String>)((mode) -> {
         switch (mode) {
            case Single:
               return "Single: Locks onto and attacks one target at a time.";
            case Switch:
               return "Switch: Cycles between valid targets instead of staying on one target.";
            default:
               return "";
         }
      }));
      this.sortMode = (new Setting("AuraSorting", Aura.AuraSorting.Smart)).withDescription((Function<AuraSorting, String>)((mode) -> {
         switch (mode) {
            case Health:
               return "Health: Prioritizes the target with the lowest health.";
            case Distance:
               return "Distance: Prioritizes the target closest to you.";
            case Smart:
               return "Smart: Balances health and distance to choose the best target.";
            default:
               return "";
         }
      }));
      this.smartHealth = new Setting<Boolean>("SmartHealth+", false, (v) -> this.sortMode.getValue() == Aura.AuraSorting.Health || this.sortMode.getValue() == Aura.AuraSorting.Smart, "Improves health-based target sorting by accounting for additional target conditions.");
      this.sword = (new Setting("SwordMode", Aura.SwordMode.Require)).withDescription((Function<SwordMode, String>)((mode) -> {
         switch (mode) {
            case Switch:
               return "Switch: Automatically switches to a sword when a valid target is found.";
            case Require:
               return "Require: Requires you to hold a sword before Aura can attack.";
            case None:
               return "None: Attacks regardless of what item you are holding.";
            default:
               return "";
         }
      }));
      this.tpsSync = (new Setting("TpsSync", Aura.TpsSync.Average)).withDescription((Function<TpsSync, String>)((mode) -> {
         switch (mode) {
            case Average:
               return "Average: Paces attacks using the server's averaged TPS.";
            case Latest:
               return "Latest: Paces attacks using the most recent TPS reading.";
            default:
               return "";
         }
      }));
      this.bone = (new Setting("Bone", Aura.Location.Chest)).withDescription((Function<Location, String>)((mode) -> {
         switch (mode) {
            case Head:
               return "Head: Aims rotations at the target's head.";
            case Chest:
               return "Chest: Aims rotations at the target's chest.";
            case Legs:
               return "Legs: Aims rotations at the target's legs.";
            case Feet:
               return "Feet: Aims rotations at the target's feet.";
            default:
               return "";
         }
      }));
      this.rotate = new Setting<Boolean>("Rotate", true, "Rotates toward the target before attacking.");
      this.rotateHold = new Setting<Boolean>("RotateHold", true, "Keeps the attack rotation active instead of immediately releasing it.");
      this.silentRotate = new Setting<Boolean>("SilentRotate", false, "Applies rotations through packets without visibly turning your client view.");
      this.pick = new Setting<Boolean>("Pickaxe", false, (v) -> this.sword.getValue() == Aura.SwordMode.None, "Allows Aura to attack while holding a pickaxe when SwordMode is None.");
      this.stopSneak = new Setting<Boolean>("StopSneak", false, "Stops sneaking while performing an attack.");
      this.stopSprint = new Setting<Boolean>("StopSprint", false, "Stops sprinting while performing an attack.");
      this.stopShield = new Setting<Boolean>("StopShield", false, "Stops blocking with a shield before attacking.");
      this.invis = new Setting<Boolean>("Invisibles", true, "Allows invisible entities to be targeted.");
      this.monsters = new Setting<Boolean>("Monsters", false, "Allows hostile mobs to be targeted.");
      this.render = new Setting<Boolean>("Render", true, "Renders a visual highlight around the current target.");
      this.renderMode = (new Setting("RenderMode", Aura.RenderMode.Both, (v) -> (Boolean)this.render.getValue())).withDescription((Function<RenderMode, String>)((mode) -> {
         switch (mode) {
            case Box:
               return "Box: Draws a filled box around the current target.";
            case Outline:
               return "Outline: Draws an outline around the current target.";
            case Both:
               return "Both: Draws both a filled box and an outline around the current target.";
            default:
               return "";
         }
      }));
      this.boxAlpha = new Setting<Integer>("BoxAlpha", 45, 0, 255, (v) -> (Boolean)this.render.getValue(), "Controls the transparency of the target box.");
      this.lineAlpha = new Setting<Integer>("LineAlpha", 150, 0, 255, (v) -> (Boolean)this.render.getValue(), "Controls the transparency of the target outline.");
      this.targets = new ArrayList();
      this.timer = new Timer();
      this.register(this.range);
      this.register(this.wallRange);
      this.register(this.targeting);
      this.register(this.sortMode);
      this.register(this.smartHealth);
      this.register(this.sword);
      this.register(this.tpsSync);
      this.register(this.bone);
      this.register(this.rotate);
      this.register(this.rotateHold);
      this.register(this.silentRotate);
      this.register(this.pick);
      this.register(this.stopSneak);
      this.register(this.stopSprint);
      this.register(this.stopShield);
      this.register(this.invis);
      this.register(this.monsters);
      this.register(this.render);
      this.register(this.renderMode);
      this.register(this.boxAlpha);
      this.register(this.lineAlpha);
   }

   public String getDisplayInfo() {
      return ((Targeting)this.targeting.getValue()).name();
   }

   public void onUpdate() {
      if (!Offhand.shouldPauseAura() && !AutoTrap.shouldPauseAura() && !SelfTrap.shouldPauseAura()) {
         this.targets.removeIf((t) -> !this.isValid(t));

         for(Entity entity : mc.world.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
               EntityLivingBase living = (EntityLivingBase)entity;
               if (this.isValid(living) && !this.targets.contains(living)) {
                  this.targets.add(living);
               }
            }
         }

         this.target = (EntityLivingBase)this.targets.stream().filter(this::isValid).min(Comparator.comparingDouble((e) -> {
            if (this.sortMode.getValue() == Aura.AuraSorting.Health) {
               return (Boolean)this.smartHealth.getValue() ? this.smartHealthValue(e) : (double)EntityUtil.getHealth(e);
            } else if (this.sortMode.getValue() == Aura.AuraSorting.Distance) {
               return (double)mc.player.getDistance(e);
            } else {
               return this.sortMode.getValue() == Aura.AuraSorting.Smart ? this.smartTargetValue(e) : Double.MAX_VALUE;
            }
         })).orElse(null);
      } else {
         this.target = null;
      }
   }

   @SubscribeEvent
   public void onWalkingUpdate(MotionUpdateEvent event) {
      if (event.getStage() == 0) {
         if (!Offhand.shouldPauseAura()) {
            if (!AutoTrap.shouldPauseAura()) {
               if (!SelfTrap.shouldPauseAura()) {
                  if (this.target != null) {
                     if (this.isValidItem()) {
                        if ((Boolean)this.rotate.getValue() && (Boolean)this.rotateHold.getValue()) {
                           this.rotations = RotationUtil.getRotationsAtLocation(this.bone.getValue(), this.target);
                           this.applyRotations(event);
                        }

                        float factor = 20.0F;
                        switch ((TpsSync)this.tpsSync.getValue()) {
                           case Average:
                              factor = 20.0F - Vonware.serverManager.getCurrentTps();
                              break;
                           case Latest:
                              factor = 20.0F - Vonware.serverManager.getTPS();
                        }

                        if (factor > 20.0F) {
                           factor = 20.0F;
                        }

                        switch ((Targeting)this.targeting.getValue()) {
                           case Single:
                              this.attack(this.target, event, factor);
                              break;
                           case Switch:
                              this.attack(this.target, event, factor);
                              this.attack(this.target, event, factor);
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private double smartHealthValue(EntityLivingBase e) {
      return (double)EntityUtil.getHealth(e) - (double)e.getAbsorptionAmount() * (double)0.5F;
   }

   private double smartTargetValue(EntityLivingBase e) {
      double healthFactor = (Boolean)this.smartHealth.getValue() ? this.smartHealthValue(e) : (double)EntityUtil.getHealth(e);
      double distanceFactor = (double)mc.player.getDistance(e);
      return healthFactor + distanceFactor * (double)0.5F;
   }

   @SubscribeEvent
   public void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.getValue() && this.target != null) {
         if (this.sword.getValue() != Aura.SwordMode.None) {
            boolean holdingPick = (Boolean)this.pick.getValue() && InventoryUtil.isHolding(ItemPickaxe.class);
            if (InventoryUtil.isHolding(ItemSword.class) || holdingPick) {
               this.render();
            }
         } else {
            this.render();
         }
      }

   }

   private void render() {
      RenderMethods.startRender();
      Vec3d vec = Interpolation.interpolateEntity(this.target);
      switch ((RenderMode)this.renderMode.getValue()) {
         case Box:
            this.renderBox(this.target, vec);
            break;
         case Outline:
            this.renderOutline(this.target, vec);
            break;
         case Both:
            this.renderBox(this.target, vec);
            this.renderOutline(this.target, vec);
      }

      RenderMethods.endRender();
   }

   private void renderBox(Entity entity, Vec3d vec) {
      Color baseColor;
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         baseColor = ColorUtil.rainbow(0);
      } else {
         baseColor = Global.getGlobalColor();
      }

      RenderMethods.drawBox((new AxisAlignedBB((double)0.0F, (double)0.0F, (double)0.0F, (double)entity.width, (double)entity.height, (double)entity.width)).offset(vec.x - (double)(entity.width / 2.0F), vec.y, vec.z - (double)(entity.width / 2.0F)), new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (Integer)this.boxAlpha.getValue()));
   }

   private void renderOutline(Entity entity, Vec3d vec) {
      Color baseColor;
      if ((Boolean)Colors.getInstance().rainbow.getValue()) {
         baseColor = ColorUtil.rainbow(0);
      } else {
         baseColor = Global.getGlobalColor();
      }

      RenderMethods.drawOutline((new AxisAlignedBB((double)0.0F, (double)0.0F, (double)0.0F, (double)entity.width, (double)entity.height, (double)entity.width)).offset(vec.x - (double)(entity.width / 2.0F), vec.y, vec.z - (double)(entity.width / 2.0F)), 1.3F, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (Integer)this.lineAlpha.getValue()));
   }

   public void onToggle() {
      this.target = null;
      this.targets.clear();
      this.rotations = null;
   }

   protected void attack(EntityLivingBase target, MotionUpdateEvent event, float factor) {
      boolean doStopSneak = (Boolean)this.stopSneak.getValue() && mc.player.isSneaking();
      boolean doStopSprint = (Boolean)this.stopSprint.getValue() && mc.player.isSprinting();
      boolean doStopShield = (Boolean)this.stopShield.getValue() && mc.player.isActiveItemStackBlocking();
      if (doStopSneak) {
         PacketUtil.send(new CPacketEntityAction(mc.player, Action.STOP_SNEAKING));
      }

      if (doStopSprint) {
         PacketUtil.send(new CPacketEntityAction(mc.player, Action.STOP_SPRINTING));
      }

      if (doStopShield) {
         this.releaseShield();
      }

      if (mc.player.getCooledAttackStrength(factor) >= 0.45F && (Boolean)this.rotate.getValue() && !(Boolean)this.rotateHold.getValue()) {
         this.rotations = RotationUtil.getRotationsAtLocation(this.bone.getValue(), target);
         this.applyRotations(event);
      }

      if (mc.player.getCooledAttackStrength(factor) >= 1.0F) {
         mc.playerController.attackEntity(mc.player, target);
         mc.player.swingArm(EnumHand.MAIN_HAND);
         this.timer.reset();
         if (this.targeting.getValue() == Aura.Targeting.Switch && !this.timer.passed(150L)) {
            this.removeTarget();
         }
      }

      if (doStopSneak) {
         PacketUtil.send(new CPacketEntityAction(mc.player, Action.STOP_SNEAKING));
      }

      if (doStopSprint) {
         PacketUtil.send(new CPacketEntityAction(mc.player, Action.STOP_SPRINTING));
      }

      if (doStopShield) {
         this.useShield();
      }

      this.rotations = null;
   }

   protected void applyRotations(MotionUpdateEvent event) {
      if (this.rotations != null) {
         if ((Boolean)this.rotate.getValue()) {
            RotationManager.INSTANCE.requestRotation(RotationManager.RotationSource.AURA, this.rotations[0], this.rotations[1]);
         } else if ((Boolean)this.silentRotate.getValue()) {
            PacketUtil.rotate(this.rotations, mc.player.onGround);
         }

      }
   }

   protected boolean isValidItem() {
      switch ((SwordMode)this.sword.getValue()) {
         case Switch:
            int swordSlot = InventoryUtil.getItemSlot(ItemSword.class);
            int pickSlot = InventoryUtil.getItemSlot(ItemPickaxe.class);
            if (swordSlot != -1) {
               InventoryUtil.switchTo(swordSlot);
               return true;
            } else {
               if ((Boolean)this.pick.getValue() && pickSlot != -1) {
                  InventoryUtil.switchTo(pickSlot);
                  return true;
               }

               return false;
            }
         case Require:
            return mc.player.getHeldItemMainhand().getItem() instanceof ItemSword || mc.player.getHeldItemMainhand().getItem() instanceof ItemPickaxe && (Boolean)this.pick.getValue();
         case None:
            return true;
         default:
            return false;
      }
   }

   protected boolean isValid(EntityLivingBase entity) {
      assert mc.player != null;

      if (entity instanceof FreecamEntity) {
         return false;
      } else if (entity == mc.player) {
         return false;
      } else {
         return this.isValidEntity(entity) && (!entity.isInvisible() || (Boolean)this.invis.getValue()) && !Vonware.friendManager.isFriend(entity.getName()) && entity.isEntityAlive() && EntityUtil.getHealth(entity) >= 0.0F && this.isInRange(mc.player.getPositionVector(), entity);
      }
   }

   protected boolean isValidEntity(EntityLivingBase entity) {
      return entity instanceof EntityPlayer || EntityUtil.isMonster(entity) && (Boolean)this.monsters.getValue();
   }

   protected void releaseShield() {
      if (mc.player.getHeldItemOffhand().getItem() instanceof ItemShield) {
         PacketUtil.send(new CPacketPlayerDigging(net.minecraft.network.play.client.CPacketPlayerDigging.Action.RELEASE_USE_ITEM, new BlockPos(mc.player), EnumFacing.getFacingFromVector((float)mc.player.posX, (float)mc.player.posY, (float)mc.player.posZ)));
      }

   }

   protected void useShield() {
      if ((mc.player.getHeldItemMainhand().getItem() instanceof ItemSword || mc.player.getHeldItemMainhand().getItem() instanceof ItemAxe) && mc.player.getHeldItemOffhand().getItem() instanceof ItemShield) {
         mc.playerController.processRightClick(mc.player, mc.world, EnumHand.OFF_HAND);
      }

   }

   public boolean isInRange(Vec3d from, Entity target) {
      double distance = from.squareDistanceTo(target.getPositionVector());
      return distance < MathUtil.square((double)(Float)this.range.getValue()) && (distance < MathUtil.square((double)(Float)this.wallRange.getValue()) || mc.world.rayTraceBlocks(new Vec3d(from.x, from.y + (double)mc.player.getEyeHeight(), from.z), new Vec3d(target.posX, target.posY + (double)target.getEyeHeight(), target.posZ), false, true, false) == null);
   }

   private void removeTarget() {
      if (this.targets.size() > 1) {
         this.targets.remove(this.target);
         this.target = null;
      } else {
         this.targets.remove(this.target);
         this.target = null;
      }
   }

   public Setting<Float> getRange() {
      return this.range;
   }

   public Setting<Boolean> getRotate() {
      return this.rotate;
   }

   public Setting<Boolean> getRender() {
      return this.render;
   }

   public EntityLivingBase getTarget() {
      return this.target;
   }

   public Timer getTimer() {
      return this.timer;
   }

   public float[] getRotations() {
      return this.rotations;
   }

   public static enum AuraSorting {
      Health,
      Distance,
      Smart;
   }

   public static enum Location {
      Head,
      Chest,
      Legs,
      Feet;
   }

   public static enum SwordMode {
      Require,
      Switch,
      None;
   }

   public static enum Targeting {
      Single,
      Switch;
   }

   public static enum TpsSync {
      Average,
      Latest;
   }

   public static enum RenderMode {
      Box,
      Outline,
      Both;
   }
}
