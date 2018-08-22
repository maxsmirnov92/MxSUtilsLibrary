package net.maxsmr.commonutils.shell;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ShellUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ShellUtils.class);

    private ShellUtils() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    private static Process createAndStartProcess(@NonNull List<String> cmds, @Nullable String workingDir,
                                                 @Nullable IProcessBuilderConfigurator configurator,
                                                 @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {

        for (int i = 0; i < cmds.size(); i++) {
            cmds.set(i, String.format(Locale.US, "%s", cmds.get(i)));
        }

        ProcessBuilder pb = new ProcessBuilder(cmds);

        if (!TextUtils.isEmpty(workingDir)) {
            if (FileHelper.isDirExists(workingDir)) {
                pb.directory(new File(workingDir));
            } else {
                logger.w("working directory " + workingDir + " not exists");
            }
        }

        if (sc != null && sc.needToLogCommands()) {
            StringBuilder cmdlog = new StringBuilder();
            for (String cmd : cmds) {
                cmdlog.append(cmd);
                cmdlog.append(' ');
            }
            sc.shellOut(ShellCallback.StreamType.CMD, cmdlog.toString());
        }

        if (configurator != null) {
            configurator.configure(pb);
        }

        Process process = null;
        IOException startEx = null;

        try {
            process = pb.start();
        } catch (IOException e) {
            startEx = e;
        }

        if (process == null) {
            if (sc != null) {
                sc.processStartFailed(startEx);
            }
            return null;
        }

        Thread outThread;
        (outThread = new StreamConsumeThread(process.getInputStream(), ShellCallback.StreamType.OUT, sc)).start();
        Thread errThread;
        (errThread = new StreamConsumeThread(process.getErrorStream(), ShellCallback.StreamType.ERR, sc)).start();

        if (tc != null) {
            tc.onThreadsStarted(new ArrayList<>(Arrays.asList(outThread, errThread)));
        }

        return process;
    }


    public static boolean execProcessAsync(@NonNull String cmd, @Nullable String workingDir, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        return execProcessAsync(cmd, workingDir, null, sc, tc);
    }

    public static boolean execProcessAsync(@NonNull String cmd, @Nullable String workingDir, @Nullable IProcessBuilderConfigurator configurator, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        return execProcessAsync(Collections.singletonList(cmd), workingDir, configurator, sc, tc);
    }

    public static boolean execProcessAsync(@NonNull List<String> cmds, @Nullable String workingDir, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        return execProcessAsync(cmds, workingDir, null, sc, tc);
    }

    /**
     * @return true if started successfully, false - otherwise
     */
    public static boolean execProcessAsync(@NonNull List<String> cmds, @Nullable String workingDir, @Nullable IProcessBuilderConfigurator configurator, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        logger.d("execProcessAsync(), cmds=" + cmds + ", workingDir=" + workingDir + ", sc=" + sc + ", tc=" + tc);
        Process process = createAndStartProcess(cmds, workingDir, configurator, sc, tc);
        if (process != null) {
            Thread waitThread;
            (waitThread = new ProcessWaitThread(process, sc)).start();
            if (tc != null) {
                tc.onThreadsStarted(Collections.singletonList(waitThread));
            }
        }
        return process != null;
    }

    public static CommandResult execProcess(@NonNull String cmd, @Nullable String workingDir, @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        return execProcess(cmd, workingDir, null, CommandResult.DEFAULT_TARGET_CODE, sc, tc);
    }

    public static CommandResult execProcess(@NonNull String cmd, @Nullable String workingDir, @Nullable IProcessBuilderConfigurator configurator, @Nullable Integer targetExitCode, @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        return execProcess(Collections.singletonList(cmd), workingDir, configurator, targetExitCode, sc, tc);
    }

    public static CommandResult execProcess(@NonNull List<String> cmds, @Nullable String workingDir, @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        return execProcess(cmds, workingDir, null, CommandResult.DEFAULT_TARGET_CODE, sc, tc);
    }

    /**
     * @return result code; -1 if start failed or interrupted
     */
    public static CommandResult execProcess(@NonNull List<String> cmds, @Nullable String workingDir,
                                            @Nullable IProcessBuilderConfigurator configurator,
                                            @Nullable Integer targetExitCode,
                                            @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        logger.d("execProcess(), cmds=" + cmds + ", workingDir=" + workingDir + ", targetExitCode=" + targetExitCode + ", sc=" + sc + ", tc=" + tc);

        final List<String> stdOutLines = new ArrayList<>();
        final List<String> stdErrLines = new ArrayList<>();

        Process process = createAndStartProcess(cmds, workingDir, configurator, new ShellCallback() {

            @Override
            public boolean needToLogCommands() {
                return sc != null && sc.needToLogCommands();
            }

            @Override
            public void shellOut(@NonNull StreamType from, String shellLine) {
                switch (from) {
                    case OUT:
                        stdOutLines.add(shellLine);
                        break;
                    case ERR:
                        stdErrLines.add(shellLine);
                        break;
                }
                if (sc != null) {
                    sc.shellOut(from, shellLine);
                }
            }

            @Override
            public void processStartFailed(Throwable t) {
                if (sc != null) {
                    sc.processStartFailed(t);
                }
            }

            @Override
            public void processComplete(int exitValue) {
                if (sc != null) {
                    sc.processComplete(exitValue);
                }
            }
        }, tc);

        int exitCode = -1;
        try {
            exitCode = process != null ? process.waitFor() : -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.e("an InterruptedException occurred during waitFor(): " + e.getMessage(), e);
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (sc != null) {
                sc.processComplete(exitCode);
            }
        }

        return new CommandResult(targetExitCode, exitCode, stdOutLines, stdErrLines);
    }

    private static class StreamConsumeThread extends Thread {

        @NonNull
        final InputStream is;

        @NonNull
        final ShellCallback.StreamType type;

        @Nullable
        final ShellCallback sc;

        StreamConsumeThread(@NonNull InputStream is, @NonNull ShellCallback.StreamType type, @Nullable ShellCallback sc) {
            this.is = is;
            this.type = type;
            this.sc = sc;
            this.setName(type.name);
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while (!isInterrupted() && (line = br.readLine()) != null)
                    if (sc != null) {
                        sc.shellOut(type, line);
                    }
            } catch (IOException e) {
                logger.e("an IOException occurred: " + e.getMessage(), e);
            }
        }
    }

    private static class ProcessWaitThread extends Thread {

        @NonNull
        final Process process;

        @Nullable
        final ShellCallback sc;

        public ProcessWaitThread(@NonNull Process process, @Nullable ShellCallback sc) {
            super(ProcessWaitThread.class.getName());
            this.process = process;
            this.sc = sc;
        }

        @Override
        public void run() {

            int exitVal = -1;

            try {
                exitVal = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.e("an InterruptedException occurred during waitFor(): " + e.getMessage(), e);
            }

            process.destroy();

            if (sc != null) {
                sc.processComplete(exitVal);
            }

        }
    }

    public interface ShellCallback {

        enum StreamType {
            CMD("cmd"), OUT("out"), ERR("err");

            public final String name;

            StreamType(String name) {
                this.name = name;
            }
        }

        boolean needToLogCommands();

        void shellOut(@NonNull StreamType from, String shellLine);

        void processStartFailed(@Nullable Throwable t);

        void processComplete(int exitValue);
    }

    public interface ThreadsCallback {

        void onThreadsStarted(List<Thread> threads);
    }

    public interface IProcessBuilderConfigurator {

        void configure(@NonNull ProcessBuilder builder);
    }

}
