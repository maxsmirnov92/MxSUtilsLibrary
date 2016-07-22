package net.maxsmr.networkutils.loadutil.executors.base.upload;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.networkutils.loadutil.executors.base.LoadExecutor;
import net.maxsmr.networkutils.loadutil.executors.base.LoadFileListener;
import net.maxsmr.networkutils.loadutil.executors.base.LoadFileListener.STATE;
import net.maxsmr.networkutils.loadutil.executors.base.LoadRunnableInfo;
import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;
import net.maxsmr.tasksutils.taskrunnable.TaskRunnable;


public class UploadExecutor extends LoadExecutor<UploadRunnableInfo, UploadExecutor.UploadRunnable> {

    private static final Logger logger = LoggerFactory.getLogger(UploadExecutor.class);

    public UploadExecutor(int uploadsLimit, int corePoolSize) {
        super(UploadExecutor.class.getName(), uploadsLimit, corePoolSize, UploadRunnable.class);
    }

    public final void upload(UploadRunnableInfo rInfo) throws NullPointerException {
        logger.debug("upload(), rInfo=" + rInfo);
        doActionOnFile(rInfo);
    }

    @Override
    protected UploadRunnable newRunnable(UploadRunnableInfo rInfo) {
        return new UploadRunnable(rInfo);
    }

    private static final String LINE_FEED = "\r\n";
    private static final String CHARSET = Charset.defaultCharset().name();

    protected class UploadRunnable extends TaskRunnable<UploadRunnableInfo> {

        final String boundary = "++++" + System.currentTimeMillis();

        public UploadRunnable(UploadRunnableInfo rInfo) {
            super(rInfo);
        }

        @Override
        protected boolean checkArgs() {

            if (!rInfo.verifyUrl()) {
                throw new IllegalArgumentException("incorrect url: " + rInfo.url);
            }

            return true;
        }

        @Override
        public void run() {
            super.run();
            doUpload();
        }

        int totalUploaded = 0;
        int totalDownloaded = 0;

        boolean uploadSuccess = false;
        Throwable lastException = null;

        private void doUpload() {

            final long startUploadTime = System.currentTimeMillis();

            if (rInfo.isCancelled()) {
                logger.warn("upload " + rInfo + " cancelled");
//                notifyStateChanged(STATE.CANCELLED, rInfo, System.currentTimeMillis() - startUploadTime, 0, 0, null);
                return;
            }

            if (rInfo.fileFormField != null && !FileHelper.isFileCorrect(rInfo.fileFormField.sourceFile)) {
                logger.error("source file " + rInfo.fileFormField.sourceFile + " is not correct");
                notifyStateChanged(STATE.FAILED_NOT_STARTED, rInfo, System.currentTimeMillis() - startUploadTime, 0, 0, lastException = new RuntimeException("upload not started: incorrect source file" + rInfo.fileFormField.sourceFile));
                return;

            } else if (rInfo.fileFormField == null) {

                if (rInfo.getHeaderFields().isEmpty() && rInfo.getFormFields().isEmpty()) {
                    logger.error("nothing to upload: fileFormField, headerFields, formFields not specified");
                    notifyStateChanged(STATE.FAILED_NOT_STARTED, rInfo, System.currentTimeMillis() - startUploadTime, 0, 0, lastException = new RuntimeException("nothing to upload: fileFormField, headerFields, formFields not specified"));
                    return;
                }
            }

            int retriesCount = 0;

            final long fileLength = rInfo.fileFormField != null ? rInfo.fileFormField.sourceFile.length() : 0;

            while (!uploadSuccess && !rInfo.isCancelled() && (rInfo.settings.retryLimit == LoadRunnableInfo.RETRY_LIMIT_NONE || retriesCount < rInfo.settings.retryLimit)) {

                if (rInfo.isCancelled()) {
                    logger.warn("upload " + rInfo + " cancelled");
                    notifyStateChanged(STATE.CANCELLED, rInfo, System.currentTimeMillis() - startUploadTime, 0, 0, null);
                    return;
                }

                FileLock lock = null;
                if (fileLength > 0) {
                    lock = FileHelper.lockFileChannel(rInfo.fileFormField.sourceFile, true);
                }

                notifyStateChanged(STATE.STARTING, rInfo, System.currentTimeMillis() - startUploadTime, 0, 0, null);

                HttpURLConnection connection = null;
                InputStream input = null;
                DataOutputStream requestStream = null;

                totalUploaded = 0;

                lastException = null;

                try {
                    logger.debug("opening connection on " + rInfo.url + "...");
                    connection = (HttpURLConnection) rInfo.url.openConnection();

                    connection.setConnectTimeout(rInfo.settings.connectionTimeout);
                    connection.setReadTimeout(rInfo.settings.readTimeout);

                    connection.setUseCaches(false);
                    connection.setDoOutput(true); // indicates POST method`
                    connection.setDoInput(true);

                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("Cache-Control", "no-cache");
                    connection.setRequestProperty("Content-Type",
                            "multipart/form-data; boundary=" + boundary);

//                    connection.setRequestProperty("Accept", "*/*");
//                    connection.setRequestProperty("ENCTYPE", "multipart/form-data");
//                    connection.setRequestProperty("Expect", "100-continue");
//                    connection.setRequestProperty("Content-Length", String.valueOf(rInfo.fileFormField.sourceFile.length()));
//                    connection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");

                    if (rInfo.settings.logRequestData) {
                        String query = rInfo.url.getQuery();
                        logger.debug("URL: " + rInfo.url);
                        logger.debug("POST " + rInfo.url.getPath() + (!TextUtils.isEmpty(query) ? "?" + query : ""));
                        logger.debug("Host: " + rInfo.url.getHost());
                        logger.debug("Connection: " + "Keep-Alive");
                        logger.debug("Cache-Control: " + "no-cache");
                        logger.debug("Content-Type: " + "multipart/form-data; boundary=" + boundary);
                    }

                    requestStream = new DataOutputStream(connection.getOutputStream()); // new PrintWriter(new OutputStreamWriter(output, CHARSET), true);

                    logger.debug("starting write to output stream...");

                    for (UploadRunnableInfo.Field f : rInfo.getHeaderFields()) {
                        addHeaderField(requestStream, f.name, f.value, rInfo.settings.logRequestData);
                    }

                    for (UploadRunnableInfo.Field f : rInfo.getFormFields()) {
                        addFormField(requestStream, f.name, f.value, rInfo.settings.logRequestData);
                    }

                    if (fileLength > 0) {

                        final IWriteNotifier notifier = new IWriteNotifier() {

                            long lastProcessingNotifyTime = 0;

                            @Override
                            public boolean isCancelled() {
                                return rInfo.isCancelled();
                            }

                            @Override
                            public void onWriteBytes(int count) {
//                                logger.debug("onWriteBytes(), count=" + count);
                                totalUploaded += count;

                                if (rInfo.settings.notifyWrite) {
                                    synchronized (loadListeners) {
                                        if (loadListeners.size() > 0) {
                                            boolean notified = false;
                                            for (LoadFileListener<UploadRunnableInfo> l : loadListeners) {
                                                final long interval = System.currentTimeMillis() - lastProcessingNotifyTime;
//                                                logger.debug("interval=" + interval);
                                                long targetInterval = l.getProcessingNotifyInterval(rInfo);
                                                targetInterval = targetInterval == LoadFileListener.INTERVAL_NOT_SPECIFIED? LoadFileListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL : targetInterval;
                                                if (targetInterval > 0 && interval >= targetInterval || totalUploaded >= fileLength) {
                                                    final int id = l.getId(rInfo);
                                                    if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                                        logger.debug("updating uploading state (processing)...");
                                                        l.onUpdateState(STATE.PROCESSING, rInfo, System.currentTimeMillis() - startUploadTime, (float) totalUploaded / 1024f,
                                                                (float) fileLength / 1024f, null);
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

                        addFilePart(requestStream, rInfo.fileFormField.name, rInfo.fileFormField.sourceFile, notifier, rInfo.settings.logRequestData);
                    }

                    addCloseLine(requestStream, rInfo.settings.logRequestData);

                    logger.debug("closing output stream...");
                    requestStream.close();
                    requestStream = null;

                    logger.debug("reading response...");
                    final int responseCode = connection.getResponseCode();
                    final String responseMessage = connection.getResponseMessage();
                    logger.debug("response acquired!");

                    if (!rInfo.isCancelled()) {

                        boolean accepted;

                        if (rInfo.acceptableResponseCodes.size() > 0) {
                            accepted = false;
                            for (Integer code : rInfo.acceptableResponseCodes) {
                                if (code != null && code == responseCode && LoadExecutor.isResponseOk(code)) {
                                    accepted = true;
                                }
                            }
                        } else {
                            accepted = responseCode == HttpURLConnection.HTTP_OK;
                        }

                        final String responseContentType = connection.getContentType();
                        final int responseContentLength = connection.getContentLength();
                        final long responseDate = connection.getDate();

                        List<LoadRunnableInfo.Field> headers = new ArrayList<>();
                        headers.add(new LoadRunnableInfo.Field("Content-Type", responseContentType));
                        headers.add(new LoadRunnableInfo.Field("Content-Length", String.valueOf(responseContentLength)));
                        headers.add(new LoadRunnableInfo.Field("Date", String.valueOf(responseDate)));

                        List<String> response = null;

                        try {

                            final long startDownloadTime = System.currentTimeMillis();

                            IReadNotifier notifier = new IReadNotifier() {

                                long lastProcessingNotifyTime = 0;

                                @Override
                                public boolean isCancelled() {
                                    return rInfo.isCancelled();
                                }

                                @Override
                                public void onReadLine(String line) {
//                                    logger.debug("onReadLine(), line=" + line);
                                    totalDownloaded += line.getBytes().length;

                                    if (rInfo.settings.notifyRead) {
                                        synchronized (loadListeners) {
                                            if (loadListeners.size() > 0) {
                                                boolean notified = false;
                                                for (LoadFileListener<UploadRunnableInfo> l : loadListeners) {
                                                    final long interval = System.currentTimeMillis() - lastProcessingNotifyTime;
//                                                    logger.debug("interval=" + interval);
                                                    long targetInterval = l.getProcessingNotifyInterval(rInfo);
                                                    targetInterval = targetInterval == LoadFileListener.INTERVAL_NOT_SPECIFIED? LoadFileListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL : targetInterval;
                                                    if (targetInterval > 0 && interval >= targetInterval || totalDownloaded >= responseContentLength) {
                                                        final int id = l.getId(rInfo);
                                                        if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                                            logger.debug("updating downloading state (processing)...");
                                                            l.onUpdateState(STATE.PROCESSING, rInfo, System.currentTimeMillis() - startDownloadTime, (float) totalDownloaded / 1024f,
                                                                    (float) responseContentLength / 1024f, null);
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
                            logger.debug("reading response body...");
                            response = readResponse(connection, isResponseOk(responseCode), notifier);
                            logger.debug("response body acquired!");
                        } catch (IOException e) {
                            e.printStackTrace();
                            logger.error("an IOException occurred during readResponse()", e);
                        }

                        if (rInfo.settings.logResponseData) {
                            logger.debug("code: " + responseCode + ", message: " + responseMessage + ", data: " + response);
                            logger.debug("headers: " + headers);
                            logger.debug("response: " + response);
                        }

                        synchronized (loadListeners) {
                            LoadFileListener.ResponseStatus status = accepted ? LoadFileListener.ResponseStatus.ACCEPTED : LoadFileListener.ResponseStatus.DECLINED;
                            for (LoadFileListener<UploadRunnableInfo> l : loadListeners) {
                                final int id = l.getId(rInfo);
                                if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                    l.onResponseCode(status, rInfo, responseCode, responseMessage);
                                    l.onResponseHeaders(status, rInfo, headers);
                                    l.onResponseBody(status, rInfo, response);
                                }
                            }
                        }

                        if (!(uploadSuccess = accepted)) {
//                            retriesCount++;
                            break;
                        }
                    }

                } catch (Exception e) {
                    logger.error("an Exception occurred", lastException = e);
                    e.printStackTrace();
                    retriesCount++;

                } finally {
                    logger.debug("finally");

                    try {

                        if (requestStream != null) {
                            requestStream.close();
                        }
                        if (input != null) {
                            input.close();
                        }

                    } catch (IOException e) {
                        logger.error("an IOException occurred during close()", e);
                    }

                    if (fileLength > 0) {
                        FileHelper.releaseLockNoThrow(lock);
                    }

                    if (connection != null)
                        connection.disconnect();

                    if (uploadSuccess) {

                        logger.info("upload " + rInfo + " success");
                        notifyStateChanged(STATE.SUCCESS, rInfo, System.currentTimeMillis() - startUploadTime, (float) totalUploaded / 1024f, ((float) fileLength / 1024f), null);

                        if (fileLength > 0) {
                            if (rInfo.fileFormField.deleteUploadedFile) {
                                logger.info("deleting uploaded file: " + rInfo.fileFormField.sourceFile + "...");
                                if (!rInfo.fileFormField.sourceFile.delete()) {
                                    logger.error("can't delete file: " + rInfo.fileFormField.sourceFile);
                                }
                            }
                        }

                    } else {

                        if (!rInfo.isCancelled()) {
                            logger.error("upload " + rInfo + " failed, retries left: " + (rInfo.settings.retryLimit - retriesCount));
                            if (retriesCount < rInfo.settings.retryLimit) {
                                notifyStateChanged(STATE.FAILED, rInfo, System.currentTimeMillis() - startUploadTime, (float) totalUploaded / 1024f, (float) fileLength / 1024f, lastException = new Throwable("upload with id " + rInfo.id + " failed, retries left: " + (rInfo.settings.retryLimit - retriesCount), lastException));
                            } else {
                                notifyStateChanged(STATE.FAILED_RETRIES_EXCEEDED, rInfo, System.currentTimeMillis() - startUploadTime, (float) totalUploaded / 1024f, (float) fileLength / 1024f, null);
                            }
                        } else {
                            logger.error("upload " + rInfo + " cancelled");
                            notifyStateChanged(STATE.CANCELLED, rInfo, System.currentTimeMillis() - startUploadTime, (float) totalUploaded / 1024f,
                                    (float) fileLength / 1024f, lastException = new Throwable("upload with id " + rInfo.id + " was cancelled"));
                        }
                    }
                }
            }

        }

        /**
         * Adds a header field to the request.
         *
         * @param name  - name of the header field
         * @param value - value of the header field
         */
        private void addHeaderField(@NonNull DataOutputStream requestStream, @NonNull String name, @NonNull String value, boolean log) throws IOException {
            logger.debug("addHeaderField(), name=" + name + ", value=" + value + ", log=" + log);

            if (log)
                logger.debug("headerField=" + name + ": " + value + "LINE_FEED");

            requestStream.writeBytes(name + ": " + value);
            requestStream.writeBytes(LINE_FEED);
        }

        /**
         * Adds a form field to the request
         *
         * @param name  field name
         * @param value field value
         */
        private void addFormField(@NonNull DataOutputStream requestStream, @NonNull String name, @NonNull String value, boolean log) throws IOException {
            logger.debug("addFormField(), name=" + name + ", value=" + value + ", log=" + log);

            if (log)
                logger.debug("formField=" + "--" + boundary + "LINE_FEED" + "Content-Disposition: form-data; name=" + name + "LINE_FEED" + "Content-Type: text/plain; charset=" + CHARSET + "LINE_FEED" + "LINE_FEED" + value + "LINE_FEED");

            requestStream.writeBytes("--" + boundary);
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes("Content-Type: text/plain; charset=" + CHARSET);
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes(value);
            requestStream.writeBytes(LINE_FEED);
        }

        /**
         * Adds a upload file section to the request
         *
         * @param fieldName  name attribute in <input type="file" name="..." />
         * @param uploadFile a File to be uploaded
         * @throws RuntimeException, IOException
         */
        private void addFilePart(@NonNull DataOutputStream requestStream, @NonNull String fieldName, @NonNull File uploadFile, @Nullable IWriteNotifier notifier, boolean log) throws RuntimeException, IOException {
            logger.debug("addFilePart(), fieldName=" + fieldName + ", uploadFile=" + uploadFile + ", log=" + log);

            if (!FileHelper.isFileCorrect(uploadFile)) {
                throw new RuntimeException("incorrect upload file: " + uploadFile);
            }

            String fileName = uploadFile.getName();
            String contentType = URLConnection.guessContentTypeFromName(fileName);

            if (log)
                logger.debug("filePart=" + "--" + boundary + "LINE_FEED" + "Content-Disposition: form-data; name=" + fieldName + "; filename=" + fileName + "LINE_FEED" + "Content-Type: " + contentType + "LINE_FEED" + "Content-Transfer-Encoding: binary" + "LINE_FEED" + "LINE_FEED" + "-----file------" + "LINE_FEED");

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
            requestStream.writeBytes("Content-Transfer-Encoding: binary");
            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes(LINE_FEED);

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[BUF_SIZE];

            int bytesRead;
            while ((notifier == null || !notifier.isCancelled()) && (bytesRead = inputStream.read(buffer)) > -1) {
                if (notifier != null) {
                    notifier.onWriteBytes(bytesRead);
                }
                requestStream.write(buffer, 0, bytesRead);
            }

            requestStream.writeBytes(LINE_FEED);
        }

        private void addCloseLine(@NonNull DataOutputStream requestStream, boolean log) throws IOException {
            logger.debug("addCloseLine()");

            if (log)
                logger.debug("closeLine=" + "LINE_FEED" + "--" + boundary + "--" + "LINE_FEED");

            requestStream.writeBytes(LINE_FEED);
            requestStream.writeBytes("--" + boundary + "--");
            requestStream.writeBytes(LINE_FEED);
        }

        @NonNull
        private List<String> readResponse(@NonNull HttpURLConnection connection, boolean input, @Nullable IReadNotifier readNotifier) throws IOException {
            final List<String> response = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    input ? connection.getInputStream() : connection.getErrorStream()));
            String line;
            while ((readNotifier == null || !readNotifier.isCancelled()) && ((line = reader.readLine()) != null)) {
                if (readNotifier != null) {
                    readNotifier.onReadLine(line);
                }
                response.add(line);
            }
            return response;
        }
    }

    private interface IWriteNotifier {

        boolean isCancelled();

        void onWriteBytes(int count);
    }

    private interface IReadNotifier {

        boolean isCancelled();

        void onReadLine(String line);
    }
}
