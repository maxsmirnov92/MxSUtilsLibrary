package net.maxsmr.testapp.taskutils;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestTaskRunnable extends TaskRunnable<TestRunnableInfo, Void, Boolean> {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(TestTaskRunnable.class);

    public TestTaskRunnable(@NotNull TestRunnableInfo rInfo) {
        super(rInfo);
    }

    @Nullable
    @Override
    public Boolean doWork() throws Throwable {
        logger.d("starting sleeping...");
        Thread.sleep(rInfo.delay);
        logger.d("stopped sleeping");
        return true;
    }
}
