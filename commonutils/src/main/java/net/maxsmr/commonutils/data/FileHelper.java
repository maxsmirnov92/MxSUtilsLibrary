package net.maxsmr.commonutils.data;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.text.TextUtils;

import net.maxsmr.commonutils.R;
import net.maxsmr.commonutils.data.sort.AbsOptionableComparator;
import net.maxsmr.commonutils.data.sort.ISortOption;
import net.maxsmr.commonutils.shell.ShellUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static net.maxsmr.commonutils.shell.ShellUtils.execProcess;

public final class FileHelper {

    private final static Logger logger = LoggerFactory.getLogger(FileHelper.class);

    public final static int DEPTH_UNLIMITED = -1;

    public FileHelper() {
        throw new AssertionError("no instances.");
    }

    public static float getPartitionTotalSpace(String path, @NonNull SizeUnit unit) {
        return isDirExists(path) ? SizeUnit.convert(new File(path).getTotalSpace(), SizeUnit.BYTES, unit) : 0L;
    }

    public static float getPartitionFreeSpace(String path, @NonNull SizeUnit unit) {
        return isDirExists(path) ? SizeUnit.convert(new File(path).getFreeSpace(), SizeUnit.BYTES, unit) : 0L;
    }

    public static boolean isSizeCorrect(File file) {
        return (file != null && file.length() > 0);
    }

    public static boolean isFileCorrect(File file) {
        return (file != null && file.isFile() && isSizeCorrect(file));
    }

    public static boolean isFileExists(String fileName, String parentPath) {

        if (TextUtils.isEmpty(fileName) || fileName.contains("/")) {
            return false;
        }

        if (TextUtils.isEmpty(parentPath)) {
            return false;
        }

        File f = new File(parentPath, fileName);
        return f.exists() && f.isFile();
    }

    public static boolean isFileExists(File file) {
        return file != null && isFileExists(file.getAbsolutePath());
    }

    public static boolean isFileExists(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            File f = new File(filePath);
            return (f.exists() && f.isFile());
        }
        return false;
    }

    @Nullable
    public static FileLock lockFileChannel(File f, boolean blocking) {

        if (f == null || !f.isFile() || !f.exists()) {
            logger.error("incorrect file: " + f);
            return null;
        }

        RandomAccessFile randomAccFile = null;
        FileChannel channel = null;

        try {
            randomAccFile = new RandomAccessFile(f, "rw");
            channel = randomAccFile.getChannel();

            try {
                return !blocking ? channel.tryLock() : channel.lock();

            } catch (IOException e) {
                logger.error("an IOException occurred during tryLock()", e);
            } catch (OverlappingFileLockException e) {
                logger.error("an OverlappingFileLockException occurred during tryLock()", e);
            }

        } catch (FileNotFoundException e) {
            logger.error("a FileNotFoundException occurred during new RandomAccessFile()", e);

        } finally {
            try {
                if (channel != null)
                    channel.close();
                if (randomAccFile != null)
                    randomAccFile.close();
            } catch (IOException e) {
                logger.error("an IOException occurred during close()", e);
            }
        }

        return null;
    }

    public static boolean releaseLockNoThrow(@Nullable FileLock lock) {
        try {
            if (lock != null) {
                lock.release();
                return true;
            }
        } catch (IOException e) {
            logger.error("an IOException occurred during release()", e);
        }
        return false;
    }

    public static boolean isFileLocked(File f) {
        final FileLock l = lockFileChannel(f, false);
        try {
            return l == null;
        } finally {
            releaseLockNoThrow(l);
        }
    }

    public static boolean isDirExists(String dirPath) {

        if (dirPath == null || dirPath.length() == 0) {
            return false;
        }

        File dir = new File(dirPath);

        return (dir.exists() && dir.isDirectory());
    }

    public static boolean checkFileNoThrow(@Nullable File file) {
        return file != null && (file.exists() && file.isFile() || createNewFile(file.getName(), file.getParent()) != null);
    }

    public static void checkFile(@Nullable File file) {
        if (!checkFileNoThrow(file)) {
            throw new IllegalArgumentException("incorrect file: " + file);
        }
    }

    public static boolean checkFileNoThrow(@Nullable String file) {
        return !TextUtils.isEmpty(file) && checkFileNoThrow(new File(file));
    }

    public static void checkFile(@Nullable String file) {
        if (!checkFileNoThrow(file)) {
            throw new IllegalArgumentException("incorrect file: " + file);
        }
    }

    public static boolean checkDirNoThrow(@Nullable String dirPath) {
        if (!isDirExists(dirPath)) {
            if (createNewDir(dirPath) == null) {
                return false;
            }
        }
        return true;
    }

    public static void checkDir(@Nullable String dirPath) {
        if (!checkDirNoThrow(dirPath)) {
            throw new IllegalArgumentException("incorrect directory path: " + dirPath);
        }
    }

    public static File checkPathNoThrow(String parent, String fileName) {
        if (checkDirNoThrow(parent)) {
            if (!TextUtils.isEmpty(fileName)) {
                File f = new File(parent, fileName);
                if (checkFileNoThrow(f)) {
                    return f;
                }
            }
        }
        return null;
    }

    public static File checkPath(String parent, String fileName) {
        File f = checkPathNoThrow(parent, fileName);
        if (f == null) {
            throw new IllegalArgumentException("incorrect path: " + parent + File.separator + fileName);
        }
        return f;
    }

    @Nullable
    private static File createFile(String fileName, String parentPath, boolean recreate) {
        final File file;

        if (recreate) {
            file = createNewFile(fileName, parentPath);
        } else {
            if (!isFileExists(fileName, parentPath))
                file = createNewFile(fileName, parentPath);
            else
                file = new File(parentPath, fileName);
        }

        if (file == null) {
            logger.error("can't create file: " + parentPath + File.separator + fileName);
        }

        return file;
    }

    @Nullable
    public static File createNewDir(String dirPath) {

        if (TextUtils.isEmpty(dirPath)) {
            logger.error("path is empty");
            return null;
        }

        File dir = new File(dirPath);

        if (dir.isDirectory() && dir.exists())
            return dir;

        if (dir.mkdirs())
            return dir;

        return null;
    }

    public static File createNewFile(String fileName, String parentPath) {
        return createNewFile(fileName, parentPath, true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Nullable
    public static File createNewFile(String fileName, String parentPath, boolean recreate) {

        if (TextUtils.isEmpty(fileName) || fileName.contains(File.separator)) {
            return null;
        }

        if (TextUtils.isEmpty(parentPath)) {
            return null;
        }

        File newFile = null;

        File parentDir = new File(parentPath);

        boolean created = false;
        try {
            created = parentDir.mkdirs();
        } catch (SecurityException e) {
            logger.error("exception occurred: " + e);
        }

        if (created || parentDir.exists() && parentDir.isDirectory()) {

            newFile = new File(parentDir, fileName);

            if (newFile.exists() && newFile.isFile()) {
                if (!recreate || !newFile.delete()) {
                    newFile = null;
                }
            }

            if (newFile != null) {
                try {
                    if (!newFile.createNewFile()) {
                        newFile = null;
                    }
                } catch (IOException e) {
                    logger.error("exception occurred: " + e);
                    return null;
                }
            }
        }

        return newFile;
    }

    public static boolean isBinaryFile(File f) throws FileNotFoundException, IOException {

        byte[] data = readBytesFromFile(f);

        if (data == null) {
            return false;
        }

        int ascii = 0;
        int other = 0;

        for (byte b : data) {
            if (b < 0x09)
                return true;

            if (b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D)
                ascii++;
            else if (b >= 0x20 && b <= 0x7E)
                ascii++;
            else
                other++;
        }

        return other != 0 && 100 * other / (ascii + other) > 95;
    }

    public static boolean revectorStream(InputStream in, OutputStream out) {
        return revectorStream(in, out);
    }

    public static boolean revectorStream(InputStream in, OutputStream out, @Nullable IStreamNotifier notifier) {

        if (in == null || out == null)
            return false;

        try {
            byte[] buff = new byte[256];

            int bytesWriteCount = 0;
            int totalBytesCount = 0;
            try {
                totalBytesCount = in.available();
            } catch (IOException e) {
                logger.error("an IOException occurred", e);
            }

            int len;
            long lastNotifyTime = 0;
            while ((len = in.read(buff)) > 0) {
                if (notifier != null) {
                    long interval = notifier.notifyInterval();
                    if (interval <= 0 || lastNotifyTime == 0 || (System.currentTimeMillis() - lastNotifyTime) >= interval) {
                        if (!notifier.onProcessing(in, out, bytesWriteCount,
                                totalBytesCount > 0 && bytesWriteCount <= totalBytesCount ? totalBytesCount - bytesWriteCount : 0)) {
                            break;
                        }
                        lastNotifyTime = System.currentTimeMillis();
                    }

                }
                out.write(buff, 0, len);
                bytesWriteCount += len;
            }

        } catch (IOException e) {
            logger.error("an IOException occurred", e);
            return false;
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                logger.error("an IOException occurred during close()", e);
            }
        }

        return true;

    }

    @Nullable
    public static byte[] readBytesFromInputStream(InputStream inputStream) {

        if (inputStream != null) {

            try {

                byte[] data = new byte[inputStream.available()];
                int readByteCount;
                do {
                    readByteCount = inputStream.read(data, 0, data.length);
                } while (readByteCount > 0);

                return data;

            } catch (IOException e) {
                logger.error("an Exception occurred", e);

            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.error("an IOException occurred during close()", e);
                }
            }
        }

        return null;
    }

    @Nullable
    public static byte[] readBytesFromFile(File file) {

        if (!isFileCorrect(file)) {
            logger.error("incorrect file: " + file);
            return null;
        }

        if (!file.canRead()) {
            logger.error("can't read from file: " + file);
            return null;
        }

        try {
            return readBytesFromInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            logger.error("a FileNotFoundException occurred", e);
            return null;
        }
    }

    @NonNull
    public static List<String> readStringsFromFile(File file) {

        List<String> lines = new ArrayList<>();

        if (!isFileCorrect(file)) {
            logger.error("incorrect file: " + file);
            return lines;
        }

        if (!file.canRead()) {
            logger.error("can't read from file: " + file);
            return lines;
        }

        try {
            return readStringsFromInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            logger.error("an IOException occurred", e);
            return lines;
        }
    }

    @Nullable
    public static String readStringFromFile(File file) {
        List<String> strings = readStringsFromFile(file);
        return !strings.isEmpty() ? TextUtils.join(System.getProperty("line.separator"), strings) : null;
    }

    @NonNull
    public static Collection<String> readStringsFromAsset(@NonNull Context context, String assetName) {
        try {
            return readStringsFromInputStream(context.getAssets().open(assetName));
        } catch (IOException e) {
            logger.error("an IOException occurred during open()", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    public static String readStringFromAsset(@NonNull Context context, String assetName) {
        try {
            return readStringFromInputStream(context.getAssets().open(assetName));
        } catch (IOException e) {
            logger.error("an IOException occurred during open()", e);
            return null;
        }
    }

    @NonNull
    public static Collection<String> readStringsFromRes(@NonNull Context context, @RawRes int resId) {
        try {
            return readStringsFromInputStream(context.getResources().openRawResource(resId));
        } catch (Resources.NotFoundException e) {
            logger.error("an IOException occurred during openRawResource()", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    public static String readStringFromRes(@NonNull Context context, @RawRes int resId) {
        try {
            return readStringFromInputStream(context.getResources().openRawResource(resId));
        } catch (Resources.NotFoundException e) {
            logger.error("an IOException occurred during openRawResource()", e);
            return null;
        }
    }

    @NonNull
    public static List<String> readStringsFromInputStream(@Nullable InputStream is) {
        if (is != null) {
            BufferedReader in = null;
            try {
                List<String> out = new ArrayList<>();
                in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    out.add(line);
                }
                return out;
            } catch (IOException e) {
                logger.error("an IOException occurred", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.error("an IOException occurred during close()", e);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    public static String readStringFromInputStream(@Nullable InputStream is) {
        Collection<String> strings = readStringsFromInputStream(is);
        return !strings.isEmpty() ? TextUtils.join(System.getProperty("line.separator"), strings) : null;
    }

    public static String convertInputStreamToString(InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        revectorStream(inputStream, result);
        try {
            return result.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("exception occurred: " + e);
            return null;
        }
    }

    public static boolean writeBytesToFile(@NonNull File file, byte[] data, boolean append) {
        if (data == null || data.length == 0) {
            return false;
        }
        if (!isFileExists(file.getAbsolutePath()) && (file = createNewFile(file.getName(), file.getAbsolutePath(), !append)) == null) {
            return false;
        }
        if (!file.canWrite()) {
            logger.error("can't write to file: " + file);
            return false;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath(), append);
        } catch (FileNotFoundException e) {
            logger.error("exception occurred: " + e);
        }
        if (fos != null) {
            try {
                fos.write(data);
                fos.flush();
                return true;
            } catch (IOException e) {
                logger.error("exception occurred: " + e);
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error("exception occurred: " + e);
                }
            }
        }
        return false;

    }

    @Nullable
    public static File writeFromStreamToFile(InputStream data, String fileName, String parentPath, boolean append) {
        return writeFromStreamToFile(data, fileName, parentPath, append, null);
    }

    @Nullable
    public static File writeFromStreamToFile(InputStream data, String fileName, String parentPath, boolean append, IStreamNotifier notifier) {
        logger.debug("writeFromStreamToFile(), data=" + data + ", fileName=" + fileName + ", parentPath=" + parentPath + ", append=" + append);

        final File file = createFile(fileName, parentPath, !append);

        if (file == null) {
            return null;
        }

        if (!file.canWrite()) {
            return file;
        }

        try {
            if (revectorStream(data, new FileOutputStream(file), notifier)) {
                return file;
            }
        } catch (FileNotFoundException e) {
            logger.error("exception occurred: " + e);
        }

        return null;
    }

    public static boolean writeStringToFile(File file, String data, boolean append) {
        return writeStringsToFile(file, Collections.singletonList(data), append);
    }

    public static boolean writeStringsToFile(@Nullable File file, @Nullable Collection<String> data, boolean append) {

        if (data == null || data.isEmpty()) {
            return false;
        }

        if (file == null || !isFileExists(file.getAbsolutePath()) && (file = createNewFile(file.getName(), file.getAbsolutePath(), !append)) == null) {
            return false;
        }
        if (!file.canWrite()) {
            logger.error("can't write to file: " + file);
            return false;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            logger.debug("an IOException occurred", e);
            return false;
        }

        BufferedWriter bw = new BufferedWriter(writer);

        try {
            for (String line : data) {
                bw.append(line);
                bw.append(System.getProperty("line.separator"));
                bw.flush();
            }
            return true;
        } catch (IOException e) {
            logger.error("an IOException occurred during write", e);
        } finally {
            try {
                bw.close();
            } catch (IOException e) {
                logger.error("an IOException occurred during close()", e);
            }
        }

        return false;
    }

    @Nullable
    public static File compressFilesToZip(Collection<File> srcFiles, String destZipName, String destZipParent, boolean recreate) {

        if (srcFiles == null || srcFiles.isEmpty()) {
            logger.error("source files is null or empty");
            return null;
        }

        File zipFile = createFile(destZipName, destZipParent, recreate);

        if (FileHelper.isFileExists(zipFile)) {
            logger.error("cannot create zip file");
            return null;
        }

        try {
            OutputStream os = new FileOutputStream(destZipName);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));

            try {
                int zippedFiles = 0;

                for (File srcFile : new ArrayList<>(srcFiles)) {

                    if (!isFileCorrect(srcFile)) {
                        logger.error("incorrect file to zip: " + srcFile);
                        continue;
                    }

                    byte[] bytes = readBytesFromFile(srcFile);

                    ZipEntry entry = new ZipEntry(srcFile.getName());
                    zos.putNextEntry(entry);
                    if (bytes != null) {
                        zos.write(bytes);
                    }
                    zos.closeEntry();

                    zippedFiles++;
                }

                return zippedFiles > 0 ? new File(destZipName) : null;

            } catch (Exception e) {
                logger.error("an Exception occurred", e);

            } finally {

                try {
                    zos.close();
                    os.close();
                } catch (IOException e) {
                    logger.error("an IOException occurred during close()", e);
                }

            }

        } catch (IOException e) {
            logger.error("an IOException occurred", e);
        }

        return null;
    }

    public static boolean unzipFile(File zipFile, File destPath, boolean saveDirHierarchy) {

        if (!isFileCorrect(zipFile)) {
            logger.error("incorrect zip file: " + zipFile);
            return false;
        }

        if (destPath == null) {
            logger.error("destPath is null");
            return false;
        }

        ZipFile zip = null;

        InputStream zis = null;
        OutputStream fos = null;

        try {
            zip = new ZipFile(zipFile);

            for (ZipEntry e : Collections.list(zip.entries())) {

                if (e.isDirectory() && !saveDirHierarchy) {
                    continue;
                }

                final String[] parts = e.getName().split(File.separator);
                final String entryName = !saveDirHierarchy && parts.length > 0 ? parts[parts.length - 1] : e.getName();

                final File path = new File(destPath, entryName);

                if (e.isDirectory()) {
                    if (!checkDirNoThrow(path.getAbsolutePath())) {
                        logger.error("can't create directory: " + path);
                        return false;
                    }

                } else {
                    if (createNewFile(path.getName(), path.getParent()) == null) {
                        logger.error("can't create new file: " + path);
                        return false;
                    }

                    zis = zip.getInputStream(e);
                    fos = new FileOutputStream(path);

                    if (!revectorStream(zis, fos)) {
                        logger.error("revectorStream() failed");
                        return false;
                    }

                    zis.close();
                    fos.close();
                }
            }

        } catch (IOException e) {
            logger.error("an IOException occurred", e);
            return false;

        } finally {

            try {
                if (zip != null) {
                    zip.close();
                }
                if (zis != null) {
                    zis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.error("an IOException occurred during close()", e);
            }
        }

        return true;
    }

    public final static String[] IMAGES_EXTENSIONS = {"bmp", "jpg", "jpeg", "png"};

    public static boolean isPicture(String ext) {
        for (String pictureExt : IMAGES_EXTENSIONS) {
            if (pictureExt.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    public final static String[] VIDEO_EXTENSIONS = {"3gp", "mp4", "mov", "mpg"};

    public static boolean isVideo(String ext) {
        for (String videoExt : VIDEO_EXTENSIONS) {
            if (videoExt.equalsIgnoreCase(ext)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static String getFileExtension(String name) {
        int index = name.lastIndexOf('.');
        return (index > 0 && index < name.length() - 1) ? name.substring(index + 1) : "";
    }

    public static String removeExtension(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            String ext = getFileExtension(fileName);
            if (!TextUtils.isEmpty(ext)) {
                int startIndex = fileName.lastIndexOf('.');
                if (startIndex >= 0) {
                    fileName = StringUtils.replace(fileName, startIndex, fileName.length(), "");
                }
                return fileName;
            }
        }
        return fileName;
    }

    /**
     * @return same sorted list or created sorted array list
     */
    public static Collection<File> sortFiles(Collection<File> files, boolean allowModifyCollection, @NonNull Comparator<? super File> comparator) {

        if (files == null || files.isEmpty()) {
            return files;
        }

        if (allowModifyCollection) {
            List<File> sortedList = files instanceof List ? (List<File>) files : new ArrayList<>(files);
            Collections.sort(sortedList, comparator);
            return sortedList;
        } else {
            File[] filesArray = files.toArray(new File[files.size()]);
            Arrays.sort(filesArray, comparator);
            return new ArrayList<>(Arrays.asList(filesArray));
        }
    }

    public static Collection<File> sortFilesByName(Collection<File> filesList, boolean ascending, boolean allowModifyList) {
        return sortFiles(filesList, allowModifyList, new FileComparator(Collections.singletonMap(FileComparator.SortOption.NAME, ascending)));
    }

    public static Collection<File> sortFilesBySize(Collection<File> filesList, boolean ascending, boolean allowModifyList) {
        return sortFiles(filesList, allowModifyList, new FileComparator(Collections.singletonMap(FileComparator.SortOption.SIZE, ascending)));
    }

    public static Collection<File> sortFilesByLastModified(Collection<File> filesList, boolean ascending, boolean allowModifyList) {
        return sortFiles(filesList, allowModifyList, new FileComparator(Collections.singletonMap(FileComparator.SortOption.LAST_MODIFIED, ascending)));
    }

    @NonNull
    public static Set<File> getFiles(Collection<File> fromFiles, @NonNull GetMode mode, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
        Set<File> collected = new LinkedHashSet<>();
        if (fromFiles != null) {
            for (File fromFile : fromFiles) {
                collected.addAll(getFiles(fromFile, mode, comparator, notifier, depth));
            }
        }
        return collected;
    }

    /**
     * @param fromFile file or directory
     * @return collected set of files or directories from specified directories without source files
     */
    @NonNull
    public static Set<File> getFiles(File fromFile, @NonNull GetMode mode, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
        return getFiles(fromFile, mode, comparator, notifier, depth, 0, null);
    }

    @NonNull
    private static Set<File> getFiles(File fromFile, @NonNull GetMode mode,
                                      @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier,
                                      int depth, int currentLevel, @Nullable Set<File> collected) {

        final Set<File> result = new LinkedHashSet<>();

        if (collected == null) {
            collected = new LinkedHashSet<>();
        }

        if (fromFile != null && fromFile.exists()) {

            if (notifier != null) {
                if (!notifier.onProcessing(fromFile, Collections.unmodifiableSet(collected), currentLevel)) {
                    return result;
                }
            }

//            if (mode == GetMode.FOLDERS || mode == GetMode.ALL) {
//                if (notifier == null || notifier.onGet(fromFile)) {
//                    result.add(fromFile);
//                }
//            }

            boolean isCorrect = true;

            if (fromFile.isDirectory()) {

                File[] files = fromFile.listFiles();

                if (files != null) {

                    for (File f : files) {

                        if (f.isDirectory()) {

                            if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                result.addAll(getFiles(f, mode, comparator, notifier, depth, ++currentLevel, collected));
                            }

                        } else if (f.isFile()) {
                            result.addAll(getFiles(f, mode, comparator, notifier, depth, currentLevel, collected));
                        } else {
                            logger.error("incorrect file or folder: " + f);
                        }
                    }
                }
            } else if (!fromFile.isFile()) {
                logger.error("incorrect file or folder: " + fromFile);
                isCorrect = false;
            }

            if (isCorrect) {
                if (fromFile.isFile() ? mode == GetMode.FILES : mode == GetMode.FOLDERS || mode == GetMode.ALL) {
                    if (notifier == null || (fromFile.isFile() ? notifier.onGetFile(fromFile) : notifier.onGetFolder(fromFile))) {
                        result.add(fromFile);
                        collected.add(fromFile);
                    }
                }
            }
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(result);
            Collections.sort(sortedList, comparator);
            result.clear();
            result.addAll(sortedList);
        }

        return result;
    }

    @NonNull
    public static Set<File> searchByName(String name, Collection<File> searchFiles, @NonNull GetMode mode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
        Set<File> collected = new LinkedHashSet<>();
        if (searchFiles != null) {
            for (File searchFile : searchFiles) {
                collected.addAll(searchByName(name, searchFile, mode, searchFlags, comparator, notifier, depth, 0, null));
            }
        }
        return collected;
    }

    /**
     * @param comparator to sort each folders list and result set
     * @return found set of files or directories with matched name
     */
    @NonNull
    public static Set<File> searchByName(String name, File searchFile, @NonNull GetMode mode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
        return searchByName(name, searchFile, mode, searchFlags, comparator, notifier, depth, 0, null);
    }

    @NonNull
    private static Set<File> searchByName(String name, File searchFile, @NonNull GetMode mode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier,
                                          int depth, int currentLevel, @Nullable Set<File> foundFiles) {

        Set<File> result = new LinkedHashSet<>();

        if (foundFiles == null) {
            foundFiles = new LinkedHashSet<>();
        }

        if (searchFile != null && searchFile.exists()) {

            if (notifier != null) {
                if (!notifier.onProcessing(searchFile, Collections.unmodifiableSet(foundFiles), currentLevel)) {
                    return result;
                }
            }

            boolean isCorrect = true;

            if (searchFile.isDirectory()) {

                File[] files = searchFile.listFiles();

                if (files != null) {

                    if (comparator != null) {
                        List<File> sorted = new ArrayList<>(Arrays.asList(files));
                        Collections.sort(sorted, comparator);
                        files = sorted.toArray(new File[sorted.size()]);
                    }

                    for (File f : files) {

                        if (f.isDirectory()) {
                            if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                result.addAll(searchByName(name, f, mode, searchFlags, comparator, notifier, depth, ++currentLevel, foundFiles));
                            }
                        } else if (f.isFile()) {
                            result.addAll(searchByName(name, f, mode, searchFlags, comparator, notifier, depth, currentLevel, foundFiles));
                        } else {
                            logger.error("incorrect file or folder: " + f);
                        }

                    }
                }

            } else if (!searchFile.isFile()) {
                logger.error("incorrect file or folder: " + searchFile);
                isCorrect = false;
            }

            if (isCorrect) {
                if (searchFile.isFile() ? mode == GetMode.FILES : mode == GetMode.FOLDERS || mode == GetMode.ALL) {
                    if (CompareUtils.stringMatches(searchFile.getName(), name, searchFlags)) {
                        if (notifier == null || (searchFile.isFile() ? notifier.onGetFile(searchFile) : notifier.onGetFolder(searchFile))) {
                            result.add(searchFile);
                            foundFiles.add(searchFile);
                        }
                    }
                }
            }
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(result);
            Collections.sort(sortedList, comparator);
            result.clear();
            result.addAll(sortedList);
        }

        return result;
    }

    @NonNull
    public static Set<File> searchByNameFirst(String name, Collection<File> searchFiles, @NonNull GetMode getMode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable final IGetNotifier notifier, int depth) {
        Set<File> collected = new LinkedHashSet<>();
        for (File file : searchFiles) {
            collected.addAll(searchByName(name, file, getMode, searchFlags, comparator, notifier, depth));
        }
        return collected;
    }

    @Nullable
    public static File searchByNameFirst(String name, File searchFile, @NonNull GetMode getMode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable final IGetNotifier notifier, int depth) {
        Set<File> found = searchByName(name, searchFile, getMode, searchFlags, comparator, new IGetNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> found, int currentLevel) {
                return (notifier == null || notifier.onProcessing(current, found, currentLevel)) && found.size() == 0;
            }

            @Override
            public boolean onGetFile(@NonNull File file) {
                return notifier == null || notifier.onGetFile(file);
            }

            @Override
            public boolean onGetFolder(@NonNull File folder) {
                return notifier == null || notifier.onGetFolder(folder);
            }
        }, depth);
        return !found.isEmpty() ? new ArrayList<>(found).get(0) : null;
    }

    @NonNull
    public static Set<File> searchByNameWithStat(final String name, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier) {
        return searchByNameWithStat(name, null, comparator, notifier);
    }

    /**
     * @param name        file or folder name part
     * @param searchFiles if null or empty, 'PATH' environment variable will be used
     */
    @NonNull
    public static Set<File> searchByNameWithStat(final String name, @Nullable Collection<File> searchFiles, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier) {

        final Set<File> foundFiles = new LinkedHashSet<>();

        if (searchFiles == null || searchFiles.isEmpty()) {
            searchFiles = getEnvPathFiles();
        }

        for (File file : new ArrayList<>(searchFiles)) {

            if (file == null) {
                continue;
            }

            String path = file.getAbsolutePath();

            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }

            final String currentPath = path;

            execProcess(Arrays.asList("stat", currentPath + name), null, new ShellUtils.ShellCallback() {

                @Override
                public boolean needToLogCommands() {
                    return false;
                }

                @Override
                public void processStartFailed(Throwable t) {
                    logger.error("processStartFailed(), t=" + t);
                }

                @Override
                public void shellOut(@NonNull StreamType from, String shellLine) {
                    if (shellLine.contains("File: ") && shellLine.contains(name)) {
                        foundFiles.add(new File(currentPath));
                    }
                }

                @Override
                public void processComplete(int exitValue) {
                }

            }, null);
        }

        if (!foundFiles.isEmpty()) {

            for (File file : foundFiles) {

                if (notifier != null) {
                    if (!notifier.onProcessing(file, Collections.unmodifiableSet(foundFiles), 0)) {
                        break;
                    }
                }

                String path = file.getAbsolutePath();

                if (!path.endsWith(File.separator)) {
                    path += File.separator;
                }

                foundFiles.add(new File(path));
            }
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(foundFiles);
            Collections.sort(sortedList, comparator);
            foundFiles.clear();
            foundFiles.addAll(sortedList);
        }

        return foundFiles;
    }


    public static Set<File> getEnvPathFiles() {
        Set<File> searchFiles = new HashSet<>();
        String[] paths = System.getenv("PATH").split(":");
        if (paths.length == 0) {
            return Collections.emptySet();
        }
        for (String path : paths) {
            searchFiles.add(new File(path));
        }
        return searchFiles;
    }

    public static boolean deleteDir(File dir) {
        return dir != null && dir.isDirectory() && (dir.listFiles() == null || dir.listFiles().length == 0) && dir.delete();
    }

    public static boolean deleteFile(File file) {
        return file != null && file.isFile() && file.exists() && file.delete();
    }

    public static boolean deleteFile(String fileName, String parentPath) {
        if (isFileExists(fileName, parentPath)) {
            File f = new File(parentPath, fileName);
            return f.delete();
        }
        return false;
    }

    public static boolean deleteFile(String filePath) {
        if (isFileExists(filePath)) {
            File f = new File(filePath);
            return f.delete();
        }
        return false;
    }

    @NonNull
    public static Set<File> delete(Collection<File> fromFiles, boolean deleteEmptyDirs, @Nullable Collection<File> excludeFiles, @Nullable Comparator<? super File> comparator, @Nullable IDeleteNotifier notifier, int depth) {
        Set<File> collected = new LinkedHashSet<>();
        if (fromFiles != null) {
            for (File file : fromFiles) {
                collected.addAll(delete(file, deleteEmptyDirs, excludeFiles, comparator, notifier, depth));
            }
        }
        return collected;
    }

    /**
     * @param comparator to sort each folders list
     * @return set of deleted files
     */
    @NonNull
    public static Set<File> delete(File fromFile, boolean deleteEmptyDirs, @Nullable Collection<File> excludeFiles, @Nullable Comparator<? super File> comparator, @Nullable IDeleteNotifier notifier, int depth) {
        return delete(fromFile, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, 0, null);
    }

    @NonNull
    private static Set<File> delete(File fromFile, boolean deleteEmptyDirs, @Nullable Collection<File> excludeFiles, @Nullable Comparator<? super File> comparator, @Nullable IDeleteNotifier notifier,
                                    int depth, int currentLevel, @Nullable Set<File> deletedFiles) {

        Set<File> result = new LinkedHashSet<>();

        if (deletedFiles == null) {
            deletedFiles = new LinkedHashSet<>();
        }

        if (fromFile != null && fromFile.exists()) {

            if (notifier != null) {
                if (!notifier.onProcessing(fromFile, Collections.unmodifiableSet(deletedFiles), currentLevel)) {
                    return result;
                }
            }

            if (fromFile.isDirectory()) {
                File[] files = fromFile.listFiles();

                if (files != null) {

                    if (comparator != null) {
                        List<File> sorted = new ArrayList<>(Arrays.asList(files));
                        Collections.sort(sorted, comparator);
                        files = sorted.toArray(new File[sorted.size()]);
                    }

                    for (File f : files) {

                        if (excludeFiles == null || !excludeFiles.contains(f)) {
                            if (f.isDirectory()) {
                                if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                    result.addAll(delete(f, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, ++currentLevel, deletedFiles));
                                }
                                if (deleteEmptyDirs) {
                                    if (notifier == null || notifier.confirmDeleteFolder(f)) {
                                        if (f.delete()) {
                                            result.add(f);
                                            deletedFiles.add(f);
                                        }
                                    }
                                }
                            } else if (f.isFile()) {
                                result.addAll(delete(f, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, currentLevel, deletedFiles));
                            } else {
                                logger.error("incorrect file or folder: " + f);
                            }
                        }
                    }

                    File[] remainFiles = fromFile.listFiles();

                    if (remainFiles == null || remainFiles.length == 0) {
                        if (deleteEmptyDirs) {
                            if (notifier == null || notifier.confirmDeleteFolder(fromFile)) {
                                if (fromFile.delete()) {
                                    result.add(fromFile);
                                    deletedFiles.add(fromFile);
                                }
                            }
                        }
                    }
                }
            } else if (fromFile.isFile()) {

                if (notifier == null || notifier.confirmDeleteFile(fromFile)) {
                    if (fromFile.delete()) {
                        result.add(fromFile);
                        deletedFiles.add(fromFile);
                    }
                }

            } else {
                logger.error("incorrect file or folder: " + fromFile);
            }
        }

        return result;
    }

    /**
     * This function will return size in form of bytes
     * реккурсивно подсчитывает размер папки в байтах
     *
     * @param f     файл или папка
     * @param depth глубина вложенности, 0 - текущий уровень
     */
    public static long getSize(File f, int depth) {
        return getSize(f, depth, 0);
    }


    private static long getSize(File f, int depth, int currentLevel) {
        long size = 0;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                        size += getSize(file, depth, ++currentLevel);
                    }
                }
            }
        } else if (f.isFile()) {
            size = f.length();
        }
        return size;
    }

    public static String getSizeWithValue(Context context, File file, int depth) {
        return getSizeWithValue(context, file, depth, null);
    }

    public static String getSizeWithValue(Context context, File file, int depth, @Nullable Collection<SizeUnit> sizeUnitsToExclude) {
        return sizeToString(context, getSize(file, depth), SizeUnit.BYTES, sizeUnitsToExclude);
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosDocument(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosDocument(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * users have read and run access to file; owner can modify
     */
    public final static String FILE_PERMISSIONS_ALL = "755";

    /**
     * only owner has r/w/x access to file
     */
    public final static String FILE_PERMISSIONS_OWNER = "700";

    /**
     * Copies a raw resource file, given its ID to the given location
     *
     * @param ctx  context
     * @param mode file permissions (E.g.: "755")
     * @throws IOException          on error
     * @throws InterruptedException when interrupted
     */
    public static boolean copyRawFile(Context ctx, @RawRes int resId, File destFile, String mode) throws IOException, InterruptedException {
        logger.debug("copyRawFile(), resId=" + resId + ", destFile=" + destFile + ", mode=" + mode);

        if (mode == null || mode.length() == 0)
            mode = FILE_PERMISSIONS_ALL;

        final String destFilePath = destFile.getAbsolutePath();

        revectorStream(ctx.getResources().openRawResource(resId), new FileOutputStream(destFile));

        return (Runtime.getRuntime().exec("chmod " + mode + " " + destFilePath).waitFor() == 0);
    }

    public static boolean copyFromAssets(Context ctx, String assetsPath, File to, boolean rewrite) {

        to = createNewFile(to != null ? to.getName() : null, to != null ? to.getParent() : null, rewrite);

        if (to == null) {
            return false;
        }

        FileOutputStream out = null;
        InputStream in = null;
        try {
            out = new FileOutputStream(to);
            in = ctx.getAssets().open(assetsPath);
            return revectorStream(in, out);
        } catch (IOException e) {
            logger.error("an IOException occurred", e);
            return false;
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                logger.error("an IOException occurred during close()", e);
            }
        }
    }

    /**
     * @return dest file
     */
    public static File copyFile(File sourceFile, String destName, String destDir, boolean rewrite, boolean preserveFileDate) {

        if (!isFileCorrect(sourceFile)) {
            logger.error("source file not exists: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        File destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            logger.error("can't create dest file: " + destDir + File.separator + targetName);
            return null;
        }

        if (writeBytesToFile(destFile, readBytesFromFile(sourceFile), !rewrite)) {
            if (preserveFileDate) {
                destFile.setLastModified(sourceFile.lastModified());
            }
            return destFile;
        } else {
            logger.error("can't write to dest file: " + destDir + File.separator + targetName);
        }


        return null;
    }

    /**
     * @return dest file
     */
    public static File copyFileWithBuffering(final File sourceFile, String destName, String destDir, boolean rewrite, boolean preserveFileDate,
                                             @Nullable final ISingleCopyNotifier notifier) {

        if (!isFileExists(sourceFile)) {
            logger.error("source file not exists: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        final File destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            return null;
        }

        final long totalBytesCount = sourceFile.length();

        try {
            if (writeFromStreamToFile(new FileInputStream(sourceFile), destFile.getName(), destFile.getParent(), !rewrite, notifier != null ? new IStreamNotifier() {
                @Override
                public long notifyInterval() {
                    return notifier.notifyInterval();
                }

                @Override
                public boolean onProcessing(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, long bytesWrite, long bytesLeft) {
                    return notifier.onProcessing(sourceFile, destFile, bytesWrite, totalBytesCount);
                }
            } : null) != null) {
                if (preserveFileDate) {
                    destFile.setLastModified(sourceFile.lastModified());
                }
            }
        } catch (FileNotFoundException e) {
            logger.error("exception occurred: " + e);
        }

        return null;
    }

    /**
     * @param fromFile file or directory
     */
    @NonNull
    public static Set<File> copyFilesWithBuffering(File fromFile, File destDir,
                                                   @Nullable Comparator<? super File> comparator,
                                                   @Nullable final ISingleCopyNotifier singleNotifier, @Nullable final IMultipleCopyNotifier multipleCopyNotifier,
                                                   boolean preserveFileDate, int depth) {
        return copyFilesWithBuffering(fromFile, destDir, comparator, singleNotifier, multipleCopyNotifier, preserveFileDate, depth, 0, 0, null, null);
    }

    @NonNull
    private static Set<File> copyFilesWithBuffering(File fromFile, File destDir,
                                                    @Nullable Comparator<? super File> comparator,
                                                    @Nullable final ISingleCopyNotifier singleNotifier, @Nullable final IMultipleCopyNotifier multipleCopyNotifier,
                                                    boolean preserveFileDate, int depth,
                                                    int currentLevel, int totalFilesCount, @Nullable Set<File> copied, List<String> exclusionList) {

        Set<File> result = new LinkedHashSet<>();

        if (copied == null) {
            copied = new LinkedHashSet<>();
        }

        boolean isCorrect = false;

        if (destDir != null) {

            if (fromFile != null && fromFile.exists()) {

                isCorrect = true;

                if (currentLevel == 0) {
                    totalFilesCount = getFiles(fromFile, GetMode.FILES, comparator, multipleCopyNotifier != null ? new IGetNotifier() {
                        @Override
                        public boolean onProcessing(@NonNull File current, @NonNull Set<File> collected, int currentLevel) {
                            return multipleCopyNotifier.onCalculatingSize(current, collected, currentLevel);
                        }

                        @Override
                        public boolean onGetFile(@NonNull File file) {
                            return true;
                        }

                        @Override
                        public boolean onGetFolder(@NonNull File folder) {
                            return false;
                        }
                    } : null, depth).size();


                    if (fromFile.isDirectory() && destDir.getAbsolutePath().startsWith(fromFile.getAbsolutePath())) {

                        File[] srcFiles = fromFile.listFiles();

                        if (srcFiles != null && srcFiles.length > 0) {
                            exclusionList = new ArrayList<>(srcFiles.length);
                            for (File srcFile : srcFiles) {
                                exclusionList.add(new File(destDir, srcFile.getName()).getAbsolutePath());
                            }
                        }
                    }

                }

                if (multipleCopyNotifier != null) {
                    if (!multipleCopyNotifier.onProcessing(fromFile, destDir, Collections.unmodifiableSet(copied), totalFilesCount, currentLevel)) {
                        return result;
                    }
                }

                if (fromFile.isDirectory()) {

                    File[] files = fromFile.listFiles();

                    if (files != null) {

                        if (comparator != null) {
                            List<File> sorted = new ArrayList<>(Arrays.asList(files));
                            Collections.sort(sorted, comparator);
                            files = sorted.toArray(new File[sorted.size()]);
                        }

                        for (File f : files) {

                            if (f.isDirectory()) {
                                if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                    // FIXME
                                    result.addAll(copyFilesWithBuffering(f, destDir /*new File(destDir, f.getName())*/, comparator,
                                            singleNotifier, multipleCopyNotifier, preserveFileDate, depth, ++currentLevel, totalFilesCount, copied, exclusionList));
                                }
                            } else if (isFileExists(f)) {
                                // FIXME
                                result.addAll(copyFilesWithBuffering(f, destDir /*new File(destDir, fromFile.getName())*/, comparator,
                                        singleNotifier, multipleCopyNotifier, preserveFileDate, depth, currentLevel, totalFilesCount, copied, exclusionList));
                            }
                        }

                    }
                } else if (isFileExists(fromFile)) {

                    File destFile = null;

                    boolean confirmCopy = true;

                    if (multipleCopyNotifier != null) {
                        confirmCopy = multipleCopyNotifier.onConfirmCopy(fromFile, destDir, currentLevel);
                    }

                    if (confirmCopy) {

                        if (multipleCopyNotifier != null) {
                            destFile = multipleCopyNotifier.onBeforeCopy(fromFile, destDir, currentLevel);
                        }

                        if (destFile == null) {
                            destFile = new File(destDir, fromFile.getName());
                        }

                        boolean rewrite = false;

                        if (multipleCopyNotifier != null && isFileExists(destFile)) {
                            rewrite = multipleCopyNotifier.onExists(destFile, currentLevel);
                        }

                        File resultFile = null;

                        if (exclusionList == null || !exclusionList.contains(fromFile.getAbsolutePath())) {
                            resultFile = copyFileWithBuffering(fromFile, destFile.getName(), destFile.getParent(), rewrite,
                                    preserveFileDate, singleNotifier);
                        }

                        if (resultFile != null) {
                            result.add(resultFile);
                            copied.add(resultFile);
                        } else {
                            isCorrect = false;
                        }
                    }
                } else {
                    isCorrect = false;
                }
            }

            if (!isCorrect) {
                logger.error("incorrect file or folder or failed to copy from: " + fromFile);
                if (multipleCopyNotifier != null) {
                    multipleCopyNotifier.onFailed(fromFile, destDir, currentLevel);
                }
            }
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(result);
            Collections.sort(sortedList, comparator);
            result.clear();
            result.addAll(sortedList);
        }
        return result;
    }

    public static boolean resetFile(File f) {
        if (f.isFile() && f.exists()) {

            if (f.length() == 0) {
                return true;
            }

            RandomAccessFile ra = null;
            try {
                ra = new RandomAccessFile(f, "rw");
                ra.setLength(0);
                return true;

            } catch (IOException e) {
                logger.error("an IOException occurred", e);
            } finally {
                if (ra != null) {
                    try {
                        ra.close();
                    } catch (IOException e) {
                        logger.error("an IOException occurred during close()", e);
                    }
                }
            }
        }
        return false;
    }

    public static boolean writeExifLocation(File f, Location loc) {

        if (!isFileCorrect(f) || !isPicture(getFileExtension(f.getName()))) {
            logger.error("incorrect picture file: " + f);
            return false;
        }

        if (loc == null) {
            logger.warn("location is null");
            return false;
        }

        try {

            ExifInterface exif = new ExifInterface(f.getAbsolutePath());

            if (!CompareUtils.objectsEqual(loc.getLatitude(), 0.0d) || !CompareUtils.objectsEqual(loc.getLongitude(), 0.0d)) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, String.valueOf(loc.getLatitude()));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf(loc.getLongitude()));
            }
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, String.valueOf(loc.getAltitude()));

            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, String.valueOf(loc.getProvider()));

            exif.saveAttributes();
            return true;

        } catch (IOException e) {
            logger.error("an IOException occurred", e);
            return false;
        }
    }

    /**
     * @param id      the constant value of resource subclass field
     * @param resType subclass where the static final field with given id value declared
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static boolean checkResourceIdExists(int id, String resName, Class<?> resType) throws NullPointerException, IllegalArgumentException, IllegalAccessException {

        if (resType == null)
            throw new NullPointerException("resType is null");

        Field[] fields = resType.getDeclaredFields();

        if (fields == null || fields.length == 0)
            return false;

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.getName().equals(resName)) {
                try {
                    if (field.getInt(null) == id)
                        return true;
                } catch (Exception e) {
                    logger.error("an Exception occurred during getInt()");
                }
            }
        }

        return false;
    }

    public enum GetMode {
        FILES, FOLDERS, ALL
    }

    public interface IGetNotifier {

        /**
         * @return false if client code wants to interrupt collecting
         */
        boolean onProcessing(@NonNull File current, @NonNull Set<File> collected, int currentLevel);

        /**
         * @return false if client code doesn't want to append this file to result
         */
        boolean onGetFile(@NonNull File file);


        /**
         * @return false if client code doesn't want to append this folder to result
         */
        boolean onGetFolder(@NonNull File folder);
    }

    public interface IDeleteNotifier {

        /**
         * @return false if client code wants to interrupt deleting
         */
        boolean onProcessing(@NonNull File current, @NonNull Set<File> deleted, int currentLevel);

        /**
         * @return false if client code doesn't want to delete this file
         */
        boolean confirmDeleteFile(File file);

        /**
         * @return false if client code doesn't want to delete this folder
         */
        boolean confirmDeleteFolder(File folder);
    }

    public interface IStreamNotifier {

        long notifyInterval();

        boolean onProcessing(@NonNull InputStream inputStream, @NonNull OutputStream outputStream, long bytesWrite, long bytesLeft);
    }

    public interface ISingleCopyNotifier {

        long notifyInterval();

        boolean onProcessing(@NonNull File sourceFile, @NonNull File destFile, long bytesCopied, long bytesTotal);
    }

    public interface IMultipleCopyNotifier {

        boolean onCalculatingSize(@NonNull File current, @NonNull Set<File> collected, int currentLevel);

        boolean onProcessing(@NonNull File currentFile, @NonNull File destDir, @NonNull Set<File> copied, long filesTotal, int currentLevel);

        boolean onConfirmCopy(@NonNull File currentFile, @NonNull File destDir, int currentLevel);

        /**
         * @return target file to copy in or null if default
         */
        File onBeforeCopy(@NonNull File currentFile, @NonNull File destDir, int currentLevel);

        boolean onExists(@NonNull File destFile, int currentLevel);

        void onFailed(@Nullable File currentFile, @NonNull File destFile, int currentLevel);
    }

    public static class FileComparator extends AbsOptionableComparator<FileComparator.SortOption, File> {

        public enum SortOption implements ISortOption {

            NAME, SIZE, LAST_MODIFIED;

            @Override
            public String getName() {
                return name();
            }
        }

        @SuppressWarnings("unchecked")
        public FileComparator(@Nullable Map<FileComparator.SortOption, Boolean> sortOptions) {
            super(sortOptions);
        }

        @Override
        protected int compare(@Nullable File lhs, @Nullable File rhs, @NonNull SortOption option, boolean ascending) {

            int result = 0;

            if (lhs == null && rhs == null) {
                return result;
            }

            switch (option) {
                case NAME:
                    result = CompareUtils.compareStrings(lhs != null ? lhs.getAbsolutePath() : null, rhs != null ? rhs.getAbsolutePath() : null, ascending, true);
                    break;
                case SIZE:
                    result = CompareUtils.compareLongs(lhs != null ? lhs.length() : null, rhs != null ? rhs.length() : null, ascending);
                    break;
                case LAST_MODIFIED:
                    result = CompareUtils.compareLongs(lhs != null ? lhs.lastModified() : null, rhs != null ? rhs.lastModified() : null, ascending);
                    break;
            }

            return result;
        }
    }

    @NonNull
    public static Map<File, Long> suListFiles(@NonNull Collection<File> fromDirs, @Nullable Comparator<? super File> comparator, @Nullable final ISuGetNotifier notifier) {
        final Map<File, Long> collectedMap = new LinkedHashMap<>();
        final Set<File> collected = new LinkedHashSet<>();
        for (final File dir : new LinkedHashSet<>(fromDirs)) {

            if (dir != null/* && isDirExists(dir.getAbsolutePath())*/) {
                execProcess(Arrays.asList("su", "-c", "ls", dir.getAbsolutePath()), null, new ShellUtils.ShellCallback() {
                    @Override
                    public boolean needToLogCommands() {
                        return true;
                    }

                    @Override
                    public void shellOut(@NonNull StreamType from, String shellLine) {
                        if (from == StreamType.OUT && !TextUtils.isEmpty(shellLine)) {
                            File current = new File(dir, shellLine);
                            if (notifier != null) {
                                notifier.onProcessing(current, collected, 0);
                            }
                            if (notifier == null || notifier.onGetFile(current)) {
                                collected.add(current);
                            }
                        }
                    }

                    @Override
                    public void processStartFailed(Throwable t) {

                    }

                    @Override
                    public void processComplete(int exitValue) {

                    }
                }, null);
            }
        }
        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(collected);
            Collections.sort(sortedList, comparator);
            collected.clear();
            collected.addAll(sortedList);
        }
        for (final File current : collected) {
            // option "-b" is not supported on android version
            execProcess(Arrays.asList("su", "-c", "du", "-s", current.getAbsolutePath()), null, new ShellUtils.ShellCallback() {

                        @Override
                        public boolean needToLogCommands() {
                            return true;
                        }

                        @Override
                        public void shellOut(@NonNull StreamType from, String shellLine) {
                            if (from == StreamType.OUT && !TextUtils.isEmpty(shellLine)) {
                                long size = 0;
                                String[] parts = shellLine.split("\\t");
                                if (parts.length > 1) {
                                    try {
                                        size = Long.parseLong(parts[0]);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                                collectedMap.put(current, SizeUnit.KBYTES.toBytes(size));
                            }
                        }

                        @Override
                        public void processStartFailed(Throwable t) {
                            if (notifier != null) {
                                notifier.onStartFailed(t, current);
                            }
                        }

                        @Override
                        public void processComplete(int exitValue) {
                            if (exitValue != ShellUtils.PROCESS_EXIT_CODE_SUCCESS && notifier != null) {
                                notifier.onNonSuccessExitCode(exitValue, current);
                            }
                        }
                    }

                    , null);
        }

        return Collections.unmodifiableMap(collectedMap);
    }

    public interface ISuGetNotifier extends FileHelper.IGetNotifier {

        void onStartFailed(Throwable t, File forFile);

        void onNonSuccessExitCode(int exitCode, File forFile);
    }

    public static String filesToString(@NonNull Context context, Collection<File> files) {
        StringBuilder sb = new StringBuilder();
        if (files != null) {
            for (File f : files) {
                if (f != null) {
                    sb.append(f.getAbsolutePath());
                    sb.append(" : ");
                    sb.append(sizeToString(context, getSize(f, DEPTH_UNLIMITED), SizeUnit.BYTES));
                    sb.append(" ");
                    sb.append(System.getProperty("line.separator"));
                }
            }
        }
        return sb.toString();
    }

    /**
     * @param files file <-> size in bytes </->
     */
    public static String filesWithSizeToString(@NonNull Context context, Collection<Map.Entry<File, Long>> files) {
        StringBuilder sb = new StringBuilder();
        if (files != null) {
            for (Map.Entry<File, Long> f : files) {
                if (f != null && f.getKey() != null) {
                    sb.append(f.getKey().getAbsolutePath());
                    sb.append(" : ");
                    sb.append(f.getValue() != null ? sizeToString(context, f.getValue(), SizeUnit.BYTES) : 0);
                    sb.append("  ");
                    sb.append(System.getProperty("line.separator"));
                }
            }
        }
        return sb.toString();
    }

    public static String sizeToString(@NonNull Context context, float s, @NonNull SizeUnit sizeUnit) {
        return sizeToString(context, s, sizeUnit, null);
    }

    /**
     * @param s                  size
     * @param sizeUnit           unit for s
     * @param sizeUnitsToExclude list of units to avoid in result string
     */
    public static String sizeToString(@NonNull Context context, float s, @NonNull SizeUnit sizeUnit, @Nullable Collection<SizeUnit> sizeUnitsToExclude) {
        if (s < 0) {
            throw new IllegalArgumentException("incorrect size: " + s);
        }
        if (!sizeUnit.isBytes()) {
            throw new IllegalArgumentException("sizeUnit must be bytes only");
        }
        if (sizeUnitsToExclude == null) {
            sizeUnitsToExclude = Collections.emptySet();
        }
        s = sizeUnit.toBytes(s);
        StringBuilder sb = new StringBuilder();
        if (s < SizeUnit.C1 && !sizeUnitsToExclude.contains(SizeUnit.BYTES)) {
            sb.append((int) s);
            sb.append(" ");
            sb.append(context.getString(R.string.size_suffix_bytes));
        } else if (s >= SizeUnit.C1 && s < SizeUnit.C2 && !sizeUnitsToExclude.contains(SizeUnit.KBYTES)) {
            float kbytes = SizeUnit.BYTES.toKBytes(s);
            sb.append(!sizeUnitsToExclude.contains(SizeUnit.BYTES) ? (long) kbytes : kbytes);
            sb.append(" ");
            sb.append(context.getString(R.string.size_suffix_kbytes));
            float restBytes = s - (long) SizeUnit.KBYTES.toBytes(kbytes);
            if (restBytes > 0) {
                sb.append(", ");
                sb.append(sizeToString(context, restBytes, SizeUnit.BYTES, sizeUnitsToExclude));
            }
        } else if (s >= SizeUnit.C2 && s < SizeUnit.C3 && !sizeUnitsToExclude.contains(SizeUnit.MBYTES)) {
            float mbytes = SizeUnit.BYTES.toMBytes(s);
            sb.append(!sizeUnitsToExclude.contains(SizeUnit.KBYTES) ? (long) mbytes : mbytes);
            sb.append(" ");
            sb.append(context.getString(R.string.size_suffix_mbytes));
            float restBytes = s - (long) SizeUnit.MBYTES.toBytes(mbytes);
            if (restBytes > 0) {
                sb.append(", ");
                sb.append(sizeToString(context, restBytes, SizeUnit.BYTES, sizeUnitsToExclude));
            }
        } else if (!sizeUnitsToExclude.contains(SizeUnit.GBYTES)) {
            float gbytes = SizeUnit.BYTES.toGBytes(s);
            sb.append(!sizeUnitsToExclude.contains(SizeUnit.MBYTES) ? (long) gbytes : gbytes);
            sb.append(" ");
            sb.append(context.getString(R.string.size_suffix_gbytes));
            float restBytes = s - (long) SizeUnit.GBYTES.toBytes(gbytes);
            if (restBytes > 0) {
                sb.append(", ");
                sb.append(sizeToString(context, restBytes, SizeUnit.BYTES, sizeUnitsToExclude));
            }
        }
        if (sb.length() == 0) {
            sb.append(context.getString(R.string.size_unknown));
        }
        return sb.toString();
    }

    public static double sum(Collection<? extends Number> numbers) {
        double result = 0;
        if (numbers != null) {
            for (Number n : numbers) {
                if (n != null) {
                    result += n.doubleValue();
                }
            }
        }
        return result;
    }

    public enum SizeUnit {

        BYTES {
            @Override
            public long toBytes(float s) {
                return (long) s;
            }

            @Override
            public float toKBytes(float s) {
                return s / C1;
            }

            @Override
            public float toMBytes(float s) {
                return s / C2;
            }

            @Override
            public float toGBytes(float s) {
                return s / C3;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        KBYTES {
            @Override
            public long toBytes(float s) {
                return (long) (s * C1);
            }

            @Override
            public float toKBytes(float s) {
                return s;
            }

            @Override
            public float toMBytes(float s) {
                return s / C1;
            }

            @Override
            public float toGBytes(float s) {
                return s / C2;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        MBYTES {
            @Override
            public long toBytes(float s) {
                return (long) (s * C2);
            }

            @Override
            public float toKBytes(float s) {
                return s * C1;
            }

            @Override
            public float toMBytes(float s) {
                return s;
            }

            @Override
            public float toGBytes(float s) {
                return s / C1;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        GBYTES {
            @Override
            public long toBytes(float s) {
                return (long) (s * C3);
            }

            @Override
            public float toKBytes(float s) {
                return s * C2;
            }

            @Override
            public float toMBytes(float s) {
                return s * C1;
            }

            @Override
            public float toGBytes(float s) {
                return s;
            }

            @Override
            public long toBits(float s) {
                return (long) toBitsFromBytes(s);
            }

            @Override
            public float toKBits(float s) {
                return toBitsFromBytes(toKBytes(s));
            }

            @Override
            public float toMBits(float s) {
                return toBitsFromBytes(toMBytes(s));
            }

            @Override
            public float toGBits(float s) {
                return toBitsFromBytes(toGBytes(s));
            }
        },

        BITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) s;
            }

            @Override
            public float toKBits(float s) {
                return s / C1;
            }

            @Override
            public float toMBits(float s) {
                return s / C2;
            }

            @Override
            public float toGBits(float s) {
                return s / C3;
            }
        },

        KBITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) (s * C1);
            }

            @Override
            public float toKBits(float s) {
                return s;
            }

            @Override
            public float toMBits(float s) {
                return s / C2;
            }

            @Override
            public float toGBits(float s) {
                return s / C3;
            }
        },

        MBITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) (s * C2);
            }

            @Override
            public float toKBits(float s) {
                return s * C1;
            }

            @Override
            public float toMBits(float s) {
                return s;
            }

            @Override
            public float toGBits(float s) {
                return s / C1;
            }
        },

        GBITS {
            @Override
            public long toBytes(float s) {
                return (long) toBytesFromBits(s);
            }

            @Override
            public float toKBytes(float s) {
                return toBytesFromBits(toKBits(s));
            }

            @Override
            public float toMBytes(float s) {
                return toBytesFromBits(toMBits(s));
            }

            @Override
            public float toGBytes(float s) {
                return toBytesFromBits(toGBits(s));
            }

            @Override
            public long toBits(float s) {
                return (long) (s * C3);
            }

            @Override
            public float toKBits(float s) {
                return s * C2;
            }

            @Override
            public float toMBits(float s) {
                return s * C1;
            }

            @Override
            public float toGBits(float s) {
                return s;
            }
        };

        public static final long C0 = 8;
        public static final long C1 = 1024L;
        public static final long C2 = C1 * 1024L;
        public static final long C3 = C2 * 1024L;

        public abstract long toBytes(float s);

        public abstract float toKBytes(float s);

        public abstract float toMBytes(float s);

        public abstract float toGBytes(float s);

        public abstract long toBits(float s);

        public abstract float toKBits(float s);

        public abstract float toMBits(float s);

        public abstract float toGBits(float s);

        public boolean isBits() {
            return this == BITS || this == KBITS || this == MBITS || this == GBITS;
        }

        public boolean isBytes() {
            return this == BYTES || this == KBYTES || this == MBYTES || this == GBYTES;
        }

        public static float toBitsFromBytes(float s) {
            return s * C0;
        }

        public static float toBytesFromBits(float s) {
            return s / C0;
        }

        public static float convert(long what, @NonNull SizeUnit from, @NonNull SizeUnit to) {
            final float result;
            switch (to) {
                case BITS:
                    result = from.toBits(what);
                    break;
                case BYTES:
                    result = from.toBytes(what);
                    break;
                case KBITS:
                    result = from.toKBits(what);
                    break;
                case KBYTES:
                    result = from.toKBytes(what);
                    break;
                case MBITS:
                    result = from.toMBits(what);
                    break;
                case MBYTES:
                    result = from.toMBytes(what);
                    break;
                case GBITS:
                    result = from.toGBits(what);
                    break;
                case GBYTES:
                    result = from.toGBytes(what);
                    break;
                default:
                    result = 0f;
                    break;
            }
            return result;
        }
    }


}
