package mp3.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MyPoolExecutor {
    private static ExecutorService executor = Executors.newCachedThreadPool();

    public static Future<?> submitTask(Runnable runnable) {
        return executor.submit(runnable);
    }

    public static void shutdownNow() {
        executor.shutdownNow();
    }
}
