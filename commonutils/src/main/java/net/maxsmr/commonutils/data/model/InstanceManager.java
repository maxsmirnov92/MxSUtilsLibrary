package net.maxsmr.commonutils.data.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.base.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

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
    protected abstract T deserializeFromByteArray(@NonNull byte[] data);

    @Nullable
    protected abstract T deserializeFromString(String data);

    @Nullable
    public static <T extends Serializable> byte[] asByteArray(@Nullable T object) {
        if (object != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = null;
            try {
                out = new ObjectOutputStream(bos);
                out.writeObject(object);
                return bos.toByteArray();
            } catch (IOException e) {
                logger.error(e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    logger.error(e);
                }
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        return null;
    }

    public static <T extends Serializable> void toOutputStream(@Nullable T object, @NonNull OutputStream outputStream) {
        if (object != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput objectOutput = null;
            try {
                objectOutput = new ObjectOutputStream(bos);
                objectOutput.writeObject(object);
                bos.writeTo(outputStream);
            } catch (IOException e) {
                logger.error(e);
            } finally {
                try {
                    if (objectOutput != null) {
                        objectOutput.close();
                    }
                    bos.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
    }

    @Nullable
    public static <T extends Serializable> T fromByteArray(@NonNull Class<T> clazz, @Nullable byte[] data) {
        if (data != null && data.length > 0) {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = null;
            try {
                in = new ObjectInputStream(bis);
                Object o = in.readObject();
                if (clazz.isAssignableFrom(o.getClass())) {
                    return (T) o;
                }
            } catch (Exception e) {
                logger.error(e);
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    logger.error(e);
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Serializable> T fromInputStream(@NonNull Class<T> clazz, InputStream inputStream) {
        if (inputStream != null) {
            ObjectInput objectInput = null;
            try {
                objectInput = new ObjectInputStream(inputStream);
                Object o = objectInput.readObject();
                if (clazz.isAssignableFrom(o.getClass())) {
                    return (T) o;
                }
            } catch (Exception e) {
                logger.error(e);
            } finally {
                try {
                    if (objectInput != null) {
                        objectInput.close();
                    }
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        return null;
    }

    public void saveAsByteArray(@NonNull T inst) {
        FileHelper.writeBytesToFile(file, serializeAsByteArray(inst), false);
    }

    public void saveAsString(@NonNull T inst) {
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
