package net.maxsmr.tasksutils.storage.ids;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class IdHolder {

    public static final int NO_ID = -1;

    private final AtomicInteger lastId ;

    private final int initialValue;

    public IdHolder(@NotNull IdHolder idHolder) {
        this.initialValue = idHolder.initialValue;
        this.lastId = idHolder.lastId;
    }

    public IdHolder(int initialValue) {
       lastId = new AtomicInteger(this.initialValue = initialValue);
    }

    public int get() {
        return lastId.get();
    }

    public int getAndIncrement() {
        return lastId.incrementAndGet();
    }

    public int incrementAndGet() {
        return lastId.incrementAndGet();
    }

    public void reset() {
        lastId.set(initialValue);
    }
}
