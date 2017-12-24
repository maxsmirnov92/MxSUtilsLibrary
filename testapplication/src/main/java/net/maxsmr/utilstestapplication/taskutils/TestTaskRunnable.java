package net.maxsmr.utilstestapplication.taskutils;

import android.support.annotation.NonNull;

import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTaskRunnable extends TaskRunnable<TestRunnableInfo> {

    private static final Logger logger = LoggerFactory.getLogger(TestTaskRunnable.class);

    public TestTaskRunnable(@NonNull TestRunnableInfo rInfo) {
        super(rInfo);
    }

    @Override
    public void run() {
        logger.debug("starting sleeping...");
        try {
            Thread.sleep(rInfo.delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        logger.debug("stopped sleeping");
    }
}
