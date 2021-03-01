package net.maxsmr.testapp;

import android.graphics.Point;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static net.maxsmr.commonutils.gui.ViewSizeCalculatorKt.getAutoScaledSize;
import static net.maxsmr.commonutils.gui.ViewSizeCalculatorKt.getFixedViewSize;

@RunWith(JUnit4.class)
public class ViewSizeCalculatorTest extends LoggerTest {

    private static final Point sourceSize = new Point(1920, 1080);
    private static final Point contentSize = new Point(1280, 1024);

    @Test
    @Override
    public void test() {

        float contentScale = (float) contentSize.x / contentSize.y;

        Point fixedSize = getFixedViewSize(contentSize, sourceSize);
        float fixedScale = (float) fixedSize.x / fixedSize.y;
        Assert.assertEquals(fixedScale, contentScale, 0);

        Point autoScaledSize = getAutoScaledSize(contentSize, sourceSize.x);
        float autoScaledScale = (float) autoScaledSize.x / autoScaledSize.y;
        Assert.assertEquals(autoScaledScale, contentScale, 0);
    }
}
