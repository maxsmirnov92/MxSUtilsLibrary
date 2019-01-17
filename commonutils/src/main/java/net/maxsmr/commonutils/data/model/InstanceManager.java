package net.maxsmr.commonutils.data.model;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class InstanceManager<T> {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(InstanceManager.class);

    protected final File file;

    public InstanceManager(String fileName) {
        FileHelper.checkFile(file = new File(fileName));
    }

    @Nullable
    protected abstract byte[] serializeAsByteArray(T inst);

    @Nullable
    protected abstract String serializeAsString(T inst);

    @Nullable
    protected abstract T deserializeFromByteArray(@NotNull byte[] data);

    @Nullable
    protected abstract T deserializeFromString(String data);

    public void saveAsByteArray(@NotNull T inst) {
        FileHelper.writeBytesToFile(file, serializeAsByteArray(inst), false);
    }

    public void saveAsString(@NotNull T inst) {
        FileHelper.writeStringToFile(file, serializeAsString(inst), false);
    }

    @Nullable
    public T loadFromByteArray() {
        byte[] data = FileHelper.readBytesFromFile(file);
        if (data != null && data.length > 0) {
            return deserializeFromByteArray(data);
        }
        return null;
    }

    @Nullable
    public T loadFromString() {
        List<String> data = FileHelper.readStringsFromFile(file);
        if (data.size() > 0) {
            return deserializeFromString(data.get(0));
        }
        return null;
    }



}
