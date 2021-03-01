package net.maxsmr.tasksutils;

import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static net.maxsmr.commonutils.text.TextUtilsKt.isEmpty;

public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadId = new AtomicInteger(0);

    public final String threadName;

    public NamedThreadFactory(String threadName) {
        if (isEmpty(threadName)) {
            throw new IllegalArgumentException("threadName can't be empty");
        }
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        return new Thread(r, r instanceof TaskRunnable? threadName + " :: " + ((TaskRunnable) r).rInfo.id : threadName + " :: " + threadId.getAndIncrement());
    }
}