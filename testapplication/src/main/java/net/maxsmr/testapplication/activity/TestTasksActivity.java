package net.maxsmr.testapplication.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.MathUtils;
import net.maxsmr.tasksutils.storage.ids.IdHolder;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.AbstractCollectionSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.ListSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.ExecInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor;
import net.maxsmr.testapplication.R;
import net.maxsmr.testapplication.taskutils.TestRunnableInfo;
import net.maxsmr.testapplication.taskutils.TestTaskRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

public class TestTasksActivity extends AppCompatActivity implements AbstractSyncStorage.IStorageListener, TaskRunnableExecutor.Callbacks<TestRunnableInfo, TestTaskRunnable> {

    private static final Logger logger = LoggerFactory.getLogger(TestTasksActivity.class);

    private static final int TASKS_COUNT = 10;

    private AbstractCollectionSyncStorage<TestRunnableInfo> storage;
    private TaskRunnableExecutor<TestRunnableInfo, TestTaskRunnable> executor;

    private View contentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentView = LayoutInflater.from(this).inflate(R.layout.activity_test, null));

        storage = new ListSyncStorage<>(getFilesDir().getAbsolutePath() + File.separator + "queue", null,
                TestRunnableInfo.class, true, ListSyncStorage.MAX_SIZE_UNLIMITED,
                new AbstractSyncStorage.DefaultAddRule<TestRunnableInfo>());
        storage.addStorageListener(this);

        TaskRunnable.ITaskResultValidator<TestRunnableInfo, TestTaskRunnable> validator = new TaskRunnable.ITaskResultValidator<TestRunnableInfo, TestTaskRunnable>() {
            @Override
            public boolean needToReAddTask(TestTaskRunnable runnable, Throwable t) {
                return executor.containsTask(runnable.getId()) && t != null; // на этот момент таска всё ещё числится в "Active", но уже не "running"
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
                validator, storage, new Handler(Looper.getMainLooper()));
        executor.registerCallback(this);
        executor.restoreQueueByRestorer(restorer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        storage.clear(); // специально удаляем вместе с файлами для следующего теста
        storage.release();
        executor.shutdown();
        executor.unregisterCallback(this);
    }

    @Override
    public void onStorageRestoreStarted(long startTime) {

    }

    @Override
    public void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount) {
        Pair<Integer, TestRunnableInfo> max = storage.findByMinMaxId(false);
        IdHolder idHolder = max == null ? new IdHolder(0) : new IdHolder(max.second.id);
        for (int i = 0; i < TASKS_COUNT; i++) {
            executor.execute(new TestTaskRunnable(new TestRunnableInfo(idHolder.getAndIncrement(), "TestRunnable", MathUtils.randInt(0, 5000))));
        }
        assertFiles(storage.getSize());
    }

    @Override
    public void onStorageSizeChanged(int currentSize, int previousSize) {
        assertFiles(currentSize);
    }

    private void assertFiles(int currentSize) {
        Set<File> storageFiles = FileHelper.getFiles(Collections.singletonList(storage.getStorageDirPath()), FileHelper.GetMode.FILES, null, null, FileHelper.DEPTH_UNLIMITED);
        assertEquals(storageFiles.size(), currentSize);
        Pair<Integer, TestRunnableInfo> min = storage.findByMinMaxId(true);
        Pair<Integer, TestRunnableInfo> max = storage.findByMinMaxId(false);
        logger.debug("element index min=" + min + " / index max=" + max);
    }

    @Override
    public void onAddedToQueue(final TestTaskRunnable r, final int waitingCount, final int activeCount) {
        logger.debug("onAddedToQueue(), r=" + r + ", waitingCount=" + waitingCount + ", activeCount=" + activeCount);
        Snackbar.make(contentView, "task with id " + r.getId() + " was added to queue (waiting: " + waitingCount + "), active: " + activeCount, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onBeforeExecute(Thread t, final TestTaskRunnable r, ExecInfo<TestRunnableInfo, TestTaskRunnable> execInfo, final int waitingCount, final int activeCount) {
        logger.debug("onBeforeExecute(), r=" + r + ", execInfo=" + execInfo);
        Snackbar.make(contentView, "task with id " + r.getId() + " starting executing (waiting: " + waitingCount + "), active: " + activeCount, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onAfterExecute(final TestTaskRunnable r, Throwable t, final ExecInfo<TestRunnableInfo, TestTaskRunnable> execInfo, final int waitingCount, final int activeCount) {
        logger.debug("onAfterExecute(), r=" + r + ", t=" + t + ", execInfo=" + execInfo);
        Snackbar.make(contentView, "task with id " + r.getId() + " finished executing in " + execInfo.getTimeExecuting()
                + " ms (waiting: " + waitingCount + "), active: " + activeCount, Snackbar.LENGTH_SHORT).show();
    }
}
