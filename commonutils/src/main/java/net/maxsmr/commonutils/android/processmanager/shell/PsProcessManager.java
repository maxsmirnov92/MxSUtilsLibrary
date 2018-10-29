package net.maxsmr.commonutils.android.processmanager.shell;

import android.content.Context;

import net.maxsmr.commonutils.android.processmanager.shell.base.AbstractShellProcessManager;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Ps Process Manager
 * <p>
 * Line format should be like this:
 * <p>
 * USER (0), PID (1), PPID (2), VSIZE (3), RSS (4), WCHAN (5), PC (6), NAME (7) (same as toolbox ps -P, but without 'PCY')
 * or this
 * UID (0), PID (1), PPID (2), C (3), STIME (4), TTY (5), TIME (6), CMD (8)
 *
 * No ability to detect bg/fg (only with -P, but not working as usual)
 */
public class PsProcessManager extends AbstractShellProcessManager {

    public PsProcessManager(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    protected String[] getCommands() {
        return new String[]{"ps", "-A", "-f"};
    }

    @NotNull
    @Override
    protected Map<Column, Set<String>> getColumnNamesMap() {
        Map<Column, Set<String>> map = new LinkedHashMap<>();
        map.put(Column.USER, new HashSet<>(Arrays.asList("USER", "UID")));
        map.put(Column.PID, Collections.singleton("PID"));
        map.put(Column.PPID, Collections.singleton("PPID"));
        map.put(Column.VSIZE, Collections.singleton("VSIZE"));
        map.put(Column.RSS, Collections.singleton("RSS"));
        map.put(Column.NAME, new HashSet<>(Arrays.asList("NAME", "CMD")));
        return map;
    }


}
