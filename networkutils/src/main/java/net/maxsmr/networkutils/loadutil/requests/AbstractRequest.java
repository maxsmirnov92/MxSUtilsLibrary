package net.maxsmr.networkutils.loadutil.requests;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRequest<C extends AbstractRequest.Callback> {

    @Nullable
    protected final Handler handler;

    @NonNull
    protected final NetworkLoadManager manager;

    @Nullable
    private C callback;

    public AbstractRequest(@NonNull NetworkLoadManager manager) {
        this.manager = manager;
        this.handler = Looper.myLooper() != null ? new Handler(Looper.myLooper()) : new Handler(Looper.getMainLooper());
    }

    @NonNull
    public NetworkLoadManager getManager() {
        return manager;
    }

    @Nullable
    public C getCallback() {
        return callback;
    }

    public static List<Integer> defaultAcceptableResponseCodes() {
        List<Integer> codes = new ArrayList<>();
        for (int c = 200; c <= 299; c++) {
            codes.add(c);
        }
        return codes;
    }

    public final boolean isRunning() {
        return manager.isLoadRunning(getId());
    }

    public final boolean isCancelled() {
        return manager.isLoadCancelled(getId());
    }

    public final void cancel() {
        if (isRunning()) {
            manager.cancelLoad(getId());
        }
    }

    protected abstract int getId();

    @NonNull
    protected abstract URL getUrl();

    @NonNull
    protected abstract LoadRunnableInfo.LoadSettings getLoadSettings();

    @Nullable
    protected abstract List<LoadRunnableInfo.NameValuePair> getHeaders();

    @Nullable
    protected abstract List<LoadRunnableInfo.NameValuePair> getFormFields();

    @NonNull
    protected List<Integer> getAcceptableResponseCodes() {
        return defaultAcceptableResponseCodes();
    }

    @Nullable
    protected abstract LoadRunnableInfo.FileBody getFileFormField();

    @Nullable
    protected abstract File getDownloadFile();


    public void enqueue(@Nullable C callback) {

        this.callback = callback;
        if (callback != null) {
            callback.handler = handler;
            callback.manager = manager;
            manager.addLoadListener(callback);
        }

        LoadRunnableInfo.Builder builder = new LoadRunnableInfo.Builder(getId(), getUrl(), getLoadSettings());
        builder.addAcceptableResponseCodes(getAcceptableResponseCodes());
        builder.addHeaders(getHeaders());
        builder.addFormFields(getFormFields());
        builder.body(getFileFormField());
        builder.downloadFile(getDownloadFile());

        manager.enqueueLoad(builder.build());
    }

    public abstract static class Callback implements LoadListener<LoadRunnableInfo> {

        public Callback(int id) {
            this.id = id;
        }

        final int id;

        @Nullable
        Handler handler;

        @Nullable
        NetworkLoadManager manager;

        @Nullable
        NetworkLoadManager.LoadProcessInfo loadProcessInfo;

        @Nullable
        NetworkLoadManager.Response response;

        @Nullable
        public NetworkLoadManager.LoadProcessInfo getLoadProcessInfo() {
            return loadProcessInfo;
        }

        @Nullable
        public NetworkLoadManager.Response getResponse() {
            return response;
        }

        @Override
        public final int getId(@NonNull LoadRunnableInfo info) {
            return id;
        }

        @Override
        public long getProcessingNotifyInterval(@NonNull LoadRunnableInfo info) {
            return DEFAULT_PROCESSING_NOTIFY_INTERVAL;
        }

        @Override
        public final void onUpdateState(@NonNull STATE state, @NonNull LoadRunnableInfo loadInfo, @NonNull final NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {

            this.loadProcessInfo = loadProcessInfo;

            switch (state) {

                case STARTING:
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onStarting();
                            }
                        });
                    } else {
                        onStarting();
                    }
                    break;

                case FAILED_RETRIES_EXCEEDED:
                    onFailed(null, loadProcessInfo);
                    break;

                case CANCELLED:
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onCancelled();
                            }
                        });
                    } else {
                        onCancelled();
                    }
                    break;

                case UPLOADING:
                case DOWNLOADING:
                    if (handler != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                onProcessing(loadProcessInfo);
                            }
                        });
                    } else {
                        onProcessing(loadProcessInfo);
                    }
                    break;
            }
        }

        @Override
        public void onLoadAddedToQueue(int id, int waitingLoads, int activeLoads) {

        }

        @Override
        public void onLoadRemovedFromQueue(int id, int waitingLoads, int activeLoads) {

        }

        @Override
        public void onResponse(@NonNull LoadRunnableInfo loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NonNull NetworkLoadManager.Response response) {
            this.loadProcessInfo = loadProcessInfo;
            this.response = response;
            switch (response.getStatus()) {
                case ACCEPTED:
                    onSuccess(response, loadProcessInfo);
                    break;

                case DECLINED:
                    onFailed(response, loadProcessInfo);
                    break;
            }
        }

        protected void onStarting() {

        }

        protected void onProcessing(@NonNull NetworkLoadManager.LoadProcessInfo currentLoadProcessInfo) {

        }

        @CallSuper
        protected void onFinished() {
            if (manager != null) {
                manager.removeLoadListener(this);
            }
        }

        @CallSuper
        protected void onSuccess(@Nullable NetworkLoadManager.Response response, @NonNull NetworkLoadManager.LoadProcessInfo info) {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onFinished();
                    }
                });
            } else {
                onFinished();
            }
        }

        @CallSuper
        protected void onFailed(@Nullable NetworkLoadManager.Response response, @NonNull NetworkLoadManager.LoadProcessInfo info) {
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        onFinished();
                    }
                });
            } else {
                onFinished();
            }
        }

        @CallSuper
        protected void onCancelled() {
            onFinished();
        }
    }


}
