package net.maxsmr.tasksutils.handler

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper

abstract class HandlerRunnable<T>(
        val handler: Handler = Handler(Looper.getMainLooper())
) : Runnable {

    @Volatile
    private var status = AsyncTask.Status.PENDING

    override fun run() {
        if (status != AsyncTask.Status.PENDING) {
            when (status) {
                AsyncTask.Status.RUNNING -> throw IllegalStateException("Cannot execute task:"
                        + " the task is already running.")
                AsyncTask.Status.FINISHED -> throw IllegalStateException("Cannot execute task:"
                        + " the task has already been executed "
                        + "(a task can be executed only once)")
                else -> throw IllegalStateException("Unknown " + AsyncTask.Status::class.java.simpleName + ": " + status)
            }
        }
        status = AsyncTask.Status.RUNNING
        handler.post { preExecute() }
        val result: T? = doWork()
        handler.post { postExecute(result) }
        status = AsyncTask.Status.FINISHED
    }

    protected abstract fun doWork(): T?

    protected open fun preExecute() {}

    protected open fun postExecute(result: T?) {}
}