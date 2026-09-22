package fat.vonware.util.Animations;

import net.minecraft.util.math.MathHelper;

public class ChatAnimation {
   private final double start;
   private final double end;
   private double current;
   private double progress;
   private boolean playing;
   private long lastTime;
   private double per;

   public ChatAnimation(long durationMs, double start, double end) {
      this.start = start;
      this.end = end;
      this.current = start;
      this.per = (end - start) / (double)durationMs;
      this.playing = true;
      this.lastTime = System.currentTimeMillis();
   }

   public void update() {
      if (this.playing) {
         long now = System.currentTimeMillis();
         long delta = now - this.lastTime;
         this.lastTime = now;
         this.current = this.start + this.progress;
         this.progress += this.per * (double)delta;
         this.current = MathHelper.clamp(this.current, Math.min(this.start, this.end), Math.max(this.start, this.end));
         if (this.current >= this.end) {
            this.current = this.end;
            this.playing = false;
         }
      }

   }

   public float getCurrent() {
      return (float)this.current;
   }

   public boolean isPlaying() {
      return this.playing;
   }
}
