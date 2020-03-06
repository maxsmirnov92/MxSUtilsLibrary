package net.maxsmr.commonutils.android.gui;

import android.view.KeyEvent;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.maxsmr.commonutils.android.gui.GuiUtilsKt.isEnterKeyPressed;

/**
 * {@linkplain TextView.OnEditorActionListener}, позволяющий реагировать на onEditorAction,
 * но с проверкой интервала для исключения лишних срабатываний
 * в дефолтном случае - проверка на Enter
 */
public abstract class IntervalEditorActionListener implements TextView.OnEditorActionListener {

    private static final long DEFAULT_TARGET_INTERVAL = 200;

    private final long targetInterval;

    private long lastActionTime = 0;

    public IntervalEditorActionListener() {
        this(DEFAULT_TARGET_INTERVAL);
    }

    public IntervalEditorActionListener(long targetInterval) {
        if (targetInterval < 0) {
            throw new IllegalArgumentException("Incorrect target interval: " + targetInterval);
        }
        this.targetInterval = targetInterval;
    }

    @Override
    public final boolean onEditorAction(@NotNull TextView v, int actionId, @Nullable KeyEvent event) {
        boolean result = false;
        if (shouldDoAction(v, actionId, event)) {
            final long currentTime = System.currentTimeMillis();
            if (targetInterval <= 0 || lastActionTime == 0 || currentTime - lastActionTime >= targetInterval) {
                doAction(v, actionId, event);
                lastActionTime = currentTime;
                result = true;
            }
        }
        return result;
    }

    protected boolean shouldDoAction(@NotNull TextView v, int actionId, @Nullable KeyEvent event) {
        return event == null || isEnterKeyPressed(event, actionId);
    }

    protected abstract boolean doAction(@NotNull TextView v, int actionId, @Nullable KeyEvent event);
}