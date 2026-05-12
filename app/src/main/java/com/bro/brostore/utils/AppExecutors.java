package com.example.appstoredemo.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides a shared {@link ExecutorService} for background operations.
 */
public final class AppExecutors {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private AppExecutors() {}

    /**
     * Returns the singleton IO executor.
     *
     * @return executor service for IO bound work.
     * @throws IllegalStateException if the executor has been shut down.
     */
    public static ExecutorService io() {
        if (IO.isShutdown()) {
            throw new IllegalStateException("Executor has been shut down");
        }
        return IO;
    }
}
