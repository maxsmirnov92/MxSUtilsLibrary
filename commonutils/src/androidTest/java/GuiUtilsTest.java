import android.graphics.Point;

import net.maxsmr.commonutils.android.gui.GuiUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GuiUtilsTest {

    private static final Point sourceSize = new Point(1920, 1080);
    private static final Point contentSize = new Point(1280, 1024);

    @Test
    public void resizeMethodsTest() {

        float contentScale = (float) contentSize.x / contentSize.y;

        Point fixedSize = GuiUtils.getFixedViewSize(contentSize, sourceSize);
        float fixedScale = (float) fixedSize.x / fixedSize.y;
        Assert.assertEquals(fixedScale, contentScale, 0);

        Point autoScaledSize = GuiUtils.getAutoScaledSize(contentSize, sourceSize.x);
        float autoScaledScale = (float) autoScaledSize.x / autoScaledSize.y;
        Assert.assertEquals(autoScaledScale, contentScale, 0);
    }
}
