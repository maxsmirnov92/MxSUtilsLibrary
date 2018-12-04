package net.maxsmr.commonutils.android.processmanager.shell.base;

import android.content.Context;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.Predicate;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractTopProcessManager extends AbstractShellProcessManager {

    protected AbstractTopProcessManager(@NotNull Context context) {
        super(context);
    }

    @Override
    protected boolean isOutputParseAllowed(int currentIndex, @NotNull String[] fields, @NotNull List<String> columnNames) {
        return true;
    }

    @Override
    protected int getOutputHeaderIndex(@NotNull List<String> output) {
        // in 'top' header is usually not located in 0 index
        Pair<Integer, String> headerIndex = Predicate.Methods.findWithIndex(output, element -> {
            if (!TextUtils.isEmpty(element)) {
                element = element.trim();
                return element.toLowerCase().contains(Column.PID.name().toLowerCase()); // try to find by PID string
            }
            return false;
        });
        if (headerIndex != null && headerIndex.first != null && headerIndex.first >= 0) {
            return headerIndex.first;
        } else {
            return super.getOutputHeaderIndex(output);
        }
    }
}