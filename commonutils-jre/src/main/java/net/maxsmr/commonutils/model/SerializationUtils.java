package net.maxsmr.commonutils.model;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Map;

import static net.maxsmr.commonutils.FileUtilsKt.checkFile;
import static net.maxsmr.commonutils.FileUtilsKt.openInputStream;
import static net.maxsmr.commonutils.FileUtilsKt.openOutputStream;
import static net.maxsmr.commonutils.FileUtilsKt.resetFile;
import static net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.logException;

// TODO kotlin conversion, orThrow methods variation
public final class SerializationUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(SerializationUtils.class);

    public SerializationUtils() {
        throw new AssertionError("no instances");
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Serializable> T fromByteArray(@NotNull Class<T> clazz, @Nullable byte[] data) {
        if (data != null && data.length > 0) {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInput in = null;
            try {
                in = new ObjectInputStream(bis);
                Object o = in.readObject();
                if (o != null) {
                    if (clazz.isAssignableFrom(o.getClass())) {
                        return (T) o;
                    }
                }
            } catch (Exception e) {
                logException(logger, e);
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    logException(logger, e, "close");
                }
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException e) {
                    logException(logger, e, "close");
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Serializable> T fromInputStream(@NotNull Class<T> clazz, @Nullable InputStream inputStream) {
        if (inputStream != null) {
            ObjectInput objectInput = null;
            try {
                objectInput = new ObjectInputStream(inputStream);
                Object o = objectInput.readObject();
                if (o != null) {
                    if (clazz.isAssignableFrom(o.getClass())) {
                        return (T) o;
                    }
                }
            } catch (Exception e) {
                logException(logger, e);
            } finally {
                try {
                    if (objectInput != null) {
                        objectInput.close();
                    }
                } catch (IOException e) {
                    logException(logger, e, "close");
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Serializable> T fromFile(@NotNull Class<T> clazz, @Nullable File file) {
        FileInputStream stream = openInputStream(file);
        return fromInputStream(clazz, stream);
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
                logException(logger, e);
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    logException(logger, e, "close");
                }
                try {
                    bos.close();
                } catch (IOException e) {
                    logException(logger, e, "close");
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
                byte[] data = toByteArray(o);
                if (data != null) {
                    result.put(o, data);
                }
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

    public static <T extends Serializable> boolean toOutputStream(@Nullable T object, @Nullable OutputStream outputStream) {
        if (object != null && outputStream != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput objectOutput = null;
            try {
                objectOutput = new ObjectOutputStream(bos);
                objectOutput.writeObject(object);
                bos.writeTo(outputStream);
                return true;
            } catch (IOException e) {
                logException(logger, e);
            } finally {
                try {
                    if (objectOutput != null) {
                        objectOutput.close();
                    }
                    bos.close();
                } catch (IOException e) {
                    logException(logger, e, "close");
                }
            }
        }
        return false;
    }

    public static <T extends Serializable> boolean toFile(@Nullable T object, @Nullable File file) {
        return toFile(object, file, true);
    }

    public static <T extends Serializable> boolean toFile(@Nullable T object, @Nullable File file, boolean resetOnFail) {
        if (checkFile(file)) {
            FileOutputStream stream = openOutputStream(file);
            if (toOutputStream(object, stream)) {
                return true;
            } else if (resetOnFail) {
                resetFile(file);
            }
        }
        return false;
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
