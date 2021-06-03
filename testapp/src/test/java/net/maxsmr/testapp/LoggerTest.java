package net.maxsmr.testapp;

import android.content.Context;

import androidx.annotation.CallSuper;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.LogcatLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LoggerTest {

    protected Context context;

    protected BaseLogger logger;

    protected final Class<?> getLoggerClass() {
        return getClass();
    }

    @Before
    @CallSuper
    public void prepare() {
        context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext();
        // may be already initialized in static Application
        Holder.releaseInstance();
        Holder.initInstance(Holder::new);
        logger = Holder.getInstance().getLogger(getLoggerClass());
        logger.setLoggingEnabled(true);
    }

    @Test
    public void test() {
        Assert.assertEquals(1, Holder.getInstance().getLoggersCount());
    }

    private static class Holder extends BaseLoggerHolder {

        @Override
        protected BaseLogger createLogger(@NotNull String className) {
            return new LogcatLogger(className);
        }
    }
}
