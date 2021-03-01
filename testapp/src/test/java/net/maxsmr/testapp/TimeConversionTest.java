package net.maxsmr.testapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.maxsmr.commonutils.text.TextUtilsKt;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.commonutils.conversion.NumberConversionKt.toIntSafe;
import static net.maxsmr.commonutils.format.TimeFormatUtilsKt.timeToString;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class TimeConversionTest extends LoggerTest {

    @Test
    @Override
    public void test() {
        final long targetTime =
                TimeUnit.DAYS.toMillis(5) +
                        TimeUnit.HOURS.toMillis(25) +
                        TimeUnit.MINUTES.toMillis(50) +
                        TimeUnit.SECONDS.toMillis(40) +
                        TimeUnit.MILLISECONDS.toMillis(900) +
                        TimeUnit.MICROSECONDS.toMillis(1000000) +
                        TimeUnit.NANOSECONDS.toMillis(1000000000);
        final String conversionResult = timeToString(targetTime, TimeUnit.MILLISECONDS, Collections.singleton(TimeUnit.SECONDS), (resId, quantity) -> {
            if (quantity != null) {
                return context.getResources().getQuantityString(resId, toIntSafe(quantity));
            } else {
                return context.getString(resId);
            }
        });
        logger.i("conversionResult: " + conversionResult);
        assertFalse(TextUtilsKt.isEmpty(conversionResult));
    }
}
