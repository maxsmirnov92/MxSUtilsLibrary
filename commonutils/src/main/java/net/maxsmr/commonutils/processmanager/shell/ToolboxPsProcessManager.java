package net.maxsmr.commonutils.processmanager.shell;

import android.content.Context;

import net.maxsmr.commonutils.processmanager.shell.base.AbstractShellProcessManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Toolbox Ps Process Manager
 * <p>
 * Line format should be like this:
 * <p>
 * USER (0) PID (1) PPID (2) VSIZE (3) RSS (4) PCY (5) WCHAN (6) PC (7)
 * STAT (8, column header may be missing, but value present!) NAME (9)
 */
public class ToolboxPsProcessManager extends AbstractShellProcessManager {

    public ToolboxPsProcessManager(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    protected String[] getCommands() {
        return new String[]{"toolbox", "ps", "-P"};
    }

    @NotNull
    @Override
    protected Map<Column, Set<String>> getColumnNamesMap() {
        Map<Column, Set<String>> map = new LinkedHashMap<>();
        map.put(Column.USER, Collections.singleton("USER"));
        map.put(Column.PID, Collections.singleton("PID"));
        map.put(Column.PPID, Collections.singleton("PPID"));
        map.put(Column.VSIZE, Collections.singleton("VSIZE"));
        map.put(Column.RSS, Collections.singleton("RSS"));
        map.put(Column.PCY, Collections.singleton("PCY"));
        map.put(Column.STAT, Collections.singleton("STAT"));
        map.put(Column.NAME, Collections.singleton("NAME"));
        return map;
    }

    @Override
    protected boolean isOutputParseAllowed(int currentIndex, @NotNull String[] fields, @NotNull List<String> columnNames) {
        final int diff = fields.length - columnNames.size();
        return super.isOutputParseAllowed(currentIndex, fields, columnNames) || (diff >= 0 && diff <= 1);
    }

    // костыль для отсутствующего названия столбца 'STAT' в шапке
    @Override
    protected int getValueIndex(@NotNull List<String> columnNames, @NotNull Map<Column, Integer> indexMap, @NotNull Column column, @NotNull String[] fields) {
        int index = super.getValueIndex(columnNames, indexMap, column, fields);
        if (fields.length > columnNames.size()) { // only when output values count > declared columns count
            final int diff = fields.length - columnNames.size();
            if (diff == 1) {
                final int nameIndex = super.getValueIndex(columnNames, indexMap, Column.NAME, fields);
                if (nameIndex >= 0) {
                    if (index < 0 && column == Column.STAT) {
                        if (nameIndex >= 1) {
                            index = nameIndex;
                        }
                    } else if (index >= nameIndex) {
                        // 'NAME' or some next is after missing 'STAT'
                        // should be corrected on +1
                        index++;
                    }
                }
            } // i give up if diff > 1
        }
        return index;
    }
}
