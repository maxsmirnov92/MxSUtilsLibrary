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
}
