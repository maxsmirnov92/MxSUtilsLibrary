package net.maxsmr.networkutils.loadutil.managers;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.networkutils.NetworkHelper;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;

import static net.maxsmr.commonutils.data.MathUtils.safeLongToInt;

public class FtpConnectionManager {

    private final static Logger logger = LoggerFactory.getLogger(FtpConnectionManager.class);

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
        logger.debug("setAddress(), newAddr=" + newAddr);

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
        logger.debug("setPort(), newPort=" + newPort);

        if (!(newPort == DEFAULT_FTP_PORT || newPort == DEFAULT_SFTP_PORT)) {
            logger.error("incorrect port: " + newPort);
            return false;
        }

        this.ftpPort = newPort;
        return true;
    }

    public boolean setUserAndPassword(String user, String password) {
        logger.debug("setUserAndPassword(), user=" + user + ", password=" + password);

        if (user == null || password == null) {
            return false;
        }

        if (user.isEmpty()) {
            user = "anonymous";
        }

        if (!CompareUtils.stringsEqual(this.user, user, false)
                || !CompareUtils.stringsEqual(this.password, password, false)) {

            this.user = user;
            this.password = password;
        }

        return true;
    }

    public synchronized boolean isConnected() {
        return (ftpClient != null && ftpClient.isConnected());
    }

    public synchronized boolean disconnect() {
        logger.debug("disconnect()");

        if (!isConnected()) {
            logger.error("can't disconnect: null not connected");
            return false;
        }

        try {

            long startDisconnectTime = System.currentTimeMillis();

            ftpClient.logout();
            ftpClient.disconnect();

            lastDisconnectTime = System.currentTimeMillis() - startDisconnectTime;

            if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient = null;
                logger.error("disconnect success, time: " + lastDisconnectTime);
                return true;
            }

        } catch (Exception e) {
            logger.error("an Exception occurred", e);

        }

        logger.error("disconnect failed");

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.CLOSE_CONNECTION, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }
        ftpClient = null;
        return false;
    }

    public synchronized boolean connect(boolean needToLogin, int connectionTimeoutMs, int soTimeoutMs, int dataTimeoutMs,
                                        int controlKeepAliveTimeout, int retryCount, long retryDelay) {
        logger.debug("connect(), needToLogin=" + needToLogin);


        if (isConnected()) {
            logger.debug("already connected, need to disconnect first...");
            if (!disconnect()) {
                return false;
            }
        }

        if (ftpInetAddress == null) {
            logger.error("ftpInetAddress is null");
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

                logger.error("connect failed: FTP server refused connection");


                if (listener != null) {
                    listener.onFtpError(FtpAction.ESTABLISH_CONNECTION, ftpClient.getReplyCode(), ftpClient.getReplyString());
                }

                if (retryCount > 0) {
                    logger.warn("retrying (retries left " + retryCount + ")...");
                    disconnect();
                    if (retryDelay > 0) {
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException e) {
                            logger.error("an InterruptedException occurred during sleep", e);
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
                logger.error("can't login by user " + user + " and password " + password);

                if (listener != null) {
                    listener.onFtpError(FtpAction.LOGIN, ftpClient.getReplyCode(), ftpClient.getReplyString());
                }

                return false;
            }

            ftpClient.setSoTimeout(soTimeoutMs);
            ftpClient.setDataTimeout(dataTimeoutMs);
            ftpClient.setControlKeepAliveTimeout(controlKeepAliveTimeout);

            lastConnectTime = System.currentTimeMillis() - startLoginTime + connectTime;
            logger.info("connect success, time: " + lastConnectTime + " ms");

            return true;

        } catch (Exception e) {
            logger.error("an Exception occurred", e);
        }

        logger.error("connect failed");

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.ESTABLISH_CONNECTION, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        if (retryCount > 0) {
            logger.warn("retrying (retries left " + retryCount + ")...");
            disconnect();
            if (retryDelay > 0) {
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException e) {
                    logger.error("an InterruptedException occurred during sleep", e);
                    Thread.currentThread().interrupt();
                }
            }
            return connect(needToLogin, connectionTimeoutMs, soTimeoutMs, dataTimeoutMs, controlKeepAliveTimeout, --retryCount, retryDelay);
        }
        return false;
    }

    public synchronized boolean completePendingCommand() {
        logger.debug("completePendingCommand()");

        if (!isConnected()) {
            return false;
        }

        try {
            if (ftpClient.completePendingCommand()) { // HANGS IF STREAM IS NOT CLOSED
                logger.debug("success complete pending command!");
                return true;

            }
        } catch (Exception e) {
            logger.error("an Exception occurred during completePendingCommand()", e);
        }

        logger.debug("fail complete pending command!");
        return false;
    }

    @Nullable
    public synchronized Pair<InputStream, Long> retrieveFtpFileData(String workingDir, String fileName, FileType fileType) {
        logger.debug("retrieveFtpFileData(), workingDir=" + workingDir + ", fileName=" + fileName + ", fileType=" + fileType);

        if (TextUtils.isEmpty(workingDir) || TextUtils.isEmpty(fileName)) {
            logger.error("incorrect remote working directory name or remote file name");
            return null;
        }

        if (!isConnected()) {
            logger.error("ftpClient not connected");
            return null;
        }

        try {

            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!CompareUtils.stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.error("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return null;
                }
                logger.debug("working directory changed to: " + workingDir);
            }

            if (fileType == FileType.TEXT) {
                ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
            } else if (fileType == FileType.BINARY) {
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            }
            logger.debug("set file type: " + fileType.getValue());

            final String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

            FTPFile[] ftpFiles = ftpClient.listFiles(encodedFileName);

            if (ftpFiles == null || ftpFiles.length == 0) {
                logger.error("no such file: " + fileName);
                return null;
            }

            for (FTPFile ftpFile : ftpFiles) {

                if (ftpFile.isFile() && ftpFile.getSize() > 0 && CompareUtils.stringsEqual(ftpFile.getName(), encodedFileName, false)) {

                    logger.debug("starting retrieving stream...");

                    InputStream inStream = ftpClient.retrieveFileStream(encodedFileName);

                    if (inStream != null) {
                        logger.debug("retrieve stream from file success!");
                        return new Pair<>(inStream, ftpFile.getSize());
                    } else {
                        completePendingCommand();
                    }
                }
            }

        } catch (Exception e) {
            logger.error("an Exception occurred", e);
        }

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.RETRIEVE_DATA, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        logger.error("retrieve stream file " + fileName + " from working directory " + workingDir + " failed");
        return null;
    }


    @SuppressWarnings("ConstantConditions")
    @Nullable
    public synchronized File downloadFtpFile(String localWorkingDir, String workingDir, String fileName, FileType fileType,
                                             boolean deleteOnSuccess, boolean withRestart, final FileHelper.IStreamNotifier notifier) {
        logger.debug("downloadFtpFileWithRestart(), localWorkingDir=" + localWorkingDir + ", workingDir=" + workingDir + ", fileName=" + fileName
                + ", fileType=" + fileType + ", deleteOnSuccess=" + deleteOnSuccess);

        if (!FileHelper.checkDirNoThrow(localWorkingDir)) {
            logger.error("incorrect local working directory: " + localWorkingDir);
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
                        logger.debug("setting restart offset: " + localFileSize + "...");
                        ftpClient.setRestartOffset(localFileSize);
                    } else {
                        if (localFileSize > 0) {
                            randomAccessLocalFile.setLength(0);
                        }
                        randomAccessLocalFile.seek(0);
                    }

                    logger.debug("localFileSize=" + localFileSize + " bytes / ftpFileSize=" + ftpFileSize + " bytes");
                    logger.debug("starting retrieving file...");

                    final long startDownloadTime = System.currentTimeMillis();

                    ByteArrayOutputStream dataStream = new ByteArrayOutputStream();

                    if (FileHelper.revectorStream(inStream.first, dataStream, notifier != null? new FileHelper.IStreamNotifier() {
                        @Override
                        public long notifyInterval() {
                            return notifier.notifyInterval();
                        }

                        @Override
                        public boolean onProcessing(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, long bytesWrite, long bytesLeft) {
                            return notifier.onProcessing(inputStream, outputStream, bytesWrite, inStream.second != null && inStream.second > bytesWrite? inStream.second - bytesWrite : bytesLeft);
                        }
                    } : null)) {

                        randomAccessLocalFile.write(dataStream.toByteArray(), safeLongToInt(randomAccessLocalFile.length()),
                                dataStream.size());

                        lastDownloadTime = System.currentTimeMillis() - startDownloadTime;
                        logger.info("retrieve success, time: " + lastDownloadTime + " ms");

                        if (deleteOnSuccess) {

                            boolean deleteResult = false;

                            logger.debug("deleting remote file " + fileName + "...");
                            try {
                                deleteResult = ftpClient.deleteFile(fileName);
                            } catch (IOException e) {
                                logger.error("an IOException occurred during delete()", e);
                            }

                            if (!deleteResult) {
                                logger.error("cannot delete file " + fileName + " in working dir " + workingDir);

                                if (listener != null && ftpClient != null) {
                                    listener.onFtpError(FtpAction.DELETE, ftpClient.getReplyCode(), ftpClient.getReplyString());
                                }
                            }
                        }

                        return localFile;
                    }
                }


            } catch (IOException e) {
                logger.error("an IOException occurred", e);

            } finally {

                try {

                    if (randomAccessLocalFile != null) {
                        randomAccessLocalFile.close();
                    }

                    inStream.first.close();

                } catch (IOException e) {
                    logger.error("an IOException occurred during close()", e);
                }

                completePendingCommand();
            }
        }

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.RETRIEVE_DATA, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        logger.error("retrieve file " + fileName + " from working directory " + workingDir + " to directory " + localWorkingDir + " failed");
        return null;
    }

    public synchronized boolean uploadLocalFile(String workingDir, String fileName, File localFile, FileType fileType, boolean deleteOnSuccess, @NonNull WriteMode writeMode, final FileHelper.IStreamNotifier notifier) {
        logger.debug("uploadLocalFile(), workingDir=" + workingDir + ", fileName=" + fileName + ", localFile=" + localFile + ", fileType=" + fileType + ", deleteOnSuccess=" + deleteOnSuccess + ", notifier=" + notifier);

        if (workingDir == null || workingDir.length() == 0 || fileName == null || fileName.length() == 0) {
            logger.error("incorrect remote working directory name or remote file name");
            return false;
        }

        if (!FileHelper.isFileCorrect(localFile)) {
            logger.error("incorrect local file: " + localFile);
            return false;
        }

        if (!isConnected()) {
            logger.error("ftpClient is not connected");
            return false;
        }

        boolean exists = isFtpFileExists(workingDir, fileName);

        FileInputStream localStream = null;

        try {
            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!CompareUtils.stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.error("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return false;
                }
                logger.debug("working directory changed to: " + workingDir);
            }

            if (exists) {

                if (writeMode == WriteMode.REWRITE) {

                    boolean deleteResult = false;

                    logger.debug("deleting remote file " + fileName + "...");
                    try {
                        deleteResult = ftpClient.deleteFile(fileName);
                    } catch (IOException e) {
                        logger.error("an IOException occurred during delete()", e);
                    }

                    if (!deleteResult) {
                        logger.error("cannot delete file " + fileName + " in working dir " + workingDir);

                        if (listener != null && ftpClient != null) {
                            listener.onFtpError(FtpAction.DELETE, ftpClient.getReplyCode(), ftpClient.getReplyString());
                        }

                        return false;
                    }


                } else if (writeMode == WriteMode.DO_NOTING) {
                    logger.error("file " + fileName + " already exists in working dir " + workingDir);
                    return  false;
                }
            }


                if (fileType == FileType.TEXT) {
                    ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
                } else if (fileType == FileType.BINARY) {
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                }
                logger.debug("set file type: " + fileType);

                final String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

                localStream = new FileInputStream(localFile);

                ftpClient.setBufferSize(BUFFER_SIZE);

                logger.debug("starting storing file...");

                final long startUploadTime = System.currentTimeMillis();

                final OutputStream outputStream;

                if (!exists || writeMode != WriteMode.APPEND) {
                    outputStream = ftpClient.storeFileStream(encodedFileName);
                } else {
                    outputStream = ftpClient.appendFileStream(encodedFileName);
                }

                final long localFileSize = localFile.length();

                if (FileHelper.revectorStream(localStream, outputStream, notifier != null? new FileHelper.IStreamNotifier() {

                    @Override
                    public long notifyInterval() {
                        return notifier.notifyInterval();
                    }

                    @Override
                    public boolean onProcessing(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, long bytesWrite, long bytesLeft) {
                        return notifier.onProcessing(inputStream, outputStream, bytesWrite, localFileSize > bytesWrite? localFileSize - bytesWrite : bytesLeft);
                    }
                } : null)) {

                    if (deleteOnSuccess) {
                        if (!FileHelper.deleteFile(localFile)) {
                            logger.error("cannot delete local file " + localFile);
                        }
                    }

                    lastUploadTime = System.currentTimeMillis() - startUploadTime;
                    logger.info("storing success, time: " + lastUploadTime + " ms");

                    return true;
                }

        } catch (Exception e) {
            logger.error("an Exception occurred", e);

        } finally {

            if (localStream != null) {
                try {
                    localStream.close();
                } catch (IOException e) {
                    logger.error("an IOException occurred during close(): " + e.getMessage());

                }
            }

            completePendingCommand();
        }

        if (listener != null && ftpClient != null) {
            listener.onFtpError(FtpAction.SEND_DATA, ftpClient.getReplyCode(), ftpClient.getReplyString());
        }

        logger.error("storing local file " + localFile + " failed, new name: " + fileName + ", working directory: " + workingDir);
        return false;
    }

    public synchronized boolean isFtpFileExists(String workingDir, String fileName) {
        logger.debug("isFtpFileExists(), workingDir=" + workingDir + ", fileName=" + fileName);

        if (TextUtils.isEmpty(workingDir) || TextUtils.isEmpty(fileName)) {
            logger.error("incorrect remote working directory name or remote file name");
            return false;
        }

        if (!isConnected()) {
            logger.error("ftpClient not connected");
            return false;
        }

        try {

            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!CompareUtils.stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.error("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return false;
                }
                logger.debug("working directory changed to: " + workingDir);
            }

            final String encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");

            FTPFile[] ftpFiles = ftpClient.listFiles(encodedFileName);

            if (ftpFiles != null && ftpFiles.length > 0) {
                for (FTPFile ftpFile : ftpFiles) {

                    if (ftpFile.isFile()) {

                        logger.debug("current ftp file: " + ftpFile.getName());

                        final boolean equals = CompareUtils.stringsEqual(ftpFile.getName(), encodedFileName, false);
                        logger.info("equals " + encodedFileName + ": " + equals);

                        if (equals) {
                            logger.info("ftp file exists");
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("an Exception occurred", e);

            if (listener != null && ftpClient != null) {
                listener.onFtpError(FtpAction.CHECK_FILE, ftpClient.getReplyCode(), ftpClient.getReplyString());
            }

            logger.error("check file " + fileName + " in working directory " + workingDir + " failed");
        }

        return false;
    }

    public synchronized boolean renameFtpFile(String workingDir, String srcFileName, String dstFileName, boolean overwriteExisting) {
        logger.debug("renameFtpFile(), workingDir=" + workingDir + ", srcFileName=" + srcFileName + ", dstFileName=" + dstFileName,
                ", overwriteExisting=" + overwriteExisting);


        if (workingDir == null || workingDir.length() == 0 || srcFileName == null || srcFileName.length() == 0 || dstFileName == null
                || dstFileName.length() == 0) {
            logger.error("incorrect remote working directory name or remote source/destination file name");
            return false;
        }

        if (!isConnected()) {
            logger.error("ftpClient not connected");
            return false;
        }

        try {

            final String encodedWorkingDir = new String(workingDir.getBytes("UTF-8"), "ISO-8859-1");
            final String currentWorkingDir = ftpClient.printWorkingDirectory();

            if (!CompareUtils.stringsEqual(currentWorkingDir, encodedWorkingDir, false)) {
                if (!ftpClient.changeWorkingDirectory(encodedWorkingDir)) {
                    logger.error("can't change working dir");

                    if (listener != null && ftpClient != null) {
                        listener.onFtpError(FtpAction.CHANGE_WORKING_DIR, ftpClient.getReplyCode(), ftpClient.getReplyString());
                    }

                    return false;
                }
                logger.debug("working directory changed to: " + workingDir);
            }

            final String encodedDstFileName = new String(dstFileName.getBytes("UTF-8"), "ISO-8859-1");

            String[] dstFtpFileNames = ftpClient.listNames(encodedDstFileName);

            boolean proceed = true;

            if (dstFtpFileNames != null && dstFtpFileNames.length > 0) {
                for (String ftpFileName : dstFtpFileNames) {
                    if (!TextUtils.isEmpty(ftpFileName)) {
                        if (ftpFileName.equalsIgnoreCase(encodedDstFileName)) {
                            logger.warn("ftp file with name " + encodedDstFileName + " already exists");
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

                        logger.debug("current ftp file: " + ftpFile.getName());

                        final boolean equals = CompareUtils.stringsEqual(ftpFile.getName(), encodedSrcFileName, false);
                        logger.info("equals " + encodedSrcFileName + ": " + equals);

                        if (equals) {
                            logger.info("renaming to " + encodedDstFileName + "...");
                            if (ftpClient.rename(encodedSrcFileName, encodedDstFileName)) {
                                logger.debug("rename " + srcFileName + " to " + dstFileName + " was successful");
                                return true;
                            }
                        }
                    }
                }
            }


        } catch (Exception e) {
            logger.error("an Exception occurred", e);
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
