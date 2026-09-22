package fat.vonware.ducks;

import net.minecraft.util.Timer;

public interface IMinecraft {
   int getRightClickDelay();

   void setRightClickDelay(int var1);

   Timer getTimer();
}
