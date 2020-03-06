package net.maxsmr.networkutils.loadutil.managers;

import android.net.Uri;
import android.os.Handler;

import net.maxsmr.commonutils.data.FileHelper;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.networkutils.loadutil.managers.base.BaseNetworkLoadManager;
import net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.commonutils.data.TextUtilsKt.isEmpty;
import static net.maxsmr.commonutils.data.TextUtilsKt.join;
import static net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo.ContentType.MULTIPART_FORM_DATA;
import static net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo.LoadSettings.DownloadWriteMode.OVERWRITE;
import static net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo.LoadSettings.DownloadWriteMode.RESUME_DOWNLOAD;
import static net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo.LoadSettings.ReadBodyMode.BYTE_ARRAY;
import static net.maxsmr.networkutils.loadutil.managers.base.info.LoadRunnableInfo.LoadSettings.ReadBodyMode.FILE;

public class NetworkLoadManager<B extends LoadRunnableInfo.Body, LI extends LoadRunnableInfo<B>>
        extends BaseNetworkLoadManager<B, LI, NetworkLoadManager.Response, BaseNetworkLoadManager.TaskRunnable<LI, Void, NetworkLoadManager.Response>> {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(NetworkLoadManager.class);

    private static final String LINE_FEED = "\r\n";

    public static final int BUF_SIZE = 1024;

    public NetworkLoadManager() {
        super();
    }

    public NetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                              @Nullable TaskRunnable.ITaskResultValidator<LI, Void, NetworkLoadManager.Response, TaskRunnable<LI, Void, Response>> validator) {
        this(limit, concurrentLoadsCount, storage, validator, null);
    }

    public NetworkLoadManager(int limit, int concurrentLoadsCount, @Nullable AbstractSyncStorage<LI> storage,
                              @Nullable TaskRunnable.ITaskResultValidator<LI, Void, NetworkLoadManager.Response, TaskRunnable<LI, Void, Response>> validator,
                              @Nullable Handler callbacksHandler) {
        super(limit, concurrentLoadsCount, storage, validator, callbacksHandler);

        doRestore(runnableInfos -> {
            List<TaskRunnable<LI, Void, Response>> result = new ArrayList<>();
            for (LI info : runnableInfos) {
                result.add(newRunnable(info));
            }
            return result;
        });
    }

    @Nullable
    public LoadProcessInfo getCurrentLoadProcessInfoForId(int loadId) {
        synchronized (executor) {
            LoadRunnable runnable = findLoadRunnableById(loadId);
            return runnable != null ? runnable.getCurrentLoadInfo() : null;
        }
    }

    @NotNull
    public LoadListener.STATE getLastStateForId(int loadId) {
        LoadProcessInfo processInfo = getCurrentLoadProcessInfoForId(loadId);
        return processInfo != null ? processInfo.state : LoadListener.STATE.UNKNOWN;
    }

    @Nullable
    public Response getLastResponseForId(int loadId) {
        synchronized (executor) {
            LoadRunnable runnable = findLoadRunnableById(loadId);
            return runnable != null ? runnable.getLastResponse() : null;
        }
    }

    @Nullable
    protected LoadRunnable findLoadRunnableById(int loadId) {
        checkReleased();
        return (LoadRunnable) executor.findRunnableById(loadId);
    }

    @NotNull
    @Override
    protected LoadRunnable newRunnable(@NotNull LI rInfo) {
        return new LoadRunnable(rInfo);
    }

    @Override
    protected Class<?> getLoggerClass() {
        return NetworkLoadManager.class;
    }

    private final class LoadRunnable extends TaskRunnable<LI, Void, Response> {

        @NotNull
        LoadProcessInfo currentLoadInfo = new LoadProcessInfo();

        @Nullable
        Throwable lastException;

        @Nullable
        Response lastResponse;

        @Nullable
        File lastDownloadFile;

        @Nullable
        Set<File> lastUploadFiles;

        @NotNull
        public LoadProcessInfo getCurrentLoadInfo() {
            return currentLoadInfo;
        }

        @NotNull
        public LoadListener.STATE getLastState() {
            return currentLoadInfo.getState();
        }

        @Nullable
        public Response getLastResponse() {
            return lastResponse;
        }

        public LoadRunnable(LI rInfo) {
            super(rInfo);
        }


        protected boolean checkArgs() {
            if (isEmpty(rInfo.contentType.value)) {
                throw new IllegalArgumentException("content type might not be empty");
            }
            if (rInfo.requestMethod == LoadRunnableInfo.RequestMethod.GET
                    && (rInfo.hasFormFields() || rInfo.hasBody())) {
                throw new IllegalArgumentException("request method might not be " + LoadRunnableInfo.RequestMethod.GET + " and has form fields / body");
            }

            if (rInfo.settings.readBodyMode == LoadRunnableInfo.LoadSettings.ReadBodyMode.FILE) {
                if (rInfo.downloadFile == null && rInfo.downloadDirectory == null)
                    throw new IllegalArgumentException("read body mode is " + rInfo.settings.readBodyMode + ", but neither file nor directory is specified");
            }

            return true;
        }

        private void notifyStateChanged(@NotNull LoadListener.STATE state) {
            currentLoadInfo.state = state;
            loadObservable.notifyStateChanged(rInfo, currentLoadInfo, lastException);
        }

        private void notifyStateProcessing(@NotNull LoadListener.STATE state, @NotNull LoadListener<LI> l) {
            if (!state.isRunning()) {
                throw new IllegalArgumentException("incorrect state: " + state + ", must be running");
            }
            currentLoadInfo.state = state;
            l.onUpdateState(rInfo, currentLoadInfo, lastException);
        }

        @Nullable
        @Override
        public NetworkLoadManager.Response doWork() throws Throwable {
            checkArgs();
            doLoad();
            return null;
        }

        private void doLoad() {

            boolean success = false;

            currentLoadInfo = new LoadProcessInfo();

            while (!success && !rInfo.isCanceled() &&
                    (rInfo.settings.retryLimit == LoadRunnableInfo.LoadSettings.RETRY_LIMIT_UNLIMITED
                            || (currentLoadInfo.retriesCount == -1 || rInfo.settings.retryLimit != LoadRunnableInfo.LoadSettings.RETRY_LIMIT_NONE && currentLoadInfo.retriesCount < rInfo.settings.retryLimit))) {

                HttpURLConnection connection = null;
                DataOutputStream requestStream = null;
                BufferedInputStream responseInput = null;
                BufferedOutputStream responseOutput = null;

                boolean isFileReasonFail = false;

                int previousRetriesCount = currentLoadInfo.retriesCount;
                currentLoadInfo = new LoadProcessInfo();
                currentLoadInfo.retriesCount = previousRetriesCount;
                currentLoadInfo.retriesCount++;

                lastResponse = null;
                lastException = null;

                lastDownloadFile = null;
                lastUploadFiles = null;

                if (Thread.currentThread().isInterrupted()) {
                    logger.w("thread is interrupted, cancelling load...");
                    rInfo.cancel();
                }

                try {

                    if (rInfo.isCanceled()) {
                        throw new RuntimeException("load with id " + rInfo.id + " was canceled");
                    }

                    final String boundary;

                    if (rInfo.body != null && !(rInfo.body instanceof LoadRunnableInfo.EmptyBody)) {

                        currentLoadInfo.totalUploadBytesCount = (rInfo.body).getByteCount();

                        switch (rInfo.contentType) {

                            case TEXT_PLAIN:
                            case TEXT_HTML:
                            case TEXT_CSS:
                            case APPLICATION_JSON:
                            case APPLICATION_JAVASCRIPT:
                            case APPLICATION_XML:
                            case APPLICATION_ATOM_XML:

                                if (!(rInfo.body instanceof LoadRunnableInfo.StringBody)) {
                                    throw new RuntimeException("incorrect body type: " + rInfo.body.getClass() + ", must be: " + LoadRunnableInfo.StringBody.class);
                                }
                                lastUploadFiles = null;
                                boundary = null;
                                break;

                            case MULTIPART_FORM_DATA:

                                lastUploadFiles = new LinkedHashSet<>();

                                if (rInfo.body instanceof LoadRunnableInfo.FilesBody) {
                                    lastUploadFiles.addAll(((LoadRunnableInfo.FilesBody) rInfo.body).getSourceFiles());
                                } else {
                                    throw new RuntimeException("incorrect body type: " + rInfo.body.getClass() + ", must be: " + LoadRunnableInfo.FilesBody.class);
                                }

                                if (!((LoadRunnableInfo.FilesBody) rInfo.body).ignoreIncorrect) {
                                    for (File uploadFile : lastUploadFiles) {
                                        if (!FileHelper.isFileCorrect(uploadFile)) {
                                            isFileReasonFail = true;
                                            throw new RuntimeException("incorrect source upload file" + uploadFile);
                                        } else if (!uploadFile.canRead()) {
                                            isFileReasonFail = true;
                                            throw new RuntimeException("can't read from source upload file: " + uploadFile);
                                        }
                                    }
                                }

                                boundary = "++++" + System.currentTimeMillis();
                                break;

                            default:
                                throw new RuntimeException("contentType is " + rInfo.contentType + " is not applicable to body " + rInfo.body.getClass());
                        }


                    } else {

                        lastUploadFiles = null;
                        currentLoadInfo.totalUploadBytesCount = 0;
                        boundary = null;
                    }

                    notifyStateChanged(LoadListener.STATE.STARTING);

                    URL url = null;
                    Exception innerException = null;

                    try {
                        url = new URL(rInfo.url);
                    } catch (MalformedURLException e) {
                        innerException = e;
                    }

                    logger.d("opening connection on " + rInfo.url + "...");
                    connection = url != null ? (HttpURLConnection) url.openConnection() : null;

                    if (connection == null) {
                        throw new RuntimeException("cannot open connection on " + rInfo.url, innerException);
                    }

                    if (rInfo.settings.logRequestData) {
                        String query = url.getQuery();
                        logger.d("URL: " + rInfo.url);
                        logger.d(rInfo.requestMethod.toString() + " " + url.getPath() + (!isEmpty(query) ? "?" + query : ""));
                        logger.d("Host: " + url.getHost());
                    }

                    connection.setConnectTimeout((int) rInfo.settings.connectionTimeout);
                    connection.setReadTimeout((int) rInfo.settings.readWriteTimeout);

                    connection.setUseCaches(false);
                    if (rInfo.requestMethod != LoadRunnableInfo.RequestMethod.GET) {
                        connection.setDoOutput(true);
                    }
                    connection.setDoInput(true);

                    connection.setRequestMethod(rInfo.requestMethod.toString());
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("Cache-Control", "no-cache");

                    if (rInfo.settings.logRequestData) {
                        logger.d("Connection: " + "Keep-Alive");
                        logger.d("Cache-Control: " + "no-cache");
                    }

                    for (LoadRunnableInfo.NameValuePair h : rInfo.getHeaders()) {
                        if (!isEmpty(h.name)) {
//                        Utils.addHeaderField(requestStream, h.name, h.value, rInfo.settings.logRequestData);
                            connection.setRequestProperty(h.name, h.value);
                            if (rInfo.settings.logRequestData)
                                logger.d(h.name + ": " + h.value);
                        } else {
                            throw new RuntimeException("header name might not be empty");
                        }
                    }

                    if (rInfo.contentType != LoadRunnableInfo.ContentType.NOT_SPECIFIED) {
                        switch (rInfo.contentType) {
                            case MULTIPART_FORM_DATA:
                                connection.setRequestProperty("Content-Type",
                                        rInfo.contentType.value + "; boundary=" + boundary);
                                if (rInfo.settings.logRequestData) {
                                    logger.d("Content-Type: " + rInfo.contentType.value + "; boundary=" + boundary);
                                }
                                break;
                            default:
                                connection.setRequestProperty("Content-Type",
                                        rInfo.contentType.value);
                                if (rInfo.settings.logRequestData) {
                                    logger.d("Content-Type: " + rInfo.contentType.value);
                                }
                                break;
                        }
                    }


                    if (rInfo.settings.readBodyMode == FILE && rInfo.settings.downloadWriteMode == RESUME_DOWNLOAD && FileHelper.isFileCorrect(lastDownloadFile)) {
                        currentLoadInfo.downloadedBytesCount = (int) lastDownloadFile.length();
                        connection.setRequestProperty("Range", "bytes=" + currentLoadInfo.downloadedBytesCount + "-");
                    }

//                    if (currentLoadInfo.totalUploadBytesCount > 0) {
//                        logger.d("total=" + requestStream.size());
//                        connection.setRequestProperty("Content-Length", String.valueOf(requestStream.size()));
//                        if (rInfo.settings.logRequestData) {
//                            logger.d("Content-Length: " + currentLoadInfo.totalUploadBytesCount);
//                        }
//                    }

//                    connection.setRequestProperty("Accept", "*/*");
//                    connection.setRequestProperty("ENCTYPE", "multipart/form-data");
//                    connection.setRequestProperty("Expect", "100-continue");
//                    connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");

                    notifyStateChanged(LoadListener.STATE.CONNECTING);

                    logger.d("connecting...");
                    connection.connect();

                    notifyStateChanged(LoadListener.STATE.CONNECTED);

//                    if (rInfo.requestMethod != LoadRunnableInfo.RequestMethod.GET) {

                    logger.d("writing request to output stream...");
                    final long startUploadTime = System.currentTimeMillis();

                    requestStream = new DataOutputStream(connection.getOutputStream()); // new PrintWriter(new OutputStreamWriter(output, DEFAULT_CHARSET), true);

                    final IWriteNotifier writeNotifier = new IWriteNotifier() {

                        long waitTime = 0;

                        long lastProcessingNotifyTime = 0;

                        @Override
                        public boolean isPaused() {
                            return rInfo.isPaused();
                        }

                        @Override
                        public boolean isCanceled() {
                            return rInfo.isCanceled();
                        }

                        @Override
                        public void onWriteBytes(int count) {
                            currentLoadInfo.uploadedBytesCount += count;

                            currentLoadInfo.passedUploadTime = System.currentTimeMillis() - startUploadTime - waitTime;
                            currentLoadInfo.uploadSpeed = currentLoadInfo.passedUploadTime > 0 ? (float) currentLoadInfo.uploadedBytesCount / (float) currentLoadInfo.passedUploadTime : 0;
                            currentLoadInfo.leftUploadTime = currentLoadInfo.uploadSpeed > 0 ? (long) ((float) (currentLoadInfo.totalUploadBytesCount - currentLoadInfo.uploadedBytesCount) / currentLoadInfo.uploadSpeed) : 0;

                            if (rInfo.settings.notifyWrite) {
                                synchronized (loadObservable) {
                                    if (loadObservable.getObservers().size() > 0) {
                                        boolean notified = false;
                                        for (LoadListener<LI> l : loadObservable.copyOfObservers()) {
                                            final int id = l.getId(rInfo);
                                            if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                                final long interval = System.currentTimeMillis() - lastProcessingNotifyTime;
                                                long targetInterval = l.getProcessingNotifyInterval(rInfo);
                                                targetInterval = targetInterval == LoadListener.INTERVAL_NOT_SPECIFIED ? LoadListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL : targetInterval;
                                                if (targetInterval > 0 && interval >= targetInterval || currentLoadInfo.uploadedBytesCount >= currentLoadInfo.totalUploadBytesCount) {
                                                    long currentTime = System.currentTimeMillis();
//                                                        logger.d("updating uploading state (processing)...");
                                                    notifyStateProcessing(LoadListener.STATE.UPLOADING, l);
                                                    waitTime += System.currentTimeMillis() - currentTime;
                                                    notified = true;
                                                }
                                            }
                                        }
                                        if (notified)
                                            lastProcessingNotifyTime = System.currentTimeMillis();
                                    }
                                }
                            }
                        }
                    };

                    switch (rInfo.contentType) {

                        case TEXT_PLAIN:
                        case TEXT_HTML:
                        case TEXT_CSS:
                        case APPLICATION_JSON:
                        case APPLICATION_JAVASCRIPT:
                        case APPLICATION_XML:
                        case APPLICATION_ATOM_XML:

                            if (currentLoadInfo.totalUploadBytesCount > 0) {
                                Utils.addBody(requestStream, (LoadRunnableInfo.StringBody) rInfo.body, rInfo.settings.logRequestData);
                            }
                            break;

                        case APPLICATION_URLENCODED:

                            for (LoadRunnableInfo.NameValuePair f : rInfo.getFormFields()) {

                                if (!isEmpty(f.name) && !isEmpty(f.value)) {
                                    Utils.addFormFieldUrlEncoded(requestStream, f.name, f.value, rInfo.settings.logRequestData);
                                } else {
                                    throw new RuntimeException("form field name or value might not be empty");
                                }

                            }
                            break;


                        case MULTIPART_FORM_DATA:

                            for (LoadRunnableInfo.NameValuePair f : rInfo.getFormFields()) {
                                if (!isEmpty(f.name) && !isEmpty(f.value)) {
                                    Utils.addFormFieldMultipart(requestStream, f.name, f.value, rInfo.settings.uploadCharset, boundary, rInfo.settings.logRequestData);
                                } else {
                                    throw new RuntimeException("form field name or value might not be empty");
                                }
                            }

                            if (currentLoadInfo.totalUploadBytesCount > 0) {
                                if (!lastUploadFiles.isEmpty()) {
                                    if (((LoadRunnableInfo.FilesBody) rInfo.body).hasCorrectSourceFiles()) {
                                        if (((LoadRunnableInfo.FilesBody) rInfo.body).asArray) {
                                            Utils.addFilePartsArray(requestStream, rInfo.body.name, lastUploadFiles, ((LoadRunnableInfo.FilesBody) rInfo.body).ignoreIncorrect, writeNotifier, boundary, rInfo.settings.logRequestData);
                                        } else {
                                            Utils.addFileParts(requestStream, rInfo.body.name, lastUploadFiles, ((LoadRunnableInfo.FilesBody) rInfo.body).ignoreIncorrect, writeNotifier, boundary, rInfo.settings.logRequestData);
                                        }
                                    } else {
                                        logger.e("no correct files in body: " + rInfo.body);
                                    }
                                }
                            }

                            break;
                    }

                    if (requestStream.size() > 0) {
                        if (!isEmpty(boundary)) {
                            Utils.addCloseLineMultipart(requestStream, boundary, rInfo.settings.logRequestData);
                        } else {
                            requestStream.writeBytes(LINE_FEED);
                        }
                    }

                    logger.d("closing output stream...");
                    requestStream.close();
//                    requestStream = null;
//                    }

                    if (!rInfo.isCanceled()) {

                        logger.d("reading response...");
                        lastResponse = new Response();
                        lastResponse.code = connection.getResponseCode(); // FIXME hang
                        lastResponse.message = connection.getResponseMessage();
                        logger.d("response acquired!");

                        boolean accepted;

                        if (rInfo.hasAcceptableResponseCodes()) {
                            accepted = false;
                            for (Integer code : rInfo.getAcceptableResponseCodes()) {
                                if (code != null && code == lastResponse.code && isResponseOk(code)) {
                                    accepted = true;
                                }
                            }
                        } else {
                            accepted = isResponseOk(lastResponse.code);
                        }


                        lastResponse.contentType = connection.getContentType();
                        lastResponse.contentLength = connection.getContentLength();
                        lastResponse.date = connection.getDate();
                        lastResponse.status = accepted ? Response.Status.ACCEPTED : Response.Status.DECLINED;

                        Map<String, List<String>> headerFields = connection.getHeaderFields();
                        if (headerFields != null) {
                            for (Map.Entry<String, List<String>> e : headerFields.entrySet()) {
                                List<String> parts = e.getValue();
                                if (!isEmpty(e.getKey())) {
                                    lastResponse.headers.add(new LoadRunnableInfo.NameValuePair(e.getKey(), join(LINE_FEED, parts)));
                                }
                            }
                        }
                        lastResponse.headers.add(new LoadRunnableInfo.NameValuePair("Content-Length", String.valueOf(lastResponse.contentLength)));

                        currentLoadInfo.totalDownloadBytesCount = lastResponse.contentLength > 0 ? lastResponse.contentLength : 0;

                        final boolean readSuccess;

                        final long startDownloadTime = System.currentTimeMillis();

                        IReadBytesNotifier readNotifier = new IReadBytesNotifier() {

                            long waitTime = 0;

                            long lastProcessingNotifyTime = 0;

                            @Override
                            public boolean isPaused() {
                                return rInfo.isPaused();
                            }

                            @Override
                            public boolean isCanceled() {
                                return rInfo.isCanceled();
                            }

                            @Override
                            public void onReadBytes(int count, byte[] data) {
                                currentLoadInfo.downloadedBytesCount += count;

                                currentLoadInfo.passedDownloadTime = System.currentTimeMillis() - startDownloadTime - waitTime;
                                currentLoadInfo.downloadSpeed = currentLoadInfo.passedDownloadTime > 0 ? (float) currentLoadInfo.downloadedBytesCount / (float) currentLoadInfo.passedDownloadTime : 0;
                                if (currentLoadInfo.totalDownloadBytesCount > 0) {
                                    currentLoadInfo.leftDownloadTime = currentLoadInfo.downloadSpeed > 0 ? (long) ((float) (currentLoadInfo.totalDownloadBytesCount - currentLoadInfo.downloadedBytesCount) / currentLoadInfo.downloadSpeed) : 0;
                                } else {
                                    currentLoadInfo.leftDownloadTime = 0;
                                }

                                synchronized (loadObservable) {
                                    if (loadObservable.getObservers().size() > 0) {
                                        boolean notified = false;
                                        for (LoadListener<LI> l : loadObservable.copyOfObservers()) {
                                            final int id = l.getId(rInfo);
                                            if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                                final long interval = System.currentTimeMillis() - lastProcessingNotifyTime;
                                                long targetInterval = l.getProcessingNotifyInterval(rInfo);
                                                targetInterval = targetInterval == LoadListener.INTERVAL_NOT_SPECIFIED ? LoadListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL : targetInterval;
                                                if (targetInterval > 0 && interval >= targetInterval || currentLoadInfo.downloadedBytesCount >= currentLoadInfo.totalDownloadBytesCount) {
                                                    long currentTime = System.currentTimeMillis();
//                                                        logger.d("updating downloading state (processing)...");
                                                    notifyStateProcessing(LoadListener.STATE.DOWNLOADING, l);
                                                    waitTime += System.currentTimeMillis() - currentTime;
                                                    notified = true;
                                                }
                                            }
                                        }
                                        if (notified)
                                            lastProcessingNotifyTime = System.currentTimeMillis();
                                    }
                                }
                            }
                        };

                        if (rInfo.settings.readBodyMode != LoadRunnableInfo.LoadSettings.ReadBodyMode.FILE || !isResponseOk(lastResponse.code)) {

                            logger.d("reading response to memory...");

                            LoadRunnableInfo.LoadSettings.ReadBodyMode mode = rInfo.settings.readBodyMode == LoadRunnableInfo.LoadSettings.ReadBodyMode.FILE ? BYTE_ARRAY : rInfo.settings.readBodyMode;

                            switch (mode) {
                                case BYTE_ARRAY:
                                    lastResponse.body = new LoadRunnableInfo.ByteArrayBody("", Utils.readResponseAsByteArray(connection, isResponseOk(lastResponse.code), readNotifier));
                                    break;

                                case STRING:
                                    lastResponse.body = new LoadRunnableInfo.StringBody("", Utils.readResponseAsStringBuffered(connection, isResponseOk(lastResponse.code), rInfo.settings.downloadCharset, readNotifier), rInfo.settings.downloadCharset);
                                    break;

                                default:
                                    throw new RuntimeException("unknown " + LoadRunnableInfo.LoadSettings.ReadBodyMode.class.getSimpleName() + ": " + mode);
                            }

                            readSuccess = true;
                            logger.d("response body successfully acquired to memory");

                        } else {

                            logger.d("reading response to file...");

                            try {
                                doStuffWithDownloadFile();
                            } catch (RuntimeException e) {
                                isFileReasonFail = true;
                                throw e;
                            }

                            // download the file
                            responseInput = new BufferedInputStream(connection.getInputStream());
                            FileOutputStream fos = lastDownloadFile.length() == 0 ? new FileOutputStream(lastDownloadFile) : new FileOutputStream(lastDownloadFile, true);
                            responseOutput = new BufferedOutputStream(fos, BUF_SIZE);
                            Utils.readResponseToOutputStream(responseInput, responseOutput, readNotifier);
                            fos.flush();

                            lastResponse.body = new LoadRunnableInfo.FileBody(lastDownloadFile.getName(), lastDownloadFile, false, false);
                            readSuccess = true;
                            logger.d("response body successfully acquired to file: " + lastDownloadFile + " / size: " + lastDownloadFile.length());
                        }

                        if (rInfo.settings.logResponseData) {
                            logger.d("code: " + lastResponse.code + ", message: " + lastResponse.message + ", data: " + lastResponse.body);
                            logger.d("headers: " + lastResponse.headers);
                        }

                        synchronized (loadObservable) {
                            for (LoadListener<LI> l : loadObservable.copyOfObservers()) {
                                final int id = l.getId(rInfo);
                                if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                    l.onResponse(rInfo, currentLoadInfo, lastResponse);
                                }
                            }
                        }

                        if (!accepted) {
                            success = false;
                            logger.e("not accepted: read success: " + readSuccess + ", response: " + lastResponse);
//                            currentLoadInfo.retriesCount = rInfo.settings.retryLimit;
//                            return;
                        } else {
                            success = readSuccess;
                        }
                    }

                } catch (Exception e) {
                    logger.e("an Exception occurred: " + e.getMessage(), lastException = e);

                } finally {
                    logger.d("finally");

                    try {

                        if (requestStream != null) {
                            requestStream.close();
                        }
                        if (responseInput != null) {
                            responseInput.close();
                        }
                        if (responseOutput != null) {
                            responseOutput.close();
                        }

                    } catch (IOException e) {
                        logger.e("an IOException occurred during close()", e);
                    }

//                    if (uploadLock != null) {
//                        FileHelper.releaseLockNoThrow(uploadLock);
//                    }
//
//                    if (downloadLock != null) {
//                        FileHelper.releaseLockNoThrow(downloadLock);
//                    }

                    if (connection != null) {
                        connection.disconnect();
                    }

                    if (success && !rInfo.isCanceled()) {

                        lastException = null;
                        logger.i("load " + rInfo + " success");
                        notifyStateChanged(LoadListener.STATE.SUCCESS);

                        if (currentLoadInfo.totalUploadBytesCount > 0 && rInfo.contentType == MULTIPART_FORM_DATA) {
                            if (rInfo.settings.allowDeleteUploadFiles) {
                                if (lastUploadFiles != null) {
                                    for (File uploadFile : lastUploadFiles) {
                                        if (uploadFile != null) {
                                            logger.i("deleting successfully uploaded file: " + uploadFile + "...");
                                            if (!FileHelper.deleteFile(uploadFile)) {
                                                logger.e("can't delete successfully uploaded file: " + uploadFile);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } else {

                        if (!success) {

                            if (lastDownloadFile != null) {
                                if (rInfo.settings.allowDeleteDownloadFile && FileHelper.isFileExists(lastDownloadFile.getAbsolutePath())) {
                                    logger.i("deleting unfinished download file: " + lastDownloadFile + "...");
                                    if (!FileHelper.deleteFile(lastDownloadFile)) {
                                        logger.e("can't delete unfinished download file: " + lastDownloadFile);
                                    }
                                }
                            }
                        }

                        if (!rInfo.isCanceled()) {

                            if (!success) {

                                notifyStateChanged(!isFileReasonFail ? LoadListener.STATE.FAILED : LoadListener.STATE.FAILED_FILE_REASON);

                                logger.e("load " + rInfo + " failed");
                                if (rInfo.settings.retryLimit == LoadRunnableInfo.LoadSettings.RETRY_LIMIT_UNLIMITED ||
                                        rInfo.settings.retryLimit != LoadRunnableInfo.LoadSettings.RETRY_LIMIT_NONE && currentLoadInfo.retriesCount < rInfo.settings.retryLimit) {

                                    int retriesLeft = rInfo.settings.retryLimit - currentLoadInfo.retriesCount;
                                    logger.e("load with id " + rInfo.id + " failed, retries left: " + retriesLeft);

                                    if (rInfo.settings.retryDelay > 0) {
                                        try {
                                            Thread.sleep(rInfo.settings.retryDelay);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                            logger.w("thread interrupted, cancelling load...");
                                            rInfo.cancel();
                                        }
                                    }

                                } else {
                                    logger.e("no retries left");
                                    notifyStateChanged(LoadListener.STATE.FAILED_RETRIES_EXCEEDED);
                                }

                            }
                        } else {
                            logger.w("load " + rInfo + " canceled");
                            final String message = "load with id " + rInfo.id + " was canceled";
                            lastException = lastException != null ? new RuntimeException(message, lastException) : new RuntimeException(message);
                            notifyStateChanged(LoadListener.STATE.CANCELLED);
                        }
                    }
                }
            }

        }

        /**
         * @return true if {@link LoadRunnableInfo.LoadSettings.ReadBodyMode} not {@link LoadRunnableInfo.LoadSettings.ReadBodyMode#FILE} or file was handled
         */
        boolean doStuffWithDownloadFile() throws RuntimeException {

            boolean handled = true;

            if (rInfo.settings.readBodyMode == LoadRunnableInfo.LoadSettings.ReadBodyMode.FILE) {

                handled = false;

                lastDownloadFile = rInfo.downloadFile;

                if (lastResponse != null) {

                    // response was acquired

                    if (lastDownloadFile == null) {

                        File downloadDirectory = rInfo.downloadDirectory;

                        if (downloadDirectory != null) {

                            if (FileHelper.checkDirNoThrow(downloadDirectory.getAbsolutePath())) {

                                String headerFileName = Utils.extractFileNameFromHeaders(lastResponse.headers);
                                if (isEmpty(headerFileName)) {
                                    headerFileName = Uri.parse(rInfo.getUrlString()).getLastPathSegment();
                                }
                                lastDownloadFile = FileHelper.createNewFile(headerFileName, downloadDirectory.getAbsolutePath());

                                if (lastDownloadFile != null) {
                                    handled = true;
                                } else {
                                    throw new RuntimeException("can't create download file: " + downloadDirectory.getAbsolutePath() + File.separator + headerFileName);
                                }

                            } else {
                                throw new RuntimeException("can't create download directory: " + downloadDirectory);
                            }

                        } else {
                            throw new RuntimeException("download directory was not specified");
                        }

                    } else {
                        // download file already handled
                        handled = true;
                    }

                }

                if (!handled) {
                    if (lastDownloadFile != null) {

                        handled = false;

                        if (lastDownloadFile.exists() && lastDownloadFile.isFile()) {

                            LoadRunnableInfo.LoadSettings.DownloadWriteMode writeMode = rInfo.settings.downloadWriteMode;

                            if (writeMode == RESUME_DOWNLOAD && lastResponse != null &&
                                    (lastResponse.contentLength > -1 && lastDownloadFile.length() < lastResponse.contentLength)) {
                                writeMode = null;
                                handled = true;
                            } else {
                                writeMode = OVERWRITE;
                            }

                            if (writeMode != null) {

                                switch (writeMode) {

                                    case OVERWRITE:
                                        if (FileHelper.createNewFile(lastDownloadFile.getName(), lastDownloadFile.getParent(), true) == null) {
                                            throw new RuntimeException("can't overwrite download file: " + lastDownloadFile);
                                        } else {
                                            handled = true;
                                        }
                                        break;

                                    case CREATE_NEW:
                                        int it = 1;
                                        while (lastDownloadFile.exists()) {
                                            String newName = lastDownloadFile.getName();
                                            String ext = FileHelper.getFileExtension(newName);
                                            if (!isEmpty(ext)) {
                                                newName = FileHelper.removeExtension(newName) + " (" + it + ")." + ext;
                                            } else {
                                                newName += " (" + it + ")";
                                            }
                                            lastDownloadFile = new File(lastDownloadFile.getParent(), newName);
                                            if (!lastDownloadFile.exists()) {
                                                if (FileHelper.createNewFile(lastDownloadFile.getName(), lastDownloadFile.getParent()) == null) {
                                                    throw new RuntimeException("can't create download file: " + lastDownloadFile);
                                                } else {
                                                    handled = true;
                                                    break;
                                                }
                                            }
                                            it++;
                                        }
                                        break;


                                    case DO_NOTING:
                                        throw new RuntimeException("overwriting download file " + lastDownloadFile + " is not allowed");

                                    default:
                                        throw new RuntimeException("unknown mode: " + rInfo.settings.downloadWriteMode);
                                }
                            }


                        } else {

                            String name = lastDownloadFile.getName();
                            String parent = lastDownloadFile.getParent();
                            lastDownloadFile = FileHelper.createNewFile(name, parent);

                            if (lastDownloadFile == null) {
                                throw new RuntimeException("can't create download file: " + parent + File.separator + name);
                            } else {
                                handled = true;
                            }
                        }

                        if (handled) {
                            if (!lastDownloadFile.canWrite()) {
                                throw new RuntimeException("can't write to download file: " + lastDownloadFile);
                            } else {
                                handled = true;
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("can't create file + in download directory: " + rInfo.downloadDirectory);
                }


            }


            if (!handled) {
                currentLoadInfo.totalDownloadBytesCount = 0;
            } else if (FileHelper.isFileCorrect(lastDownloadFile)) {
                currentLoadInfo.totalDownloadBytesCount = lastDownloadFile.length();
            }

            return handled;
        }
    }

    static final class Utils {

        /**
         * Adds a header field to the request.
         *
         * @param name  - name of the header field
         * @param value - value of the header field
         */
        static void addHeaderField(@NotNull DataOutputStream requestStream, @NotNull String name, @NotNull String value, boolean log) throws IOException {
            logger.d("addHeaderField(), name=" + name + ", value=" + value + ", log=" + log);

            if (log)
                logger.d("headerField=" + name + ": " + value + " LINE_FEED ");

            requestStream.writeBytes(name + ": " + value);
            requestStream.writeBytes(LINE_FEED);
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */
        static void addFormFieldMultipart(@NotNull DataOutputStream requestStream, @NotNull String name, @NotNull String value, String charset, String boundary, boolean log) throws IOException {
            logger.d("addFormField(), name=" + name + ", value=" + value + ", log=" + log);

            if (log)
                logger.d("formFieldMultipart=" + "--" + boundary + " LINE_FEED " + "Content-Disposition: form-data; name=" + name + " LINE_FEED " + "Content-Type: text/plain; charset=" + charset + " LINE_FEED " + " LINE_FEED " + value + " LINE_FEED ");

            requestStream.writeBytes("--" + boundary);
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes("Content-Type: text/plain; charset=" + charset);
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes(value);
            requestStream.writeBytes(LINE_FEED);
        }

        /**
         * Adds a upload file section to the request
         *
         * @param uploadFiles a File to be uploaded
         * @throws RuntimeException, IOException
         */
        static void addFileParts(@NotNull DataOutputStream requestStream, @NotNull Map<File, String> uploadFiles, boolean ignoreIncorrect, @Nullable IWriteNotifier notifier, String boundary, boolean log) throws RuntimeException, IOException {
            logger.d("addFileParts(), uploadFiles=" + uploadFiles + ", ignoreIncorrect=" + ignoreIncorrect + ", boundary=" + boundary + ", log=" + log);

            int bodySize = 0;

            for (File file : uploadFiles.keySet()) {

                if (!FileHelper.isFileCorrect(file) || !file.canRead()) {
                    logger.e("incorrect upload file: " + file);
                    if (!ignoreIncorrect) {
                        logger.e("aborting adding file parts...");
                        throw new RuntimeException("incorrect upload file: " + file);
                    } else {
                        continue;
                    }
                }

                String fieldName = uploadFiles.get(file);

                String fileName = file.getName();
                String contentType = URLConnection.guessContentTypeFromName(fileName);

                if (log)
                    logger.d("filePart=" + "--" + boundary + " LINE_FEED " + "Content-Disposition: form-data; name=" + fieldName + "; filename=" + fileName + "LINE_FEED " + "Content-Type: " + contentType + " LINE_FEED " + "Content-Transfer-Encoding: binary" + " LINE_FEED " + " LINE_FEED " + "-----file------" + " LINE_FEED");

                requestStream.writeBytes("--" + boundary);
                requestStream.writeBytes(LINE_FEED);
                requestStream.writeBytes(
                        "Content-Disposition: form-data; name=\"" + fieldName
                                + "\"; filename=\"" + fileName + "\"");
                requestStream.writeBytes(LINE_FEED);
                requestStream.writeBytes(
                        "Content-Type: "
                                + contentType);
                requestStream.writeBytes(LINE_FEED);
//                requestStream.writeBytes("Content-Transfer-Encoding: binary");
//                requestStream.writeBytes(LINE_FEED);
                requestStream.writeBytes(LINE_FEED);

                FileInputStream inputStream = new FileInputStream(file);
                byte[] buffer = new byte[BUF_SIZE];

                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) > -1) {

                    if (notifier != null) {
                        while (notifier.isPaused()) ;
                        if (notifier.isCanceled()) {
                            return;
                        }
                        notifier.onWriteBytes(bytesRead);
                    }
                    requestStream.write(buffer, 0, bytesRead);
                    bodySize += bytesRead;
                }
                requestStream.writeBytes(LINE_FEED);
            }

            if (bodySize > 0) {
                logger.d("body size: " + bodySize);
                logger.d("body added");
            }
        }

        /**
         * @param fieldName name attribute in <input type="file" name="..." />
         */
        static void addFileParts(@NotNull DataOutputStream requestStream, String fieldName, Set<File> uploadFiles, boolean ignoreIncorrect, @Nullable IWriteNotifier notifier, String boundary, boolean log) throws RuntimeException, IOException {
            Map<File, String> map = new LinkedHashMap<>();
            for (File file : uploadFiles) {
                map.put(file, fieldName);
            }
            addFileParts(requestStream, map, ignoreIncorrect, notifier, boundary, log);
        }

        /**
         * @param fieldName optional; if empty - name[] will be inserted
         */
        static void addFilePartsArray(@NotNull DataOutputStream requestStream, @Nullable String fieldName, Set<File> uploadFiles, boolean ignoreIncorrect, @Nullable IWriteNotifier notifier, String boundary, boolean log) throws RuntimeException, IOException {
            addFileParts(requestStream, isEmpty(fieldName) ? "name[]" : fieldName + "[]", uploadFiles, ignoreIncorrect, notifier, boundary, log);
        }

        static void addCloseLineMultipart(@NotNull DataOutputStream requestStream, String boundary, boolean log) throws IOException {
            logger.d("addCloseLineMultipart()");

            if (log)
                logger.d("closeLine=" + " LINE_FEED " + "--" + boundary + "--" + " LINE_FEED");

            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes("--" + boundary + "--");
            requestStream.writeBytes(LINE_FEED);
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */
        static void addFormFieldUrlEncoded(@NotNull DataOutputStream requestStream, @NotNull String name, @NotNull String value, boolean log) throws IOException {
            logger.d("addFormFieldUrlEncoded(), name=" + name + ", value=" + value + ", log=" + log);

            if (log)
                logger.d("formFieldUrlEncoded=" + "LINE_FEED " + name + "=" + value + " LINE_FEED");

            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes(name + "=" + value);
            requestStream.writeBytes(LINE_FEED);
        }

        static void addBody(@NotNull DataOutputStream requestStream, @NotNull LoadRunnableInfo.StringBody body, boolean log) throws RuntimeException, IOException {
            logger.d("addBody(), body=" + body + ", log=" + log);

            if (body.isEmpty()) {
                logger.d("body is empty");
                return;
            }

            if (log)
                logger.d("body=" + "LINE_FEED " + body.getString() + " LINE_FEED ");

            byte[] data = body.getBytes();
            logger.d("body size: " + data.length);

            requestStream.writeBytes(LINE_FEED);
            requestStream.write(data);
            requestStream.writeBytes(LINE_FEED);
            logger.d("body added");
        }

        @NotNull
        static byte[] readResponseAsByteArray(@NotNull HttpURLConnection connection, boolean inputOrError, @Nullable IReadBytesNotifier readNotifier) throws IOException {

            BufferedInputStream bis = new BufferedInputStream(
                    inputOrError ? connection.getInputStream() : connection.getErrorStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int count;
            byte[] data = new byte[BUF_SIZE];

            while ((count = bis.read(data, 0, data.length)) > -1) {
                if (readNotifier != null) {
                    while (readNotifier.isPaused()) ;
                    if (readNotifier.isCanceled()) {
                        break;
                    }
                    readNotifier.onReadBytes(count, data);
                }
                bos.write(data, 0, count);
            }
            bos.flush();

            bis.close();
            bos.close();
            return bos.toByteArray();
        }

        @NotNull
        static String readResponseAsStringBuffered(@NotNull HttpURLConnection connection, boolean inputOrError, String charset, @Nullable IReadBytesNotifier readNotifier) throws IOException {
            return new String(readResponseAsByteArray(connection, inputOrError, readNotifier), Charset.forName(charset));
        }

        @NotNull
        static String readResponseAsStringByteToByte(@NotNull HttpURLConnection connection, boolean inputOrError, @Nullable IReadBytesNotifier readNotifier) throws IOException {

            BufferedInputStream bis = new BufferedInputStream(
                    inputOrError ? connection.getInputStream() : connection.getErrorStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int readByte;
            while ((readByte = bis.read()) > -1) {
                if (readNotifier != null) {
                    while (readNotifier.isPaused()) ;
                    if (readNotifier.isCanceled()) {
                        break;
                    }
                    readNotifier.onReadBytes(1, new byte[]{(byte) readByte});
                }
                bos.write(readByte);
            }
            bos.flush();

            bis.close();
            bos.close();
            return bos.toString();
        }

        @NotNull
        static List<String> readResponseAsStrings(@NotNull HttpURLConnection connection, boolean inputOrError, @Nullable IReadLineNotifier readNotifier) throws IOException {
            final List<String> response = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputOrError ? connection.getInputStream() : connection.getErrorStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (readNotifier != null) {
                    while (readNotifier.isPaused()) ;
                    if (readNotifier.isCanceled()) {
                        break;
                    }
                    readNotifier.onReadLine(line);
                }
                response.add(line);
            }
            reader.close();
            return response;
        }

        static void readResponseToOutputStream(@NotNull InputStream inStream, @NotNull OutputStream outStream, @Nullable IReadBytesNotifier readNotifier) throws IOException {
            byte data[] = new byte[BUF_SIZE];
            int count;

            while ((count = inStream.read(data, 0, BUF_SIZE)) >= 0) {
                if (readNotifier != null) {
                    while (readNotifier.isPaused()) ;
                    if (readNotifier.isCanceled()) {
                        break;
                    }
                    readNotifier.onReadBytes(count, data);
                }
                outStream.write(data, 0, count);
            }
            outStream.flush();
        }

        @Nullable
        static String extractFileNameFromHeaders(Set<LoadRunnableInfo.NameValuePair> headers) {
            for (LoadRunnableInfo.NameValuePair pair : headers) {
                if (pair.name.equalsIgnoreCase("Content-Disposition")) {
                    if (!isEmpty(pair.value)) {
                        String[] parts = pair.value.split(";");
                        if (parts.length > 1) {
                            for (String part : parts) {
                                if (!isEmpty(part) && part.contains("=")) {
                                    String[] subparts = part.split("=");
                                    if (subparts.length == 2) {
                                        if (subparts[0].equalsIgnoreCase("filename")) {
                                            return subparts[subparts.length - 1];
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    public static class LoadProcessInfo {

        @NotNull
        LoadListener.STATE state = LoadListener.STATE.UNKNOWN;

        int retriesCount = -1;

        long passedUploadTime;
        long passedDownloadTime;

        long leftUploadTime;
        long leftDownloadTime;

        long uploadedBytesCount;
        long downloadedBytesCount;

        long totalUploadBytesCount;
        long totalDownloadBytesCount;

        /**
         * bytes/ms
         */
        float downloadSpeed;
        /**
         * bytes/ms
         */
        float uploadSpeed;

        @NotNull
        public LoadListener.STATE getState() {
            return state;
        }

        public int getRetriesCount() {
            return retriesCount + 1;
        }

        public long getPassedUploadTimeMs() {
            return passedUploadTime;
        }

        public float getPassedUploadTimeS() {
            return TimeUnit.MILLISECONDS.toSeconds(passedUploadTime);
        }

        public long getPassedDownloadTimeMs() {
            return passedDownloadTime;
        }

        public float getPassedDownloadTimeS() {
            return TimeUnit.MILLISECONDS.toSeconds(passedDownloadTime);
        }

        public long getLeftUploadTimeMsMs() {
            return leftUploadTime;
        }

        public float getLeftUploadTimeS() {
            return TimeUnit.MILLISECONDS.toSeconds(leftUploadTime);
        }

        public long getLeftDownloadTimeMs() {
            return leftDownloadTime;
        }

        public float getLeftDownloadTimeS() {
            return TimeUnit.MILLISECONDS.toSeconds(leftDownloadTime);
        }

        public long getUploadedBytesCount() {
            return uploadedBytesCount;
        }

        public long getDownloadedBytesCount() {
            return downloadedBytesCount;
        }


        public long getTotalUploadBytesCount() {
            return totalUploadBytesCount;
        }

        public long getTotalDownloadBytesCount() {
            return totalDownloadBytesCount;
        }


        /**
         * bytes/ms
         */
        public float getDownloadSpeed() {
            return downloadSpeed;
        }

        /**
         * bytes/ms
         */
        public float getUploadSpeed() {
            return uploadSpeed;
        }

        public float getUploadedPercentage() {
            return (totalUploadBytesCount > 0 ? (float) uploadedBytesCount / totalUploadBytesCount : 0f) * 100f;
        }

        public float getDownloadedPercentage() {
            return (totalDownloadBytesCount > 0 ? (float) downloadedBytesCount / totalDownloadBytesCount : 0f) * 100f;
        }

        void setToInitial() {
            retriesCount = -1;
            passedUploadTime = 0;
            passedDownloadTime = 0;
            leftUploadTime = 0;
            leftDownloadTime = 0;
            uploadedBytesCount = 0;
            downloadedBytesCount = 0;
            totalUploadBytesCount = 0;
            totalDownloadBytesCount = 0;
        }

        @Override
        public String toString() {
            return "LoadProcessInfo{" +
                    "retriesCount=" + retriesCount +
                    ", passedUploadTime=" + passedUploadTime +
                    ", passedDownloadTime=" + passedDownloadTime +
                    ", leftUploadTime=" + leftUploadTime +
                    ", leftDownloadTime=" + leftDownloadTime +
                    ", uploadedBytesCount=" + uploadedBytesCount +
                    ", downloadedBytesCount=" + downloadedBytesCount +
                    ", totalUploadBytesCount=" + totalUploadBytesCount +
                    ", totalDownloadBytesCount=" + totalDownloadBytesCount +
                    ", downloadSpeed=" + downloadSpeed +
                    ", uploadSpeed=" + uploadSpeed +
                    '}';
        }
    }

    public static class Response {

        public static final int RESPONSE_CODE_UNKNOWN = -1;

        @NotNull
        Status status = Status.DECLINED;

        int code = RESPONSE_CODE_UNKNOWN;

        String message;

        @NotNull
        Set<LoadRunnableInfo.NameValuePair> headers = new LinkedHashSet<>();

        /**
         * will be empty if response is not ok
         */
        @Nullable
        LoadRunnableInfo.Body body;

        String contentType;

        /**
         * this will be useful to display download percentage
         * might be -1: server did not report the length
         */
        int contentLength = -1;

        long date;

        @NotNull
        public Status getStatus() {
            return status;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        @NotNull
        public Set<LoadRunnableInfo.NameValuePair> getHeaders() {
            return Collections.unmodifiableSet(headers);
        }

        @Nullable
        public LoadRunnableInfo.Body getBody() {
            return body;
        }

        public String getContentType() {
            return contentType;
        }

        public int getContentLength() {
            return contentLength;
        }

        public long getDate() {
            return date;
        }

        void setToInitial() {
            code = RESPONSE_CODE_UNKNOWN;
            message = null;
            headers = new LinkedHashSet<>();
            body = null;
            contentType = null;
            contentLength = -1;
            date = 0;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    ", headers=" + headers +
                    ", body=" + body +
                    ", contentType='" + contentType + '\'' +
                    ", contentLength=" + contentLength +
                    ", date=" + date +
                    ", status=" + status +
                    '}';
        }

        public enum Status {
            ACCEPTED, DECLINED
        }
    }

    private interface INotifier {
        boolean isCanceled();

        boolean isPaused();
    }

    private interface IWriteNotifier extends INotifier {

        void onWriteBytes(int count);
    }

    private interface IReadLineNotifier extends INotifier {

        void onReadLine(String line);
    }

    private interface IReadBytesNotifier extends INotifier {

        void onReadBytes(int count, byte[] data);
    }
}
