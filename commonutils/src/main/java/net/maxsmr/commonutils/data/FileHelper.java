package net.maxsmr.commonutils.data;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RawRes;
import android.support.media.ExifInterface;
import android.support.v4.util.ArraySet;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.sort.AbsOptionableComparator;
import net.maxsmr.commonutils.data.sort.ISortOption;
import net.maxsmr.commonutils.graphic.GraphicUtils;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.ShellUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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

import static net.maxsmr.commonutils.data.StreamUtils.readBytesFromInputStream;
import static net.maxsmr.commonutils.data.StreamUtils.readStringFromInputStream;
import static net.maxsmr.commonutils.data.StreamUtils.readStringsFromInputStream;
import static net.maxsmr.commonutils.data.StreamUtils.revectorStream;
import static net.maxsmr.commonutils.data.Units.sizeToString;
import static net.maxsmr.commonutils.shell.ShellUtils.execProcess;

public final class FileHelper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(FileHelper.class);

    public final static int DEPTH_UNLIMITED = -1;

    private FileHelper() {
        throw new AssertionError("no instances.");
    }

    public static boolean isExternalStorageMounted() {
        return Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED);
    }

    public static boolean isExternalStorageMountedAndWritable() {
        return isExternalStorageMounted() && !Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED_READ_ONLY);
    }

    @NotNull
    public static Set<File> getExternalFilesDirs(@NotNull Context context) {
        return getExternalFilesDirs(context, null);
    }

    @NotNull
    public static Set<File> getExternalFilesDirs(@NotNull Context context, @Nullable String type) {
        Set<File> result = null;
        if (isExternalStorageMounted()) {
            if (type == null) {
                type = "";
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                result = new ArraySet<>(Arrays.asList(context.getExternalFilesDirs(type)));
            } else {
                result = new ArraySet<>(Collections.singletonList(context.getExternalFilesDir(type)));
            }
        }
        return result != null ? result : Collections.<File>emptySet();
    }

    public static Set<File> getFilteredExternalFilesDirs(@NotNull Context context, boolean includeNotRemovable, boolean includePrimaryExternalStorage, boolean onlyRootNames) {
        return getFilteredExternalFilesDirs(context, null, includeNotRemovable, includePrimaryExternalStorage, onlyRootNames);
    }

    public static Set<File> getFilteredExternalFilesDirs(@NotNull Context context, @Nullable String type, boolean includeNotRemovable, boolean includePrimaryExternalStorage, boolean onlyRootNames) {
        Set<File> result = new ArraySet<>();
        Set<File> external = getExternalFilesDirs(context, type);
        for (File d : external) {
            if (d != null) {
                String path = d.getAbsolutePath();
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP || (includeNotRemovable == !Environment.isExternalStorageRemovable(d))) {
                    File primaryExternalStorage = Environment.getExternalStorageDirectory();
                    if (includePrimaryExternalStorage || (primaryExternalStorage == null || !path.startsWith(primaryExternalStorage.getAbsolutePath()))) {
                        if (onlyRootNames) {
                            int index = path.lastIndexOf(File.separator + "Android" + File.separator + "data");
                            if (index > 0) {
                                d = new File(path.substring(0, index));
                            }
                        }
                        result.add(d);
                    }
                }
            }
        }
        return result;
    }

    public static double getPartitionTotalSpace(String path, @NotNull Units.SizeUnit unit) {
        if (isDirExists(path)) {
            try {
                return Units.SizeUnit.convert(new File(path).getTotalSpace(), Units.SizeUnit.BYTES, unit);
            } catch (SecurityException e) {
                logger.e("a SecurityException occurred during convert(): " + e.getMessage(), e);
            }
        }
        return 0;
    }

    public static double getPartitionFreeSpace(String path, @NotNull Units.SizeUnit unit) {
        if (isDirExists(path)) {
            try {
                return Units.SizeUnit.convert(new File(path).getFreeSpace(), Units.SizeUnit.BYTES, unit);
            } catch (SecurityException e) {
                logger.e("a SecurityException occurred during convert(): " + e.getMessage(), e);
            }
        }
        return 0;
    }

    @Nullable
    public static String getCanonicalPath(File file) {
        if (file != null) {
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                logger.e("an IOException occurred during getCanonicalPath(): " + e.getMessage(), e);
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public static boolean isFileLocked(File f) {
        final FileLock l = lockFileChannel(f, false);
        try {
            return l == null;
        } finally {
            releaseLockNoThrow(l);
        }
    }

    @Nullable
    public static FileLock lockFileChannel(@Nullable File f, boolean blocking) {

        if (!isFileExists(f)) {
            logger.e("File '" + f + "' is not exists");
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
                logger.e("an IOException occurred during tryLock()", e);
            } catch (OverlappingFileLockException e) {
                logger.e("an OverlappingFileLockException occurred during tryLock()", e);
            }

        } catch (FileNotFoundException e) {
            logger.e("a FileNotFoundException occurred during new RandomAccessFile()", e);

        } finally {
            try {
                if (channel != null)
                    channel.close();
                if (randomAccFile != null)
                    randomAccFile.close();
            } catch (IOException e) {
                logger.e("an IOException occurred during close()", e);
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
            logger.e("an IOException occurred during release()", e);
        }
        return false;
    }

    public static boolean isFileCorrect(File file) {
        return isFileExists(file) && file.length() > 0;
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

    public static boolean isFileReadAccessible(@Nullable File file) {
        return isFileExists(file) && file.canRead();
    }

    public static boolean isFileWriteAccessible(@Nullable File file) {
        return isFileExists(file) && file.canWrite();
    }

    public static boolean isDirExists(@Nullable File dir) {
        return dir != null && isDirExists(dir.getAbsolutePath());
    }

    public static boolean isDirExists(@Nullable String dirPath) {
        if (dirPath == null) {
            return false;
        }
        File dir = new File(dirPath);
        return dir.exists() && dir.isDirectory();
    }

    public static boolean isDirReadAccessible(@Nullable File dir) {
        return isDirExists(dir) && dir.canRead();
    }

    public static boolean isDirWriteAccessible(@Nullable File dir) {
        return isDirExists(dir) && dir.canWrite();
    }

    public static boolean isDirEmpty(File dir) {
        if (isDirExists(dir)) {
            File[] files = dir.listFiles();
            return files == null || files.length == 0;
        }
        return false;
    }

    public static void checkFile(File file) {
        checkFile(file, true);
    }

    public static void checkFile(File file, boolean createIfNotExists) {
        if (!checkFileNoThrow(file, createIfNotExists)) {
            throw new IllegalArgumentException("incorrect file: " + file);
        }
    }

    public static void checkFile(String file) {
        checkFile(file, true);
    }

    public static void checkFile(String file, boolean createIfNotExists) {
        if (!checkFileNoThrow(file, createIfNotExists)) {
            throw new IllegalArgumentException("incorrect file: " + file);
        }
    }

    public static boolean checkFileNoThrow(File file) {
        return checkFileNoThrow(file, true);
    }

    public static boolean checkFileNoThrow(File file, boolean createIfNotExists) {
        return file != null && (file.exists() && file.isFile() || (createIfNotExists && createNewFile(file.getName(), file.getParent()) != null));
    }

    public static boolean checkFileNoThrow(String file) {
        return checkFileNoThrow(file, true);
    }

    public static boolean checkFileNoThrow(String file, boolean createIfNotExists) {
        return !TextUtils.isEmpty(file) && checkFileNoThrow(new File(file), createIfNotExists);
    }

    public static void checkDir(String dirPath) {
        checkDir(dirPath, true);
    }

    public static void checkDir(String dirPath, boolean createIfNotExists) {
        if (!checkDirNoThrow(dirPath, createIfNotExists)) {
            throw new IllegalArgumentException("incorrect directory path: " + dirPath);
        }
    }

    public static boolean checkDirNoThrow(String dirPath) {
        return checkDirNoThrow(dirPath, true);
    }

    public static boolean checkDirNoThrow(String dirPath, boolean createIfNotExists) {
        if (!isDirExists(dirPath)) {
            if (!createIfNotExists) {
                return false;
            }
            if (createNewDir(dirPath) == null) {
                return false;
            }
        }
        return true;
    }

    public static File checkPath(String parent, String fileName) {
        return checkPath(parent, fileName, true);
    }

    public static File checkPath(String parent, String fileName, boolean createIfNotExists) {
        File f = checkPathNoThrow(parent, fileName, createIfNotExists);
        if (f == null) {
            throw new IllegalArgumentException("incorrect path: " + parent + File.separator + fileName);
        }
        return f;
    }

    public static File checkPathNoThrow(String parent, String fileName) {
        return checkPathNoThrow(parent, fileName, true);
    }

    public static File checkPathNoThrow(String parent, String fileName, boolean createIfNotExists) {
        if (checkDirNoThrow(parent, createIfNotExists)) {
            if (!TextUtils.isEmpty(fileName)) {
                File f = new File(parent, fileName);
                if (checkFileNoThrow(f, createIfNotExists)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * @return created or existing file
     */
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
            logger.e("can't create file: " + parentPath + File.separator + fileName);
        }

        return file;
    }

    /**
     * @return null if target file already exists and was not recreated
     */
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
            logger.e("an Exception occurred during mkdirs(): " + e.getMessage());
        }

        if (created || isDirExists(parentDir)) {

            newFile = new File(parentDir, fileName);

            if (isFileExists(newFile)) {
                if (recreate && !newFile.delete()) {
                    logger.e("Cannot delete file: " + newFile);
                    newFile = null;
                }
            }

            if (recreate && newFile != null) {
                try {
                    if (!newFile.createNewFile()) {
                        newFile = null;
                    }
                } catch (IOException e) {
                    logger.e("an Exception occurred during createNewFile(): " + e.getMessage());
                    return null;
                }
            }
        }

        return newFile;
    }

    /**
     * @return existing or created empty directory
     */
    @Nullable
    public static File createNewDir(String dirPath) {

        if (TextUtils.isEmpty(dirPath)) {
            logger.e("path is empty");
            return null;
        }

        File dir = new File(dirPath);

        if (dir.isDirectory() && dir.exists())
            return dir;

        if (dir.mkdirs())
            return dir;

        return null;
    }

    @Nullable
    public static File renameFile(File sourceFile, String destinationDir, String newFileName, boolean deleteIfExists, boolean deleteEmptyDirs) {

        if (!isFileExists(sourceFile)) {
            logger.e("Source file not exists: " + sourceFile);
            return null;
        }

        if (TextUtils.isEmpty(newFileName)) {
            logger.e("File name for new file is not specified");
            return null;
        }

        File newFile = null;

        File newDir = createNewDir(destinationDir);
        if (newDir != null) {
            newFile = new File(newDir, newFileName);

            if (!CompareUtils.objectsEqual(newFile, sourceFile)) {

                if (isFileExists(newFile)) {
                    logger.d("Target file " + newFile + " already exists");
                    if (deleteIfExists) {
                        if (!deleteFile(newFile)) {
                            logger.e("Delete file " + newFile + " failed");
                            newFile = null;
                        }
                    } else {
                        logger.w("Not deleting existing file " + newFile);
                        newFile = null;
                    }
                }

                if (newFile != null) {
                    logger.d("Renaming file " + sourceFile + " to " + newFile + "...");
                    if (sourceFile.renameTo(newFile)) {
                        logger.d("File " + sourceFile + " renamed successfully to " + newFile);
                        File sourceParentDir = sourceFile.getParentFile();
                        if (deleteEmptyDirs) {
                            deleteEmptyDir(sourceParentDir);
                        }
                    } else {
                        logger.e("File " + sourceFile + " rename failed to " + newFile);
                        newFile = null;
                    }
                }
            } else {
                logger.e("New file " + newFile + " is same as source file");
            }

        } else {
            logger.e("Create new dir: " + destinationDir + " failed");
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

    @Nullable
    public static byte[] readBytesFromFile(File file) {

        if (!isFileCorrect(file)) {
            logger.e("incorrect file: " + file);
            return null;
        }

        if (!file.canRead()) {
            logger.e("can't read from file: " + file);
            return null;
        }

        try {
            return readBytesFromInputStream(new FileInputStream(file), true);
        } catch (FileNotFoundException e) {
            logger.e("a FileNotFoundException occurred", e);
            return null;
        }
    }

    @NotNull
    public static List<String> readStringsFromFile(File file) {

        List<String> lines = new ArrayList<>();

        if (!isFileCorrect(file)) {
            logger.e("incorrect file: " + file);
            return lines;
        }

        if (!file.canRead()) {
            logger.e("can't read from file: " + file);
            return lines;
        }

        try {
            return readStringsFromInputStream(new FileInputStream(file), true);
        } catch (FileNotFoundException e) {
            logger.e("an IOException occurred", e);
            return lines;
        }
    }

    @Nullable
    public static String readStringFromFile(File file) {
        List<String> strings = readStringsFromFile(file);
        return !strings.isEmpty() ? TextUtils.join(System.getProperty("line.separator"), strings) : null;
    }

    @NotNull
    public static Collection<String> readStringsFromAsset(@NotNull Context context, String assetName) {
        try {
            return readStringsFromInputStream(context.getAssets().open(assetName), true);
        } catch (IOException e) {
            logger.e("an IOException occurred during open()", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    public static String readStringFromAsset(@NotNull Context context, String assetName) {
        try {
            return readStringFromInputStream(context.getAssets().open(assetName), true);
        } catch (IOException e) {
            logger.e("an IOException occurred during open()", e);
            return null;
        }
    }

    @NotNull
    public static Collection<String> readStringsFromRes(@NotNull Context context, @RawRes int resId) {
        try {
            return readStringsFromInputStream(context.getResources().openRawResource(resId), true);
        } catch (Resources.NotFoundException e) {
            logger.e("an IOException occurred during openRawResource()", e);
            return Collections.emptyList();
        }
    }

    @Nullable
    public static String readStringFromRes(@NotNull Context context, @RawRes int resId) {
        try {
            return readStringFromInputStream(context.getResources().openRawResource(resId), true);
        } catch (Resources.NotFoundException e) {
            logger.e("an IOException occurred during openRawResource()", e);
            return null;
        }
    }


    public static boolean writeBytesToFile(@NotNull File file, byte[] data, boolean append) {
        if (data == null || data.length == 0) {
            return false;
        }
        if (!isFileExists(file.getAbsolutePath()) && (file = createNewFile(file.getName(), file.getAbsolutePath(), !append)) == null) {
            return false;
        }
        if (!file.canWrite()) {
            logger.e("can't write to file: " + file);
            return false;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file.getAbsolutePath(), append);
        } catch (FileNotFoundException e) {
            logger.e("an Exception occurred", e);
        }
        if (fos != null) {
            try {
                fos.write(data);
                fos.flush();
                return true;
            } catch (IOException e) {
                logger.e("an Exception occurred", e);
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.e("an Exception occurred", e);
                }
            }
        }
        return false;

    }

    @Nullable
    public static File writeFromStreamToFile(InputStream data, File targetFile, boolean append) {
        return writeFromStreamToFile(data, targetFile, append, null);
    }

    @Nullable
    public static File writeFromStreamToFile(InputStream data, String fileName, String parentPath, boolean append) {
        return writeFromStreamToFile(data, fileName, parentPath, append, null);
    }

    @Nullable
    public static File writeFromStreamToFile(InputStream data, File targetFile, boolean append, StreamUtils.IStreamNotifier notifier) {
        return writeFromStreamToFile(data, targetFile != null ? targetFile.getName() : null, targetFile != null ? targetFile.getParent() : null, append, notifier);
    }

    @Nullable
    public static File writeFromStreamToFile(InputStream data, String fileName, String parentPath, boolean append, StreamUtils.IStreamNotifier notifier) {
        logger.d("writeFromStreamToFile(), data=" + data + ", fileName=" + fileName + ", parentPath=" + parentPath + ", append=" + append);

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
            logger.e("an Exception occurred", e);
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
            logger.e("can't write to file: " + file);
            return false;
        }

        FileWriter writer;
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            logger.d("an IOException occurred", e);
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
            logger.e("an IOException occurred during write", e);
        } finally {
            try {
                bw.close();
            } catch (IOException e) {
                logger.e("an IOException occurred during close()", e);
            }
        }

        return false;
    }

    @Nullable
    public static File compressFilesToZip(Collection<File> srcFiles, String destZipName, String destZipParent, boolean recreate) {

        if (srcFiles == null || srcFiles.isEmpty()) {
            logger.e("source files is null or empty");
            return null;
        }

        File zipFile = createFile(destZipName, destZipParent, recreate);

        if (FileHelper.isFileExists(zipFile)) {
            logger.e("cannot create zip file");
            return null;
        }

        try {
            OutputStream os = new FileOutputStream(destZipName);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));

            try {
                int zippedFiles = 0;

                for (File srcFile : new ArrayList<>(srcFiles)) {

                    if (!isFileCorrect(srcFile)) {
                        logger.e("incorrect file to zip: " + srcFile);
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
                logger.e("an Exception occurred", e);

            } finally {

                try {
                    zos.close();
                    os.close();
                } catch (IOException e) {
                    logger.e("an IOException occurred during close()", e);
                }

            }

        } catch (IOException e) {
            logger.e("an IOException occurred", e);
        }

        return null;
    }

    public static boolean unzipFile(File zipFile, File destPath, boolean saveDirHierarchy) {

        if (!isFileCorrect(zipFile)) {
            logger.e("incorrect zip file: " + zipFile);
            return false;
        }

        if (destPath == null) {
            logger.e("destPath is null");
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

                File path = new File(destPath, entryName);

                if (e.isDirectory()) {
                    if (createNewDir(path.getAbsolutePath()) == null) {
                        logger.e("can't create directory: " + path);
                        return false;
                    }

                } else {
                    if (createNewFile(path.getName(), path.getParent()) == null) {
                        logger.e("can't create new file: " + path);
                        return false;
                    }

                    zis = zip.getInputStream(e);
                    fos = new FileOutputStream(path);

                    if (!revectorStream(zis, fos)) {
                        logger.e("revectorStream() failed");
                        return false;
                    }

                    zis.close();
                    fos.close();
                }
            }

        } catch (IOException e) {
            logger.e("an IOException occurred", e);
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
                logger.e("an IOException occurred during close()", e);
            }
        }

        return true;
    }

    @NotNull
    public static String getFileExtension(@Nullable File file) {
        return getFileExtension(file != null? file.getName() : null);
    }

    @NotNull
    public static String getFileExtension(@Nullable String name) {
        if (name == null) {
            name = "";
        }
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
    public static Collection<File> sortFiles(Collection<File> files, boolean allowModifyCollection, @NotNull Comparator<? super File> comparator) {

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

    @NotNull
    public static Set<File> getFiles(Collection<File> fromFiles, @NotNull GetMode mode, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
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
    @NotNull
    public static Set<File> getFiles(File fromFile, @NotNull GetMode mode, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
        return getFiles(fromFile, mode, comparator, notifier, depth, 0, null);
    }

    @NotNull
    private static Set<File> getFiles(File fromFile, @NotNull GetMode mode,
                                      @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier,
                                      int depth, int currentLevel, @Nullable Set<File> collected) {

        final Set<File> result = new LinkedHashSet<>();

        if (collected == null) {
            collected = new LinkedHashSet<>();
        }

        if (fromFile != null && fromFile.exists()) {

            boolean shouldBreak = false;

            if (notifier != null) {
                if (!notifier.onProcessing(fromFile, Collections.unmodifiableSet(collected), currentLevel)) {
                    shouldBreak = true;
                }
            }

            if (!shouldBreak) {

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
                                    result.addAll(getFiles(f, mode, comparator, notifier, depth, currentLevel + 1, collected));
                                }

                            } else if (f.isFile()) {
                                result.addAll(getFiles(f, mode, comparator, notifier, depth, currentLevel, collected));
                            } else {
                                logger.e("incorrect file or folder: " + f);
                            }
                        }
                    }
                } else if (!fromFile.isFile()) {
                    logger.e("incorrect file or folder: " + fromFile);
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
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(result);
            Collections.sort(sortedList, comparator);
            result.clear();
            result.addAll(sortedList);
        }

        return result;
    }

    @NotNull
    public static Set<File> searchByName(String name, Collection<File> searchFiles, @NotNull GetMode mode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
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
    @NotNull
    public static Set<File> searchByName(String name, File searchFile, @NotNull GetMode mode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier, int depth) {
        return searchByName(name, searchFile, mode, searchFlags, comparator, notifier, depth, 0, null);
    }

    @NotNull
    private static Set<File> searchByName(String name, File searchFile, @NotNull GetMode mode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier,
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
                                result.addAll(searchByName(name, f, mode, searchFlags, comparator, notifier, depth, currentLevel + 1, foundFiles));
                            }
                        } else if (f.isFile()) {
                            result.addAll(searchByName(name, f, mode, searchFlags, comparator, notifier, depth, currentLevel, foundFiles));
                        } else {
                            logger.e("incorrect file or folder: " + f);
                        }

                    }
                }

            } else if (!searchFile.isFile()) {
                logger.e("incorrect file or folder: " + searchFile);
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

    @NotNull
    public static Set<File> searchByNameFirst(String name, Collection<File> searchFiles, @NotNull GetMode getMode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable final IGetNotifier notifier, int depth) {
        Set<File> collected = new LinkedHashSet<>();
        for (File file : searchFiles) {
            collected.addAll(searchByName(name, file, getMode, searchFlags, comparator, notifier, depth));
        }
        return collected;
    }

    @Nullable
    public static File searchByNameFirst(String name, File searchFile, @NotNull GetMode getMode, int searchFlags, @Nullable Comparator<? super File> comparator, @Nullable final IGetNotifier notifier, int depth) {
        Set<File> found = searchByName(name, searchFile, getMode, searchFlags, comparator, new IGetNotifier() {
            @Override
            public boolean onProcessing(@NotNull File current, @NotNull Set<File> found, int currentLevel) {
                return (notifier == null || notifier.onProcessing(current, found, currentLevel)) && found.size() == 0;
            }

            @Override
            public boolean onGetFile(@NotNull File file) {
                return notifier == null || notifier.onGetFile(file);
            }

            @Override
            public boolean onGetFolder(@NotNull File folder) {
                return notifier == null || notifier.onGetFolder(folder);
            }
        }, depth);
        return !found.isEmpty() ? new ArrayList<>(found).get(0) : null;
    }

    @NotNull
    public static Set<File> searchByNameWithStat(final String name, @Nullable Comparator<? super File> comparator, @Nullable IGetNotifier notifier) {
        return searchByNameWithStat(name, null, comparator, notifier);
    }

    /**
     * @param name        file or folder name part
     * @param searchFiles if null or empty, 'PATH' environment variable will be used
     */
    @NotNull
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
                    logger.e("processStartFailed(), t=" + t);
                }

                @Override
                public void shellOut(@NotNull StreamType from, String shellLine) {
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

    public static boolean deleteEmptyDir(File dir) {
        return isDirEmpty(dir) && dir.delete();
    }

    public static boolean deleteFile(File file) {
        return isFileExists(file) && file.delete();
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

    @NotNull
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
    @NotNull
    public static Set<File> delete(File fromFile, boolean deleteEmptyDirs, @Nullable Collection<File> excludeFiles, @Nullable Comparator<? super File> comparator, @Nullable IDeleteNotifier notifier, int depth) {
        return delete(fromFile, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, 0, null);
    }

    @NotNull
    private static Set<File> delete(File fromFile, boolean deleteEmptyDirs, @Nullable Collection<File> excludeFiles, @Nullable Comparator<? super File> comparator, @Nullable IDeleteNotifier notifier,
                                    int depth, int currentLevel, @Nullable Set<File> deletedFiles) {

        Set<File> result = new LinkedHashSet<>();

        if (deletedFiles == null) {
            deletedFiles = new LinkedHashSet<>();
        }

        if (fromFile != null && fromFile.exists()) {

            boolean shouldBreak = false;

            if (notifier != null) {
                if (!notifier.onProcessing(fromFile, Collections.unmodifiableSet(deletedFiles), currentLevel)) {
                    shouldBreak = true;
                }
            }

            if (!shouldBreak) {

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
                                        result.addAll(delete(f, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, currentLevel + 1, deletedFiles));
                                    }
                                    if (deleteEmptyDirs && isDirEmpty(f)) {
                                        if (notifier == null || notifier.confirmDeleteFolder(f)) {
                                            if (f.delete()) {
                                                result.add(f);
                                                deletedFiles.add(f);
                                            } else if (notifier != null) {
                                                notifier.onDeleteFolderFailed(f);
                                            }
                                        }
                                    }
                                } else if (f.isFile()) {
                                    result.addAll(delete(f, deleteEmptyDirs, excludeFiles, comparator, notifier, depth, currentLevel, deletedFiles));
                                } else {
                                    logger.e("incorrect file or folder: " + f);
                                }
                            }
                        }

                        if (deleteEmptyDirs && isDirEmpty(fromFile)) {
                            if (notifier == null || notifier.confirmDeleteFolder(fromFile)) {
                                if (fromFile.delete()) {
                                    result.add(fromFile);
                                    deletedFiles.add(fromFile);
                                } else if (notifier != null) {
                                    notifier.onDeleteFolderFailed(fromFile);
                                }
                            }
                        }
                    }
                } else if (fromFile.isFile()) {

                    if (notifier == null || notifier.confirmDeleteFile(fromFile)) {
                        if (fromFile.delete()) {
                            result.add(fromFile);
                            deletedFiles.add(fromFile);
                        } else if (notifier != null) {
                            notifier.onDeleteFileFailed(fromFile);
                        }
                    }

                } else {
                    logger.e("incorrect file or folder: " + fromFile);
                }
            }
        }

        return result;
    }

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
                        size += getSize(file, depth, currentLevel + 1);
                    }
                }
            }
        } else if (f.isFile()) {
            size = f.length();
        }
        return size;
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
     * Copies a raw resource file, given its ID to the given location
     *
     * @param ctx  context
     * @param mode file permissions (E.g.: "755")
     */
    public static boolean copyRawFile(Context ctx, @RawRes int resId, File destFile, int mode) {
        logger.d("copyRawFile(), resId=" + resId + ", destFile=" + destFile + ", mode=" + mode);

        final String destFilePath = destFile.getAbsolutePath();

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(destFile);
        } catch (FileNotFoundException e) {
            logger.e("a FileNotFoundException occurred during open stream", e);
        }

        boolean result = false;

        if (out != null) {
            result = revectorStream(ctx.getResources().openRawResource(resId), out);

            if (result && mode > 0) {
                return ShellUtils.execProcess(Arrays.asList("chmod", String.valueOf(mode), destFilePath), null, null, null).isSuccessful();
            }
        }

        return result;
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
            logger.e("an IOException occurred", e);
            return false;
        } finally {
            try {
                if (out != null)
                    out.close();
                if (in != null)
                    in.close();
            } catch (IOException e) {
                logger.e("an IOException occurred during close()", e);
            }
        }
    }

    /**
     * @return dest file
     */
    public static File copyFile(File sourceFile, String destName, String destDir, boolean rewrite, boolean preserveFileDate) {

        if (!isFileCorrect(sourceFile)) {
            logger.e("source file not exists: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        File destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            logger.e("can't create dest file: " + destDir + File.separator + targetName);
            return null;
        }

        if (writeBytesToFile(destFile, readBytesFromFile(sourceFile), !rewrite)) {
            if (preserveFileDate) {
                destFile.setLastModified(sourceFile.lastModified());
            }
            return destFile;
        } else {
            logger.e("can't write to dest file: " + destDir + File.separator + targetName);
        }


        return null;
    }

    /**
     * @return dest file
     */
    @Nullable
    public static File copyFileWithBuffering(final File sourceFile, String destName, String destDir, boolean rewrite, boolean preserveFileDate,
                                             @Nullable final ISingleCopyNotifier notifier) {

        if (!isFileExists(sourceFile)) {
            logger.e("source file not exists: " + sourceFile);
            return null;
        }

        String targetName = TextUtils.isEmpty(destName) ? sourceFile.getName() : destName;

        File destFile = destDir != null && !TextUtils.isEmpty(targetName) ? new File(destDir, targetName) : null;

        if (destFile == null || destFile.equals(sourceFile)) {
            logger.e("Incorrect destination file: " + destDir + " (source file: " + sourceFile + ")");
            return null;
        }

        destFile = createNewFile(targetName, destDir, rewrite);

        if (destFile == null) {
            logger.e("Can't create destination file: " + destDir + File.separator + targetName);
            return null;
        }

        final long totalBytesCount = sourceFile.length();

        try {
            File finalDestFile = destFile;
            if (writeFromStreamToFile(new FileInputStream(sourceFile), destFile.getName(), destFile.getParent(), !rewrite, notifier != null ? new StreamUtils.IStreamNotifier() {
                @Override
                public long notifyInterval() {
                    return notifier.notifyInterval();
                }

                @Override
                public boolean onProcessing(@NotNull InputStream inputStream, @NotNull OutputStream outputStream, long bytesWrite, long bytesLeft) {
                    return notifier.onProcessing(sourceFile, finalDestFile, bytesWrite, totalBytesCount);
                }
            } : null) != null) {
                if (preserveFileDate) {
                    if (!destFile.setLastModified(sourceFile.lastModified())) {
                        logger.e("Can't set last modified on destination file: " + destFile);
                    }
                }
                return destFile;
            }
        } catch (FileNotFoundException e) {
            logger.e("an Exception occurred", e);
        }

        return null;
    }

    /**
     * @param fromFile file or directory
     */
    @NotNull
    @Deprecated
    public static Set<File> copyFilesWithBuffering(File fromFile, File destDir,
                                                   @Nullable Comparator<? super File> comparator,
                                                   @Nullable final ISingleCopyNotifier singleNotifier, @Nullable final IMultipleCopyNotifier multipleCopyNotifier,
                                                   boolean preserveFileDate, int depth) {
        return copyFilesWithBuffering(fromFile, destDir, comparator, singleNotifier, multipleCopyNotifier, preserveFileDate, depth, 0, 0, null, null);
    }

    @NotNull
    @Deprecated
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
                        public boolean onProcessing(@NotNull File current, @NotNull Set<File> collected, int currentLevel) {
                            return multipleCopyNotifier.onCalculatingSize(current, collected, currentLevel);
                        }

                        @Override
                        public boolean onGetFile(@NotNull File file) {
                            return true;
                        }

                        @Override
                        public boolean onGetFolder(@NotNull File folder) {
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

//                            if (currentLevel >= 1) {
//                                String tmpPath = destDir.getAbsolutePath();
//                                int index = tmpPath.lastIndexOf(File.separator);
//                                if (index > 0 && index < tmpPath.length() - 1) {
//                                    tmpPath = tmpPath.substring(0, index);
//                                }
//                                destDir = new File(tmpPath);
//                            }

                            if (f.isDirectory()) {
                                if (depth == DEPTH_UNLIMITED || depth > currentLevel) {
                                    result.addAll(copyFilesWithBuffering(f, /*new File(destDir + File.separator + fromFile.getName(), f.getName())*/ destDir, comparator,
                                            singleNotifier, multipleCopyNotifier, preserveFileDate, depth, currentLevel + 1, totalFilesCount, copied, exclusionList));
                                }
                            } else {
                                result.addAll(copyFilesWithBuffering(f, /*new File(destDir, fromFile.getName()) */ destDir, comparator,
                                        singleNotifier, multipleCopyNotifier, preserveFileDate, depth, currentLevel, totalFilesCount, copied, exclusionList));
                            }
                        }

                    }

                    if (files == null || files.length == 0) {
                        String emptyDir = currentLevel == 0 ? destDir + File.separator + fromFile.getName() : destDir.getAbsolutePath();
                        if (!isDirExists(emptyDir)) {
                            createNewDir(emptyDir);
                        }
                    }
                } else if (isFileExists(fromFile)) {

                    File destFile = null;

                    boolean confirmCopy = true;

                    if (multipleCopyNotifier != null) {
                        confirmCopy = multipleCopyNotifier.confirmCopy(fromFile, destDir, currentLevel);
                    }

                    if (confirmCopy) {

                        if (multipleCopyNotifier != null) {
                            destFile = multipleCopyNotifier.onBeforeCopy(fromFile, destDir, currentLevel);
                        }

                        if (destFile == null || destFile.equals(fromFile)) {
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
                logger.e("incorrect file or folder or failed to copy from: " + fromFile);
                if (multipleCopyNotifier != null) {
                    multipleCopyNotifier.onFailed(fromFile, destDir, currentLevel);
                }
            }
        } else {
            logger.e("destination dir is not specified");
        }

        if (comparator != null) {
            List<File> sortedList = new ArrayList<>(result);
            Collections.sort(sortedList, comparator);
            result.clear();
            result.addAll(sortedList);
        }
        return result;
    }

    public static Set<File> copyFilesWithBuffering2(File fromFile, File destDir,
                                                    Comparator<? super File> comparator,
                                                    final ISingleCopyNotifier singleNotifier, final IMultipleCopyNotifier2 multipleCopyNotifier,
                                                    boolean preserveFileDate, int depth,
                                                    List<File> exclusionList) {

        Set<File> result = new LinkedHashSet<>();

        if (destDir != null) {
            destDir = FileHelper.createNewDir(destDir.getAbsolutePath());
        }

        if (destDir == null) {
            logger.e("Can't create destination directory");
            return result;
        }

        if (destDir.equals(fromFile)) {
            logger.e("Destination directory " + destDir + " is same as source directory/file " + fromFile);
            return result;
        }

        final Set<File> files = getFiles(fromFile, GetMode.FILES, comparator, multipleCopyNotifier != null ? new IGetNotifier() {
            @Override
            public boolean onProcessing(@NotNull File current, @NotNull Set<File> collected, int currentLevel) {
                return multipleCopyNotifier.onCalculatingSize(current, collected);
            }

            @Override
            public boolean onGetFile(@NotNull File file) {
                return true;
            }

            @Override
            public boolean onGetFolder(@NotNull File folder) {
                return false;
            }
        } : null, depth);

        if (comparator != null) {
            List<File> sorted = new ArrayList<>(files);
            Collections.sort(sorted, comparator);
            files.clear();
            files.addAll(sorted);
        }

        int filesProcessed = 0;
        for (File f : files) {

            if (!isFileExists(f)) {
                continue;
            }

            File currentDestDir = null;
            if (!f.equals(fromFile)) {
                String part = f.getParent();
                if (part.startsWith(fromFile.getAbsolutePath())) {
                    part = part.substring(fromFile.getAbsolutePath().length(), part.length());
                }
                if (!TextUtils.isEmpty(part)) {
                    currentDestDir = new File(destDir, part);
                }
            }
            if (currentDestDir == null) {
                currentDestDir = destDir;
            }

            if (multipleCopyNotifier != null) {
                if (!multipleCopyNotifier.onProcessing(f, currentDestDir, Collections.unmodifiableSet(result), filesProcessed, files.size())) {
                    break;
                }
            }

            if (exclusionList == null || !exclusionList.contains(f)) {

                File destFile = null;

                boolean confirmCopy = true;

                if (multipleCopyNotifier != null) {
                    confirmCopy = multipleCopyNotifier.confirmCopy(f, currentDestDir);
                }

                if (confirmCopy) {

                    if (multipleCopyNotifier != null) {
                        destFile = multipleCopyNotifier.onBeforeCopy(f, currentDestDir);
                    }

                    if (destFile == null || destFile.equals(f)) {
                        destFile = new File(currentDestDir, f.getName());
                    }

                    boolean rewrite = false;

                    if (multipleCopyNotifier != null && isFileExists(destFile)) {
                        rewrite = multipleCopyNotifier.onExists(destFile);
                    }

                    File resultFile = copyFileWithBuffering(f, destFile.getName(), destFile.getParent(), rewrite,
                            preserveFileDate, singleNotifier);

                    if (resultFile != null) {
                        if (multipleCopyNotifier != null) {
                            multipleCopyNotifier.onSucceeded(f, resultFile);
                        }
                        result.add(resultFile);
                    } else {
                        if (multipleCopyNotifier != null) {
                            multipleCopyNotifier.onFailed(f, currentDestDir);
                        }
                    }
                }
            }

            filesProcessed++;
        }

        if (comparator != null) {
            List<File> sorted = new ArrayList<>(result);
            Collections.sort(sorted, comparator);
            result.clear();
            result.addAll(sorted);
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
                logger.e("an IOException occurred", e);
            } finally {
                if (ra != null) {
                    try {
                        ra.close();
                    } catch (IOException e) {
                        logger.e("an IOException occurred during close()", e);
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    public static Location readExifLocation(File imageFile) {
        if (!GraphicUtils.canDecodeImage(imageFile)) {
            logger.e("incorrect picture file: " + imageFile);
            return null;
        }

        try {

            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());

            String provider = exif.getAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD);

            Location result = new Location(provider);

            String value;

            double latitude = 0d;
            try {
                value = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                if (value != null) {
                    latitude = Location.convert(value);
                }
            } catch (IllegalArgumentException e) {
                logger.e("a IllegalArgumentException occurred", e);
            }

            double longitude = 0d;
            try {
                value = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                if (value != null) {
                    longitude = Location.convert(value);
                }
            } catch (NumberFormatException e) {
                logger.e("a IllegalArgumentException occurred", e);
            }

            double altitude = 0d;
            try {
                value = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                if (value != null) {
                    altitude = Location.convert(value);
                }
            } catch (NumberFormatException e) {
                logger.e("a IllegalArgumentException occurred", e);
            }

            long timestamp = 0L;
            try {
                value = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                if (value != null) {
                    timestamp = Long.valueOf(value);
                }
            } catch (NumberFormatException e) {
                logger.e("a NumberFormatException occurred", e);
            }

            result.setLatitude(latitude);
            result.setLongitude(longitude);
            result.setAltitude(altitude);
            result.setTime(timestamp);

            return result;

        } catch (IOException e) {
            logger.e("an IOException occurred", e);
            return null;
        }
    }

    public static boolean writeExifLocation(File imageFile, Location loc) {

        if (!GraphicUtils.canDecodeImage(imageFile)) {
            logger.e("incorrect picture file: " + imageFile);
            return false;
        }

        if (loc == null) {
            logger.w("location is null");
            return false;
        }

        try {

            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());

            double latitude = loc.getLatitude();
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convertLocationDoubleToString(latitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude > 0 ? "N" : "S");

            double longitude = loc.getLongitude();
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convertLocationDoubleToString(longitude));
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude > 0 ? "E" : "W");

            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, convertLocationDoubleToString((loc.getAltitude())));

            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, String.valueOf(loc.getTime()));

            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, String.valueOf(loc.getProvider()));

            exif.saveAttributes();
            return true;

        } catch (IOException e) {
            logger.e("an IOException occurred", e);
            return false;
        }
    }

    public static String convertLocationDoubleToString(double value) {
        String result = null;

        double aValue = Math.abs(value);
        String dms = Location.convert(aValue, Location.FORMAT_SECONDS);
        String[] splits = dms.split(":");

        if (splits.length >= 3) {

            String[] seconds = (splits[2]).split("\\.");
            String secondsStr;

            if (seconds.length == 0) {
                secondsStr = splits[2];
            } else {
                secondsStr = seconds[0];
            }

            result = splits[0] + "/1," + splits[1] + "/1," + secondsStr + "/1";
        }

        return result;
    }

    public enum GetMode {
        FILES, FOLDERS, ALL
    }

    public interface IGetNotifier {

        /**
         * @return false if client code wants to interrupt collecting
         */
        boolean onProcessing(@NotNull File current, @NotNull Set<File> collected, int currentLevel);

        /**
         * @return false if client code doesn't want to append this file to result
         */
        boolean onGetFile(@NotNull File file);


        /**
         * @return false if client code doesn't want to append this folder to result
         */
        boolean onGetFolder(@NotNull File folder);
    }

    public interface IDeleteNotifier {

        /**
         * @return false if client code wants to interrupt deleting
         */
        boolean onProcessing(@NotNull File current, @NotNull Set<File> deleted, int currentLevel);

        /**
         * @return false if client code doesn't want to delete this file
         */
        boolean confirmDeleteFile(File file);

        /**
         * @return false if client code doesn't want to delete this folder
         */
        boolean confirmDeleteFolder(File folder);

        void onDeleteFileFailed(File file);

        void onDeleteFolderFailed(File folder);
    }

    public interface ISingleCopyNotifier {

        long notifyInterval();

        boolean onProcessing(@NotNull File sourceFile, @NotNull File destFile, long bytesCopied, long bytesTotal);
    }

    public interface IMultipleCopyNotifier {

        boolean onCalculatingSize(@NotNull File current, @NotNull Set<File> collected, int currentLevel);

        boolean onProcessing(@NotNull File currentFile, @NotNull File destDir, @NotNull Set<File> copied, long filesTotal, int currentLevel);

        boolean confirmCopy(@NotNull File currentFile, @NotNull File destDir, int currentLevel);

        File onBeforeCopy(@NotNull File currentFile, @NotNull File destDir, int currentLevel);

        boolean onExists(@NotNull File destFile, int currentLevel);

        void onFailed(@Nullable File currentFile, @NotNull File destFile, int currentLevel);
    }

    public interface IMultipleCopyNotifier2 {

        /**
         * @return false if process should be interrupted
         */
        boolean onCalculatingSize(File current, Set<File> collected);

        /**
         * @return false if process should be interrupted
         */
        boolean onProcessing(File currentFile, File destDir, Set<File> copied, long filesProcessed, long filesTotal);

        /**
         * true if copying confirmed by client code, false to cancel
         */
        boolean confirmCopy(File currentFile, File destDir);

        /**
         * @return target file to copy in or null for default
         */
        File onBeforeCopy(File currentFile, File destDir);

        /**
         * @return true if specified destination file is should be replaced (it currently exists)
         */
        boolean onExists(File destFile);

        void onSucceeded(File currentFile, File resultFile);

        void onFailed(File currentFile, File destDir);
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
        protected int compare(@Nullable File lhs, @Nullable File rhs, @NotNull SortOption option, boolean ascending) {

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

    @NotNull
    public static Map<File, Long> suListFiles(@NotNull Collection<File> fromDirs, @Nullable Comparator<? super File> comparator, @Nullable final ISuGetNotifier notifier) {
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
                    public void shellOut(@NotNull StreamType from, String shellLine) {
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
                        public void shellOut(@NotNull StreamType from, String shellLine) {
                            if (from == StreamType.OUT && !TextUtils.isEmpty(shellLine)) {
                                long size = 0;
                                String[] parts = shellLine.split("\\t");
                                if (parts.length > 1) {
                                    try {
                                        size = Long.parseLong(parts[0]);
                                    } catch (NumberFormatException e) {
                                        logger.e("an NumberFormatException occurred during parseLong(): " + e.getMessage(), e);
                                    }
                                }
                                collectedMap.put(current, Units.SizeUnit.KBYTES.toBytes(size));
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
                            if (exitValue != CommandResult.PROCESS_EXIT_CODE_SUCCESS && notifier != null) {
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

    public static String filesToString(@NotNull Context context, Collection<File> files, int depth) {
        if (files != null) {
            Map<File, Long> map = new LinkedHashMap<>();
            for (File f : files) {
                if (f != null) {
                    map.put(f, getSize(f, depth));
                }
            }
            return filesWithSizeToString(context, map);
        }
        return "";
    }

    public static String filePairsToString(@NotNull Context context, Collection<Pair<File, File>> files, int depth) {
        if (files != null) {
            Map<Pair<File, File>, Long> map = new LinkedHashMap<>();
            for (Pair<File, File> p : files) {
                if (p != null && p.first != null) {
                    map.put(p, getSize(p.first, depth));
                }
            }
            return filePairsWithSizeToString(context, map);
        }
        return "";
    }

    public static String filesWithSizeToString(@NotNull Context context, Map<File, Long> files) {
        return filesWithSizeToString(context, files.entrySet());
    }

    /**
     * @param files file < - > size in bytes
     */
    public static String filesWithSizeToString(@NotNull Context context, Collection<Map.Entry<File, Long>> files) {
        StringBuilder sb = new StringBuilder();
        if (files != null) {
            boolean isFirst = false;
            for (Map.Entry<File, Long> f : files) {
                if (f != null && f.getKey() != null) {
                    if (!isFirst) {
                        isFirst = true;
                    } else {
                        sb.append(System.getProperty("line.separator"));
                    }
                    sb.append(f.getKey().getAbsolutePath());
                    sb.append(": ");
                    final Long size = f.getValue();
                    sb.append(size != null ? sizeToString(context, size, Units.SizeUnit.BYTES) : 0);
                }
            }
        }
        return sb.toString();
    }

    public static String filePairsWithSizeToString(@NotNull Context context, Map<Pair<File, File>, Long> files) {
        return filePairsWithSizeToString(context, files.entrySet());
    }

    /**
     * @param files pair < source file - destination file > <-> size in bytes
     */
    public static String filePairsWithSizeToString(@NotNull Context context, Collection<Map.Entry<Pair<File, File>, Long>> files) {
        StringBuilder sb = new StringBuilder();
        if (files != null) {
            boolean isFirst = false;
            for (Map.Entry<Pair<File, File>, Long> f : files) {
                if (f != null && f.getKey() != null) {
                    final File sourceFile = f.getKey().first;
                    final File destinationFile = f.getKey().second;
                    if (sourceFile != null) {
                        if (!isFirst) {
                            isFirst = true;
                        } else {
                            sb.append(System.getProperty("line.separator"));
                        }
                        sb.append(sourceFile.getAbsolutePath());
                        if (destinationFile != null) {
                            sb.append(" -> ");
                            sb.append(destinationFile.getAbsolutePath());
                        }
                        sb.append(": ");
                        final Long size = f.getValue();
                        sb.append(size != null ? sizeToString(context, size, Units.SizeUnit.BYTES) : 0);
                    }
                }
            }
        }
        return sb.toString();
    }


}
