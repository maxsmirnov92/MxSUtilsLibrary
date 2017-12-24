package net.maxsmr.tasksutils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadId = new AtomicInteger(0);

    public final String threadName;

    public NamedThreadFactory(String threadName) {
        if (TextUtils.isEmpty(threadName)) {
            throw new IllegalArgumentException("threadName can't be empty");
        }
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(@NonNull Runnable r) {
        return new Thread(r, r instanceof TaskRunnable? threadName + " :: " + ((TaskRunnable) r).rInfo.id : threadName + " :: " + threadId.getAndIncrement());
    }

}