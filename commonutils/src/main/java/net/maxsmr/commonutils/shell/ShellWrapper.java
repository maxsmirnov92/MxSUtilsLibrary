package net.maxsmr.commonutils.shell;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.maxsmr.commonutils.shell.CommandResult.DEFAULT_TARGET_CODE;

public class ShellWrapper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ShellWrapper.class);

    private static final String EMULATED_STORAGE_SOURCE;
    private static final String EMULATED_STORAGE_TARGET;

    static {
        EMULATED_STORAGE_SOURCE = getEmulatedStorageVariable("EMULATED_STORAGE_SOURCE");
        EMULATED_STORAGE_TARGET = getEmulatedStorageVariable("EMULATED_STORAGE_TARGET");
    }

    private final AtomicInteger commandId = new AtomicInteger(1);

    private final Map<Integer, CommandInfo> commandsMap = new LinkedHashMap<>();

    private int targetCode = DEFAULT_TARGET_CODE;

    private String workingDir;

    private ShellUtils.IProcessBuilderConfigurator configurator;

    private boolean isDisposed = false;

    public Map<Integer, CommandInfo> getCommandsMap() {

        if (isDisposed) {
            throw new IllegalStateException(ShellWrapper.class.getSimpleName() + " is disposed");
        }

        Map<Integer, CommandInfo> result = new LinkedHashMap<>();
        synchronized (commandsMap) {
            for (Map.Entry<Integer, CommandInfo> e : commandsMap.entrySet()) {
                CommandInfo info = e.getValue();
                if (info != null) {
                    result.put(e.getKey(), new CommandInfo(info));
                }
            }
            return result;
        }
    }

    private void clearCommandsMap() {

        if (isDisposed) {
            throw new IllegalStateException(ShellWrapper.class.getSimpleName() + " is disposed");
        }

        synchronized (commandsMap) {
            for (CommandInfo v : commandsMap.values()) {
                if (v != null) {
                    v.result = null;
                    v.runningThreads = null;
                }
            }
            commandsMap.clear();
        }
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public void dispose() {
        if (isDisposed) {
            throw new IllegalStateException(ShellWrapper.class.getSimpleName() + " is already disposed");
        }

        clearCommandsMap();

        isDisposed = true;
    }

    public int getTargetCode() {
        return targetCode;
    }

    public void setTargetCode(int targetCode) {
        this.targetCode = targetCode;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public ShellUtils.IProcessBuilderConfigurator getConfigurator() {
        return configurator;
    }

    public void setConfigurator(ShellUtils.IProcessBuilderConfigurator configurator) {
        this.configurator = configurator;
    }

    public synchronized CommandResult executeCommand(String command, boolean useSU) {
        return executeCommand(Collections.singletonList(command), useSU);
    }

    public CommandResult executeCommand(List<String> commands, boolean useSU) {
        logger.d("Execute commands: \"" + commands + "\", useSU: " + useSU);

        if (isDisposed) {
            throw new IllegalStateException(ShellWrapper.class.getSimpleName() + " is disposed");
        }

        if (commands == null || commands.isEmpty()) {
            throw new IllegalArgumentException("Nothing to execute");
        }

        commands = new ArrayList<>(commands);

        if (useSU) {
            commands.add(0, "su");
            commands.add(1, "-c");
        }

        final int commandId = this.commandId.getAndIncrement();
        final CommandInfo commandInfo = new CommandInfo(commands);

        synchronized (commandsMap) {
            commandsMap.put(commandId, commandInfo);
        }

        final CommandResult result = ShellUtils.execProcess(commands, workingDir, configurator, targetCode, new ShellUtils.ShellCallback() {

            @Override
            public boolean needToLogCommands() {
                return true;
            }

            @Override
            public void shellOut(@NonNull StreamType from, String shellLine) {
                logger.d("Command \"" + commandInfo.getCommandsToRun() + "\" output " + from + ": " + shellLine);
            }

            @Override
            public void processStartFailed(Throwable t) {
                logger.d("Command \"" + commandInfo.getCommandsToRun() + "\" start failed");
//                commandInfo.setResult(new CommandResult(targetCode, -1, null, null));
            }

            @Override
            public void processComplete(int exitValue) {

            }
        }, new ShellUtils.ThreadsCallback() {
            @Override
            public void onThreadsStarted(List<Thread> threads) {
                commandInfo.setRunningThreads(threads);
            }
        });

        commandInfo.setResult(result);
        logger.d("Command completed: " + commandInfo);

        return result;
    }

    public static String getShellPath(File file) {
        return getShellPath(FileHelper.getCanonicalPath(file));
    }

    public static String getShellPath(String path) {
        if (EMULATED_STORAGE_SOURCE != null && EMULATED_STORAGE_TARGET != null
                && path.startsWith(EMULATED_STORAGE_TARGET)) {
            path = EMULATED_STORAGE_SOURCE + path.substring(EMULATED_STORAGE_TARGET.length());
        }
        return path;
    }

    private static String getEmulatedStorageVariable(String variable) {
        String result = System.getenv(variable);
        if (result != null) {
            result = FileHelper.getCanonicalPath(new File(result));
            if (!result.endsWith("/")) {
                result += "/";
            }
        }
        return result;
    }

    public static class CommandInfo {

        @NonNull
        private final List<String> commandsToRun;

        private List<Thread> runningThreads;

        @Nullable
        private CommandResult result;

        private CommandInfo(@Nullable List<String> commandsToRun) {
            this.commandsToRun = commandsToRun != null ? new ArrayList<String>() : new ArrayList<String>();
        }

        public CommandInfo(@NonNull CommandInfo info) {
            this.commandsToRun = info.commandsToRun;
            this.runningThreads = info.runningThreads;
            this.result = info.result;
        }

        @NonNull
        public List<String> getCommandsToRun() {
            return new ArrayList<>(commandsToRun);
        }

        public boolean isAnyThreadRunning() {
            return Predicate.Methods.find(getRunningThreads(), new Predicate<Thread>() {
                @Override
                public boolean apply(Thread e) {
                    return e != null && e.isAlive();
                }
            }) != null;
        }

        public List<Thread> getRunningThreads() {
            if (runningThreads == null) {
                runningThreads = new ArrayList<>();
            }
            return runningThreads;
        }

        public void setRunningThreads(@Nullable List<Thread> runningThreads) {
            this.runningThreads = runningThreads != null ? new ArrayList<>(runningThreads) : new ArrayList<Thread>();
        }

        public boolean isCompleted() {
            return result != null && result.isCompleted();
        }

        public boolean isSuccessful() {
            return result != null && result.isSuccessful();
        }

        @Nullable
        public CommandResult getResult() {
            return result;
        }

        public void setResult(@Nullable CommandResult result) {
            this.result = result;
        }

        @Override
        public String toString() {
            return "CommandInfo{" +
                    "commandsToRun=" + commandsToRun +
                    ", runningThreads=" + runningThreads +
                    ", result=" + result +
                    '}';
        }
    }

}