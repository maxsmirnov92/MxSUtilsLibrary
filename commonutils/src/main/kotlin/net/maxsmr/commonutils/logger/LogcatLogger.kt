package net.maxsmr.commonutils.logger

import android.util.Log

class LogcatLogger(tag: String) : BaseLogger(tag) {

    override fun _v(message: String) {
        Log.v(tag, message)
    }

    override fun _v(exception: Throwable) {
        Log.v(tag, exception.message, exception)
    }

    override fun _v(message: String, exception: Throwable?) {
        Log.v(tag, message, exception)
    }

    override fun _d(message: String) {
        Log.d(tag, message)
    }

    override fun _d(exception: Throwable) {
        Log.d(tag, exception.message, exception)
    }

    override fun _d(message: String, exception: Throwable?) {
        Log.d(tag, message, exception)
    }

    override fun _i(message: String) {
        Log.i(tag, message)
    }

    override fun _i(exception: Throwable) {
        Log.i(tag, exception.message, exception)
    }

    override fun _i(message: String, exception: Throwable?) {
        Log.i(tag, message, exception)
    }

    override fun _w(message: String) {
        Log.w(tag, message)
    }

    override fun _w(exception: Throwable) {
        Log.w(tag, exception.message, exception)
    }

    override fun _w(message: String, exception: Throwable?) {
        Log.w(tag, message, exception)
    }

    override fun _e(message: String) {
        Log.e(tag, message)
    }

    override fun _e(exception: Throwable) {
        Log.e(tag, exception.message, exception)
    }

    override fun _e(message: String, exception: Throwable?) {
        Log.e(tag, message, exception)
    }

    override fun _wtf(message: String) {
        Log.wtf(tag, message)
    }

    override fun _wtf(exception: Throwable) {
        Log.wtf(tag, exception.message, exception)
    }

    override fun _wtf(message: String, exception: Throwable?) {
        Log.wtf(tag, message, exception)
    }
}
