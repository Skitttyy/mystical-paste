package fat.vonware.util.BlockHighlight;

public class SlideUtil {
   protected long start;
   protected long length;

   public SlideUtil(long ms) {
      this.length = ms;
      this.reset();
   }

   public void reset() {
      this.start = System.currentTimeMillis();
   }

   public boolean isEnd() {
      return this.getTime() >= this.length;
   }

   protected long getTime() {
      return System.currentTimeMillis() - this.start;
   }

   public void setLength(long length) {
      this.length = length;
   }

   private double getFadeOne() {
      return this.isEnd() ? (double)1.0F : (double)this.getTime() / (double)this.length;
   }

   public double easeOutQuad() {
      return this.length == 0L ? (double)1.0F : (double)1.0F - ((double)1.0F - this.getFadeOne()) * ((double)1.0F - this.getFadeOne());
   }

   public double easeInQuad() {
      return this.getFadeOne() * this.getFadeOne();
   }
}
