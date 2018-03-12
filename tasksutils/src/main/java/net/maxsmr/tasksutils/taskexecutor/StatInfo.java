package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StatInfo<I extends RunnableInfo, T extends TaskRunnable<I>> {

    @NonNull
    private final List<ExecInfo<I, T>> execInfos = new ArrayList<>();

    @NonNull
    private final T taskRunnable;

    public StatInfo(@NonNull T taskRunnable) {
        this.taskRunnable = taskRunnable;
    }

    @NonNull
    public T getTaskRunnable() {
        return taskRunnable;
    }

    public int getCompletedTimesCount() {
        return execInfos.size();
    }

    public List<ExecInfo<I, T>> getExecInfos() {
        return Collections.unmodifiableList(execInfos);
    }

    void addExecInfo(@NonNull ExecInfo<I, T> execInfo) {
        execInfos.add(execInfo);
    }

    void resetExecInfos() {
        for (ExecInfo<I, T> e : execInfos) {
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
