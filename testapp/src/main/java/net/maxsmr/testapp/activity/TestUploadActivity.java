package net.maxsmr.testapp.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import net.maxsmr.commonutils.GetMode;
import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.networkutils.loadutil.requests.AbstractRequest;
import net.maxsmr.tasksutils.storage.ids.IdHolder;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.AbstractCollectionSyncStorage;
import net.maxsmr.tasksutils.storage.sync.collection.ListSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static android.os.Environment.DIRECTORY_PICTURES;
import static net.maxsmr.commonutils.FileUtilsKt.getFiles;
import static net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo.LoadSettings.RETRY_LIMIT_NONE;

public class TestUploadActivity extends AppCompatActivity implements AbstractSyncStorage.IStorageListener, LoadListener<LoadRunnableInfo<LoadRunnableInfo.FileBody>> {

    private static final File DATA_DIRECTORY = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES);

    private static final String UPLOAD_URL = "http://google.com";

    private final IdHolder ids = new IdHolder(0);

    private AbstractCollectionSyncStorage<LoadRunnableInfo<LoadRunnableInfo.FileBody>> storage;
    private NetworkLoadManager<LoadRunnableInfo.FileBody, LoadRunnableInfo<LoadRunnableInfo.FileBody>> loadManager;

    private View rootView;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        storage = new ListSyncStorage(getFilesDir().getAbsolutePath() + File.separator + "queue", null,
                LoadRunnableInfo.class, true, AbstractSyncStorage.MAX_SIZE_UNLIMITED, new AbstractSyncStorage.DefaultAddRule<>(), true);
//        storage.setCallbacksHandler(new Handler(Looper.getMainLooper()));
        storage.addStorageListener(this);

        loadManager = new NetworkLoadManager(
                NetworkLoadManager.LOADS_NO_LIMIT, 1, storage, new TaskRunnable.ITaskResultValidator<LoadRunnableInfo<LoadRunnableInfo.FileBody>, Void, Void, TaskRunnable<LoadRunnableInfo<LoadRunnableInfo.FileBody>, Void, Void>>() {

            @Override
            public boolean needToReAddTask(TaskRunnable<LoadRunnableInfo<LoadRunnableInfo.FileBody>, Void, Void> runnable, Throwable t) {
                LoadListener.STATE state = loadManager.getLastStateForId(runnable.getId());
                return state == LoadListener.STATE.FAILED_RETRIES_EXCEEDED;
            }
        });
        loadManager.registerLoadListener(this);
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        rootView = parent;
        return super.onCreateView(parent, name, context, attrs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enqueueLoads(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loadManager.cancelAllLoads();
        storage.removeStorageListener(this);
        loadManager.unregisterLoadListener(this);
    }

    private void enqueueLoads(boolean viaRequest) {
        Set<File> filesToUpload = getFiles(DATA_DIRECTORY, GetMode.FILES, null);
        for (File f : filesToUpload) {
            LoadRunnableInfo.LoadSettings.Builder settingsBuilder = new LoadRunnableInfo.LoadSettings.Builder();
            settingsBuilder.connectionTimeout(TimeUnit.SECONDS.toMillis(30));
            settingsBuilder.readWriteTimeout(TimeUnit.SECONDS.toMillis(40));
            settingsBuilder.retryLimit(RETRY_LIMIT_NONE);
            settingsBuilder.notifyRead(true);
            settingsBuilder.notifyWrite(true);
            settingsBuilder.readBodyMode(LoadRunnableInfo.LoadSettings.ReadBodyMode.STRING);
            final LoadRunnableInfo.LoadSettings loadSettings = settingsBuilder.build();

            final int id = ids.incrementAndGet();

            final LoadRunnableInfo.FileBody body = new LoadRunnableInfo.FileBody(f.getName(), f, false, false);
            final String name = body.name + " (" + id + ")";

            if (viaRequest) {

                AbstractRequest<LoadRunnableInfo.FileBody, AbstractRequest.Callback> request = new AbstractRequest<LoadRunnableInfo.FileBody, AbstractRequest.Callback>(id, loadManager) {

                    @NotNull
                    @Override
                    protected String getName() {
                        return name;
                    }

                    @NotNull
                    @Override
                    protected String getUrl() {
                        return UPLOAD_URL;
                    }

                    @NotNull
                    @Override
                    protected LoadRunnableInfo.LoadSettings getLoadSettings() {
                        return loadSettings;
                    }

                    @NotNull
                    @Override
                    protected LoadRunnableInfo.ContentType getContentType() {
                        return LoadRunnableInfo.ContentType.MULTIPART_FORM_DATA;
                    }

                    @NotNull
                    @Override
                    protected LoadRunnableInfo.FileBody getBody() {
                        return body;
                    }

                    @NotNull
                    @Override
                    protected List<Integer> getAcceptableResponseCodes() {
                        return Collections.emptyList();
                    }
                };
                if (!loadManager.containsLoad(id)) {
                    request.enqueue(new AbstractRequest.Callback(id, true));
                }

            } else {

                LoadRunnableInfo.Builder<LoadRunnableInfo.FileBody, LoadRunnableInfo<LoadRunnableInfo.FileBody>> builder =
                        new LoadRunnableInfo.Builder<>(id, UPLOAD_URL, loadSettings);
                builder.requestMethod(LoadRunnableInfo.RequestMethod.POST);
                builder.contentType(LoadRunnableInfo.ContentType.MULTIPART_FORM_DATA);
                builder.name(name);
                builder.body(body);

                if (!loadManager.containsLoad(id)) {
                    loadManager.enqueueLoad(builder.build());
                }
            }

        }
    }

    @Override
    public void onStorageRestoreStarted(long startTime) {

    }

    @Override
    public void onStorageRestoreFinished(long endTime, long processingTime, int restoredElementsCount) {

    }

    @Override
    public void onStorageSizeChanged(int currentSize, int previousSize) {

    }


    @Override
    public void onLoadAddedToQueue(int id, int waitingLoads, int activeLoads) {

    }

    @Override
    public void onLoadRemovedFromQueue(int id, int waitingLoads, int activeLoads) {

    }

    @Override
    public int getId() {
        return RunnableInfo.NO_ID;
    }

    @Override
    public int getId(@NotNull LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo) {
        return getId();
    }

    @Override
    public long getProcessingNotifyInterval(@NotNull LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo) {
        return LoadListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL;
    }

    @Override
    public void onUpdateState(@NotNull LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo, @NotNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {

    }

    @Override
    public void onResponse(@NotNull final LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo, @NotNull final NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NotNull NetworkLoadManager.Response response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(rootView, "load with id " + loadInfo.id + " (name: " + loadInfo.name + ")  finished, state: " + loadProcessInfo.getState(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
