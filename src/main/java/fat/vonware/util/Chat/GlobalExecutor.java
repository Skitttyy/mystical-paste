package fat.vonware.util.Chat;

import java.util.concurrent.ExecutorService;

public interface GlobalExecutor {
   ExecutorService EXECUTOR = ThreadUtil.newDaemonCachedThreadPool();
   ExecutorService FIXED_EXECUTOR = ThreadUtil.newFixedThreadPool((int)((double)Runtime.getRuntime().availableProcessors() / (double)1.5F));

   static void shutdownNow() {
      EXECUTOR.shutdownNow();
      FIXED_EXECUTOR.shutdownNow();
   }
}
