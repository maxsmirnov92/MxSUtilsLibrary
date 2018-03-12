package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.data.model.InstanceManager;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RunnableInfo implements Serializable {

    private static final long serialVersionUID = 1439677745602974231L;

    public static final int NO_ID = -1;

    public final int id;
    public final String name;

    boolean isRunning = false;

    private boolean isCancelled = false;

    public RunnableInfo(int id) {
        this(id, null);
    }

    public RunnableInfo(int id, String name) {

        if (id < 0)
            throw new IllegalArgumentException("incorrect runnable id: " + id);

        this.id = id;
        this.name = !TextUtils.isEmpty(name)? name : getClass().getSimpleName();
    }

    public synchronized boolean isRunning() {
        return isRunning;
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
        return InstanceManager.asByteArray(this);
    }

    public void toOutputStream(@NonNull OutputStream outputStream) {
        InstanceManager.toOutputStream(this, outputStream); // TODO return boolean
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RunnableInfo that = (RunnableInfo) o;

        if (id != that.id) return false;
        if (isCancelled != that.isCancelled) return false;
        return !(name != null ? !name.equals(that.name) : that.name != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (isCancelled ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RunnableInfo{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isRunning=" + isRunning +
                ", isCanceled=" + isCancelled +
                '}';
    }

    @NonNull
    public static <I extends RunnableInfo, T extends TaskRunnable<I>> List<I> fromTasks(@Nullable Collection<T> runnables) {
        List<I> infos = new ArrayList<>();
        if (runnables != null) {
            for (T runnable : runnables) {
                infos.add(runnable.rInfo);
            }
        }
        return infos;
    }

    @NonNull
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
        return InstanceManager.fromByteArray(clazz, byteArray);
    }

    @Nullable
    public static <I extends RunnableInfo> I fromInputStream(Class<I> clazz, InputStream inputStream) {
        return InstanceManager.fromInputStream(clazz, inputStream);
    }

    @Nullable
    public static <I extends RunnableInfo> I findRunnableInfoById(final int id, Collection<I> tasks) {
        if (id < 0) {
            throw new IllegalArgumentException("incorrect id: " + id);
        }
        return Predicate.Methods.find(tasks, new Predicate<I>() {
            @Override
            public boolean apply(I element) {
                return element != null && id == element.id;
            }
        });
    }

    @NonNull
    public static <I extends RunnableInfo> List<I> filter(final Collection<I> what, final Collection<I> by, final boolean contains) {
        return Predicate.Methods.filter(what, new Predicate<I>() {
            @Override
            public boolean apply(I element) {
                I info = element != null? findRunnableInfoById(element.id, by) : null;
                return contains && info != null || !contains && info == null;
            }
        });
    }

    public static void setRunning(@Nullable Collection<? extends RunnableInfo> infos, boolean isRunning) {
        if (infos != null) {
            for (RunnableInfo i : infos) {
                if (i != null) {
                    i.isRunning = isRunning;
                }
            }
        }
    }

    public static void cancel(@Nullable Collection<? extends RunnableInfo> infos) {
        if (infos != null) {
            for (RunnableInfo i : infos) {
                if (i != null) {
                    i.cancel();
                }
            }
        }
    }

}
