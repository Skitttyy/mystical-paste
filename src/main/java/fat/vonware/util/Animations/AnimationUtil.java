package fat.vonware.util.Animations;

import fat.vonware.util.Timer;
import java.util.function.Function;

public final class AnimationUtil {
   private AnimationUtil() {
   }

   public static float easeInOutCubic(float t) {
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow((double)(-2.0F * t + 2.0F), (double)3.0F) / 2.0F;
   }

   public static float easeInCubic(float t) {
      return t * t * t;
   }

   public static float easeOutCubic(float t) {
      return 1.0F - (float)Math.pow((double)(1.0F - t), (double)3.0F);
   }

   public static float easeInOutQuad(float t) {
      return t < 0.5F ? 2.0F * t * t : 1.0F - (float)Math.pow((double)(-2.0F * t + 2.0F), (double)2.0F) / 2.0F;
   }

   public static float easeOutQuad(float t) {
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   public static float easeOutQuart(float t) {
      return 1.0F - (float)Math.pow((double)(1.0F - t), (double)4.0F);
   }

   public static float easeInOutQuint(float t) {
      return t < 0.5F ? 16.0F * t * t * t * t * t : 1.0F - (float)Math.pow((double)(-2.0F * t + 2.0F), (double)5.0F) / 2.0F;
   }

   public static float easeOutBack(float t) {
      float c1 = 1.70158F;
      float c3 = 2.70158F;
      return 1.0F + 2.70158F * (float)Math.pow((double)(t - 1.0F), (double)3.0F) + 1.70158F * (float)Math.pow((double)(t - 1.0F), (double)2.0F);
   }

   public static float easeOutExpo(float t) {
      return t >= 1.0F ? 1.0F : 1.0F - (float)Math.pow((double)2.0F, (double)-10.0F * (double)t);
   }

   public static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   public static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   public static final class DeltaAnimation {
      private float progress;
      private final float durationMs;
      private final Timer timer = new Timer();

      public DeltaAnimation(float initialProgress, float durationMs) {
         this.progress = initialProgress;
         this.durationMs = durationMs;
         this.timer.reset();
      }

      public float tick(boolean target) {
         long elapsedMs = this.timer.getPassedTimeMs();
         this.timer.reset();
         float delta = (float)elapsedMs / this.durationMs;
         if (target) {
            this.progress = Math.min(1.0F, this.progress + delta);
         } else {
            this.progress = Math.max(0.0F, this.progress - delta);
         }

         return this.progress;
      }

      public float tick(float target) {
         long elapsedMs = this.timer.getPassedTimeMs();
         this.timer.reset();
         float delta = (float)elapsedMs / this.durationMs;
         if (this.progress < target) {
            this.progress = Math.min(target, this.progress + delta);
         } else {
            this.progress = Math.max(target, this.progress - delta);
         }

         return this.progress;
      }

      public float tickEased(boolean target) {
         return AnimationUtil.easeInOutCubic(this.tick(target));
      }

      public float tickEased(boolean target, Function<Float, Float> easingFn) {
         return (Float)easingFn.apply(this.tick(target));
      }

      public float getProgress() {
         return this.progress;
      }

      public float getEased() {
         return AnimationUtil.easeInOutCubic(this.progress);
      }

      public void setProgress(float p) {
         this.progress = AnimationUtil.clamp(p, 0.0F, 1.0F);
      }

      public void reset(boolean open) {
         this.progress = open ? 1.0F : 0.0F;
         this.timer.reset();
      }

      public boolean isAnimating(boolean target) {
         return target ? this.progress < 1.0F : this.progress > 0.0F;
      }

      public boolean isFinished(boolean target) {
         return !this.isAnimating(target);
      }
   }

   public static final class LerpAnimation {
      private float value;
      private final float speed;
      private final float snapThreshold;
      private final Timer timer = new Timer();

      public LerpAnimation(float initialValue, float speed, float snapThreshold) {
         this.value = initialValue;
         this.speed = speed;
         this.snapThreshold = snapThreshold;
         this.timer.reset();
      }

      public float tickEased(boolean open, EasingType type) {
         float raw = this.tick(open);
         switch (type) {
            case EASE_OUT_QUART:
               return AnimationUtil.easeOutQuart(raw);
            case EASE_IN_OUT_QUINT:
               return AnimationUtil.easeInOutQuint(raw);
            case EASE_OUT_BACK:
               return AnimationUtil.easeOutBack(raw);
            case EASE_OUT_EXPO:
               return AnimationUtil.easeOutExpo(raw);
            default:
               return AnimationUtil.easeInOutCubic(raw);
         }
      }

      public float tick(float target) {
         long elapsedMs = this.timer.getPassedTimeMs();
         this.timer.reset();
         float deltaSeconds = (float)elapsedMs / 1000.0F;
         float step = 1.0F - (float)Math.pow((double)(1.0F - this.speed), (double)(deltaSeconds * 60.0F));
         this.value += (target - this.value) * step;
         if (Math.abs(this.value - target) < this.snapThreshold) {
            this.value = target;
         }

         return this.value;
      }

      public float tick(boolean open) {
         return this.tick(open ? 1.0F : 0.0F);
      }

      public float tickEased(boolean open) {
         return AnimationUtil.easeInOutCubic(this.tick(open));
      }

      public float getValue() {
         return this.value;
      }

      public float getEased() {
         return AnimationUtil.easeInOutCubic(this.value);
      }

      public void setValue(float v) {
         this.value = AnimationUtil.clamp(v, 0.0F, 1.0F);
      }

      public void reset(boolean open) {
         this.value = open ? 1.0F : 0.0F;
         this.timer.reset();
      }

      public boolean isFinished(boolean target) {
         return this.value == (target ? 1.0F : 0.0F);
      }

      public static enum EasingType {
         EASE_OUT_QUART,
         EASE_IN_OUT_QUINT,
         EASE_OUT_BACK,
         EASE_OUT_EXPO,
         EASE_IN_OUT_CUBIC;
      }
   }
}
