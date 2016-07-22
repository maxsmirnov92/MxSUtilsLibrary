package net.maxsmr.networkutils.loadutil.requests;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.maxsmr.networkutils.loadutil.executors.base.LoadFileListener;
import net.maxsmr.networkutils.loadutil.executors.base.LoadRunnableInfo;
import net.maxsmr.networkutils.loadutil.executors.base.upload.UploadExecutor;
import net.maxsmr.networkutils.loadutil.executors.base.upload.UploadRunnableInfo;
import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;

public abstract class AbstractRequest<C extends AbstractRequest.Callback> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractRequest.class);

    @NonNull
    protected final UploadExecutor executor;

    @Nullable
    private C callback;

    public AbstractRequest(@NonNull UploadExecutor executor) {
        this.executor = executor;
    }

    @NonNull
    public UploadExecutor getExecutor() {
        return executor;
    }

    @Nullable
    public C getCallback() {
        return callback;
    }

    public static Integer[] defaultAcceptableResponseCodes() {
        List<Integer> codes = new ArrayList<>();
        for (int c = 200; c <= 299; c++) {
            codes.add(c);
        }
        return codes.toArray(new Integer[codes.size()]);
    }

    public final boolean isRunning() {
        RunnableInfo runnableInfo = executor.findRunnableInfoById(getId());
        return runnableInfo != null /*&& !runnableInfo.isCancelled()*/;
    }

    public final boolean isCancelled() {
        UploadRunnableInfo runnableInfo = executor.findRunnableInfoById(getId());
        return runnableInfo != null && runnableInfo.isCancelled();
    }

    public final void cancel() {
        if (isRunning()) {
            executor.cancelLoad(getId());
        }
    }

    protected abstract int getId();

    @Nullable
    protected abstract URL getUrl();

    @NonNull
    protected abstract LoadRunnableInfo.LoadSettings getLoadSettings();

    @Nullable
    protected abstract List<UploadRunnableInfo.Field> getHeaders();

    @Nullable
    protected abstract List<UploadRunnableInfo.Field> getFormFields();

    @NonNull
    protected Integer[] getAcceptableResponseCodes() {
        return defaultAcceptableResponseCodes();
    }

    protected abstract UploadRunnableInfo.FileFormField getFileFormField();

    public void execute(@Nullable C callback) {

        this.callback = callback;
        if (callback != null) {
            executor.addLoadListener(callback);
        }

        UploadRunnableInfo runnableInfo = null;
        try {
            runnableInfo = new UploadRunnableInfo(getId(), getUrl(), getLoadSettings(), getHeaders(), getFormFields(), getFileFormField(), getAcceptableResponseCodes());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.error("a MalformedURLException occurred", e);
        }
        if (runnableInfo != null) {
            executor.upload(runnableInfo);
        } else {
            if (callback != null) {
                callback.onFailed(Callback.RESPONSE_CODE_UNKNOWN, null, new ArrayList<LoadRunnableInfo.Field>(), new ArrayList<String>());
            }
        }
    }

    public abstract static class Callback implements LoadFileListener<UploadRunnableInfo> {

        public static final int RESPONSE_CODE_UNKNOWN = -1;

        private final int id;

        public Callback(int id) {
            this.id = id;
        }

        long estimatedTime = 0;
        float currentKBytes = 0;
        float totalKBytes = 0;

        int responseCode = RESPONSE_CODE_UNKNOWN;
        String responseMessage = null;
        List<LoadRunnableInfo.Field> responseHeaders = null;
        List<String> responseBody = null;

        @Override
        public final int getId(@NonNull UploadRunnableInfo info) {
            return id;
        }

        @Override
        public long getProcessingNotifyInterval(@NonNull UploadRunnableInfo info) {
            return DEFAULT_PROCESSING_NOTIFY_INTERVAL;
        }

        public long getEstimatedTime() {
            return estimatedTime;
        }

        public float getCurrentKBytes() {
            return currentKBytes;
        }

        public float getTotalKBytes() {
            return totalKBytes;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getResponseMessage() {
            return responseMessage;
        }

        public List<LoadRunnableInfo.Field> getResponseHeaders() {
            return responseHeaders;
        }

        public List<String> getResponseBody() {
            return responseBody;
        }

        @Override
        public final void onUpdateState(@NonNull STATE state, @NonNull UploadRunnableInfo loadInfo, final long estimatedTime, final float currentKBytes, final float totalKBytes, @Nullable Throwable t) {
            logger.debug("onUpdateState(), state=" + state + ", loadInfo=" + loadInfo + ", estimatedTime=" + estimatedTime + ", currentKBytes=" + currentKBytes + ", totalKBytes=" + totalKBytes + ", t=" + t);

            this.estimatedTime = estimatedTime;
            this.currentKBytes = currentKBytes;
            this.totalKBytes = totalKBytes;

            switch (state) {

                case STARTING:
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onStarting();
                        }
                    });
                    break;

                case FAILED_NOT_STARTED:
                case FAILED_RETRIES_EXCEEDED:
                    if (responseCode == RESPONSE_CODE_UNKNOWN) {
                        onFailed(responseCode, null, new ArrayList<LoadRunnableInfo.Field>(), new ArrayList<String>());
                    }
                    break;

                case CANCELLED:
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onCancelled();
                        }
                    });
                    break;

                case PROCESSING:
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            onProcessing(estimatedTime, currentKBytes, totalKBytes);
                        }
                    });
                    break;
            }
        }

        @Override
        public final void onActiveLoadsCountChanged(int activeDownloads) {

        }

        @Override
        public final void onResponseCode(@NonNull ResponseStatus status, @NonNull UploadRunnableInfo loadInfo, int code, String responseMessage) {
            this.responseCode = code;
            this.responseMessage = responseMessage;
        }

        @Override
        public final void onResponseHeaders(@NonNull ResponseStatus status, @NonNull UploadRunnableInfo loadInfo, @NonNull List<LoadRunnableInfo.Field> fields) {
            this.responseHeaders = fields;
        }

        @Override
        public final void onResponseBody(@NonNull ResponseStatus status, @NonNull UploadRunnableInfo loadInfo, @NonNull List<String> response) {
            this.responseBody = response;

            switch (status) {
                case ACCEPTED:
                    onSuccess(responseCode, responseMessage, responseHeaders, responseBody);
                    break;

                case DECLINED:
                    onFailed(responseCode, responseMessage, responseHeaders, responseBody);
                    break;
            }
        }

        @MainThread
        protected void onStarting() {

        }

        @MainThread
        protected void onProcessing(long estimatedTime, float currentKBytes, float totalKBytes) {

        }

        @MainThread
        protected void onFinished(long estimatedTime, float currentKBytes, float totalKBytes) {

        }

        @CallSuper
        protected void onSuccess(int code, String responseMessage, @NonNull List<LoadRunnableInfo.Field> responseHeaders, @NonNull List<String> response) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    onFinished(estimatedTime, currentKBytes, totalKBytes);
                }
            });
        }

        @CallSuper
        protected void onFailed(int code, String responseMessage, @NonNull List<LoadRunnableInfo.Field> responseHeaders, @NonNull List<String> response) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    onFinished(estimatedTime, currentKBytes, totalKBytes);
                }
            });
        }

        @CallSuper
        @MainThread
        protected void onCancelled() {
            onFinished(estimatedTime, currentKBytes, totalKBytes);
        }
    }


}
