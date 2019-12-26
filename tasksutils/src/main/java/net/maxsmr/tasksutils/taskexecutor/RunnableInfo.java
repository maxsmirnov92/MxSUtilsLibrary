package net.maxsmr.tasksutils.taskexecutor;

import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.commonutils.data.model.SerializationUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.Status.FINISHED;
import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.Status.PENDING;
import static net.maxsmr.tasksutils.taskexecutor.RunnableInfo.Status.RUNNING;

public class RunnableInfo implements Serializable {

    private static final long serialVersionUID = 1439677745602974231L;

    public static final int NO_ID = -1;

    public final int id;
    public final String name;

    Status status = PENDING;

    private boolean isCancelled = false;

    public RunnableInfo(int id) {
        this(id, null);
    }

    public RunnableInfo(int id, String name) {

        if (id < 0)
            throw new IllegalArgumentException("incorrect runnable id: " + id);

        this.id = id;
        this.name = !StringUtils.isEmpty(name)? name : getClass().getSimpleName();
    }

    @NotNull
    public synchronized Status getStatus() {
        if (status == null) {
            status = PENDING;
        }
        return status;
    }

    public synchronized boolean isRunning() {
        return status == RUNNING;
    }

    public synchronized boolean isFinished() {
        return status == FINISHED;
    }

    public synchronized boolean isCanceled() {
        return isCancelled;
    }

    public synchronized void cancel() {
        isCancelled = true;
    }

    // change implementation
    public boolean isValid() {
        return id >= 0;
    }

    public byte[] toByteArray() {
        return SerializationUtils.toByteArray(this);
    }

    public boolean toOutputStream(@Nullable OutputStream outputStream) {
        return SerializationUtils.toOutputStream(this, outputStream);
    }

    public boolean toFile(@Nullable File file) {
        return SerializationUtils.toFile(this, file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RunnableInfo that = (RunnableInfo) o;

        if (id != that.id) return false;
        if (isCancelled != that.isCancelled) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return status == that.status;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (isCancelled ? 1 : 0);
        return result;
    }

    @NotNull
    public static <I extends RunnableInfo> List<Integer> idsFromInfos(@Nullable Collection<I> infos) {
        List<Integer> ids = new ArrayList<>();
        if (infos != null) {
            for (I info : infos) {
                ids.add(info.id);
            }
        }
        return ids;
    }

    @Nullable
    public static <I extends RunnableInfo> I fromByteArray(Class<I> clazz, byte[] byteArray) {
        return SerializationUtils.fromByteArray(clazz, byteArray);
    }

    @Nullable
    public static <I extends RunnableInfo> I fromInputStream(Class<I> clazz, InputStream inputStream) {
        return SerializationUtils.fromInputStream(clazz, inputStream);
    }

//    @NotNull
//    public static <I extends RunnableInfo> List<I> fromTaskRunnables(@Nullable Collection<? extends TaskRunnable<I, ?, ?>> tasks) {
//        List<I> result = new ArrayList<>();
//        if (tasks != null) {
//            for (TaskRunnable<I, ?, ?> t : tasks) {
//                if (t != null) {
//                    result.add(t.rInfo);
//                }
//            }
//        }
//        return result;
//    }

    @Nullable
    public static <I extends RunnableInfo> I findRunnableInfoById(final int id, @Nullable Collection<I> from) {
        return Predicate.Methods.find(from, element -> element != null && id == element.id);
    }

    @NotNull
    public static <I extends RunnableInfo> List<I> filter(@Nullable final Collection<I> what, @Nullable final Collection<I> by, final boolean contains) {
        return Predicate.Methods.filter(what, element -> {
            I info = element != null? findRunnableInfoById(element.id, by) : null;
            return contains && info != null || !contains && info == null;
        });
    }

    @NotNull
    public static <I extends RunnableInfo> List<I> filter(@Nullable final Collection<I> what, final boolean isCancelled) {
        return Predicate.Methods.filter(what, element -> element != null && element.isCanceled() == isCancelled);
    }

    public static void setRunning(@Nullable Collection<? extends RunnableInfo> what, boolean isRunning) {
        if (what != null) {
            for (RunnableInfo i : what) {
                if (i != null) {
                    i.status = isRunning? RUNNING : PENDING;
                }
            }
        }
    }

    public static void cancel(@Nullable Collection<? extends RunnableInfo> what) {
        if (what != null) {
            for (RunnableInfo i : what) {
                if (i != null) {
                    i.cancel();
                }
            }
        }
    }

    public enum Status {

        PENDING,
        RUNNING,
        FINISHED,
    }
}
