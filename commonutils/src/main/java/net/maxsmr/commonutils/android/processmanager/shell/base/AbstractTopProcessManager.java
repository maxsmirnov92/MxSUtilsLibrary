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
    protected int getOutputHeaderIndex(@NotNull List<String> output) {
        // in 'top' header not located in 0 index
        Pair<Integer, String> headerIndex = Predicate.Methods.findWithIndex(output, new Predicate<String>() {
            @Override
            public boolean apply(String element) {
                if (!TextUtils.isEmpty(element)) {
                    element = element.trim();
                    return element.startsWith(Column.PID.name()); // try to find by PID string
                }
                return false;
            }
        });
        if (headerIndex != null && headerIndex.first != null && headerIndex.first >= 0) {
            return headerIndex.first;
        } else {
            return super.getOutputHeaderIndex(output);
        }
    }
}
