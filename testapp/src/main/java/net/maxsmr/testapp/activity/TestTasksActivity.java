package net.maxsmr.testapp.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import net.maxsmr.commonutils.GetMode;
import net.maxsmr.commonutils.Pair;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.tasksutils.storage.ids.IdHolder;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.AbstractCollectionSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.ListSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.ExecInfo;
import net.maxsmr.tasksutils.taskexecutor.StatInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor;
import net.maxsmr.testapp.R;
import net.maxsmr.testapp.taskutils.TestRunnableInfo;
import net.maxsmr.testapp.taskutils.TestTaskRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.commonutils.FileUtilsKt.DEPTH_UNLIMITED;
import static net.maxsmr.commonutils.FileUtilsKt.getFiles;
import static net.maxsmr.commonutils.number.RandomUtilsKt.randInt;

public class TestTasksActivity extends AppCompatActivity implements AbstractSyncStorage.IStorageListener, TaskRunnableExecutor.Callbacks<TestRunnableInfo, Void, Boolean, TestTaskRunnable>, TaskRunnable.Callbacks<TestRunnableInfo, Void, Boolean, TaskRunnable<TestRunnableInfo, Void, Boolean>> {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(TestTasksActivity.class);

    private static final int TASKS_COUNT = 20;
    private static final int CONCURRENT_TASKS_COUNT = 3;

    private AbstractCollectionSyncStorage<TestRunnableInfo> storage;
    private TaskRunnableExecutor<TestRunnableInfo, Void, Boolean, TestTaskRunnable> executor;

    private View contentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(contentView = LayoutInflater.from(this).inflate(R.layout.activity_test, null));

        storage = new ListSyncStorage<>(getFilesDir().getAbsolutePath() + File.separator + "queue", null,
                TestRunnableInfo.class, true, ListSyncStorage.MAX_SIZE_UNLIMITED,
                new AbstractSyncStorage.DefaultAddRule<>(), false);
        storage.addStorageListener(this);

        TaskRunnable.ITaskResultValidator<TestRunnableInfo, Void, Boolean, TestTaskRunnable> validator = (runnable, t) -> {
            // специально передобавляем при t == null для теста
            boolean reAdd = executor.containsTask(runnable.getId()) && t == null; // на этот момент таска всё ещё числится в "Active"
            logger.i("need to re-add runnable " + runnable + ": " + String.valueOf(reAdd).toUpperCase() + ", current queue: " + executor.getAllTasksRunnableInfos());
            return reAdd;
        };

        TaskRunnable.ITaskRestorer<TestRunnableInfo, Void, Boolean, TestTaskRunnable> restorer = runnableInfos -> {
            List<TestTaskRunnable> runnables = new ArrayList<>();
            for (TestRunnableInfo info : runnableInfos) {
                if (info != null) {
                    runnables.add(new TestTaskRunnable(info).registerCallbacks(this));
                }
            }
            return runnables;
        };

        executor = new TaskRunnableExecutor<>(TaskRunnableExecutor.TASKS_NO_LIMIT, CONCURRENT_TASKS_COUNT,
                TaskRunnableExecutor.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, "Test",
                validator, storage, new Handler(Looper.getMainLooper()));
        executor.registerCallback(this);
        executor.restoreQueueByRestorer(restorer);

        storage.startRestoreThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        storage.clear(); // специально удаляем вместе с файлами для следующего теста
        storage.release();
        executor.shutdown();
    }

    @Override
    public void onStorageRestoreStarted(long startTime) {

    }

    @Override
    public void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount) {
        Pair<Integer, TestRunnableInfo> max = storage.findByMinMaxId(false);
        IdHolder idHolder = max == null || max.second == null ? new IdHolder(0) : new IdHolder(max.second.id);
        for (int i = 0; i < TASKS_COUNT; i++) {
            executor.execute(new TestTaskRunnable(new TestRunnableInfo(idHolder.getAndIncrement(), "TestRunnable_" + idHolder.get(), randInt(0, 5000))).
                    registerCallbacks(this));
        }
        assertFiles(storage.getSize());
    }

    @Override
    public void onStorageSizeChanged(int currentSize, int previousSize) {
        assertFiles(currentSize);
    }

    private void assertFiles(int currentSize) {
        Set<File> storageFiles = getFiles(storage.getStorageDirPath(), GetMode.FILES, null);
        final int filesCount = storageFiles.size();
        if (filesCount != currentSize) {
            throw new IllegalStateException("storage files count " + filesCount + " is not equals current size " + currentSize);
        }
        Pair<Integer, TestRunnableInfo> min = storage.findByMinMaxId(true);
        Pair<Integer, TestRunnableInfo> max = storage.findByMinMaxId(false);
        logger.d("element index min=" + min + " / index max=" + max);
    }

    @Override
    public void onAddedToQueue(@NotNull final TestTaskRunnable r, final int waitingCount, final int activeCount) {
        logger.d("onAddedToQueue(), r=" + r + ", waitingCount=" + waitingCount + ", activeCount=" + activeCount);
        Snackbar.make(contentView, "task with id " + r.getId() + " was added to queue (waiting: " + waitingCount + ", active: " + activeCount + ")", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onBeforeExecute(@NotNull Thread t, @NotNull final TestTaskRunnable r, @NotNull ExecInfo<TestRunnableInfo, Void, Boolean, TestTaskRunnable> execInfo, final int waitingCount, final int activeCount) {
        logger.d("onBeforeExecute(), r=" + r + ", execInfo=" + execInfo + ", waitingCount=" + waitingCount + ", activeCount=" + activeCount);
        Snackbar.make(contentView, "task with id " + r.getId() + " starting executing (waiting: " + waitingCount + ", active: " + activeCount + ")", Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onAfterExecute(@NotNull final TestTaskRunnable r, Throwable t, @NotNull final ExecInfo<TestRunnableInfo, Void, Boolean, TestTaskRunnable> execInfo, @NotNull final StatInfo<TestRunnableInfo, Void, Boolean, TestTaskRunnable> statInfo, final int waitingCount, final int activeCount) {
        logger.d("onAfterExecute(), r=" + r + ", t=" + t + ", execInfo=" + execInfo + ", statInfo=" + statInfo + ", waitingCount=" + waitingCount + ", activeCount=" + activeCount);
        Snackbar.make(contentView, "task with id " + r.getId() + " finished executing in " + execInfo.getTimeExecuting()
                + " ms (waiting: " + waitingCount + "), active: " + activeCount, Snackbar.LENGTH_SHORT).show();
        Toast.makeText(this, "task with id " + r.getId() + " completed " + statInfo.getCompletedTimesCount() + " times", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onPreExecute(@NotNull TaskRunnable<TestRunnableInfo, Void, Boolean> task) {

    }

    @Override
    public void onProgress(@NotNull TaskRunnable<TestRunnableInfo, Void, Boolean> task, @Nullable Void aVoid) {

    }

    @Override
    public void onPostExecute(@NotNull TaskRunnable<TestRunnableInfo, Void, Boolean> task, @Nullable Boolean aBoolean) {

    }

    @Override
    public void onFailed(@NotNull TaskRunnable<TestRunnableInfo, Void, Boolean> task, @NotNull Throwable e, int runCount, int maxRunCount) {

    }

    @Override
    public void onCancelled(@NotNull TaskRunnable<TestRunnableInfo, Void, Boolean> task) {

    }
}
