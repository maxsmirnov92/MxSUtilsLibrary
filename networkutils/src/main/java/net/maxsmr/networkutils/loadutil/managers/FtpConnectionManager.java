package net.maxsmr.networkutils.loadutil.managers;

import net.maxsmr.commonutils.data.Pair;


import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.StreamUtils;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.networkutils.NetworkHelper;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;

import static net.maxsmr.commonutils.data.CompareUtilsKt.stringsEqual;
import static net.maxsmr.commonutils.data.text.TextUtilsKt.isEmpty;
import static net.maxsmr.commonutils.data.conversion.NumberConversionKt.toIntSafe;

public class FtpConnectionManager {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(FtpConnectionManager.class);

    public final static int DEFAULT_FTP_PORT = 21;
    public final static int DEFAULT_SFTP_PORT = 22;

    private final static int BUFFER_SIZE = 1024 * 1024;

    @Nullable
    private FtpConnectionManagerListener listener;

    @Nullable
    private CopyStreamListener copyStreamListener;

    private FTPClient ftpClient;

    private InetAddress ftpInetAddress;

    private int ftpPort;

    private long lastConnectTime;

    private long lastDisconnectTime;

    private long lastDownloadTime;

    private long lastUploadTime;

    private String user = "anonymous";

    private String password = "";

    public long getLastConnectTime() {
        return lastConnectTime;
    }

    public long getLastDisconnectTime() {
        return lastDisconnectTime;
    }

    public long getLastUploadTime() {
        return lastUploadTime;
    }

    public long getLastDownloadTime() {
        return lastDownloadTime;
    }

    public void setFtpConnectionManagerListener(@Nullable FtpConnectionManagerListener listener) {
        this.listener = listener;
    }

    public void setCopyStreamListener(@Nullable CopyStreamListener copyStreamListener) {
        this.copyStreamListener = copyStreamListener;
    }

    public String getAddress() {
        if (ftpInetAddress == null) {
            return null;
        }
        return ftpInetAddress.getHostAddress();
    }

    public String getDomain() {
        if (ftpInetAddress == null) {
            return null;
        }
        return ftpInetAddress.getHostName();
    }

    public boolean setAddress(String newAddr) {
        logger.d("setAddress(), newAddr=" + newAddr);

        InetAddress tmpFtpInetAddress = NetworkHelper.getInetAddressByDomain(newAddr);

        if (tmpFtpInetAddress == null) {
            tmpFtpInetAddress = NetworkHelper.getInetAddressByIp(newAddr);
            if (tmpFtpInetAddress == null) {
                return false;
            }
        }

        ftpInetAddress = tmpFtpInetAddress;
        return true;
    }

    public boolean setPort(int newPort) {
        logger.d("setPort(), newPort=" + newPort);

        if (!(newPort == DEFAULT_FTP_PORT || newPort == DEFAULT_SFTP_PORT)) {
            logger.e("incorrect port: " + newPort);
            return false;
        }

        this.ftpPort = newPort;
        return true;
    }

    public boolean setUserAndPassword(String user, String password) {
        logger.d("setUserAndPassword(), user=" + user + ", password=" + password);

        if (user == null || password == null) {
            return false;
        }

        if (user.isEmpty()) {
            user = "anonymous";
        }

        if (!stringsEqual(this.user, user, false)
                || !stringsEqual(this.password, password, false)) {

            this.user = user;
            this.password = password;
        }

        return true;
    }

    public synchronized boolean isConnected() {
        return (ftpClient != null && ftpClient.isConnected());
    }

    public synchronized boolean disconnect() {
        logger.d("disconnect()");

        if (!isConnected()) {
            logger.e("can't disconnect: null not connected");
            return false;
        }

        try {

            long startDisconnectTime = System.currentTimeMillis();

            ftpClient.logout();
            ftpClient.disconnect();

            lastDisconnectTime = System.currentTimeMillis() - startDisconnectTime;

            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient = null;
                logger.e("disconnect success, time: " + lastDisconnectTime);
                return true;
            }

        } catch (Exception e) {
            logger.e("an Exception occurred", e);

        }

        logger.e("disconnect failed");

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.CLOSE_CONNECTION, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }
        ftpClient = null;
        return false;
    }

    public synchronized boolean connect(boolean needToLogin, int connectionTimeoutMs, int soTimeoutMs, int dataTimeoutMs,
                                        int controlKeepAliveTimeout, int retryCount, long retryDelay) {
        logger.d("connect(), needToLogin=" + needToLogin);


        if (isConnected()) {
            logger.d("already connected, need to disconnect first...");
            if (!disconnect()) {
                return false;
            }
        }

        if (ftpInetAddress == null) {
            logger.e("ftpInetAddress is null");
            return false;
        }

        if (needToLogin && (user == null || password == null)) {
            return false;
        }

        if (soTimeoutMs < 0) {
            soTimeoutMs = 0;
        }

        if (dataTimeoutMs < 0) {
            dataTimeoutMs = 0;
        }

        if (connectionTimeoutMs < 0) {
            connectionTimeoutMs = 0;
        }

        if (controlKeepAliveTimeout < 0) {
            controlKeepAliveTimeout = 0;
        }

        if (retryCount < 0) {
            retryCount = 0;
        }

        ftpClient = new FTPClient();
        if (copyStreamListener != null) {
            ftpClient.setCopyStreamListener(copyStreamListener);
        }

        try {

            ftpClient.setConnectTimeout(connectionTimeoutMs);

            long startConnectTime = System.currentTimeMillis();

            if (ftpPort == 0) {
                ftpClient.connect(ftpInetAddress);
            } else {
                ftpClient.connect(ftpInetAddress, ftpPort);
            }

            long connectTime = System.currentTimeMillis() - startConnectTime;

            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {

                logger.e("connect failed: FTP server refused connection");


                if (listener != null) {
                    listener.onFtpError(FtpAction.ESTABLISH_CONNECTION, ftpClient.getReplyCode(), ftpClient.getReplyString());
                }

                if (retryCount > 0) {
                    logger.w("retrying (retries left " + retryCount + ")...");
                    disconnect();
                    if (retryDelay > 0) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException e) {
                            logger.e("an InterruptedException occurred during sleep", e);
                            Thread.currentThread().interrupt();
                        }
                    }
                    return connect(needToLogin, connectionTimeoutMs, soTimeoutMs, dataTimeoutMs, controlKeepAliveTimeout, --retryCount, retryDelay);
                }
                return false;
            }

            ftpClient.enterLocalPassiveMode();

            long startLoginTime = System.currentTimeMillis();

            if (needToLogin && !ftpClient.login(user, password)) {
                logger.e("can't login by user " + user + " and password " + password);

                if (listener != null) {
                    listener.onFtpError(FtpAction.LOGIN, ftpClient.getReplyCode(), ftpClient.getReplyString());
                }

                return false;
            }

            ftpClient.setSoTimeout(soTimeoutMs);
            ftpClient.setDataTimeout(dataTimeoutMs);
            ftpClient.setControlKeepAliveTimeout(controlKeepAliveTimeout);

            lastConnectTime = System.currentTimeMillis() - startLoginTime + connectTime;
            logger.i("connect success, time: " + lastConnectTime + " ms");

            return true;

        } catch (Exception e) {
            logger.e("an Exception occurred", e);
        }

        logger.e("connect failed");

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.ESTABLISH_CONNECTION, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        if (retryCount > 0) {
            logger.w("retrying (retries left " + retryCount + ")...");
            disconnect();
            if (retryDelay > 0) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    logger.e("an InterruptedException occurred during sleep", e);
                    Thread.currentThread().interrupt();
                }
            }
            return connect(needToLogin, connectionTimeoutMs, soTimeoutMs, dataTimeoutMs, controlKeepAliveTimeout, --retryCount, retryDelay);
        }
        return false;
    }

    public synchronized boolean completePendingCommand() {
        logger.d("completePendingCommand()");

        if (!isConnected()) {
            return false;
        }

        try {
            if (ftpClient.completePendingCommand()) { // HANGS IF STREAM IS NOT CLOSED
                logger.d("success complete pending command!");
                return true;

            }
        } catch (Exception e) {
            logger.e("an Exception occurred during completePendingCommand()", e);
        }

        logger.d("fail complete pending command!");
        return false;
    }

    @Nullable
    public synchronized Pair<InputStream, Long> retrieveFtpFileData(String workingDir, String fileName, FileType fileType) {
        logger.d("retrieveFtpFileData(), workingDir=" + workingDir + ", fileName=" + fileName + ", fileType=" + fileType);

        if (isEmpty(workingDir) || isEmpty(fileName)) {
            logger.e("incorrect remote working directory name or remote file name");
            return null;
        }

        if (!isConnected()) {
            logger.e("ftpClient not connected");
            return null;
        }

        try {

            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.e("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return null;
                }
                logger.d("working directory changed to: " + workingDir);
            }

            if (fileType == FileType.TEXT) {
                ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            } else if (fileType == FileType.BINARY) {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            }
            logger.d("set file type: " + fileType.getValue());

            final String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

            FTPFile[] ftpFiles = ftpClient.listFiles(encodedFileName);

            if (ftpFiles == null || ftpFiles.length == 0) {
                logger.e("no such file: " + fileName);
                return null;
            }

            for (FTPFile ftpFile : ftpFiles) {

                if (ftpFile.isFile() && ftpFile.getSize() > 0 && stringsEqual(ftpFile.getName(), encodedFileName, false)) {

                    logger.d("starting retrieving stream...");

                    InputStream inStream = ftpClient.retrieveFileStream(encodedFileName);

                    if (inStream != null) {
                        logger.d("retrieve stream from file success!");
                        return new Pair<>(inStream, ftpFile.getSize());
                    } else {
                        completePendingCommand();
                    }
                }
            }

        } catch (Exception e) {
            logger.e("an Exception occurred", e);
        }

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.RETRIEVE_DATA, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        logger.e("retrieve stream file " + fileName + " from working directory " + workingDir + " failed");
        return null;
    }


    @SuppressWarnings("ConstantConditions")
    @Nullable
    public synchronized File downloadFtpFile(String localWorkingDir, String workingDir, String fileName, FileType fileType,
                                             boolean deleteOnSuccess, boolean withRestart, final StreamUtils.IStreamNotifier notifier) {
        logger.d("downloadFtpFileWithRestart(), localWorkingDir=" + localWorkingDir + ", workingDir=" + workingDir + ", fileName=" + fileName
                + ", fileType=" + fileType + ", deleteOnSuccess=" + deleteOnSuccess);

        if (!FileHelper.checkDirNoThrow(localWorkingDir)) {
            logger.e("incorrect local working directory: " + localWorkingDir);
            return null;
        }

        final Pair<InputStream, Long> inStream = retrieveFtpFileData(workingDir, fileName, fileType);
        RandomAccessFile randomAccessLocalFile = null;

        if (inStream != null) {

            try {

                long ftpFileSize = inStream.second;

                File localFile = new File(localWorkingDir, fileName);

                if (FileHelper.checkFileNoThrow(localFile)) {

                    ftpClient.setBufferSize(BUFFER_SIZE);

                    randomAccessLocalFile = new RandomAccessFile(localFile, "rw");

                    final long localFileSize = randomAccessLocalFile.length();

                    if (withRestart && localFileSize > 0 && localFileSize < ftpFileSize) {
                        randomAccessLocalFile.seek(localFileSize);
                        logger.d("setting restart offset: " + localFileSize + "...");
                        ftpClient.setRestartOffset(localFileSize);
                    } else {
                        if (localFileSize > 0) {
                            randomAccessLocalFile.setLength(0);
                        }
                        randomAccessLocalFile.seek(0);
                    }

                    logger.d("localFileSize=" + localFileSize + " bytes / ftpFileSize=" + ftpFileSize + " bytes");
                    logger.d("starting retrieving file...");

                    final long startDownloadTime = System.currentTimeMillis();

                    ByteArrayOutputStream dataStream = new ByteArrayOutputStream();

                    if (StreamUtils.revectorStream(inStream.first, dataStream, notifier != null? new StreamUtils.IStreamNotifier() {
                        @Override
                        public long notifyInterval() {
                            return notifier.notifyInterval();
                        }

                        @Override
                        public boolean onProcessing(@NotNull InputStream inputStream, @NotNull OutputStream outputStream, long bytesWrite, long bytesLeft) {
                            return notifier.onProcessing(inputStream, outputStream, bytesWrite, inStream.second != null && inStream.second > bytesWrite? inStream.second - bytesWrite : bytesLeft);
                        }
                    } : null)) {

                        randomAccessLocalFile.write(dataStream.toByteArray(), toIntSafe(randomAccessLocalFile.length()),
                                dataStream.size());

                        lastDownloadTime = System.currentTimeMillis() - startDownloadTime;
                        logger.i("retrieve success, time: " + lastDownloadTime + " ms");

                        if (deleteOnSuccess) {

                            boolean deleteResult = false;

                            logger.d("deleting remote file " + fileName + "...");
                            try {
                                deleteResult = ftpClient.deleteFile(fileName);
                            } catch (IOException e) {
                                logger.e("an IOException occurred during delete()", e);
                            }

                            if (!deleteResult) {
                                logger.e("cannot delete file " + fileName + " in working dir " + workingDir);

                                if (listener != null && ftpClient != null) {
                                    listener.onFtpError(FtpAction.DELETE, ftpClient.getReplyCode(), ftpClient.getReplyString());
                                }
                            }
                        }

                        return localFile;
                    }
                }


            } catch (IOException e) {
                logger.e("an IOException occurred", e);

            } finally {

                try {

                    if (randomAccessLocalFile != null) {
                        randomAccessLocalFile.close();
                    }

                    inStream.first.close();

                } catch (IOException e) {
                    logger.e("an IOException occurred during close()", e);
                }

                completePendingCommand();
            }
        }

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.RETRIEVE_DATA, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        logger.e("retrieve file " + fileName + " from working directory " + workingDir + " to directory " + localWorkingDir + " failed");
        return null;
    }

    public synchronized boolean uploadLocalFile(String workingDir, String fileName, File localFile, FileType fileType, boolean deleteOnSuccess, @NotNull WriteMode writeMode, final StreamUtils.IStreamNotifier notifier) {
        logger.d("uploadLocalFile(), workingDir=" + workingDir + ", fileName=" + fileName + ", localFile=" + localFile + ", fileType=" + fileType + ", deleteOnSuccess=" + deleteOnSuccess + ", notifier=" + notifier);

        if (workingDir == null || workingDir.length() == 0 || fileName == null || fileName.length() == 0) {
            logger.e("incorrect remote working directory name or remote file name");
            return false;
        }

        if (!FileHelper.isFileValid(localFile)) {
            logger.e("incorrect local file: " + localFile);
            return false;
        }

        if (!isConnected()) {
            logger.e("ftpClient is not connected");
            return false;
        }

        boolean exists = isFtpFileExists(workingDir, fileName);

        FileInputStream localStream = null;

        try {
            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.e("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return false;
                }
                logger.d("working directory changed to: " + workingDir);
            }

            if (exists) {

                if (writeMode == WriteMode.REWRITE) {

                    boolean deleteResult = false;

                    logger.d("deleting remote file " + fileName + "...");
                    try {
                        deleteResult = ftpClient.deleteFile(fileName);
                    } catch (IOException e) {
                        logger.e("an IOException occurred during delete()", e);
                    }

                    if (!deleteResult) {
                        logger.e("cannot delete file " + fileName + " in working dir " + workingDir);

                        if (listener != null && ftpClient != null) {
                            listener.onFtpError(FtpAction.DELETE, ftpClient.getReplyCode(), ftpClient.getReplyString());
                        }

                        return false;
                    }


                } else if (writeMode == WriteMode.DO_NOTING) {
                    logger.e("file " + fileName + " already exists in working dir " + workingDir);
                    return  false;
                }
            }


                if (fileType == FileType.TEXT) {
                    ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
                } else if (fileType == FileType.BINARY) {
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                }
                logger.d("set file type: " + fileType);

                final String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

                localStream = new FileInputStream(localFile);

                ftpClient.setBufferSize(BUFFER_SIZE);

                logger.d("starting storing file...");

                final long startUploadTime = System.currentTimeMillis();

                final OutputStream outputStream;

                if (!exists || writeMode != WriteMode.APPEND) {
                    outputStream = ftpClient.storeFileStream(encodedFileName);
                } else {
                    outputStream = ftpClient.appendFileStream(encodedFileName);
                }

                final long localFileSize = localFile.length();

                if (StreamUtils.revectorStream(localStream, outputStream, notifier != null? new StreamUtils.IStreamNotifier() {

                    @Override
                    public long notifyInterval() {
                        return notifier.notifyInterval();
                    }

                    @Override
                    public boolean onProcessing(@NotNull InputStream inputStream, @NotNull OutputStream outputStream, long bytesWrite, long bytesLeft) {
                        return notifier.onProcessing(inputStream, outputStream, bytesWrite, localFileSize > bytesWrite? localFileSize - bytesWrite : bytesLeft);
                    }
                } : null)) {

                    if (deleteOnSuccess) {
                        if (!FileHelper.deleteFile(localFile)) {
                            logger.e("cannot delete local file " + localFile);
                        }
                    }

                    lastUploadTime = System.currentTimeMillis() - startUploadTime;
                    logger.i("storing success, time: " + lastUploadTime + " ms");

                    return true;
                }

        } catch (Exception e) {
            logger.e("an Exception occurred", e);

        } finally {

            if (localStream != null) {
                try {
                    localStream.close();
                } catch (IOException e) {
                    logger.e("an IOException occurred during close(): " + e.getMessage());

                }
            }

            completePendingCommand();
        }

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.SEND_DATA, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        logger.e("storing local file " + localFile + " failed, new name: " + fileName + ", working directory: " + workingDir);
        return false;
    }

    public synchronized boolean isFtpFileExists(String workingDir, String fileName) {
        logger.d("isFtpFileExists(), workingDir=" + workingDir + ", fileName=" + fileName);

        if (isEmpty(workingDir) || isEmpty(fileName)) {
            logger.e("incorrect remote working directory name or remote file name");
            return false;
        }

        if (!isConnected()) {
            logger.e("ftpClient not connected");
            return false;
        }

        try {

            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.e("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return false;
                }
                logger.d("working directory changed to: " + workingDir);
            }

            final String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

            FTPFile[] ftpFiles = ftpClient.listFiles(encodedFileName);

            if (ftpFiles != null && ftpFiles.length > 0) {
                for (FTPFile ftpFile : ftpFiles) {

                    if (ftpFile.isFile()) {

                        logger.d("current ftp file: " + ftpFile.getName());

                        final boolean equals = stringsEqual(ftpFile.getName(), encodedFileName, false);
                        logger.i("equals " + encodedFileName + ": " + equals);

                        if (equals) {
                            logger.i("ftp file exists");
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.e("an Exception occurred", e);

            if (listener != null && ftpClient != null) {
                listener.onFtpError(FtpAction.CHECK_FILE, ftpClient.getReplyCode(), ftpClient.getReplyString());
            }

            logger.e("check file " + fileName + " in working directory " + workingDir + " failed");
        }

        return false;
    }

    public synchronized boolean renameFtpFile(String workingDir, String srcFileName, String dstFileName, boolean overwriteExisting) {
        logger.d("renameFtpFile(), workingDir=" + workingDir + ", srcFileName=" + srcFileName + ", dstFileName=" + dstFileName + ", overwriteExisting=" + overwriteExisting);

        if (workingDir == null || workingDir.length() == 0 || srcFileName == null || srcFileName.length() == 0 || dstFileName == null
                || dstFileName.length() == 0) {
            logger.e("incorrect remote working directory name or remote source/destination file name");
            return false;
        }

        if (!isConnected()) {
            logger.e("ftpClient not connected");
            return false;
        }

        try {

            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.e("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return false;
                }
                logger.d("working directory changed to: " + workingDir);
            }

            final String encodedDstFileName = new String(dstFileName.getBytes("UTF-8"), "ISO-8859-1");

            String[] dstFtpFileNames = ftpClient.listNames(encodedDstFileName);

            boolean proceed = true;

            if (dstFtpFileNames != null && dstFtpFileNames.length > 0) {
                for (String ftpFileName : dstFtpFileNames) {
                    if (!isEmpty(ftpFileName)) {
                        if (ftpFileName.equalsIgnoreCase(encodedDstFileName)) {
                            logger.w("ftp file with name " + encodedDstFileName + " already exists");
                            if (!overwriteExisting) {
                                proceed = false;
                                break;
                            }
                        }
                    }
                }
            }

            if (proceed) {

                final String encodedSrcFileName = new String(srcFileName.getBytes("UTF-8"), "ISO-8859-1");

                FTPFile[] ftpFiles = ftpClient.listFiles(encodedSrcFileName);

                if (ftpFiles != null && ftpFiles.length > 0) {
                    for (FTPFile ftpFile : ftpFiles) {

                        logger.d("current ftp file: " + ftpFile.getName());

                        final boolean equals = stringsEqual(ftpFile.getName(), encodedSrcFileName, false);
                        logger.i("equals " + encodedSrcFileName + ": " + equals);

                        if (equals) {
                            logger.i("renaming to " + encodedDstFileName + "...");
                            if (ftpClient.rename(encodedSrcFileName, encodedDstFileName)) {
                                logger.d("rename " + srcFileName + " to " + dstFileName + " was successful");
                                return true;
                            }
                        }
                    }
                }
            }


        } catch (Exception e) {
            logger.e("an Exception occurred", e);
        }

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.RENAME_FILE, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }
        return false;
    }

    public interface FtpConnectionManagerListener {

        void onFtpError(FtpAction action, int replyCode, String replyMessage);
    }

    public enum FtpAction {

        ESTABLISH_CONNECTION, LOGIN, CLOSE_CONNECTION, CHANGE_WORKING_DIR, CHECK_FILE, RENAME_FILE, RETRIEVE_DATA, SEND_DATA, DELETE
    }

    public enum FileType {

        TEXT {
            @Override
            public int getValue() {
                return 0;
            }
        },
        BINARY {
            @Override
            public int getValue() {
                return 1;
            }
        };

        public int getValue() {
            return -1;
        }
    }

    public enum WriteMode {

        REWRITE, APPEND, DO_NOTING
    }
}
