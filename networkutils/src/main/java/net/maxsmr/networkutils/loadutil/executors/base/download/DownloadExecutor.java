package net.maxsmr.networkutils.loadutil.executors.base.download;

import android.support.annotation.NonNull;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.networkutils.loadutil.executors.base.LoadExecutor;
import net.maxsmr.networkutils.loadutil.executors.base.LoadFileListener;
import net.maxsmr.networkutils.loadutil.executors.base.LoadFileListener.STATE;
import net.maxsmr.networkutils.loadutil.executors.base.LoadRunnableInfo;
import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;
import net.maxsmr.tasksutils.taskrunnable.TaskRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;


public class DownloadExecutor extends LoadExecutor<DownloadRunnableInfo, DownloadExecutor.DownloadRunnable> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadExecutor.class);

    public DownloadExecutor(int downloadsLimit, int corePoolSize) {
        super(DownloadExecutor.class.getName(), downloadsLimit, corePoolSize, DownloadRunnable.class);
    }

    public final void downloadFile(DownloadRunnableInfo rInfo) throws NullPointerException {
        logger.debug("downloadFile(), rInfo=" + rInfo);
        doActionOnFile(rInfo);
    }

    @Override
    protected DownloadRunnable newRunnable(DownloadRunnableInfo rInfo) {
        return new DownloadRunnable(rInfo);
    }


    protected class DownloadRunnable extends TaskRunnable<DownloadRunnableInfo> {

//        private final NetworkHelper networkHelper;

        private void deleteUnfinishedFile(boolean allow, @NonNull File f) {
            if (allow && FileHelper.isFileExists(f.getAbsolutePath())) {
                logger.info("deleting unfinished file: " + f + "...");
                if (!f.delete()) {
                    logger.error("can't delete file: " + f);
                }
            }
        }

        @Override
        protected boolean checkArgs() {

            if (!rInfo.verifyUrl()) {
                throw new IllegalArgumentException("incorrect url: " + rInfo.url);
            }

            if (rInfo.destFile == null) {
                throw new NullPointerException("destFile is null");
            }

            return true;
        }

        public DownloadRunnable(DownloadRunnableInfo rInfo) {
            super(rInfo);
        }

        @Override
        public void run() {
            super.run();
            doDownload();
        }

        private void doDownload() {

            final long startDownloadTime = System.currentTimeMillis();

            if (rInfo.isCancelled()) {
                logger.warn("download " + rInfo + " cancelled");
                deleteUnfinishedFile(rInfo.deleteUnfinishedFile, rInfo.destFile);
                notifyStateChanged(STATE.CANCELLED, rInfo, System.currentTimeMillis() - startDownloadTime, 0, 0, null);
                return;
            }

            File destFile = rInfo.destFile;

            if (destFile == null)
                throw new NullPointerException("destFile is null");

            if (destFile.exists() && destFile.isFile()) {

                if (rInfo.overwriteExistingFile) {

                    RandomAccessFile ra = null;
                    try {
                        ra = new RandomAccessFile(destFile, "rw");
                        ra.setLength(0);

                    } catch (Exception e) {
                        logger.error("an Exception occurred", e);
                        notifyStateChanged(STATE.FAILED_NOT_STARTED, rInfo, System.currentTimeMillis() - startDownloadTime, 0, 0, new Throwable("can't overwrite " + destFile, e));
                        return;

                    } finally {
                        if (ra != null) {
                            try {
                                ra.close();
                            } catch (IOException e) {
                                logger.error("an IOException occurred during close()", e);
                            }
                        }
                    }

                } else {
                    notifyStateChanged(STATE.FAILED, rInfo, System.currentTimeMillis() - startDownloadTime, 0, 0, new Throwable("overwriting " + destFile + " forbidden"));
                    return;
                }

            } else {

                destFile = FileHelper.createNewFile(destFile.getName(), destFile.getParent());

                if (!(destFile != null && destFile.isFile() && destFile.exists() && !FileHelper.isFileLocked(destFile))) {
                    logger.error("can't create dest file: " + destFile);
                    notifyStateChanged(STATE.FAILED_NOT_STARTED, rInfo, System.currentTimeMillis() - startDownloadTime, 0, 0, new Throwable("can't create dest file: " + destFile));
                    return;
                }
            }

            int retriesCount = 0;

            boolean downloadSuccess = false;
//            boolean downloadCancelled = false;
            Throwable lastException;

            while (!downloadSuccess && !rInfo.isCancelled() &&
                    (rInfo.settings.retryLimit == LoadRunnableInfo.LoadSettings.RETRY_LIMIT_UNLIMITED
                            || (rInfo.settings.retryLimit != LoadRunnableInfo.LoadSettings.RETRY_LIMIT_NONE && retriesCount < rInfo.settings.retryLimit))) {

                if (Thread.currentThread().isInterrupted()) {
                    logger.warn("thread interrupted, cancelling upload...");
                    rInfo.cancel();
                }

                if (rInfo.isCancelled()) {
                    if (retriesCount == 0) {
                        logger.warn("download " + rInfo + " cancelled");
                        deleteUnfinishedFile(rInfo.deleteUnfinishedFile, rInfo.destFile);
                        notifyStateChanged(STATE.CANCELLED, rInfo, System.currentTimeMillis() - startDownloadTime, 0, 0, null);
                    }
                    return;
                }

                FileLock lock = FileHelper.lockFileChannel(destFile, true);

                notifyStateChanged(STATE.STARTING, rInfo, System.currentTimeMillis() - startDownloadTime, 0, 0, null);

                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;

                int contentLength = 0;
                int total = 0;

                lastException = null;

                try {
                    connection = (HttpURLConnection) rInfo.url.openConnection();

                    connection.setConnectTimeout(rInfo.settings.connectionTimeout);
                    connection.setReadTimeout(rInfo.settings.readTimeout);

                    connection.connect();

                    final int responseCode = connection.getResponseCode();
                    final String responseMessage = connection.getResponseMessage();

                    final boolean accepted = responseCode != HttpURLConnection.HTTP_OK;

                    List<LoadRunnableInfo.Field> headers = new ArrayList<>();
                    headers.add(new LoadRunnableInfo.Field("Content-Type", connection.getContentType()));
                    headers.add(new LoadRunnableInfo.Field("Content-Length", String.valueOf(connection.getContentLength())));
                    headers.add(new LoadRunnableInfo.Field("Date", String.valueOf(connection.getDate())));

                    synchronized (loadListeners) {
                        LoadFileListener.ResponseStatus status = accepted ? LoadFileListener.ResponseStatus.ACCEPTED : LoadFileListener.ResponseStatus.DECLINED;
                        for (LoadFileListener<DownloadRunnableInfo> l : loadListeners) {
                            final int id = l.getId(rInfo);
                            if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                l.onResponseCode(status, rInfo, responseCode, responseMessage);
                                l.onResponseHeaders(status, rInfo, headers);
                            }
                        }
                    }

                    if (rInfo.settings.logResponseData) {
                        logger.debug("code: " + responseCode + ", message: " + responseMessage);
                        logger.debug("headers: " + headers);
                    }

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (accepted) {
                        logger.error("Server returned HTTP " + responseCode + " " + responseMessage);
                        lastException = new Throwable("Server returned HTTP " + responseCode + " " + responseMessage);
                        return;
                    }

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    contentLength = connection.getContentLength();

                    // download the file
                    input = connection.getInputStream();
                    output = new FileOutputStream(destFile);

                    byte data[] = new byte[BUF_SIZE];
                    int count;

                    long lastProcessingNotifyTime = 0;

                    while ((count = input.read(data)) != -1) {

                        if (rInfo.isCancelled()) {
//                            downloadCancelled = true;
                            return;
                        }

                        output.write(data, 0, count);

                        total += count;

                        synchronized (loadListeners) {
                            if (loadListeners.size() > 0) {
                                boolean notified = false;
                                for (LoadFileListener<DownloadRunnableInfo> l : loadListeners) {
                                    final long interval = System.currentTimeMillis() - lastProcessingNotifyTime;
                                    long targetInterval = l.getProcessingNotifyInterval(rInfo);
                                    targetInterval = targetInterval == LoadFileListener.INTERVAL_NOT_SPECIFIED? LoadFileListener.DEFAULT_PROCESSING_NOTIFY_INTERVAL : targetInterval;
                                    final int id = l.getId(rInfo);
                                    if (id == RunnableInfo.NO_ID || id == rInfo.id) {
                                        logger.debug("updating downloading state (processing)...");
                                        if (targetInterval > 0 && interval >= targetInterval || total >= contentLength) {
                                            l.onUpdateState(STATE.PROCESSING, rInfo, System.currentTimeMillis() - startDownloadTime, (float) total / 1024f,
                                                    (float) contentLength / 1024, null);
                                            notified = true;
                                        }
                                    }
                                }
                                if (notified)
                                    lastProcessingNotifyTime = System.currentTimeMillis();
                            }
                        }
                    }

                    downloadSuccess = true;

                } catch (Exception e) {
                    logger.error("an Exception occurred", lastException = e);
                    e.printStackTrace();
                    retriesCount++;

                } finally {
                    logger.debug("finally");

                    try {
                        if (input != null)
                            input.close();

                        if (output != null)
                            output.close();

                    } catch (IOException e) {
                        logger.error("an IOException occurred during close()", e);
                    }

                    FileHelper.releaseLockNoThrow(lock);

                    if (connection != null)
                        connection.disconnect();

                    if (downloadSuccess) {
                        logger.info("download " + rInfo + " success");
                        notifyStateChanged(STATE.SUCCESS, rInfo, System.currentTimeMillis() - startDownloadTime, (float) total / 1024f, ((float) destFile.length() / 1024f), null);

                    } else {

                        deleteUnfinishedFile(rInfo.deleteUnfinishedFile, destFile);

                        if (!rInfo.isCancelled()) {
                            logger.error("download " + rInfo + " failed, retries left: " + (rInfo.settings.retryLimit - retriesCount));
                            if (retriesCount == rInfo.settings.retryLimit) {
                                notifyStateChanged(STATE.FAILED, rInfo, System.currentTimeMillis() - startDownloadTime, (float) total / 1024f, ((float) contentLength / 1024f), lastException = new Throwable("download with id " + rInfo.id + " failed, retries left: " + (rInfo.settings.retryLimit - retriesCount), lastException));

                                if (rInfo.settings.retryDelay > 0) {
                                    try {
                                        Thread.sleep(rInfo.settings.retryDelay);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        Thread.currentThread().interrupt();

                                        logger.warn("thread interrupted, cancelling upload...");
                                        rInfo.cancel();
                                        logger.error("upload " + rInfo + " cancelled");
                                        notifyStateChanged(STATE.CANCELLED, rInfo, System.currentTimeMillis() - startDownloadTime, (float) total / 1024f,
                                                contentLength > 0 ? (float) contentLength / 1024f : 0, lastException = new Throwable("download with id " + rInfo.id + " was cancelled"));
                                    }
                                }

                            } else {
                                notifyStateChanged(STATE.FAILED_RETRIES_EXCEEDED, rInfo, System.currentTimeMillis() - startDownloadTime, (float) total / 1024f, (float) contentLength / 1024f, null);
                            }
                        } else {
                            logger.error("download " + rInfo + " cancelled");
                            notifyStateChanged(STATE.CANCELLED, rInfo, System.currentTimeMillis() - startDownloadTime, (float) total / 1024f,
                                    contentLength > 0 ? (float) contentLength / 1024f : 0, lastException = new Throwable("download with id " + rInfo.id + " was cancelled"));
                        }
                    }
                }
            }

        }

    }

}
