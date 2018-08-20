package net.maxsmr.commonutils.shell;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class CommandResult {

    public static final int PROCESS_EXIT_CODE_SUCCESS = 0;

    public static final int DEFAULT_TARGET_CODE = PROCESS_EXIT_CODE_SUCCESS;

    private final int targetExitCode;

    private Integer exitCode = null; // not defined

    @NonNull
    private List<String> stdOutLines;

    @NonNull
    private List<String> stdErrLines;

    public CommandResult() {
        this(DEFAULT_TARGET_CODE, null, null, null);
    }

    public CommandResult(int targetExitCode) {
        this(targetExitCode, null, null, null);
    }

    public CommandResult(Integer targetExitCode, Integer exitCode, @Nullable List<String> stdOutLines, @Nullable List<String> stdErrLines) {
        this.targetExitCode = targetExitCode != null? targetExitCode : DEFAULT_TARGET_CODE;
        this.exitCode = exitCode;

        this.stdOutLines = stdOutLines != null? new ArrayList<>(stdOutLines) : new ArrayList<String>();
        this.stdErrLines = stdErrLines != null? new ArrayList<>(stdErrLines) : new ArrayList<String>();
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public boolean isCompleted() {
        return exitCode != null;
    }

    public boolean isSuccessful() {
        return isCompleted() && exitCode == targetExitCode /*&& stdErrLines.isEmpty()*/;
    }

    @NonNull
    public String getStdOut() {
        return TextUtils.join(System.getProperty("line.separator"), stdOutLines);
    }

    @NonNull
    public List<String> getStdOutLines() {
        return new ArrayList<>(stdOutLines);
    }

    public void setStdOutLines(@Nullable List<String> stdOutLines) {
        this.stdOutLines.clear();
        if (stdOutLines != null) {
            this.stdOutLines.addAll(stdOutLines);
        }
    }

    @NonNull
    public String getStdErr() {
        return TextUtils.join(System.getProperty("line.separator"), stdOutLines);
    }


    @NonNull
    public List<String> getStdErrLines() {
        return new ArrayList<>(stdErrLines);
    }

    public void setStdErrLines(@Nullable List<String> stdErrLines) {
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