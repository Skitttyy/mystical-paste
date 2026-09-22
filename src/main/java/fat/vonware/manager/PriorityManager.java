package fat.vonware.manager;

public class PriorityManager {
   public static final PriorityManager INSTANCE = new PriorityManager();
   public boolean usageLock = false;
   public String usageLockCause = "NONE";

   private PriorityManager() {
   }

   public void lockUsageLock(String cause) {
      this.usageLock = true;
      this.usageLockCause = cause;
   }

   public void unlockUsageLock() {
      this.usageLock = false;
      this.usageLockCause = "NONE";
   }

   public boolean isUsageLocked() {
      return this.usageLock;
   }

   public boolean isLockedBy(String cause) {
      return this.usageLock && this.usageLockCause.equals(cause);
   }

   public boolean isLockedByOther(String cause) {
      return this.usageLock && !this.usageLockCause.equals(cause);
   }
}
