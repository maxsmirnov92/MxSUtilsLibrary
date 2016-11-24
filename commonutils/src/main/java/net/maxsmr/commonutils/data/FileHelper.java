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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static net.maxsmr.commonutils.data.CompareUtils.compareForNull;

public final class FileHelper {

    private final static Logger logger = LoggerFactory.getLogger(FileHelper.class);

    public final static String FILE_EXT_ZIP = "zip";

    public FileHelper() {
        throw new AssertionError("no instances.");
    }

    public static long getPartitionTotalSpaceKb(String path) {
        return isDirExists(path) ? new File(path).getTotalSpace() / 1024L : 0L;
    }

    public static long getPartitionFreeSpaceKb(String path) {
        return isDirExists(path) ? new File(path).getFreeSpace() / 1024L : 0L;
    }

    public static boolean isSizeCorrect(File file) {
        return (file != null && file.length() > 0);
    }

    public static boolean isFileCorrect(File file) {
        return (file != null && file.isFile() && isSizeCorrect(file));
    }

    public static boolean isFileExists(String fileName, String parentPath) {

        if (fileName == null || fileName.length() == 0 || fileName.contains("/")) {
            return false;
        }

        if (parentPath == null || parentPath.length() == 0) {
            return false;
        }

        File parentDir = new File(parentPath);

        if (!(parentDir.exists() && parentDir.isDirectory())) {
            logger.debug("directory " + parentDir.getAbsolutePath() + " not exists or not directory");
            return false;
        }

        File f = new File(parentDir, fileName);
        return (f.exists() && f.isFile());
    }

    public static boolean isFileExists(String filePath) {
        if (filePath == null || filePath.length() == 0) {
            return false;
        }
        File f = new File(filePath);
        return (f.exists() && f.isFile());
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
    public static File createNewFile(String fileName, String parentPath, boolean recreate) {

        if (TextUtils.isEmpty(fileName)) {
            logger.error("file name is empty");
            return null;
        }

        if (TextUtils.isEmpty(parentPath)) {
            logger.error("parent path is empty");
            return null;
        }

        File f = new File(parentPath, fileName);

        if (f.exists() && f.isFile()) {

            if (recreate) {

                if (f.delete()) {

                    f = FileHelper.createNewFile(f.getName(), f.getParent());

                    if (f == null) {
                        logger.error("can't create new file " + parentPath + File.separator + fileName);
                        return null;
                    }

                } else {
                    logger.error("can't delete existing file: " + f);
                    return f;
                }

            } else {
                logger.error("not overwriting, file exists: " + f);
                return f;
            }

        } else {

            f = FileHelper.createNewFile(f.getName(), f.getParent());

            if (f == null) {
                logger.error("can't create new file " + parentPath + File.separator + fileName);
                return null;
            }
        }

        return f;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Nullable
    public static File createNewFile(String fileName, String parentPath) {

        if (TextUtils.isEmpty(fileName) || fileName.contains(File.separator)) {
            logger.error("file name is empty or contains slashes");
            return null;
        }

        if (TextUtils.isEmpty(parentPath)) {
            logger.error("parent path is empty");
            return null;
        }

        File newFile = null;

        File parentDir = new File(parentPath);
        parentDir.mkdirs();

        if (parentDir.exists() && parentDir.isDirectory()) {

            newFile = new File(parentDir, fileName);

            if (newFile.exists() && newFile.isFile()) {
                if (!newFile.delete()) {
                    logger.error("can't delete: " + newFile);
                    return null;
                }
            }

            try {
                if (!newFile.createNewFile()) {
                    logger.error("can't create new file: " + newFile);
                    return null;
                }
            } catch (IOException e) {
                logger.error("an IOException occurred during createNewFile()", e);
                return null;
            }
        } else {
            logger.error("can't create directory: " + parentDir);
        }

        return newFile;
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
        else
            return null;

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

        if (in == null || out == null)
            return false;

        try {
            byte[] buff = new byte[256];

            int len;
            while ((len = in.read(buff)) > 0)
                out.write(buff, 0, len);

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
            logger.debug("a FileNotFoundException occurred", e);
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

        FileReader reader;
        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            logger.debug("a FileNotFoundException occurred", e);
            return lines;
        }

        BufferedReader br = new BufferedReader(reader);

        String line;

        try {

            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            return lines;

        } catch (IOException e) {
            logger.error("an IOException occurred during readLine()", e);

        } finally {
            try {
                br.close();
                reader.close();
            } catch (IOException e) {
                logger.error("an IOException occurred during close()", e);
            }
        }

        return lines;
    }

    @Nullable
    public static String readStringFromFile(File file) {
        List<String> strings = readStringsFromFile(file);
        return !strings.isEmpty() ? TextUtils.join(System.getProperty("line.separator"), strings) : null;
    }

    @NonNull
    public static Collection<String> readStringsFromAsset(@NonNull Context context, String assetName) {
        try {
            return readStringsFromInputStream(context, context.getAssets().open(assetName));
        } catch (IOException e) {
            logger.error("an IOException occurred during open()", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    public static String readStringFromAsset(@NonNull Context context, String assetName) {
        try {
            return readStringFromInputStream(context, context.getAssets().open(assetName));
        } catch (IOException e) {
            logger.error("an IOException occurred during open()", e);
            return null;
        }
    }

    @NonNull
    public static Collection<String> readStringsFromRes(@NonNull Context context, @RawRes int resId) {
        try {
            return readStringsFromInputStream(context, context.getResources().openRawResource(resId));
        } catch (Resources.NotFoundException e) {
            logger.error("an IOException occurred during openRawResource()", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    public static String readStringFromRes(@NonNull Context context, @RawRes int resId) {
        try {
            return readStringFromInputStream(context, context.getResources().openRawResource(resId));
        } catch (Resources.NotFoundException e) {
            logger.error("an IOException occurred during openRawResource()", e);
            return null;
        }
    }

    @NonNull
    public static Collection<String> readStringsFromInputStream(@NonNull Context context, @Nullable InputStream is) {
        if (is != null) {
            BufferedReader in = null;
            try {
                List<String> out = new ArrayList<>();
                in = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    out.add(line);
                }
                return Collections.unmodifiableCollection(out);
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
    public static String readStringFromInputStream(@NonNull Context context, @Nullable InputStream is) {
        Collection<String> strings = readStringsFromInputStream(context, is);
        return !strings.isEmpty() ? TextUtils.join(System.getProperty("line.separator"), strings) : null;
    }

    @Nullable
    private static File createFile(String fileName, String parentPath, boolean append) {
        final File file;

        if (!append) {
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

    /** */
    @Nullable
    public static File writeBytesToFile(byte[] data, String fileName, String parentPath, boolean append) {
        logger.debug("writeBytesToFile(), fileName=" + fileName + ", parentPath=" + parentPath + ", append=" + append);

        if (data == null) {
            data = new byte[0];
        }

        final File file = createFile(fileName, parentPath, append);

        if (file == null) {
            logger.error("can't create file " + parentPath + File.separator + fileName);
            return null;
        }

        if (!file.canWrite()) {
            logger.error("can't write to file: " + file);
            return file;
        }

        FileOutputStream fos = null;

        try {

            fos = new FileOutputStream(file, append);
            fos.write(data);
            fos.flush();
            fos.close();
            return file;

        } catch (IOException e) {
            logger.error("an IOException occurred", e);

        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                logger.error("an IOException occurred during close", e);
            }
        }

        return null;
    }

    @Nullable
    public static File writeFromStreamToFile(InputStream data, String fileName, String parentPath, boolean append) {
        logger.debug("writeFromStreamToFile(), data=" + data + ", fileName=" + fileName + ", parentPath=" + parentPath + ", append=" + append);

        try {
            if (data == null || data.available() == 0) {
                logger.error("data is null or not available");
                return null;
            }
        } catch (IOException e) {
            logger.error("an IOException occurred ", e);
            return null;
        }

        final File file = createFile(fileName, parentPath, append);

        if (file == null) {
            return null;
        }

        if (!file.canWrite()) {
            logger.error("can't write to file: " + file);
            return file;
        }

        try {
            revectorStream(data, new FileOutputStream(file));
            return file;
        } catch (FileNotFoundException e) {
            logger.error("an IOException occurred ", e);
        }

        return null;
    }

    public static File writeStringToFile(String data, String fileName, String parentPath, boolean append) {

        if (data == null) {
            data = "";
        }

        final File file = createFile(fileName, parentPath, append);

        if (file == null) {
            logger.error("can't create file " + parentPath + File.separator + fileName);
            return null;
        }

        if (!file.canWrite()) {
            logger.error("can't write to file: " + file);
            return file;
        }

        FileWriter writer = null;

        try {
            writer = new FileWriter(file, append);
            writer.write(data);
            writer.flush();
            return file;

        } catch (IOException e) {
            logger.error("an IOException occurred", e);

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("an IOException occurred during close()", e);
                }
            }
        }

        return null;
    }

    public static File writeStringsToFile(Collection<String> strings, String fileName, String parentPath, boolean append) {

        if (strings == null) {
            strings = new ArrayList<>();
        }

        final File file = createFile(fileName, parentPath, append);

        if (file == null) {
            return null;
        }

        if (!file.canWrite()) {
            logger.error("can't write to file: " + file);
            return file;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            logger.debug("an IOException occurred", e);
            return file;
        }

        BufferedWriter bw = new BufferedWriter(writer);

        try {
            for (String line : strings) {
                bw.append(line);
                bw.flush();
            }
        } catch (IOException e) {
            logger.error("an IOException occurred during write", e);
        } finally {
            try {
                bw.close();
            } catch (IOException e) {
                logger.error("an IOException occurred during close()", e);
            }
        }

        return file;
    }

    @Nullable
    public static File compressFilesToZip(Collection<File> srcFiles, String destZipName) {

        if (srcFiles == null || srcFiles.isEmpty()) {
            logger.error("srcFiles is null or empty");
            return null;
        }

        if (TextUtils.isEmpty(destZipName)) {
            logger.error("destZipName is null or empty");
            return null;
        }

        try {
            OutputStream os = new FileOutputStream(destZipName);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));

            try {
                int zippedFiles = 0;

                for (File srcFile : srcFiles) {

                    if (!isFileCorrect(srcFile)) {
                        logger.error("incorrect file to zip: " + srcFile);
                        continue;
                    }

                    byte[] bytes = readBytesFromFile(srcFile);

                    if (bytes == null) {

                    }

                    ZipEntry entry = new ZipEntry(srcFile.getName());
                    zos.putNextEntry(entry);
                    zos.write(bytes);
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
    public static String getFileExtension(String fileName) {
        String[] fileNameSplit = fileName.split("\\.");
        if (fileNameSplit.length == 0) {
            return "";
        }
        return fileNameSplit[fileNameSplit.length - 1];
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

    /**
     * not adding source collection
     *
     * @param fromDirs directories
     * @return collected set of files or directories from specified directories
     */
    @NonNull
    public static Set<File> getFiles(@NonNull Collection<File> fromDirs, @NonNull GetMode getMode, boolean recursive, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier) {

        final Set<File> collected = new LinkedHashSet<>();

        for (File fromDir : fromDirs) {

            if (fromDir == null || !isDirExists(fromDir.getAbsolutePath())) {
                logger.debug("directory " + fromDir + " not exists");
                continue;
            }

            if (notifier != null) {
                if (!notifier.onProcessing(fromDir, Collections.unmodifiableSet(collected))) {
                    break;
                }
            }

//            if (getMode == GetMode.FOLDERS || getMode == GetMode.ALL) {
//                if (notifier == null || notifier.onGet(fromDir)) {
//                    collected.add(fromDir);
//                }
//            }

            File[] files = fromDir.listFiles();

            if (files != null) {
                for (File f : files) {

                    if (notifier != null) {
                        if (!notifier.onProcessing(f, Collections.unmodifiableSet(collected))) {
                            break;
                        }
                    }

                    if (f.isDirectory()) {
                        if (getMode == GetMode.FOLDERS || getMode == GetMode.ALL) {
                            if (notifier == null || notifier.onGetFolder(f)) {
                                collected.add(f);
                            }
                        }
                        if (recursive) {
                            collected.addAll(getFiles(Collections.singleton(f), getMode, true, comparator, notifier));
                        }

                    } else if (f.isFile()) {
                        if (getMode == GetMode.FILES || getMode == GetMode.ALL) {
                            if (notifier == null || notifier.onGetFile(f)) {
                                collected.add(f);
                            }
                        }
                    } else {
                        logger.error("incorrect file or folder: " + f);
                    }
                }
            }
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(collected);
            Collections.sort(sortedList, comparator);
            collected.clear();
            collected.addAll(sortedList);
        }

        return collected;
    }

    /**
     * @param comparator to sort each folders list and result set
     * @return found set of files or directories with matched name
     */
    @NonNull
    public static Set<File> searchByName(String name, @NonNull Collection<File> searchFiles, @NonNull GetMode getMode, int searchFlags, boolean recursiveSearch, boolean searchInGiven, @Nullable Comparator<? super File> comparator, @Nullable ISearchNotifier notifier) {

        Set<File> foundFiles = new LinkedHashSet<>();

        for (File root : searchFiles) {

            if (root == null || !root.exists()) {
                continue;
            }

            if (notifier != null) {
                if (!notifier.onProcessing(root, Collections.unmodifiableSet(foundFiles))) {
                    break;
                }
            }

            if (root.isDirectory()) {

                if (searchInGiven) {
                    if (getMode == GetMode.FOLDERS || getMode == GetMode.ALL) {
                        if (CompareUtils.stringMatches(root.getName(), name, searchFlags)) {
                            if (notifier == null || notifier.onFoundFolder(root)) {
                                foundFiles.add(root);
                            }
                        }
                    }
                }

                File[] files = root.listFiles();

                if (files != null) {

                    if (comparator != null) {
                        List<File> sorted = new ArrayList<>(Arrays.asList(files));
                        Collections.sort(sorted, comparator);
                        files = sorted.toArray(new File[sorted.size()]);
                    }

                    for (File f : files) {

                        if (notifier != null) {
                            if (!notifier.onProcessing(f, Collections.unmodifiableSet(foundFiles))) {
                                break;
                            }
                        }

                        if (f.isDirectory()) {
                            if (getMode == GetMode.FOLDERS || getMode == GetMode.ALL) {
                                if (CompareUtils.stringMatches(f.getName(), name, searchFlags)) {
                                    if (notifier == null || notifier.onFoundFolder(f)) {
                                        foundFiles.add(f);
                                    }
                                }
                            }
                            if (recursiveSearch) {
                                final Set<File> recursiveFound = searchByName(name, Collections.singleton(f), getMode, searchFlags, true, false, comparator, notifier);
                                if (!recursiveFound.isEmpty()) {
                                    foundFiles.addAll(recursiveFound);
                                }
                            }
                        } else if (f.isFile()) {
                            if (getMode == GetMode.FILES || getMode == GetMode.ALL) {
                                if (CompareUtils.stringMatches(f.getName(), name, searchFlags)) {
                                    if (notifier == null || notifier.onFoundFile(f)) {
                                        foundFiles.add(f);
                                    }
                                }
                            }
                        } else {
                            logger.error("incorrect file or folder: " + f);
                        }
                    }
                }

            } else if (root.isFile()) {

                if (searchInGiven) {
                    if (getMode == GetMode.FILES || getMode == GetMode.ALL) {
                        if (CompareUtils.stringMatches(root.getName(), name, searchFlags)) {
                            if (notifier == null || notifier.onFoundFile(root)) {
                                foundFiles.add(root);
                            }
                        }
                    }
                }

            } else {
                logger.error("incorrect file or folder: " + root);
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

    @Nullable
    public static File searchByNameFirst(String name, @NonNull Collection<File> searchFiles, @NonNull GetMode getMode, int searchFlags, boolean recursiveSearch, boolean searchInGiven, @Nullable Comparator<? super File> comparator, @Nullable final ISearchNotifier notifier) {
        Set<File> found = searchByName(name, searchFiles, getMode, searchFlags, recursiveSearch, searchInGiven, comparator, new ISearchNotifier() {
            @Override
            public boolean onProcessing(@NonNull File current, @NonNull Set<File> found) {
                return (notifier == null || notifier.onProcessing(current, found)) && found.size() == 0;
            }

            @Override
            public boolean onFoundFile(@NonNull File file) {
                return notifier == null || notifier.onFoundFile(file);
            }

            @Override
            public boolean onFoundFolder(@NonNull File folder) {
                return notifier == null || notifier.onFoundFolder(folder);
            }
        });
        return !found.isEmpty() ? new ArrayList<>(found).get(0) : null;
    }

    @NonNull
    public static Set<File> searchByNameWithStat(final String name, @Nullable Comparator<? super File> comparator, @Nullable ISearchNotifier notifier) {
        return searchByNameWithStat(name, null, comparator, notifier);
    }

    /**
     * @param name        file or folder name part
     * @param searchFiles if null or empty, 'PATH' environment variable will be used
     */
    @NonNull
    public static Set<File> searchByNameWithStat(final String name, @Nullable Collection<File> searchFiles, @Nullable Comparator<? super File> comparator, @Nullable ISearchNotifier notifier) {

        final Set<File> foundFiles = new LinkedHashSet<>();

        if (searchFiles == null || searchFiles.isEmpty()) {
            searchFiles = getEnvPathFiles();
        }

        for (File file : searchFiles) {

            if (file == null) {
                continue;
            }

            String path = file.getAbsolutePath();

            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }

            final String currentPath = path;

            ShellUtils.execProcess(Arrays.asList("stat", currentPath + name), null, new ShellUtils.ShellCallback() {

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
                    if (!notifier.onProcessing(file, Collections.unmodifiableSet(foundFiles))) {
                        break;
                    }
                }

                String path = file.getAbsolutePath();

                if (!path.endsWith(File.separator)) {
                    path += File.separator;
                }

                file = new File(path);

                if (notifier != null) {
                    if (file.isFile() && notifier.onFoundFile(file)) {
                        foundFiles.add(file);
                    } else if (file.isDirectory() && notifier.onFoundFolder(file)) {
                        foundFiles.add(file);
                    }
                } else {
                    foundFiles.add(file);
                }
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

    /**
     * @param comparator to sort each folders list
     * @return set of deleted files
     */
    @NonNull
    public static Set<File> delete(@NonNull Collection<File> fromDirs, boolean deleteEmptyDirs, boolean recursive, @Nullable Collection<File> excludeFiles, @Nullable Comparator<? super File> comparator, @Nullable IDeleteNotifier notifier) {

        Set<File> deleted = new LinkedHashSet<>();

        for (File fromDir : fromDirs) {

            if (fromDir == null || !isDirExists(fromDir.getAbsolutePath())) {
                logger.debug("directory " + fromDir + " not exists");
                continue;
            }

            if (notifier != null) {
                if (!notifier.onProcessing(fromDir, Collections.unmodifiableSet(deleted))) {
                    break;
                }
            }

            File[] files = fromDir.listFiles();

            if (files != null) {

                if (comparator != null) {
                    List<File> sorted = new ArrayList<>(Arrays.asList(files));
                    Collections.sort(sorted, comparator);
                    files = sorted.toArray(new File[sorted.size()]);
                }

                for (File f : files) {

                    if (notifier != null) {
                        if (!notifier.onProcessing(f, Collections.unmodifiableSet(deleted))) {
                            break;
                        }
                    }

                    if (excludeFiles == null || !excludeFiles.contains(f)) {
                        if (f.isDirectory()) {
                            if (recursive) {
                                deleted.addAll(delete(Collections.singleton(f), deleteEmptyDirs, true, excludeFiles, comparator, notifier));
                            }
                            if (deleteEmptyDirs) {
                                if (notifier == null || notifier.confirmDeleteFolder(f)) {
                                    if (f.delete()) {
                                        deleted.add(f);
                                    }
                                }
                            }
                        } else if (f.isFile()) {
                            if (notifier == null || notifier.confirmDeleteFile(f)) {
                                if (f.delete()) {
                                    deleted.add(f);
                                }
                            }
                        } else {
                            logger.error("incorrect file or folder: " + f);
                        }
                    }
                }

                File[] remainFiles = fromDir.listFiles();

                if (remainFiles == null || remainFiles.length == 0) {
                    if (deleteEmptyDirs) {
                        if (notifier == null || notifier.confirmDeleteFolder(fromDir)) {
                            if (fromDir.delete()) {
                                deleted.add(fromDir);
                            }
                        }
                    }
                }
            }


        }
        return deleted;
    }

    /**
     * @param howMuch if 0 - deletes all
     * @return deleted files
     */
    public static Set<File> deleteFromCollection(@NonNull Collection<File> files, @NonNull GetMode getMode, boolean deleteFoldersOnlyIfEmpty, boolean fromStart, final int howMuch, @Nullable Collection<File> excludeFiles, @Nullable Comparator<? super File> comparator, @Nullable final IDeleteNotifier notifier) {
        logger.debug("deleteFromCollection(), files=" + files + ", getMode=" + getMode + ", deleteFoldersOnlyIfEmpty=" + deleteFoldersOnlyIfEmpty + ", fromStart=" + fromStart + ", howMuch=" + howMuch + ", excludeFiles=" + excludeFiles);

        final Set<File> deleted = new LinkedHashSet<>();

        if (howMuch < 0) {
            logger.error("incorrect howMuch: " + howMuch);
            return deleted;
        }

        List<File> filesList = new ArrayList<>(files);

        int from = fromStart ? 0 : files.size();
        int to = fromStart ? files.size() : 0;

        for (int i = from; i < to; i++) {

            File file = filesList.get(i);

            if (file != null) {

                if (file.exists()) {

                    if (notifier != null) {
                        if (!notifier.onProcessing(file, Collections.unmodifiableSet(deleted))) {
                            break;
                        }
                    }

                    if (excludeFiles == null || !excludeFiles.contains(file)) {

                        if (file.isFile()) {
                            if (getMode == GetMode.FILES || getMode == GetMode.ALL) {
                                if (notifier == null || notifier.confirmDeleteFile(file)) {
                                    if (file.delete()) {
                                        deleted.add(file);
                                        if (howMuch > 0 && deleted.size() == howMuch) {
                                            break;
                                        }
                                    }
                                }

                            }

                        } else if (file.isDirectory()) {

                            if (getMode == GetMode.FOLDERS || getMode == GetMode.ALL) {
                                File[] listFiles = file.listFiles();
                                if (listFiles != null && listFiles.length > 0) {
                                    if (!deleteFoldersOnlyIfEmpty) {
                                        delete(Collections.singleton(file), true, true, excludeFiles, comparator, new IDeleteNotifier() {
                                            @Override
                                            public boolean onProcessing(@NonNull File current, @NonNull Set<File> deletedThis) {
                                                deleted.addAll(deletedThis);
                                                return true;
                                            }

                                            @Override
                                            public boolean confirmDeleteFile(File file) {
                                                return (notifier == null || notifier.confirmDeleteFile(file)) && (howMuch == 0 || deleted.size() < howMuch);
                                            }

                                            @Override
                                            public boolean confirmDeleteFolder(File folder) {
                                                return false;
                                            }
                                        });
                                    }
                                } else {
                                    if (excludeFiles == null || !excludeFiles.contains(file)) {
                                        if (notifier == null || notifier.confirmDeleteFolder(file)) {
                                            if (file.delete()) {
                                                if (howMuch > 0 && deleted.size() == howMuch) {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.error("incorrect file or folder: " + file);
                        }
                    }
                }
            }
        }

        return deleted;
    }

    /**
     * This function will return size in form of bytes
     * реккурсивно подсчитывает размер папки в байтах
     *
     * @param f файл или папка
     */

    public static long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    size += getFolderSize(file);
                }
            }
        } else if (f.isFile()) {
            size = f.length();
        }
        return size;
    }

    public static String getFolderSizeWithValue(Context context, File file) {
        String value;
        long fileSize = getFolderSize(file) / 1024;//call function and convert bytes into Kb
        if (fileSize >= 1024)
            value = fileSize / 1024 + " " + context.getString(R.string.mb);
        else
            value = fileSize + " " + context.getString(R.string.kb);
        return value;
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

        to = FileHelper.createNewFile(to != null ? to.getName() : null, to != null ? to.getParent() : null, rewrite);

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
    public static File copyFile(File sourceFile, String destName, String destDir, boolean rewrite) {

        if (!isFileCorrect(sourceFile)) {
            logger.error("incorrect source file: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        File destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            logger.error("can't create dest file: " + destDir + File.separator + targetName);
            return null;
        }

        destFile = writeBytesToFile(readBytesFromFile(sourceFile), destFile.getName(), destFile.getParent(), !rewrite);

        if (destFile == null) {
            logger.error("can't write to dest file: " + destDir + File.separator + targetName);
        }

        return destFile;
    }

    /**
     * @return dest file
     */
    public static File copyFileWithBuffering(File sourceFile, String destName, String destDir, boolean rewrite) {

        if (!isFileCorrect(sourceFile)) {
            logger.error("incorrect source file: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        File destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            logger.error("can't create dest file: " + destDir + File.separator + targetName);
            return null;
        }

        try {
            destFile = writeFromStreamToFile(new FileInputStream(sourceFile), destFile.getName(), destFile.getParent(), !rewrite);
            if (destFile == null) {
                logger.error("can't write to dest file" + destDir + File.separator + targetName);
            }
        } catch (FileNotFoundException e) {
            logger.error("a FileNotFoundException occurred", e);
            destFile = null;
        }

        return destFile;
    }

    /**
     * problems with Android 4.2
     */
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

        if (!FileHelper.isFileCorrect(f) || !FileHelper.isPicture(FileHelper.getFileExtension(f.getName()))) {
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
        boolean onProcessing(@NonNull File current, @NonNull Set<File> collected);

        /**
         * @return false if client code doesn't want to append this file to result
         */
        boolean onGetFile(@NonNull File file);


        /**
         * @return false if client code doesn't want to append this folder to result
         */
        boolean onGetFolder(@NonNull File folder);
    }

    public interface ISearchNotifier {

        /**
         * @return false if client code wants to interrupt searching
         */
        boolean onProcessing(@NonNull File current, @NonNull Set<File> found);

        /**
         * @return false if client code doesn't want to append this file to result
         */
        boolean onFoundFile(@NonNull File file);

        /**
         * @return false if client code doesn't want to append this folder to result
         */
        boolean onFoundFolder(@NonNull File folder);
    }

    public interface IDeleteNotifier {

        /**
         * @return false if client code wants to interrupt deleting
         */
        boolean onProcessing(@NonNull File current, @NonNull Set<File> deleted);

        /**
         * @return false if client code doesn't want to delete this file
         */
        boolean confirmDeleteFile(File file);

        /**
         * @return false if client code doesn't want to delete this folder
         */
        boolean confirmDeleteFolder(File folder);
    }


    public static class FileComparator extends AbsOptionableComparator<FileComparator.SortOption, File> {

        public enum SortOption implements ISortOption {

            NAME("name"), SIZE("size"), LAST_MODIFIED("last_modified");

            public final String name;

            SortOption(String name) {
                this.name = name;
            }

            @Override
            public String getName() {
                return name;
            }
        }

        @SuppressWarnings("unchecked")
        public FileComparator(@Nullable Map<FileComparator.SortOption, Boolean> sortOptions) {
            super(sortOptions);
        }

        @Override
        protected int compare(@Nullable File lhs, @Nullable File rhs, @NonNull SortOption option, boolean ascending) {

            int result = compareForNull(lhs, rhs, ascending);

            if (result != 0) {
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


}
