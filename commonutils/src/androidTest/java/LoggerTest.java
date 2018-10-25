import android.support.annotation.CallSuper;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.LogcatLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.logger.holder.ILoggerHolderProvider;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class LoggerTest {

    protected BaseLogger logger;

    protected final Class<?> getLoggerClass() {
        return getClass();
    }

    @Before
    @CallSuper
    public void prepare() {
        Holder.initInstance(new ILoggerHolderProvider<Holder>() {
            @NotNull
            @Override
            public Holder provideHolder() {
                return new Holder();
            }
        });
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
        protected BaseLogger createLogger(@NotNull Class<?> clazz) {
            return new LogcatLogger(clazz.getSimpleName());
        }
    }
}
