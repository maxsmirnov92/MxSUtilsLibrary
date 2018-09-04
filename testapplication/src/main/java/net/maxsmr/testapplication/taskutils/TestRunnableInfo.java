package net.maxsmr.testapplication.taskutils;

import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;


public class TestRunnableInfo extends RunnableInfo {

    public final long delay;

    public TestRunnableInfo(int id, long delay) {
        this(id, null, delay);
    }

    public TestRunnableInfo(int id, String name, long delay) {
        super(id, name);
        if (delay < 0) {
            throw new IllegalArgumentException("incorrect delay: " + delay);
        }
        this.delay = delay;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TestRunnableInfo that = (TestRunnableInfo) o;

        return delay == that.delay;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (delay ^ (delay >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "TestRunnableInfo{" +
                "delay=" + delay +
                ", id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
