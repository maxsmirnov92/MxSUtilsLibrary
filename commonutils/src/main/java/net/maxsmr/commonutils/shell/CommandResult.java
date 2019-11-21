package net.maxsmr.commonutils.shell;

import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.maxsmr.commonutils.data.SymbolConstKt.NEXT_LINE;

// TODO refac
public class CommandResult {

    public static final int PROCESS_EXIT_CODE_SUCCESS = 0;

    public static final int DEFAULT_TARGET_CODE = PROCESS_EXIT_CODE_SUCCESS;

    private final int targetExitCode;

    private Integer exitCode = null; // null == not completed

    @NotNull
    private List<String> stdOutLines;

    @NotNull
    private List<String> stdErrLines;

    public CommandResult() {
        this(DEFAULT_TARGET_CODE, null, null, null);
    }

    public CommandResult(int targetExitCode) {
        this(targetExitCode, null, null, null);
    }

    public CommandResult(Integer targetExitCode, Integer exitCode, @Nullable List<String> stdOutLines, @Nullable List<String> stdErrLines) {
        this.targetExitCode = targetExitCode != null ? targetExitCode : DEFAULT_TARGET_CODE;
        this.exitCode = exitCode;

        this.stdOutLines = stdOutLines != null ? new ArrayList<>(stdOutLines) : new ArrayList<String>();
        this.stdErrLines = stdErrLines != null ? new ArrayList<>(stdErrLines) : new ArrayList<String>();
    }

    public CommandResult(@NotNull CommandResult from) {
        this(from.targetExitCode, from.exitCode, from.stdOutLines, from.stdErrLines);
    }

    public int getTargetExitCode() {
        return targetExitCode;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    @NotNull
    CommandResult setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
        return this;
    }

    public boolean isCompleted() {
        return exitCode != null;
    }

    public boolean isSuccessful() {
        return isCompleted() && exitCode == targetExitCode /*&& stdErrLines.isEmpty()*/;
    }

    public boolean isFailed() {
        return isCompleted() && !isSuccessful();
    }

    @NotNull
    public String getStdOut() {
        return TextUtils.join(NEXT_LINE, stdOutLines);
    }

    @NotNull
    public List<String> getStdOutLines() {
        return new ArrayList<>(stdOutLines);
    }

    void setStdOutLines(@Nullable List<String> stdOutLines) {
        this.stdOutLines.clear();
        if (stdOutLines != null) {
            this.stdOutLines.addAll(stdOutLines);
        }
    }

    @NotNull
    public String getStdErr() {
        return TextUtils.join(NEXT_LINE, stdOutLines);
    }


    @NotNull
    public List<String> getStdErrLines() {
        return new ArrayList<>(stdErrLines);
    }

    void setStdErrLines(@Nullable List<String> stdErrLines) {
        this.stdErrLines.clear();
        if (stdErrLines != null) {
            this.stdErrLines = stdErrLines;
        }
    }

    @Override
    public String toString() {
        return "CommandResult{" +
                "targetExitCode=" + targetExitCode +
                ", exitCode=" + exitCode +
                ", stdOutLines=" + stdOutLines +
                ", stdErrLines=" + stdErrLines +
                '}';
    }
}