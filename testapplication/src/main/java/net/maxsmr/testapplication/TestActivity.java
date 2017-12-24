package net.maxsmr.testapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import net.maxsmr.commonutils.data.MathUtils;
import net.maxsmr.tasksutils.storage.ids.IdHolder;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.ListSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor;
import net.maxsmr.testapplication.taskutils.TestRunnableInfo;
import net.maxsmr.testapplication.taskutils.TestTaskRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestActivity extends AppCompatActivity {

    private static final int TASKS_COUNT = 10;

    private AbstractSyncStorage<TestRunnableInfo> storage;
    private TaskRunnableExecutor<TestRunnableInfo, TestTaskRunnable> executor;

    private IdHolder idHolder = new IdHolder(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        storage = new ListSyncStorage<>(getFilesDir().getAbsolutePath() + "/queue", TestRunnableInfo.class, true, ListSyncStorage.MAX_SIZE_UNLIMITED, new AbstractSyncStorage.IAddRule<TestRunnableInfo>() {
            @Override
            public boolean allowAddIfFull() {
                return true;
            }

            @Override
            public void removeAny(AbstractSyncStorage<TestRunnableInfo> fromStorage) {
                fromStorage.pollFirst();
            }
        });

        TaskRunnable.ITaskResultValidator<TestRunnableInfo, TestTaskRunnable> validator = new TaskRunnable.ITaskResultValidator<TestRunnableInfo, TestTaskRunnable>() {
            @Override
            public boolean needToReAddTask(TestTaskRunnable runnable, Throwable t) {
                return executor.containsTask(runnable.getId()) && t == null;
            }
        };

        TaskRunnable.ITaskRestorer<TestRunnableInfo, TestTaskRunnable> restorer = new TaskRunnable.ITaskRestorer<TestRunnableInfo, TestTaskRunnable>() {
            @Override
            public List<TestTaskRunnable> fromRunnableInfos(Collection<TestRunnableInfo> runnableInfos) {
                List<TestTaskRunnable> runnables = new ArrayList<>();
                for (TestRunnableInfo info : runnableInfos) {
                    runnables.add(new TestTaskRunnable(info));
                }
                return runnables;
            }
        };

        executor = new TaskRunnableExecutor<>(TaskRunnableExecutor.TASKS_NO_LIMIT, 1,
                TaskRunnableExecutor.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, "Test",
                validator,
                storage,
                restorer
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        for (int i = 0; i < TASKS_COUNT; i++) {
            executor.execute(new TestTaskRunnable(new TestRunnableInfo(idHolder.incrementAndGet(), "TestRunnable", MathUtils.randInt(0, 5000))));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        executor.cancelAllTasks();
    }
}
