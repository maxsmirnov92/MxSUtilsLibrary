package net.maxsmr.tasksutils.taskexecutor;

public class ThrowRunnable implements Runnable {

    private final Throwable e;

    public ThrowRunnable(Throwable e) {
        this.e = e;
    }

    @Override
    public void run()  {
        if (e != null) {
            throw new RuntimeException(e);
        }
    }
}