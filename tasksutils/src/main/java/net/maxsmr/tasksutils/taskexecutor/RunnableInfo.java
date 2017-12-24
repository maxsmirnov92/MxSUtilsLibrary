package net.maxsmr.tasksutils.taskexecutor;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.model.InstanceManager;
import net.maxsmr.tasksutils.storage.sync.AbstractSyncStorage;

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

    private boolean isRunning = false;

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

    public synchronized void setRunning(boolean running) {
        isRunning = running;
    }

    public synchronized boolean isCancelled() {
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
        AbstractSyncStorage.toOutputStream(this, outputStream);
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
                ", isCancelled=" + isCancelled +
                '}';
    }

    @NonNull
    public static <I extends RunnableInfo, T extends TaskRunnable<I>> List<I> fromTasks(Collection<T> runnables) {
        List<I> infos = new ArrayList<>();
        for (T runnable : runnables) {
            infos.add(runnable.rInfo);
        }
        return infos;
    }

    public static <I extends RunnableInfo> I fromByteArray(Class<I> clazz, byte[] byteArray) {
        return InstanceManager.fromByteArray(clazz, byteArray);
    }

    public static <I extends RunnableInfo> I fromByteInputStream(Class<I> clazz, InputStream inputStream) {
        return AbstractSyncStorage.fromInputStream(clazz, inputStream);
    }

}
