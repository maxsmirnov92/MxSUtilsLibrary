package net.maxsmr.commonutils.shell;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.FileHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final static Logger logger = LoggerFactory.getLogger(ShellUtils.class);

    public static final int PROCESS_EXIT_CODE_SUCCESS = 0;

    public static final String SU_BINARY_NAME = "su";

    public ShellUtils() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    private static Process createAndStartProcess(@NonNull List<String> cmds, @Nullable String workingDir, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {

        for (int i = 0; i < cmds.size(); i++) {
            cmds.set(i, String.format(Locale.US, "%s", cmds.get(i)));
        }

        ProcessBuilder pb = new ProcessBuilder(cmds);

        if (!TextUtils.isEmpty(workingDir)) {
            if (FileHelper.isDirExists(workingDir)) {
                pb.directory(new File(workingDir));
            } else {
                logger.warn("working directory " + workingDir + " not exists");
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

        Process process;

        try {
            process = pb.start();
        } catch (IOException e) {
            if (sc != null) {
                sc.processStartFailed(e);
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

    /**
     * @return true if started successfully, false - otherwise
     */
    public static boolean execProcessAsync(@NonNull List<String> cmds, @Nullable String workingDir, @Nullable ShellCallback sc, @Nullable ThreadsCallback tc) {
        logger.debug("execProcessAsync(), cmds=" + cmds + ", workingDir=" + workingDir + ", sc=" + sc + ", tc=" + tc);
        Process process = createAndStartProcess(cmds, workingDir, sc, tc);
        if (process != null) {
            Thread waitThread;
            (waitThread = new ProcessWaitThread(process, sc)).start();
            if (tc != null) {
                tc.onThreadsStarted(Collections.singletonList(waitThread));
            }
        }
        return process != null;
    }

    /**
     * @return result code; -1 if start failed or interrupted
     */
    public static int execProcess(@NonNull List<String> cmds, @Nullable String workingDir, @Nullable ShellCallback sc, @Nullable final ThreadsCallback tc) {
        logger.debug("execProcess(), cmds=" + cmds + ", workingDir=" + workingDir + ", sc=" + sc + ", tc=" + tc);
        Process process = createAndStartProcess(cmds, workingDir, sc, tc);
        int exitCode = -1;
        try {
            return exitCode = process != null ? process.waitFor() : -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return exitCode;
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (sc != null) {
                sc.processComplete(exitCode);
            }
        }
    }

    public static boolean isRootAvailable() {
        return (FileHelper.searchByNameWithStat(SU_BINARY_NAME, null, null)).size() > 0;
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
                logger.error("an IOException occurred", e);
                e.printStackTrace();
            }
        }
    }

    static class ProcessWaitThread extends Thread {

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
                logger.error("an InterruptedException occurred", e);
                Thread.currentThread().interrupt();
                e.printStackTrace();
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

        void processStartFailed(Throwable t);

        void processComplete(int exitValue);
    }

    public interface ThreadsCallback {
        void onThreadsStarted(List<Thread> threads);
    }
}
