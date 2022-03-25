package net.maxsmr.commonutils.processmanager.shell.base;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;

import net.maxsmr.commonutils.Pair;
import net.maxsmr.commonutils.Predicate;
import net.maxsmr.commonutils.processmanager.AbstractProcessManager;
import net.maxsmr.commonutils.processmanager.model.ProcessInfo;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.ShellWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static net.maxsmr.commonutils.CompareUtilsKt.stringsEqual;
import static net.maxsmr.commonutils.FileUtilsKt.isFileExists;
import static net.maxsmr.commonutils.AppUtilsKt.getApplicationInfo;
import static net.maxsmr.commonutils.processmanager.model.ProcessInfo.ProcessState.S;
import static net.maxsmr.commonutils.conversion.NumberConversionUtilsKt.toIntNotNull;
import static net.maxsmr.commonutils.shell.CommandResultKt.DEFAULT_TARGET_CODE;
import static net.maxsmr.commonutils.shell.RootShellCommandsKt.isRootAvailable;
import static net.maxsmr.commonutils.text.TextUtilsKt.isEmpty;

public abstract class AbstractShellProcessManager extends AbstractProcessManager {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$");

    protected final ShellWrapper shellWrapper = new ShellWrapper(false);

    @Nullable
    private String[] cachedCommands;

    /** Target cached columns which current realization is interested in*/
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

        CommandResult commandResult = shellWrapper.executeCommand(commands, useSuForCommands(commands), DEFAULT_TARGET_CODE);

        if (!commandResult.isSuccessful()) {
            logger.e("Processes get failed: cannot execute command: " + commands + "; exit code: " + commandResult.getExitCode() +
                    ", stdErr: " + commandResult.getStdErr());
            return new ArrayList<>();
        }

        refreshPackages();

        return parseShellOutput(commandResult.getStdOutLines(), includeSystemPackages);
    }

    /**
     * commands to execute on each {@linkplain AbstractProcessManager#getProcesses(boolean)} call
     * on some non-rooted devices "su -c" may freeze
     */
    @NotNull
    protected abstract String[] getCommands();

    /**
     * @return possible names for known column
     */
    @NotNull
    protected abstract Map<Column, Set<String>> getColumnNamesMap();

    /**
     * in later Android versions 'su' is required for receiving full process list
     */
    protected boolean useSuForCommands(List<String> commands) {
        return !hasKnoxFlag() && isRootAvailable();
    }

    protected boolean isOutputParseAllowed(int currentIndex, @NotNull String[] fields, @NotNull List<String> columnNames) {
        return fields.length == columnNames.size();
    }

    protected int getOutputHeaderIndex(@NotNull List<String> output) {
        return 0;
    }

    protected int getFirstOutputLineIndex(@NotNull List<String> output, int headerIndex) {
        if (headerIndex < 0 || headerIndex >= output.size()) {
            throw new IllegalArgumentException("Incorrect header index: " + headerIndex);
        }
        return headerIndex + 1;
    }

    /**
     * @param columnNames parsed column names from first output line
     * @param indexMap    mapping: known column -- it's index in output array
     * @param column      column for which index should be returned
     * @param fields      column values in current output line (from 1 to size - 1)
     * @return value index >= 0 if present in output, -1 - otherwise
     */
    protected int getValueIndex(@NotNull List<String> columnNames, @NotNull Map<Column, Integer> indexMap, @NotNull Column column, @NotNull String[] fields) {
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

        final int headerIndex = getOutputHeaderIndex(output);

        if (headerIndex < 0 || headerIndex >= output.size() - 1) {
            logger.e("Cannot parse shell output: incorrect output header index: " + headerIndex);
            return processes;
        }

        // skipping some lines (may be = 0) before header
        final String headerLine = output.get(headerIndex);

        final Pair<Map<Column, Integer>, List<String>> columnPair = parseColumnIndexes(headerIndex, headerLine);

        if (columnPair.first == null || columnPair.second == null) {
            throw new RuntimeException("Cannot parse shell output: pair cannot be null");
        }

        final int firstLineIndex = getFirstOutputLineIndex(output, headerIndex);

        if (firstLineIndex <= headerIndex || firstLineIndex >= output.size()) {
            logger.e("Cannot parse shell output: incorrect first output line index: " + firstLineIndex);
            return processes;
        }

        for (int i = firstLineIndex; i < output.size(); i++) {
            // skipping some lines (may be = 0) before header, header self + some lines (may be = 0) after

            final String line = output.get(i);

            ProcessInfo processInfo = parseProcessInfoLine(i, line, columnPair.second, columnPair.first);

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
    private Pair<Map<Column, Integer>, List<String>> parseColumnIndexes(int headerIndex, @Nullable String headerLine) {

        if (headerIndex < 0) {
            throw new IllegalArgumentException("Incorrect header index: " + headerIndex);
        }

        final List<String> columnNamesList = new ArrayList<>();
        final Map<Column, Integer> indexMap = new LinkedHashMap<>();
        final Pair<Map<Column, Integer>, List<String>> result = new Pair<>(indexMap, columnNamesList);

        if (isEmpty(headerLine)) {
            logger.e("Cannot parse output header line " + headerIndex + ": empty");
            return result;
        }

        if (cachedColumnNamesMap == null) {
            cachedColumnNamesMap = getColumnNamesMap();
        }

        if (cachedColumnNamesMap.isEmpty()) {
            throw new IllegalArgumentException("Cannot parse output header line " + headerIndex + ": column names map is not specified");
        }

        final String[] columnNames = headerLine.split("\\s+");

        for (String name : columnNames) {
            if (!isEmpty(name)) {
                String[] parts = name.split("\\d");
                // remove trash, containing digits
                if (parts.length <= 1) {
                    int bracketIndex = name.indexOf("["); // cases with merged column names: for example, "S[%CPU]"
                    if (bracketIndex > 0) {
                        final String namePartOne = name.substring(0, bracketIndex);
                        final String namePartTwo = name.substring(bracketIndex);
                        if (!isEmpty(namePartOne)) {
                            columnNamesList.add(namePartOne);
                        }
                        if (!isEmpty(namePartTwo)) {
                            columnNamesList.add(namePartTwo);
                        }
                    } else {
                        columnNamesList.add(name);
                    }
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
                throw new IllegalArgumentException("Cannot parse output header line " + headerIndex + ": no names specified for column " + column);
            }

            for (final String columnName : names) {
                if (isEmpty(columnName)) {
                    throw new IllegalArgumentException("Cannot parse output header line " + headerIndex + ": name is not specified for column " + column);
                }
                Pair<Integer, String> indexPair = Predicate.Methods.findIndexed(columnNamesList, element -> {
                    return stringsEqual(element, columnName, true); // May be "NAME" or "Name", for example
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
     * @param line line from index 1
     */
    @Nullable
    private ProcessInfo parseProcessInfoLine(int lineIndex, @Nullable String line, @NotNull List<String> columnNames, @NotNull Map<Column, Integer> indexMap) {

        if (lineIndex < 0) {
            throw new IllegalArgumentException("Incorrect line index: " + lineIndex);
        }

        if (isEmpty(line)) {
            logger.e("Cannot parse output line " + lineIndex + ": empty");
            return null;
        }

        if (columnNames.isEmpty()) {
            logger.e("Cannot parse output line " + lineIndex + ": parsed column names is empty");
            return null;
        }

        if (indexMap.isEmpty()) {
            logger.e("Cannot parse output line " + lineIndex + ": index map is empty");
            return null;
        }

        final String[] fields = line.split("\\s+");

        if (!isOutputParseAllowed(lineIndex, fields, columnNames)) {
            logger.e("Cannot parse output line " + lineIndex + ": not allowed");
            return null;
        }

        final int processNameIndex = getValueIndex(columnNames, indexMap, Column.NAME, fields);

        String processName = isValueIndexValid(processNameIndex, fields) ? fields[processNameIndex].trim() : null;

        final String[] processNameParts = processName != null ? processName.split("[ ]+") : null;

        if (processNameParts != null && processNameParts.length > 0) {
            processName = processNameParts[0];
        }

        final boolean isPackageValid = !isEmpty(processName)
                && PACKAGE_PATTERN.matcher(processName.toLowerCase(Locale.getDefault())).matches()
                && isPackageInstalled(processName);

        if (!isPackageValid) {
            return null;
        }

        final ApplicationInfo appInfo = getApplicationInfo(context, processName, 0);

        if (appInfo == null) {
            return null;
        }

        final int userIndex = getValueIndex(columnNames, indexMap, Column.USER, fields);
        final int pidIndex = getValueIndex(columnNames, indexMap, Column.PID, fields);
        final int pPidIndex = getValueIndex(columnNames, indexMap, Column.PPID, fields);
        final int vSizeIndex = getValueIndex(columnNames, indexMap, Column.VSIZE, fields);
        final int rssIndex = getValueIndex(columnNames, indexMap, Column.RSS, fields);
        final int pcyIndex = getValueIndex(columnNames, indexMap, Column.PCY, fields);
        final int statIndex = getValueIndex(columnNames, indexMap, Column.STAT, fields);

        final String user = isValueIndexValid(userIndex, fields) ? fields[userIndex].trim() : null;
        final int userId = toIntNotNull(user);
        final int pid = toIntNotNull(isValueIndexValid(pidIndex, fields) ? fields[pidIndex].trim() : null);
        final int pPid = toIntNotNull(isValueIndexValid(pPidIndex, fields) ? fields[pPidIndex].trim() : null);
        final int vSize = toIntNotNull(isValueIndexValid(vSizeIndex, fields) ? fields[vSizeIndex].trim() : null);
        final int rss = toIntNotNull(isValueIndexValid(rssIndex, fields) ? fields[rssIndex].trim() : null);

        final ProcessInfo.PCY pcy = ProcessInfo.PCY.fromName(isValueIndexValid(pcyIndex, fields) ? fields[pcyIndex].trim() : null);
        final ProcessInfo.ProcessState state = ProcessInfo.ProcessState.fromName(isValueIndexValid(statIndex, fields) ? fields[statIndex].trim() : null);

        Boolean isForeground = null;

        if (pcy != null) {
            isForeground = pcy == ProcessInfo.PCY.fg;
        } else if (state != null) {
            isForeground = state != S;
        }

        return new ProcessInfo(processName, appInfo.loadLabel(context.getPackageManager()),
                pid, pPid, user, userId, rss, vSize, state, isForeground, isSystemPackage(processName));
    }

    private static boolean isValueIndexValid(int index, String[] fields) {
        int length = fields != null ? fields.length : 0;
        return index >= 0 && index < length;
    }

    private static boolean hasKnoxFlag() {
        return isFileExists("knox", Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    protected enum Column {

        USER, PID, PPID, VSIZE, RSS, STAT, PCY, NAME
    }
}
