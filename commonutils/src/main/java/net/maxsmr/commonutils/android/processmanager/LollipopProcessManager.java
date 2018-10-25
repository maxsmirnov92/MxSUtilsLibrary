package net.maxsmr.commonutils.android.processmanager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import net.maxsmr.commonutils.android.AppUtils;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.ShellUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lollipop+ Process Manager
 * <p>
 * Line format should be like this:
 * <p>
 * USER (0) PID (1) PPID (2) VSIZE (3) RSS (4) PCY (5) WCHAN (6) PC (7) STAT (8) NAME (9)
 */
public class LollipopProcessManager extends AbstractProcessManager {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]$");

    public LollipopProcessManager(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public List<ProcessInfo> getProcesses(boolean includeSystemPackages) {
        CommandResult commandResult = ShellUtils.execProcess(Arrays.asList("toolbox", "ps", "-P"), null, null, null);

        if (!commandResult.isSuccessful()) {
            logger.e("Failed to execute ps. Exit code: " + commandResult.getExitCode() + ", stdErr: " + commandResult.getStdErr());
        }

        refreshPackages();

        return parsePsOutput(commandResult.getStdOutLines(), includeSystemPackages);
    }

    private List<ProcessInfo> parsePsOutput(@NonNull List<String> output, boolean includeSystemPackages) {
        List<ProcessInfo> processes = new ArrayList<>();

        if (output.size() < 2) {
            logger.e("Processes output size is too short: " + output.size());
            return processes;
        }

        for (int i = 1; i < output.size(); i++) {
            // Skipping 1st line (header)

            final String line = output.get(i);

            ProcessInfo processInfo = parseProcessInfoLine(line);

            if (processInfo == null) {
                logger.e("Cannot parse process info for '" + line + "'");
                continue;
            }

            if (!includeSystemPackages && processInfo.isSystemApp) {
                logger.e("Process '" + processInfo.packageName + "' is system and should be excluded");
                continue;
            }

            processes.add(processInfo);
        }

        return processes;
    }

    @Nullable
    private ProcessInfo parseProcessInfoLine(@NonNull String outputLine) {
        String[] fields = outputLine.split("\\s+");

        if (fields.length != 10) {
//            Logger.e(LollipopProcessManager.class, "Process info line should contain 10 columns, not " + fields.length);
//            Logger.e(LollipopProcessManager.class, outputLine);
            return null;
        }

        String user = fields[0].trim();
        String pidString = fields[1].trim();
        String vSizeString = fields[3].trim();
        String rssString = fields[4].trim();
        String pcy = fields[5].trim();
        String processName = fields[9].trim();

        boolean isPackageValid = PACKAGE_PATTERN.matcher(processName.toLowerCase()).matches()
                && isPackageInstalled(processName);

        if (!isPackageValid) {
            return null;
        }

        ApplicationInfo info = AppUtils.getApplicationInfo(context, processName);

        if (info == null) {
            return null;
        }

        int pid = 0;

        try {
            pid = Integer.parseInt(pidString);
        } catch (NumberFormatException e) {
            logger.e("Failed to parse pid: " + e.getMessage());
        }

        int vSize = 0;

        try {
            vSize = Integer.parseInt(vSizeString);
        } catch (NumberFormatException e) {
            logger.e("Failed to parse vSize: " + e.getMessage());
        }

        int rss = 0;

        try {
            rss = Integer.parseInt(rssString);
        } catch (NumberFormatException e) {
            logger.e("Failed to parse rss: " + e.getMessage());
        }

        boolean isForeground = "fg".equals(pcy);

        return new ProcessInfo(processName, info.loadLabel(context.getPackageManager()), pid, user, 0, rss, vSize, isForeground, isSystemPackage(processName));
    }
}
