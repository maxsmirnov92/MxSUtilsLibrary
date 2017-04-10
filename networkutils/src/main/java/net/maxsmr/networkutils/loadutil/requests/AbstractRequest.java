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

    protected final int id;

    @NonNull
    protected final NetworkLoadManager manager;

    @Nullable
    private C callback;

    public AbstractRequest(int id, @NonNull NetworkLoadManager manager) {
        this.id = id;
        this.manager = manager;
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
        return manager.isLoadRunning(id);
    }

    public final boolean isCancelled() {
        return manager.isLoadCancelled(id);
    }

    public final void cancel() {
        if (isRunning()) {
            manager.cancelLoad(id);
        }
    }

    @NonNull
    protected List<Integer> getAcceptableResponseCodes() {
        return defaultAcceptableResponseCodes();
    }

    @NonNull
    protected abstract URL getUrl();

    @NonNull
    protected abstract LoadRunnableInfo.LoadSettings getLoadSettings();

    @NonNull
    protected abstract LoadRunnableInfo.ContentType getContentType();

    @Nullable
    protected List<LoadRunnableInfo.NameValuePair> getHeaders() {
        return null;
    }

    @Nullable
    protected List<LoadRunnableInfo.NameValuePair> getFormFields() {
        return null;
    }

    @Nullable
    protected LoadRunnableInfo.Body getBody() {
        return null;
    }

    @Nullable
    protected File getDownloadFile() {
        return null;
    }

    public void enqueue(@Nullable C callback) {

        LoadRunnableInfo.Builder builder = new LoadRunnableInfo.Builder(id, getUrl(), getLoadSettings());
        builder.addAcceptableResponseCodes(getAcceptableResponseCodes());
        builder.contentType(getContentType());
        builder.addHeaders(getHeaders());
        builder.addFormFields(getFormFields());
        builder.body(getBody());
        builder.downloadFile(getDownloadFile());

        LoadRunnableInfo info = builder.build();

        this.callback = callback;
        if (callback != null) {
            callback.id = id;
            callback.manager = manager;
            callback.loadRunnableInfo = info;
            manager.addLoadListener(callback);
        }

        manager.enqueueLoad(info);
    }

    public abstract static class Callback implements LoadListener<LoadRunnableInfo> {

        @NonNull
        protected final Handler uiHandler = new Handler(Looper.getMainLooper());

        private final boolean callbacksOnUiThread;

        protected int id;

        protected NetworkLoadManager manager;

        protected LoadRunnableInfo loadRunnableInfo;


        public Callback(boolean callbacksOnUiThread) {
//            this.uiHandler = Looper.myLooper() != null ? new Handler(Looper.myLooper()) : null;
            this.callbacksOnUiThread = callbacksOnUiThread;
        }

        @Nullable
        public NetworkLoadManager.LoadProcessInfo getLoadProcessInfo() {
            return manager.getCurrentLoadProcessInfoForId(id);
        }

        @Nullable
        public NetworkLoadManager.Response getResponse() {
            return manager.getLastResponseForId(id);
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

            if (manager == null) {
                throw new IllegalStateException("manager was not initialized");
            }

            final NetworkLoadManager.Response response = manager.getLastResponseForId(id);

            Runnable r;

            switch (state) {

                case STARTING:

                    r = new Runnable() {
                        @Override
                        public void run() {
                            onStarting();
                        }
                    };

                    if (callbacksOnUiThread) {
                        uiHandler.post(r);
                    } else {
                        r.run();
                    }
                    break;

                case FAILED:
                    r = new Runnable() {
                        @Override
                        public void run() {
                            int retriesCount = manager.getCurrentLoadProcessInfoForId(id).getRetriesCount();
                            onFailedAttempt(response, loadProcessInfo, retriesCount, loadRunnableInfo.settings.retryLimit > 0? loadRunnableInfo.settings.retryLimit - retriesCount : loadRunnableInfo.settings.retryLimit);
                        }
                    };

                    if (callbacksOnUiThread) {
                        uiHandler.post(r);
                    } else {
                        r.run();
                    }
                    break;

                case FAILED_RETRIES_EXCEEDED:

                    r = new Runnable() {
                        @Override
                        public void run() {
                            onFailed(response, loadProcessInfo);
                        }
                    };

                    if (callbacksOnUiThread) {
                        uiHandler.post(r);
                    } else {
                        r.run();
                    }
                    break;

                case CANCELLED:

                    r = new Runnable() {
                        @Override
                        public void run() {
                            onCancelled();
                        }
                    };

                    if (callbacksOnUiThread) {
                        uiHandler.post(r);
                    } else {
                        r.run();
                    }
                    break;

                case UPLOADING:
                case DOWNLOADING:

                    r = new Runnable() {
                        @Override
                        public void run() {
                            onProcessing(loadProcessInfo);
                        }
                    };

                    if (callbacksOnUiThread) {
                        uiHandler.post(r);
                    } else {
                        r.run();
                    }
                    break;

                case SUCCESS:

                    r = new Runnable() {
                        @Override
                        public void run() {
                            if (response != null) {
                                onSuccess(response, loadProcessInfo);
                            }
                        }
                    };
                    if (callbacksOnUiThread) {
                        uiHandler.post(r);
                    } else {
                        r.run();
                    }

                    break;
            }
        }

        @Override
        public void onResponse(@NonNull LoadRunnableInfo loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NonNull NetworkLoadManager.Response response) {

        }

        @Override
        public void onLoadAddedToQueue(int id, int waitingLoads, int activeLoads) {

        }

        @Override
        public void onLoadRemovedFromQueue(int id, int waitingLoads, int activeLoads) {

        }

        protected void onStarting() {

        }

        protected void onProcessing(@NonNull NetworkLoadManager.LoadProcessInfo currentLoadProcessInfo) {

        }

        @CallSuper
        protected void onFinished() {
            if (manager == null) {
                throw new IllegalStateException("manager was not initialized");
            }
            manager.removeLoadListener(this);
        }

        @CallSuper
        protected void onSuccess(@NonNull NetworkLoadManager.Response response, @NonNull NetworkLoadManager.LoadProcessInfo info) {
            onFinished();
        }

        @CallSuper
        protected void onFailed(@Nullable NetworkLoadManager.Response response, @NonNull NetworkLoadManager.LoadProcessInfo info) {
            onFinished();
        }

        @CallSuper
        protected void onFailedAttempt(@Nullable NetworkLoadManager.Response response, @NonNull NetworkLoadManager.LoadProcessInfo info, int attemptsMade,int attemptsLeft ) {

        }

        @CallSuper
        protected void onCancelled() {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    onFinished();
                }
            };
            if (callbacksOnUiThread) {
                uiHandler.post(r);
            } else {
                r.run();
            }
        }

        public boolean hasLoad(int id) {
            if (manager == null) {
                throw new IllegalStateException("manager was not initialized");
            }
            return manager.getLastStateForId(id) != STATE.UNKNOWN;
        }
    }


}