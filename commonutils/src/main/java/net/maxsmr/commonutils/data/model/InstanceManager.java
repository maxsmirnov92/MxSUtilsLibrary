package net.maxsmr.commonutils.data.model;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public static <T extends Serializable> byte[] toByteArray(@Nullable T object) {
        if (object != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = null;
            try {
                out = new ObjectOutputStream(bos);
                out.writeObject(object);
                return bos.toByteArray();
            } catch (IOException e) {
                logger.e(e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    logger.e(e);
                }
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.e(e);
                }
            }
        }
        return null;
    }

    public static <T extends Serializable> boolean toOutputStream(@Nullable T object, @NotNull OutputStream outputStream) {
        if (object != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput objectOutput = null;
            try {
                objectOutput = new ObjectOutputStream(bos);
                objectOutput.writeObject(object);
                bos.writeTo(outputStream);
                return true;
            } catch (IOException e) {
                logger.e(e);
            } finally {
                try {
                    if (objectOutput != null) {
                        objectOutput.close();
                    }
                    bos.close();
                } catch (IOException e) {
                    logger.e(e);
                }
            }
        }
        return false;
    }

    @Nullable
    public static <T extends Serializable> T fromByteArray(@NotNull Class<T> clazz, @Nullable byte[] data) {
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
                logger.e(e);
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    logger.e(e);
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    logger.e(e);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Serializable> T fromInputStream(@NotNull Class<T> clazz, InputStream inputStream) {
        if (inputStream != null) {
            ObjectInput objectInput = null;
            try {
                objectInput = new ObjectInputStream(inputStream);
                Object o = objectInput.readObject();
                if (clazz.isAssignableFrom(o.getClass())) {
                    return (T) o;
                }
            } catch (Exception e) {
                logger.e(e);
            } finally {
                try {
                    if (objectInput != null) {
                        objectInput.close();
                    }
                } catch (IOException e) {
                    logger.e(e);
                }
            }
        }
        return null;
    }

    @NotNull
    public static <T extends Serializable> Map<T, byte[]> toByteArray(@Nullable Collection<T> listOfObjects) {
        Map<T, byte[]> result = new LinkedHashMap<>();
        if (listOfObjects != null) {
            for (T o : listOfObjects) {
                result.put(o, toByteArray(o));
            }
        }
        return result;
    }

    public static Map<String, byte[]> toByteArrays(@Nullable Collection<String> strings) {
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (strings != null) {
            for (String s : strings) {
                if (s != null) {
                    result.put(s, s.getBytes());
                }
            }
        }
        return result;
    }

    public static long getByteCount(@Nullable Collection<byte[]> bytes) {
        long result = 0;
        if (bytes != null) {
            for (byte[] b : bytes) {
                if (b != null) {
                    result += b.length;
                }
            }
        }
        return result;
    }

}
