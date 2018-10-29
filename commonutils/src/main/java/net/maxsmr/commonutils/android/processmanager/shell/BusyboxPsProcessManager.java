package net.maxsmr.commonutils.android.processmanager.shell;

import android.content.Context;

import net.maxsmr.commonutils.android.processmanager.shell.base.AbstractShellProcessManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Busybox Ps Process Manager
 * <p>
 * Line format should be like this:
 * <p>
 * PID (0), USER (1), TIME (2), COMMAND (3)
 *
 * No ability to detect bg/fg
 */
public class BusyboxPsProcessManager extends AbstractShellProcessManager {

    public BusyboxPsProcessManager(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    protected String[] getCommands() {
        return new String[]{"busybox", "ps", "-A"};
    }

    @NotNull
    @Override
    protected Map<Column, Set<String>> getColumnNamesMap() {
        Map<Column, Set<String>> map = new LinkedHashMap<>();
        map.put(Column.PID, Collections.singleton("PID"));
        map.put(Column.USER, Collections.singleton("USER"));
        map.put(Column.NAME, Collections.singleton("COMMAND"));
        return map;
    }


}
