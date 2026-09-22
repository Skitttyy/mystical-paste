package fat.vonware.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Locks {
   public static final Lock WINDOW_CLICK_LOCK = new ReentrantLock();

   public static void acquire(Lock lock, Runnable action) {
      lock.lock();

      try {
         action.run();
      } finally {
         lock.unlock();
      }

   }
}
