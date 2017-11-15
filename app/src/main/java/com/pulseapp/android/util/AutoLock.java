package com.pulseapp.android.util;

/**
 * Created by bajaj on 29/3/16.
 */
import android.annotation.TargetApi;
import android.os.Build;

import java.util.concurrent.locks.Lock;

/**
 * Makes a {@link java.util.concurrent.locks.Lock} into an {@link AutoCloseable} for use with try-with-resources:
 * <p/>
 * {@code
 * try (AutoLock al = new AutoLock(lock)) {
 * ...
 * }
 * }
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
public final class AutoLock implements AutoCloseable {

    private final Lock lock;

    /**
     * Locks the given {@link java.util.concurrent.locks.Lock}.
     *
     * @param lock lock to manage
     */
    public AutoLock(Lock lock) {
        this.lock = lock;
        lock.lock();
    }

    /**
     * Unlocks the underlying {@link java.util.concurrent.locks.Lock}.
     */
    @Override
    public void close() {
        lock.unlock();
    }
}
