package fat.vonware.util.OtherRenderShit.ca;

import fat.vonware.Vonware;
import fat.vonware.util.RotationUtil;
import fat.vonware.util.Util;

public class Smoother implements Util {
   public float easeTo(float target, float speed) {
      float yawChange = RotationUtil.wrapAngleTo180_float(target - Vonware.rotationManager.getServerYaw());
      float yawChangeFactor = Math.abs(yawChange) / 180.0F;
      float maxYawChange = speed * 100.0F * yawChangeFactor;
      if (Math.abs(yawChange) >= maxYawChange) {
         if (yawChange > 0.0F) {
            yawChange = maxYawChange;
         } else {
            yawChange = -maxYawChange;
         }
      }

      return Vonware.rotationManager.getServerYaw() + yawChange;
   }
}
