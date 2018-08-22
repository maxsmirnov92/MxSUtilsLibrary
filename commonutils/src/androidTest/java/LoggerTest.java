import android.support.annotation.NonNull;

import net.maxsmr.commonutils.logger.LogcatLogger;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.logger.holder.ILoggerHolderProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;


@RunWith(JUnit4.class)
public class LoggerTest {

    @Before
    public void init() {
        Holder.initInstance(new ILoggerHolderProvider<Holder>() {
            @NonNull
            @Override
            public Holder provideHolder() {
                return new Holder();
            }
        });
    }

    @Test
    public void start() {
        BaseLogger logger = Holder.getInstance().getLogger(LoggerTest.class);
        logger.setLoggingEnabled(true);
        logger.d("abc");
        Assert.assertEquals(1, Holder.getInstance().getLoggersCount());
    }

    private static class Holder extends BaseLoggerHolder {

        @Override
        protected BaseLogger createLogger(@NonNull Class<?> clazz) {
            return new LogcatLogger(clazz.getSimpleName());
        }
    }
}
