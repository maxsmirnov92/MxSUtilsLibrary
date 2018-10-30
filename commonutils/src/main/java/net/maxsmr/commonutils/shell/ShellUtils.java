package net.maxsmr.commonutils.shell;

import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public final class ShellUtils {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ShellUtils.class);

    private ShellUtils() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    private static Process createAndStartProcess(@NotNull List<String> cmds, @Nullable String workingDir,
                                                 @Nullable IProcessBuilderConfigurator configurator,
                                                 @Nullable ShellCallback sc, @Nullable ThreadsCallback tc, @Nullable CountDownLatch latch) {

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
        CmdThreadInfo outThreadInfo = new CmdThreadInfo(cmds, workingDir, ShellCallback.StreamType.OUT);
        (outThread = new StreamConsumeThread(outThreadInfo, process.getInputStream(), sc, tc, latch)).start();
        if (tc != null) {
            tc.onThreadStarted(outThreadInfo, outThread);
        }

        Thread errThread;
        CmdThreadInfo errThreadInfo = new CmdThreadInfo(cmds, workingDir, ShellCallback.StreamType.ERR);
        (errThread = new StreamConsumeThread(errThreadInfo, process.getErrorStream(), sc, tc, latch)).start();
        if (tc != null) {
            tc.onThreadStarted(errThreadInfo, errThread);
        }

        return process;
    }


    public static boolean execProcessAsync(@NotNull String cmd, @Nullable String workingDir, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        return execProcessAsync(cmd, workingDir, null, sc, tc);
    }

    public static boolean execProcessAsync(@NotNull String cmd, @Nullable String workingDir, @Nullable IProcessBuilderConfigurator configurator, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        return execProcessAsync(Collections.singletonList(cmd), workingDir, configurator, sc, tc);
    }

    public static boolean execProcessAsync(@NotNull List<String> cmds, @Nullable String workingDir, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        return execProcessAsync(cmds, workingDir, null, sc, tc);
    }

    /**
     * @return true if started successfully, false - otherwise
     */
    public static boolean execProcessAsync(@NotNull List<String> cmds, @Nullable String workingDir, @Nullable IProcessBuilderConfigurator configurator, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        logger.v("execProcessAsync(), cmds=" + cmds + ", workingDir=" + workingDir + ", configurator=" + configurator + ", sc=" + sc + ", tc=" + tc);
        final CountDownLatch latch = new CountDownLatch(2);
        Process process = createAndStartProcess(cmds, workingDir, configurator, sc, tc, latch);
        if (process != null) {
            new ProcessWaitThread(process, sc, latch).start();
            return true;
        }
        return false;
    }

    @NotNull
    public static CommandResult execProcess(@NotNull String cmd, @Nullable String workingDir, @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        return execProcess(cmd, workingDir, null, CommandResult.DEFAULT_TARGET_CODE, sc, tc);
    }

    @NotNull
    public static CommandResult execProcess(@NotNull String cmd, @Nullable String workingDir, @Nullable IProcessBuilderConfigurator configurator, @Nullable Integer targetExitCode, @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        return execProcess(Collections.singletonList(cmd), workingDir, configurator, targetExitCode, sc, tc);
    }

    @NotNull
    public static CommandResult execProcess(@NotNull List<String> cmds, @Nullable String workingDir, @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        return execProcess(cmds, workingDir, null, CommandResult.DEFAULT_TARGET_CODE, sc, tc);
    }

    /**
     * @return result code; -1 if start failed or interrupted
     */
    @NotNull
    public static CommandResult execProcess(@NotNull List<String> cmds, @Nullable String workingDir,
                                            @Nullable IProcessBuilderConfigurator configurator,
                                            @Nullable Integer targetExitCode,
                                            @Nullable final ShellCallback sc, @Nullable final ThreadsCallback tc) {
        logger.v("execProcess(), cmds=" + cmds + ", workingDir=" + workingDir + ", configurator=" + configurator  + ", targetExitCode=" + targetExitCode + ", sc=" + sc + ", tc=" + tc);

        final List<String> stdOutLines = new ArrayList<>();
        final List<String> stdErrLines = new ArrayList<>();

        final CountDownLatch latch = new CountDownLatch(2);

        Process process = createAndStartProcess(cmds, workingDir, configurator, new ShellCallback() {

            @Override
            public boolean needToLogCommands() {
                return sc != null && sc.needToLogCommands();
            }

            @Override
            public void shellOut(@NotNull StreamType from, String shellLine) {
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
        }, tc, latch);

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.e("an InterruptedException occurred during await(): " + e.getMessage(), e);
        }

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

        @NotNull
        private final CmdThreadInfo threadInfo;

        @NotNull
        final InputStream is;

        @Nullable
        final ShellCallback sc;

        @Nullable
        final ThreadsCallback tc;

        @Nullable
        final CountDownLatch latch;

        StreamConsumeThread(@NotNull CmdThreadInfo threadInfo, @NotNull InputStream is, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc, @Nullable CountDownLatch latch) {
            this.threadInfo = threadInfo;
            this.is = is;
            this.sc = sc;
            this.tc = tc;
            this.latch = latch;
            this.setName(threadInfo.type.name);
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while (!isInterrupted() && (line = br.readLine()) != null)
                    if (sc != null) {
                        sc.shellOut(threadInfo.type, line);
                    }
            } catch (IOException e) {
                logger.e("an IOException occurred: " + e.getMessage(), e);
            }
            if (latch != null) {
                latch.countDown();
            }
            if (tc != null) {
                tc.onThreadFinished(threadInfo, this);
            }
        }
    }

    private static class ProcessWaitThread extends Thread {

        @NotNull
        final Process process;

        @Nullable
        final ShellCallback sc;

        @Nullable
        final CountDownLatch latch;

        public ProcessWaitThread(@NotNull Process process, @Nullable ShellCallback sc, @Nullable CountDownLatch latch) {
            super(ProcessWaitThread.class.getName());
            this.process = process;
            this.sc = sc;
            this.latch = latch;
        }

        @Override
        public void run() {

            int exitVal = -1;

            if (latch != null && getLatchCounts() > 0) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    logger.e("an InterruptedException occurred during await(): " + e.getMessage(), e);
                }
            }

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

        private long getLatchCounts() {
            return latch != null? latch.getCount() : 0;
        }
    }

    public static class CmdThreadInfo {

        @NotNull
        private final List<String> cmds;

        @Nullable
        private final String workingDir;

        @NotNull
        final ShellCallback.StreamType type;

        public CmdThreadInfo(@Nullable List<String> cmds, @Nullable String workingDir, @NotNull ShellCallback.StreamType type) {
            this.cmds = cmds != null? new ArrayList<>(cmds) : new ArrayList<>();
            this.workingDir = workingDir;
            this.type = type;
        }

        @Override
        public String toString() {
            return "CmdThreadInfo{" +
                    "cmds=" + cmds +
                    ", workingDir='" + workingDir + '\'' +
                    ", type=" + type +
                    '}';
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

        void shellOut(@NotNull StreamType from, String shellLine);

        void processStartFailed(@Nullable Throwable t);

        void processComplete(int exitValue);
    }

    public interface ThreadsCallback {

        void onThreadStarted(@NotNull CmdThreadInfo info, @NotNull Thread thread);

        void onThreadFinished(@NotNull CmdThreadInfo info, @NotNull Thread thread);
    }

    public interface IProcessBuilderConfigurator {

        void configure(@NotNull ProcessBuilder builder);
    }

}
