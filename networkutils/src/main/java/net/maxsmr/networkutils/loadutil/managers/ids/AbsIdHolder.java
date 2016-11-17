package net.maxsmr.networkutils.loadutil.managers.ids;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbsIdHolder {

    private final AtomicInteger lastId ;

    private final int initialValue;

    public AbsIdHolder(int initialValue) {
       lastId = new AtomicInteger(this.initialValue = initialValue);
    }

    public final int get() {
        return lastId.get();
    }

    public int incrementAndGet() {
        return lastId.incrementAndGet();
    }

    public void reset() {
        lastId.set(initialValue);
    }
}
