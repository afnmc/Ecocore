package com.azthera.ecocore.util;
 
import com.azthera.ecocore.integration.SchedulerAdapter;
 
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
 
/**
 * Central helper for moving work off the main thread (database I/O, HTTP calls)
 * and safely hopping back onto the main/region thread via {@link SchedulerAdapter}.
 * Uses virtual threads for I/O-bound work so blocking JDBC calls do not exhaust
 * a fixed-size platform thread pool under high player counts.
 */
public final class AsyncUtil {
 
    private final SchedulerAdapter schedulerAdapter;
    private final ExecutorService ioExecutor;
 
    public AsyncUtil(SchedulerAdapter schedulerAdapter) {
        this.schedulerAdapter = schedulerAdapter;
        this.ioExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }
 
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ioExecutor);
    }
 
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, ioExecutor);
    }
 
    /**
     * Runs {@code asyncSupplier} off the main thread, then hops back onto the
     * main/region thread to consume the result via {@code syncConsumer}.
     * If the async step throws, the exception is rethrown on the main thread
     * so it surfaces in server logs instead of vanishing silently.
     */
    public <T> void supplyAsyncThenSync(Supplier<T> asyncSupplier, Consumer<T> syncConsumer) {
        supplyAsync(asyncSupplier).whenComplete((result, throwable) -> {
            if (throwable != null) {
                schedulerAdapter.runSync(() -> {
                    throw new RuntimeException(throwable);
                });
                return;
            }
            schedulerAdapter.runSync(() -> syncConsumer.accept(result));
        });
    }
 
    public void shutdown() {
        ioExecutor.shutdown();
    }
}
