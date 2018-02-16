package net.maxsmr.tasksutils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CustomHandlerThread extends HandlerThread {

    private static final Logger logger = LoggerFactory.getLogger(CustomHandlerThread.class);

    private Handler handler = null;

    @Nullable
    private ILooperPreparedListener listener;

    public CustomHandlerThread(String name) {
        this(name, null);
    }

    public CustomHandlerThread(String name, @Nullable ILooperPreparedListener listener) {
        super(name);
        this.listener = listener;
    }

    public boolean isLooperPrepared() {
        return handler != null && getLooper() != null;
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler(getLooper());
        if (listener != null) {
            listener.onLooperPrepared();
        }
    }

    public synchronized void addTask(@NonNull Runnable run) {
        addTask(run, 0);
    }

    public synchronized void addTask(@NonNull Runnable run, final long delay) {

        if (!isLooperPrepared())
            throw new RuntimeException("Looper was not prepared()");

        if (!isAlive())
            throw new RuntimeException("Thread " + getName() + " is not alive");

        if (isInterrupted())
            throw new RuntimeException("Thread " + getName() + " interrupted");

        if (!handler.postDelayed(run, delay >= 0 ? delay : 0)) {
            throw new RuntimeException("can't post handler message");
        }
    }

    public synchronized void removeTask(@NonNull Runnable r) {

        if (!isLooperPrepared())
            throw new RuntimeException("Looper was not prepared()");

        if (!isAlive())
            throw new RuntimeException("Thread " + getName() + " is not alive");

        if (isInterrupted())
            throw new RuntimeException("Thread " + getName() + " interrupted");

        handler.removeCallbacks(r);
    }


    @Override
    public boolean quit() {
        handler = null;
        interrupt();
        return super.quit();
    }

    /**
     * join() call does not release reliably after that
     */
    @Override
    public boolean quitSafely() {
        handler = null;
        interrupt();
        return super.quitSafely();
    }

    public static void awaitLatch(CountDownLatch latch, int milliseconds, Runnable callbackWhenNormal, Runnable callbackWhenExpired) {
        if (latch != null && milliseconds >= 0) {
            long startTime = System.currentTimeMillis();
            try {
                latch.await(milliseconds, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("an InterruptedException occurred during await()", e);
                if (callbackWhenExpired != null) {
                    callbackWhenExpired.run();
                }
            }
            if (milliseconds > 0 && System.currentTimeMillis() - startTime >= milliseconds) {
                if (callbackWhenExpired != null) {
                    callbackWhenExpired.run();
                }
            } else {
                if (callbackWhenNormal != null) {
                    callbackWhenNormal.run();
                }
            }
        }
    }

    public static void runOnUiThreadSync(@NonNull final Runnable run) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            run.run();
        } else {
            final CountDownLatch latch = new CountDownLatch(1);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    run.run();
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    public interface ILooperPreparedListener {

        void onLooperPrepared();
    }
}
