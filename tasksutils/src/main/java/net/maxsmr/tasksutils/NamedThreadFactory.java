package net.maxsmr.tasksutils;

import org.jetbrains.annotations.NotNull;

import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger threadId = new AtomicInteger(0);

    public final String threadName;

    public NamedThreadFactory(String threadName) {
        if (StringUtils.isEmpty(threadName)) {
            throw new IllegalArgumentException("threadName can't be empty");
        }
        this.threadName = threadName;
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        return new Thread(r, r instanceof TaskRunnable? threadName + " :: " + ((TaskRunnable) r).rInfo.id : threadName + " :: " + threadId.getAndIncrement());
    }
}