package fat.vonware.util;

public class Timer {
   private long time = -1L;
   public long startTime = System.currentTimeMillis();
   boolean paused = false;
   private long delay;

   public boolean passedS(double s) {
      return this.getMs(System.nanoTime() - this.time) >= (long)(s * (double)1000.0F);
   }

   public boolean passedMs(long ms) {
      return this.getMs(System.nanoTime() - this.time) >= ms;
   }

   public void setDelay(long delay) {
      this.delay = delay;
   }

   public long getTime() {
      return System.currentTimeMillis() - this.time;
   }

   public long getPassedTimeMs() {
      return this.getMs(System.nanoTime() - this.time);
   }

   public boolean isPassed(long delaylol) {
      return delaylol <= 1L && !this.paused || !this.paused && System.currentTimeMillis() - this.startTime >= delaylol;
   }

   public boolean isPassed() {
      return this.delay <= 1L && !this.paused || !this.paused && System.currentTimeMillis() - this.startTime >= this.delay;
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public boolean isPaused() {
      return this.paused;
   }

   public void reset() {
      this.time = System.nanoTime();
   }

   public long getStartTime() {
      return this.startTime;
   }

   public void resetDelay() {
      this.startTime = System.currentTimeMillis();
   }

   public boolean passed(long delay) {
      return this.getMs(System.nanoTime() - this.time) >= delay;
   }

   public long getMs(long time) {
      return time / 1000000L;
   }
}
