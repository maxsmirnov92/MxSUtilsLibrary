package net.maxsmr.commonutils.model;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static net.maxsmr.commonutils.FileUtilsKt.createFile;
import static net.maxsmr.commonutils.FileUtilsKt.readBytesFromFile;
import static net.maxsmr.commonutils.FileUtilsKt.readStringsFromFile;
import static net.maxsmr.commonutils.FileUtilsKt.writeBytesToFile;
import static net.maxsmr.commonutils.FileUtilsKt.writeStringsToFile;

public abstract class InstanceManager<T> {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(InstanceManager.class);

    protected final File file;

    public InstanceManager(@NotNull String fileName, @Nullable String parentPath) {
        file = createFile(fileName, parentPath, false);
    }

    @Nullable
    protected abstract byte[] serializeAsByteArray(T inst);

    @Nullable
    protected abstract String serializeAsString(T inst);

    @Nullable
    protected abstract T deserializeFromByteArray(@NotNull byte[] data);

    @Nullable
    protected abstract T deserializeFromString(String data);

    public void saveAsByteArray(@NotNull T instance) {
        writeBytesToFile(file, serializeAsByteArray(instance), false);
    }

    public void saveAsString(@NotNull T instance) {
        writeStringsToFile(file, Collections.singleton(serializeAsString(instance)), false);
    }

    @Nullable
    public T loadFromByteArray() {
        byte[] data = readBytesFromFile(file);
        if (data != null && data.length > 0) {
            return deserializeFromByteArray(data);
        }
        return null;
    }

    @Nullable
    public T loadFromString() {
        List<String> data = readStringsFromFile(file);
        if (data.size() > 0) {
            return deserializeFromString(data.get(0));
        }
        return null;
    }
}
