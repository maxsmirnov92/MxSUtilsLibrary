package net.maxsmr.commonutils.android.processmanager.shell;

import android.content.Context;

import net.maxsmr.commonutils.android.processmanager.shell.base.AbstractTopProcessManager;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Top Process Manager
 * <p>
 * Line format should be like this:
 * <p>
 * PID (0), PR (1), CPU% (2), S (3), #THR (4), VSS (5), RSS (6), PCY (7), UID (8), Name (9)
 * or this:
 * PID (0), USER (1), PR (2), NI (3), VIRT (4), RES (5), SHR (6), S[%CPU] (7, 8), %MEM (9), TIME+ (10), ARGS (11)
 */
public class TopProcessManager extends AbstractTopProcessManager {

    public TopProcessManager(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    protected String[] getCommands() {
        return new String[]{"top", "-n", "1"};
    }

    @NotNull
    @Override
    protected Map<Column, Set<String>> getColumnNamesMap() {
        Map<Column, Set<String>> map = new LinkedHashMap<>();
        map.put(Column.PID, Collections.singleton("PID"));
        map.put(Column.STAT, Collections.singleton("S"));
        map.put(Column.VSIZE, new HashSet<>(Arrays.asList("VSS", "VIRT")));
        map.put(Column.RSS, new HashSet<>(Arrays.asList("RSS", "RES")));
        map.put(Column.PCY, Collections.singleton("PCY"));
        map.put(Column.USER, new HashSet<>(Arrays.asList("UID", "USER")));
        map.put(Column.NAME, new HashSet<>(Arrays.asList("Name", "ARGS")));
        return map;
    }
}
