import androidx.annotation.CallSuper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.LogcatLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.logger.holder.ILoggerHolderProvider;

import org.jetbrains.annotations.NotNull;


@RunWith(JUnit4.class)
public class LoggerTest {

    protected BaseLogger logger;

    protected final Class<?> getLoggerClass() {
        return getClass();
    }

    @Before
    @CallSuper
    public void prepare() {
        Holder.initInstance((ILoggerHolderProvider<Holder>) Holder::new);
        logger = Holder.getInstance().getLogger(getLoggerClass());
    }

    @Test
    public void test() {
        logger.setLoggingEnabled(true);
        logger.d("abc");
        Assert.assertEquals(1, Holder.getInstance().getLoggersCount());
    }

    private static class Holder extends BaseLoggerHolder {

        private Holder() {
            super(false);
        }

        @Override
        protected BaseLogger createLogger(@NotNull String className) {
            return new LogcatLogger(className);
        }
    }
}
