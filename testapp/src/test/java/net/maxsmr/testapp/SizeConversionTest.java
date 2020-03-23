package net.maxsmr.testapp;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.maxsmr.commonutils.data.conversion.SizeUnit;
import net.maxsmr.commonutils.data.text.TextUtilsKt;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;

import static net.maxsmr.commonutils.data.conversion.SizeConversionKt.sizeToString;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class SizeConversionTest extends LoggerTest {

    @Test
    @Override
    public void test() {
        final long targetSize =
                SizeUnit.GBYTES.toBytes(5) +
                SizeUnit.MBYTES.toBytes(950) +
                SizeUnit.KBYTES.toBytes(900) +
                SizeUnit.BYTES.toBytes(850);
        final String conversionResult = sizeToString(targetSize, SizeUnit.BYTES, new HashSet<>(Arrays.asList(SizeUnit.KBYTES, SizeUnit.BYTES)), 4,  (resId, quantity) -> {
            if (quantity != null) {
                return context.getResources().getQuantityString(resId, quantity.intValue());
            } else {
                return context.getString(resId);
            }
        });
        logger.i("conversionResult: " + conversionResult);
        assertFalse(TextUtilsKt.isEmpty(conversionResult));
    }
}
