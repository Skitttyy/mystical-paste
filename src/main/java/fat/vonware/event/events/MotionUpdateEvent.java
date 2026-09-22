package fat.vonware.event.events;

import fat.vonware.event.EventStage;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class MotionUpdateEvent extends EventStage {
   private final double initialX;
   private final double initialY;
   private final double initialZ;
   private final float initialYaw;
   private final float initialPitch;
   private final boolean initialOnGround;
   private double x;
   private double y;
   private double z;
   private float rotationYaw;
   private float rotationPitch;
   private boolean onGround;
   protected boolean modified;
   protected boolean modifiedRots;

   public MotionUpdateEvent() {
      this(0, (double)0.0F, (double)0.0F, (double)0.0F, 0.0F, 0.0F, false);
   }

   public MotionUpdateEvent(int stage, MotionUpdateEvent event) {
      this(stage, event.x, event.y, event.z, event.rotationYaw, event.rotationPitch, event.onGround);
   }

   public MotionUpdateEvent(int stage, double x, double y, double z, float rotationYaw, float rotationPitch, boolean onGround) {
      super(stage);
      this.x = x;
      this.y = y;
      this.z = z;
      this.rotationYaw = rotationYaw;
      this.rotationPitch = rotationPitch;
      this.onGround = onGround;
      this.initialX = x;
      this.initialY = y;
      this.initialZ = z;
      this.initialYaw = rotationYaw;
      this.initialPitch = rotationPitch;
      this.initialOnGround = onGround;
   }

   public double getInitialX() {
      return this.initialX;
   }

   public double getInitialY() {
      return this.initialY;
   }

   public double getInitialZ() {
      return this.initialZ;
   }

   public float getInitialYaw() {
      return this.initialYaw;
   }

   public float getInitialPitch() {
      return this.initialPitch;
   }

   public boolean isInitialOnGround() {
      return this.initialOnGround;
   }

   public float getRotationYaw() {
      return this.rotationYaw;
   }

   public float getRotationPitch() {
      return this.rotationPitch;
   }

   public boolean isModified() {
      return this.modified;
   }

   public boolean isRotsModified() {
      return this.modifiedRots;
   }

   public double getX() {
      return this.x;
   }

   public void setX(double x) {
      this.modified = true;
      this.x = x;
   }

   public double getY() {
      return this.y;
   }

   public void setY(double y) {
      this.modified = true;
      this.y = y;
   }

   public double getZ() {
      return this.z;
   }

   public void setZ(double z) {
      this.modified = true;
      this.z = z;
   }

   public float getYaw() {
      return this.rotationYaw;
   }

   public void setYaw(float rotationYaw) {
      this.modified = true;
      this.rotationYaw = rotationYaw;
      this.modifiedRots = true;
   }

   public float getPitch() {
      return this.rotationPitch;
   }

   public void setPitch(float rotationPitch) {
      this.modified = true;
      this.rotationPitch = rotationPitch;
      this.modifiedRots = true;
   }

   public boolean isOnGround() {
      return this.onGround;
   }

   public void setOnGround(boolean onGround) {
      this.modified = true;
      this.onGround = onGround;
   }
}
