package fat.vonware.util.Chat;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fat.vonware.util.Util;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadUtil implements Util {
   private static final int CPU_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors());
   private static final AtomicInteger THREAD_ID = new AtomicInteger();
   public static final ThreadFactory FACTORY = namedDaemonFactory("Vonware-Worker");

   private ThreadUtil() {
   }

   public static ScheduledExecutorService newDaemonScheduledExecutor(String name) {
      ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, namedDaemonFactory("Vonware-" + name));
      executor.setRemoveOnCancelPolicy(true);
      executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
      executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
      return executor;
   }

   public static ExecutorService newDaemonCachedThreadPool() {
      return newBoundedExecutor("Vonware-Async", Math.min(4, CPU_COUNT), Math.max(4, Math.min(8, CPU_COUNT)), 128);
   }

   public static ExecutorService newFixedThreadPool(int size) {
      int threads = Math.max(1, Math.min(size, 8));
      return newBoundedExecutor("Vonware-Fixed", threads, threads, 256);
   }

   public static ExecutorService newBoundedExecutor(String name, int core, int max, int queueCapacity) {
      ThreadPoolExecutor executor = new ThreadPoolExecutor(Math.max(1, core), Math.max(Math.max(1, core), max), 30L, TimeUnit.SECONDS, new ArrayBlockingQueue(Math.max(1, queueCapacity)), namedDaemonFactory(name), new ThreadPoolExecutor.DiscardOldestPolicy());
      executor.allowCoreThreadTimeOut(true);
      return executor;
   }

   public static Thread startDaemon(String name, Runnable task) {
      Thread thread = namedDaemonFactory(name).newThread(task);
      thread.start();
      return thread;
   }

   public static Thread startDaemonDelayed(String name, long delayMs, Runnable task) {
      return startDaemon(name, () -> {
         try {
            Thread.sleep(Math.max(0L, delayMs));
            if (!Thread.currentThread().isInterrupted()) {
               task.run();
            }
         } catch (InterruptedException var5) {
            Thread.currentThread().interrupt();
         } catch (Throwable t) {
            System.err.println("[Vonware] Uncaught async error in " + name);
            t.printStackTrace();
         }

      });
   }

   public static ThreadFactoryBuilder newDaemonThreadFactoryBuilder() {
      return (new ThreadFactoryBuilder()).setDaemon(true).setUncaughtExceptionHandler((thread, throwable) -> {
         System.err.println("[Vonware] Uncaught exception in " + thread.getName());
         throwable.printStackTrace();
      });
   }

   private static ThreadFactory namedDaemonFactory(String prefix) {
      return (runnable) -> {
         Thread thread = new Thread(runnable, prefix + "-" + THREAD_ID.incrementAndGet());
         thread.setDaemon(true);
         thread.setPriority(5);
         thread.setUncaughtExceptionHandler((t, e) -> {
            System.err.println("[Vonware] Uncaught exception in " + t.getName());
            e.printStackTrace();
         });
         return thread;
      };
   }
}
