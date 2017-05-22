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

    public int getId() {
        return id;
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

    public boolean isLastStateRunning() {
        return callback != null && LoadListener.STATE.isRunning(callback.getLastState());
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
            callback.loadRunnableInfo = info;
            callback.manager = manager;
            manager.addLoadListener(callback);
        }

        manager.enqueueLoad(info);
    }

    public abstract static class Callback implements LoadListener<LoadRunnableInfo> {

        @NonNull
        protected final Handler uiHandler = new Handler(Looper.getMainLooper());

        private final boolean callbacksOnUiThread;

        @Nullable
        protected NetworkLoadManager manager;

        @Nullable
        protected LoadRunnableInfo loadRunnableInfo;

        @NonNull
        protected STATE lastState = STATE.UNKNOWN;

        @Nullable
        protected NetworkLoadManager.LoadProcessInfo lastLoadProcessInfo;

        @Nullable
        protected NetworkLoadManager.Response lastResponse;

        @Nullable
        protected Throwable lastThrowable;

        public Callback(boolean callbacksOnUiThread) {
            this(callbacksOnUiThread, null, null);
        }

        public Callback(boolean callbacksOnUiThread, @Nullable NetworkLoadManager manager, @Nullable LoadRunnableInfo loadRunnableInfo) {
//            this.uiHandler = Looper.myLooper() != null ? new Handler(Looper.myLooper()) : null;
            this.callbacksOnUiThread = callbacksOnUiThread;
            this.manager = manager;
            this.loadRunnableInfo = loadRunnableInfo;
        }

        public boolean isInitialized() {
            return manager != null && loadRunnableInfo != null;
        }

        @Nullable
        public final NetworkLoadManager getNetworkLoadManager() {
            return manager;
        }

        @Nullable
        public final LoadRunnableInfo getLoadRunnableInfo() {
            return loadRunnableInfo;
        }

        @Override
        public int getId() {
            if (loadRunnableInfo == null) {
                throw new IllegalStateException("loadRunnableInfo was not initialized");
            }
            return loadRunnableInfo.id;
        }

        // template
        @Override
        public final int getId(@NonNull LoadRunnableInfo info) {
            return getId();
        }


        @NonNull
        public STATE getLastState() {
            if (lastState == STATE.UNKNOWN) {
                if (manager == null) {
                    throw new IllegalStateException("manager was not initialized");
                }
                lastState = manager.getLastStateForId(getId());
            }
            return lastState;
        }

        public boolean isLoading() {
            STATE state = getLastState();
            return state == STATE.DOWNLOADING || state == STATE.UPLOADING;
        }

        @Nullable
        public NetworkLoadManager.LoadProcessInfo getLastLoadProcessInfo() {
            if (lastLoadProcessInfo == null) {
                if (manager == null) {
                    throw new IllegalStateException("manager was not initialized");
                }
                lastLoadProcessInfo = manager.getCurrentLoadProcessInfoForId(getId());
            }
            return lastLoadProcessInfo;
        }

        @Nullable
        public NetworkLoadManager.Response getLastResponse() {
            if (lastResponse == null) {
                if (manager == null) {
                    throw new IllegalStateException("manager was not initialized");
                }
                lastResponse = manager.getLastResponseForId(getId());
            }
            return lastResponse;
        }

        @Nullable
        public Throwable getLastThrowable() {
            return lastThrowable;
        }

        @Override
        public long getProcessingNotifyInterval(@NonNull LoadRunnableInfo info) {
            return DEFAULT_PROCESSING_NOTIFY_INTERVAL;
        }

        @Override
        @CallSuper
        public void onUpdateState(@NonNull STATE state, @NonNull LoadRunnableInfo loadInfo, @NonNull final NetworkLoadManager.LoadProcessInfo loadProcessInfo, @Nullable Throwable t) {

            // TODO         @NonNull LoadListener.STATE lastState в LoadProcessInfo

            if (manager == null) {
                throw new IllegalStateException("manager was not initialized");
            }

            if (loadRunnableInfo == null) {
                throw new IllegalStateException("loadRunnableInfo was not initialized");
            }

            this.lastState = state;
            this.lastLoadProcessInfo = loadProcessInfo;
            this.lastThrowable = t;

            final NetworkLoadManager.Response response = getLastResponse();

            final Runnable r;

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
                            int retriesCount = loadProcessInfo.getRetriesCount();
                            onFailedAttempt(response, loadProcessInfo, retriesCount, loadRunnableInfo.settings.retryLimit > 0 ? loadRunnableInfo.settings.retryLimit - retriesCount : loadRunnableInfo.settings.retryLimit);
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
        public void onResponse(@NonNull LoadRunnableInfo loadInfo, @NonNull NetworkLoadManager.LoadProcessInfo loadProcessInfo, @NonNull NetworkLoadManager.Response response) {
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
        protected void onFailedAttempt(@Nullable NetworkLoadManager.Response response, @NonNull NetworkLoadManager.LoadProcessInfo info, int attemptsMade, int attemptsLeft) {

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