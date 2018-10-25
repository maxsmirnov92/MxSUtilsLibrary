package net.maxsmr.tasksutils.taskexecutor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatInfo<I extends RunnableInfo, ProgressInfo, Result, T extends TaskRunnable<I, ProgressInfo, Result>> {

    @NotNull
    private final List<ExecInfo<I, ProgressInfo, Result, T>> execInfos = new ArrayList<>();

    @NotNull
    private final T taskRunnable;

    public StatInfo(@NotNull T taskRunnable) {
        this.taskRunnable = taskRunnable;
    }

    @NotNull
    public T getTaskRunnable() {
        return taskRunnable;
    }

    public int getCompletedTimesCount() {
        return execInfos.size();
    }

    public List<ExecInfo<I, ProgressInfo, Result, T>> getExecInfos() {
        return Collections.unmodifiableList(execInfos);
    }

    void addExecInfo(@NotNull ExecInfo<I, ProgressInfo, Result, T> execInfo) {
        execInfos.add(execInfo);
    }

    void resetExecInfos() {
        for (ExecInfo<I, ProgressInfo, Result, T> e : execInfos) {
            if (e != null) {
                e.reset();
            }
        }
        execInfos.clear();
    }


    @Override
    public String toString() {
        return "StatInfo{" +
                "execInfos=" + execInfos +
                ", taskRunnable=" + taskRunnable +
                '}';
    }
}
