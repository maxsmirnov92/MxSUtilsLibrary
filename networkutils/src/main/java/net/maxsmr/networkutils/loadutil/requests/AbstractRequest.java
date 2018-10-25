package net.maxsmr.networkutils.loadutil.requests;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import org.jetbrains.annotations.NotNull;
import android.support.annotation.Nullable;

import net.maxsmr.networkutils.loadutil.managers.LoadListener;
import net.maxsmr.networkutils.loadutil.managers.NetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRequest<B extends LoadRunnableInfo.Body, C extends AbstractRequest.Callback> {

    protected final int id;

    @NotNull
    protected final NetworkLoadManager<B, LoadRunnableInfo<B>> manager;

    @Nullable
    private C callback;

    public AbstractRequest(int id, @NotNull NetworkLoadManager<B, LoadRunnableInfo<B>> manager) {
        this.id = id;
        this.manager = manager;
    }

    public int getId() {
        return id;
    }

    @NotNull
    public NetworkLoadManager<B, LoadRunnableInfo<B>> getManager() {
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

    public boolean isLastStateRunning() {
        return callback != null && callback.getLastState().isRunning();
    }

    public final boolean isCancelled() {
        return manager.isLoadCancelled(id);
    }

    public final void cancel() {
        if (isRunning()) {
            manager.cancelLoad(id);
        }
    }

    @Nullable
    protected String getName() {
        return null;
    }

    @NotNull
    protected List<Integer> getAcceptableResponseCodes() {
        return defaultAcceptableResponseCodes();
    }

    @NotNull
    protected abstract String getUrl();

    @NotNull
    protected abstract LoadRunnableInfo.LoadSettings getLoadSettings();

    @NotNull
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
    protected B getBody() {
        return null;
    }

    @Nullable
    protected File getDownloadFile() {
        return null;
    }

    public void enqueue(@Nullable C callback) {

        LoadRunnableInfo.Builder<B, LoadRunnableInfo<B>> builder = new LoadRunnableInfo.Builder<>(id, getUrl(), getLoadSettings());
        builder.addAcceptableResponseCodes(getAcceptableResponseCodes());
        builder.name(getName());
        builder.contentType(getContentType());
        builder.addHeaders(getHeaders());
        builder.addFormFields(getFormFields());
        builder.body(getBody());
        builder.downloadFile(getDownloadFile());

        LoadRunnableInfo<B> info = builder.build();

        this.callback = callback;
        if (callback != null) {
            callback.manager = manager;
            manager.registerLoadListener(callback);
        }

        manager.enqueueLoad(info);
    }

    public static class Callback<B extends LoadRunnableInfo.Body> implements LoadListener<LoadRunnableInfo<B>> {

        @NotNull
        protected final Handler uiHandler = new Handler(Looper.getMainLooper());

        protected final int loadId;

        protected final boolean callbacksOnUiThread;

        @Nullable
        protected NetworkLoadManager<B, LoadRunnableInfo<B>> manager;

        @Nullable
        protected LoadRunnableInfo<B> lastLoadRunnableInfo;

        @Nullable
        protected NetworkLoadManager.LoadProcessInfo lastLoadProcessInfo;

        @Nullable
        protected NetworkLoadManager.Response lastResponse;

        @Nullable
        protected Throwable lastThrowable;

        public Callback(int loadId, boolean callbacksOnUiThread) {
            this.loadId = loadId;
            this.callbacksOnUiThread = callbacksOnUiThread;
        }

        @Nullable
        public final NetworkLoadManager getNetworkLoadManager() {
            return manager;
        }

        @Override
        public final int getId() {
            return loadId;
        }

        @Override
        public int getId(@NotNull LoadRunnableInfo info) {
            return getId();
        }

        @NotNull
        public STATE getLastState() {
            STATE lastState = lastLoadProcessInfo != null? lastLoadProcessInfo.getState() : STATE.UNKNOWN;
            if (lastState == STATE.UNKNOWN) {
                if (manager != null) {
                    lastState = manager.getLastStateForId(getId());
                }
            }
            return lastState;
        }

        public boolean isLoading() {
            STATE state = getLastState();
            return state == STATE.DOWNLOADING || state == STATE.UPLOADING;
        }

        @Nullable
        public LoadRunnableInfo<B> getLastLoadRunnableInfo() {
            return lastLoadRunnableInfo;
        }

        @Nullable
        public NetworkLoadManager.LoadProcessInfo getLastLoadProcessInfo() {
            if (lastLoadProcessInfo == null) {
                if (manager != null) {
                    lastLoadProcessInfo = manager.getCurrentLoadProcessInfoForId(getId());
                }
            }
            return lastLoadProcessInfo;
        }

        @Nullable
        public NetworkLoadManager.Response getLastResponse() {
            if (lastResponse == null) {
                if (manager != null) {
                    lastResponse = manager.getLastResponseForId(getId());
                }
            }
            return lastResponse;
        }

        @Nullable
        public Throwable getLastThrowable() {
            return lastThrowable;
        }

        @Override
        public long getProcessingNotifyInterval(@NotNull LoadRunnableInfo<B> info) {
            return DEFAULT_PROCESSING_NOTIFY_INTERVAL;
        }

        @Override
        @CallSuper
        public void onUpdateState(@NotNull final LoadRunnableInfo<B> loadInfo, @NotNull final NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {

            this.lastLoadRunnableInfo = loadInfo;
            this.lastLoadProcessInfo = loadProcessInfo;
            this.lastThrowable = t;

            final NetworkLoadManager.Response response = getLastResponse();

            final Runnable r;

            switch (loadProcessInfo.getState()) {

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
                            int retriesCount = loadProcessInfo.getRetriesCount();
                            onFailedAttempt(response, loadProcessInfo, retriesCount, loadInfo.settings.retryLimit > 0 ? loadInfo.settings.retryLimit - retriesCount : loadInfo.settings.retryLimit);
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
        @CallSuper
        public void onResponse(@NotNull LoadRunnableInfo<B> loadInfo, @NotNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NotNull NetworkLoadManager.Response response) {
            lastResponse = response;
        }

        @Override
        public void onLoadAddedToQueue(int id, int waitingLoads, int activeLoads) {

        }

        @Override
        public void onLoadRemovedFromQueue(int id, int waitingLoads, int activeLoads) {

        }

        protected void onStarting() {

        }

        protected void onProcessing(@NotNull NetworkLoadManager.LoadProcessInfo currentLoadProcessInfo) {

        }

        @CallSuper
        protected void onFinished() {
        }

        @CallSuper
        protected void onSuccess(@NotNull NetworkLoadManager.Response response, @NotNull NetworkLoadManager.LoadProcessInfo info) {
            onFinished();
        }

        @CallSuper
        protected void onFailed(@Nullable NetworkLoadManager.Response response, @NotNull NetworkLoadManager.LoadProcessInfo info) {
            onFinished();
        }

        @CallSuper
        protected void onFailedAttempt(@Nullable NetworkLoadManager.Response response, @NotNull NetworkLoadManager.LoadProcessInfo info, int attemptsMade, int attemptsLeft) {

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
    }


}