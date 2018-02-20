package net.maxsmr.testapplication.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;

import net.maxsmr.commonutils.data.FileHelper;
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

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static android.os.Environment.DIRECTORY_PICTURES;
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
                LoadRunnableInfo.class, true, AbstractSyncStorage.MAX_SIZE_UNLIMITED, new AbstractSyncStorage.DefaultAddRule<>());
//        storage.setCallbacksHandler(new Handler(Looper.getMainLooper()));
        storage.addStorageListener(this);

        loadManager = new NetworkLoadManager<>(
                NetworkLoadManager.LOADS_NO_LIMIT, 1, storage, new TaskRunnable.ITaskResultValidator<LoadRunnableInfo<LoadRunnableInfo.FileBody>, TaskRunnable<LoadRunnableInfo<LoadRunnableInfo.FileBody>>>() {

            @Override
            public boolean needToReAddTask(TaskRunnable<LoadRunnableInfo<LoadRunnableInfo.FileBody>> runnable, Throwable t) {
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
        storage.removeStorageListener(this);
        loadManager.unregisterLoadListener(this);
    }

    private void enqueueLoads(boolean viaRequest) {
        Set<File> filesToUpload = FileHelper.getFiles(DATA_DIRECTORY, FileHelper.GetMode.FILES, null, null, 1);
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

                    @NonNull
                    @Override
                    protected String getName() {
                        return name;
                    }

                    @NonNull
                    @Override
                    protected String getUrl() {
                        return UPLOAD_URL;
                    }

                    @NonNull
                    @Override
                    protected LoadRunnableInfo.LoadSettings getLoadSettings() {
                        return loadSettings;
                    }

                    @NonNull
                    @Override
                    protected LoadRunnableInfo.ContentType getContentType() {
                        return LoadRunnableInfo.ContentType.MULTIPART_FORM_DATA;
                    }

                    @NonNull
                    @Override
                    protected LoadRunnableInfo.FileBody getBody() {
                        return body;
                    }
                };
                if (!loadManager.containsLoad(id)) {
                    request.enqueue(null); // new Callback(id) // TODO unsubscribe
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
    public int getId(@NonNull LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo) {
        return getId();
    }

    @Override
    public long getProcessingNotifyInterval(@NonNull LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo) {
        return LoadListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL;
    }

    @Override
    public void onUpdateState(@NonNull LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {

    }

    @Override
    public void onResponse(@NonNull final LoadRunnableInfo<LoadRunnableInfo.FileBody> loadInfo, @NonNull final NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NonNull NetworkLoadManager.Response response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(rootView, "load with id " + loadInfo.id + " (name: " + loadInfo.name + ")  finished, state: " + loadProcessInfo.getState(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
