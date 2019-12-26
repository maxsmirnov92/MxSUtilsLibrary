package net.maxsmr.tasksutils.runnable;

import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;

public abstract class RunnableInfoRunnable<I extends RunnableInfo> implements Runnable {

    @NotNull
    public final I rInfo;

    protected RunnableInfoRunnable(@NotNull I rInfo) {
        this.rInfo = rInfo;
    }

    public int getId() {
        return rInfo.id;
    }

    @Override
    @NotNull
    public String toString() {
        return "RunnableInfoRunnable{" +
                "rInfo=" + rInfo +
                '}';
    }
}
