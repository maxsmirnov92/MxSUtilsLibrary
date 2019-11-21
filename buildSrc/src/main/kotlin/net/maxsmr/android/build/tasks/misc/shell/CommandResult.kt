package net.maxsmr.android.build.tasks.misc.shell

import java.util.ArrayList

const val PROCESS_EXIT_CODE_SUCCESS = 0

const val DEFAULT_TARGET_CODE = PROCESS_EXIT_CODE_SUCCESS

class CommandResult @JvmOverloads constructor(targetExitCode: Int? = DEFAULT_TARGET_CODE, exitCode: Int? = null, stdOutLines: List<String>? = null, stdErrLines: List<String>? = null) {

    val targetExitCode: Int

    private var exitCode: Int? = null // null == not completed

    private val stdOutLines: MutableList<String>

    private var stdErrLines: MutableList<String>

    val isCompleted: Boolean
        get() = exitCode != null

    /*&& stdErrLines.isEmpty()*/ val isSuccessful: Boolean
        get() = isCompleted && exitCode == targetExitCode

    val isFailed: Boolean
        get() = isCompleted && !isSuccessful

    constructor(targetExitCode: Int) : this(targetExitCode, null, null, null) {}

    init {
        this.targetExitCode = targetExitCode ?: DEFAULT_TARGET_CODE
        this.exitCode = exitCode

        this.stdOutLines = if (stdOutLines != null) ArrayList(stdOutLines) else ArrayList()
        this.stdErrLines = if (stdErrLines != null) ArrayList(stdErrLines) else ArrayList()
    }

    constructor(from: CommandResult) : this(from.targetExitCode, from.exitCode, from.stdOutLines, from.stdErrLines) {}

    fun getExitCode(): Int? {
        return exitCode
    }

    internal fun setExitCode(exitCode: Int?): CommandResult {
        this.exitCode = exitCode
        return this
    }

    fun getStdOutLines(): List<String> {
        return ArrayList(stdOutLines)
    }

    internal fun setStdOutLines(stdOutLines: List<String>?) {
        this.stdOutLines.clear()
        if (stdOutLines != null) {
            this.stdOutLines.addAll(stdOutLines)
        }
    }


    fun getStdErrLines(): List<String> {
        return ArrayList(stdErrLines)
    }

    internal fun setStdErrLines(stdErrLines: MutableList<String>?) {
        this.stdErrLines.clear()
        if (stdErrLines != null) {
            this.stdErrLines = stdErrLines
        }
    }

    override fun toString(): String {
        return "CommandResult{" +
                "targetExitCode=" + targetExitCode +
                ", exitCode=" + exitCode +
                ", stdOutLines=" + stdOutLines +
                ", stdErrLines=" + stdErrLines +
                '}'
    }
}