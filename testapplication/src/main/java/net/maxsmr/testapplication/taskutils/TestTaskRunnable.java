package net.maxsmr.testapplication.taskutils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

public class TestTaskRunnable extends TaskRunnable<TestRunnableInfo, Void, Boolean> {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(TestTaskRunnable.class);

    public TestTaskRunnable(@NonNull TestRunnableInfo rInfo) {
        super(rInfo);
    }

    @Nullable
    @Override
    protected Boolean doWork() throws Throwable {
        logger.d("starting sleeping...");
        Thread.sleep(rInfo.delay);
        logger.d("stopped sleeping");
        return true;
    }
}
