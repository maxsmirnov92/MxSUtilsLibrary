package net.maxsmr.commonutils.processmanager.shell;

import android.content.Context;
import net.maxsmr.commonutils.processmanager.shell.base.AbstractTopProcessManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Busybox Top Process Manager
 * <p>
 * Line format should be like this:
 * <p>
 * PID (0), PPID (1), USER (2), STAT (3), VSZ (4), %VSZ (5), CPU (6), %CPU (7), COMMAND (8)
 */
public class BusyboxTopProcessManager extends AbstractTopProcessManager {

    public BusyboxTopProcessManager(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    protected String[] getCommands() {
        return new String[]{"busybox", "top", "-n", "1"};
    }

    @NotNull
    @Override
    protected Map<Column, Set<String>> getColumnNamesMap() {
        Map<Column, Set<String>> map = new LinkedHashMap<>();
        map.put(Column.PID, Collections.singleton("PID"));
        map.put(Column.PPID, Collections.singleton("PPID"));
        map.put(Column.USER, Collections.singleton("USER"));
        map.put(Column.STAT, Collections.singleton("STAT"));
        map.put(Column.VSIZE, Collections.singleton("VSZ"));
        map.put(Column.NAME, Collections.singleton("COMMAND"));
        return map;
    }


}
