package net.maxsmr.commonutils.android.processmanager.shell.base;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import net.maxsmr.commonutils.android.AppUtils;
import net.maxsmr.commonutils.android.processmanager.AbstractProcessManager;
import net.maxsmr.commonutils.android.processmanager.model.ProcessInfo;
import net.maxsmr.commonutils.data.CompareUtils;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.ShellWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static net.maxsmr.commonutils.android.processmanager.model.ProcessInfo.ProcessState.S;

public abstract class AbstractShellProcessManager extends AbstractProcessManager {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$");

    protected final ShellWrapper shellWrapper = new ShellWrapper(false);

    @Nullable
    private String[] cachedCommands;

    @Nullable
    private Map<Column, Set<String>> cachedColumnNamesMap;

    protected AbstractShellProcessManager(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    public final List<ProcessInfo> getProcesses(boolean includeSystemPackages) {

        if (cachedCommands == null) {
            cachedCommands = getCommands();
        }

        if (cachedCommands.length == 0) {
            throw new IllegalArgumentException("Processes get failed: no commands to run");
        }

        final List<String> commands = Arrays.asList(cachedCommands);

        CommandResult commandResult = shellWrapper.executeCommand(commands, useSuForCommands(commands));

        if (!commandResult.isSuccessful()) {
            logger.e("Processes get failed: cannot execute command: " + commands + "; exit code: " + commandResult.getExitCode() +
                    ", stdErr: " + commandResult.getStdErr());
            return new ArrayList<>();
        }

        refreshPackages();

        return parseShellOutput(commandResult.getStdOutLines(), includeSystemPackages);
    }

    /** in later Android versions 'su' is required for receiving full process list */
    protected boolean useSuForCommands(List<String> commands) {
        return true;
    }

    /** commands to execute on each {@linkplain AbstractProcessManager#getProcesses(boolean)} call */
    @NotNull
    protected abstract String[] getCommands();

    /**
     * @return possible names for known column
     * */
    @NotNull
    protected abstract Map<Column, Set<String>> getColumnNamesMap();

    protected int getOutputHeaderIndex(@NotNull List<String> output) {
        return 0;
    }

    protected int getFirstOutputLineIndex(@NotNull List<String> output) {
        return getOutputHeaderIndex(output) + 1;
    }

    /**
     * @param columnNames parsed column names from first output line
     * @param indexMap mapping: known column -- it's index in output array
     * @param column column for which index should be returned
     * @param fields column values in current output line (from 1 to size - 1)
     * @return value index >= 0 if present in output, -1 - otherwise
     */
    protected int getIndex(@NotNull List<String> columnNames, @NotNull Map<Column, Integer> indexMap, @NotNull Column column, @NotNull String[] fields) {
        Integer index = indexMap.get(column);
        return index != null ? index : -1;
    }

    @NotNull
    private List<ProcessInfo> parseShellOutput(@NotNull List<String> output, boolean includeSystemPackages) {
        List<ProcessInfo> processes = new ArrayList<>();

        if (output.size() < 2) {
            logger.e("Cannot parse shell output: processes output size is too short: " + output);
            return processes;
        }

        final int outputHeaderIndex = getOutputHeaderIndex(output);

        if (outputHeaderIndex < 0 || outputHeaderIndex >= output.size() - 1) {
            logger.e("Cannot parse shell output: incorrect output header index: " + outputHeaderIndex);
            return processes;
        }

        // skipping some lines (may be = 0) before header
        final String outputHeaderLine = output.get(outputHeaderIndex);

        final Pair<Map<Column, Integer>, List<String>> columnPair = parseColumnIndexes(outputHeaderLine);

        if (columnPair.first == null || columnPair.second == null) {
            throw new RuntimeException("Cannot parse shell output: pair cannot be null");
        }

        final int firstOutputLineIndex = getFirstOutputLineIndex(output);

        if (firstOutputLineIndex <= outputHeaderIndex || firstOutputLineIndex >= output.size()) {
            logger.e("Cannot parse shell output: incorrect first output line index: " + firstOutputLineIndex);
            return processes;
        }

        for (int i = firstOutputLineIndex; i < output.size(); i++) {
            // skipping some lines (may be = 0) before header, header self + some lines (may be = 0) after

            final String line = output.get(i);

            ProcessInfo processInfo = parseProcessInfoLine(line, columnPair.second, columnPair.first);

            if (processInfo == null) {
//                logger.e("Cannot parse process info for '" + line + "'");
                continue;
            }

            if (!includeSystemPackages && processInfo.isSystemApp) {
//                logger.e("Process '" + processInfo.packageName + "' is system and should be excluded");
                continue;
            }

            processes.add(processInfo);
        }

        return processes;
    }

    /**
     * @return Pair: first = mapping known column - index, second = all parsed column names
     */
    @NotNull
    private Pair<Map<Column, Integer>, List<String>> parseColumnIndexes(@Nullable String outputHeaderLine) {

        final List<String> columnNamesList = new ArrayList<>();
        final Map<Column, Integer> indexMap = new LinkedHashMap<>();
        final Pair<Map<Column, Integer>, List<String>> result = new Pair<>(indexMap, columnNamesList);

        if (TextUtils.isEmpty(outputHeaderLine)) {
            logger.e("Cannot parse output header line: empty");
            return result;
        }

        if (cachedColumnNamesMap == null) {
            cachedColumnNamesMap = getColumnNamesMap();
        }

        if (cachedColumnNamesMap.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse output header line: column names map is not specified");
        }

        final String[] columnNames = outputHeaderLine.split("\\s+");

        for (String name : columnNames) {
            if (!TextUtils.isEmpty(name)) {
                int bracketIndex = name.indexOf("["); // cases with merged column names: for example, "S[%CPU]"
                if (bracketIndex > 0) {
                    final String namePartOne = name.substring(0, bracketIndex);
                    final String namePartTwo = name.substring(bracketIndex, name.length());
                    if (!TextUtils.isEmpty(namePartOne)) {
                        columnNamesList.add(namePartOne);
                    }
                    if (!TextUtils.isEmpty(namePartTwo)) {
                        columnNamesList.add(namePartTwo);
                    }
                } else {
                    columnNamesList.add(name);
                }
            }
        }

        for (final Map.Entry<Column, Set<String>> e : cachedColumnNamesMap.entrySet()) {

            final Column column = e.getKey();
            if (column == null) {
                throw new IllegalArgumentException(Column.class.getSimpleName() + " is not specified");
            }

            final Set<String> names = e.getValue();
            if (names == null || names.isEmpty()) {
                throw new IllegalArgumentException("Cannot parse output header line: no names specified for column " + column);
            }

            for (final String columnName : names) {
                if (TextUtils.isEmpty(columnName)) {
                    throw new IllegalArgumentException("Cannot parse output header line: name is not specified for column " + column);
                }
                Pair<Integer, String> indexPair = Predicate.Methods.findWithIndex(columnNamesList, new Predicate<String>() {
                    @Override
                    public boolean apply(String element) {
                        return CompareUtils.stringsEqual(element, columnName, true); // May be "NAME" or "Name", for example
                    }
                });
                if (indexPair != null && indexPair.first != null && indexPair.first >= 0) {
                    indexMap.put(column, indexPair.first);
                    break;
                }
            }
        }
        return result;
    }

    /**
     * @param outputLine line from index 1
     * */
    @Nullable
    private ProcessInfo parseProcessInfoLine(@Nullable String outputLine, @NotNull List<String> columnNames, @NotNull Map<Column, Integer> indexMap) {

        if (TextUtils.isEmpty(outputLine)) {
            logger.e("Cannot parse output line: empty");
            return null;
        }

        if (columnNames.isEmpty()) {
            logger.e("Cannot parse output line: parsed column names is empty");
            return null;
        }

        if (indexMap.isEmpty()) {
            logger.e("Cannot parse output line: index map is empty");
            return null;
        }

        final String[] fields = outputLine.split("\\s+");

        final Integer processNameIndex = getIndex(columnNames, indexMap, Column.NAME, fields);

        String processName = isIndexValid(processNameIndex, fields) ? fields[processNameIndex].trim() : null;

        final String[] processNameParts = processName != null ? processName.split("[ ]+") : null;

        if (processNameParts != null && processNameParts.length > 0) {
            processName = processNameParts[0];
        }

        final boolean isPackageValid = !TextUtils.isEmpty(processName)
                && PACKAGE_PATTERN.matcher(processName.toLowerCase()).matches()
                && isPackageInstalled(processName);

        if (!isPackageValid) {
            return null;
        }

        final ApplicationInfo appInfo = AppUtils.getApplicationInfo(context, processName);

        if (appInfo == null) {
            return null;
        }

        final Integer userIndex = getIndex(columnNames, indexMap, Column.USER, fields);
        final Integer pidIndex = getIndex(columnNames, indexMap, Column.PID, fields);
        final Integer pPidIndex = getIndex(columnNames, indexMap, Column.PPID, fields);
        final Integer vSizeIndex = getIndex(columnNames, indexMap, Column.VSIZE, fields);
        final Integer rssIndex = getIndex(columnNames, indexMap, Column.RSS, fields);
        final Integer pcyIndex = getIndex(columnNames, indexMap, Column.PCY, fields);
        final Integer statIndex = getIndex(columnNames, indexMap, Column.STAT, fields);

        final String user = isIndexValid(userIndex, fields) ? fields[userIndex].trim() : null;
        final int userId = StringUtils.strToInt(user);
        final int pid = StringUtils.strToInt(isIndexValid(pidIndex, fields) ? fields[pidIndex].trim() : null);
        final int pPid = StringUtils.strToInt(isIndexValid(pPidIndex, fields) ? fields[pPidIndex].trim() : null);
        final int vSize = StringUtils.strToInt(isIndexValid(vSizeIndex, fields) ? fields[vSizeIndex].trim() : null);
        final int rss = StringUtils.strToInt(isIndexValid(rssIndex, fields) ? fields[rssIndex].trim() : null);

        final ProcessInfo.PCY pcy = ProcessInfo.PCY.fromName(isIndexValid(pcyIndex, fields) ? fields[pcyIndex].trim() : null);
        final ProcessInfo.ProcessState state = ProcessInfo.ProcessState.fromName(isIndexValid(statIndex, fields) ? fields[statIndex].trim() : null);

        Boolean isForeground = null;

        if (pcy != null) {
            isForeground = pcy == ProcessInfo.PCY.fg;
        } else if (state != null) {
            isForeground = state != S;
        }

        return new ProcessInfo(processName, appInfo.loadLabel(context.getPackageManager()),
                pid, pPid, user, userId, rss, vSize, state, isForeground, isSystemPackage(processName));
    }

    private static boolean isIndexValid(int index, String[] fields) {
        int length = fields != null ? fields.length : 0;
        return index >= 0 && index < length;
    }

    protected enum Column {

        USER, PID, PPID, VSIZE, RSS, STAT, PCY, NAME
    }

}
