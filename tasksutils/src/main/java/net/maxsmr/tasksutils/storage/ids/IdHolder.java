package net.maxsmr.tasksutils.storage.ids;

import java.util.concurrent.atomic.AtomicInteger;

public class IdHolder {

    private final AtomicInteger lastId ;

    private final int initialValue;

    public IdHolder(int initialValue) {
       lastId = new AtomicInteger(this.initialValue = initialValue);
    }

    public int get() {
        return lastId.get();
    }

    public int incrementAndGet() {
        return lastId.incrementAndGet();
    }

    public void reset() {
        lastId.set(initialValue);
    }
}
