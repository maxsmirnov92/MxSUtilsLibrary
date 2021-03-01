package net.maxsmr.commonutils.shell

import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val logger = BaseLoggerHolder.getInstance().getLogger<BaseLogger>(ShellWrapper::class.java)

class ShellWrapper @JvmOverloads constructor(
        var addToCommandsMap: Boolean = true,

        var workingDir: String = EMPTY_STRING,

        var configurator: IProcessBuilderConfigurator? = null
) {

    private val commandId = AtomicInteger(1)

    private val commandsMap = mutableMapOf<Int, CommandInfo?>()


    var isDisposed = false
        private set

    fun getCommandsMap(): Map<Int, CommandInfo> {

        checkDisposed()

        val result = mutableMapOf<Int, CommandInfo>()
        synchronized(commandsMap) {
            for ((key, info) in commandsMap) {
                if (info != null) {
                    result[key] = CommandInfo(info)
                }
            }
            return result
        }
    }

    private fun clearCommandsMap() {

        checkDisposed()

        synchronized(commandsMap) {
            for (v in commandsMap.values) {
                if (v != null) {
                    v.result = null
                    v.startedThreads = mutableMapOf()
                }
            }
            commandsMap.clear()
        }
    }

    fun dispose() {
        check(!isDisposed) { ShellWrapper::class.java.simpleName + " is already disposed" }
        clearCommandsMap()
        isDisposed = true
    }

    @JvmOverloads
    fun executeCommand(
            command: String,
            useSU: Boolean = false,
            targetCode: Int? = DEFAULT_TARGET_CODE,
            execTimeout: Long = 0,
            execTimeoutUnit: TimeUnit = TimeUnit.MILLISECONDS,
            shellCallback: ShellCallback? = null
    ): CommandResult =
            executeCommand(mutableListOf(command), useSU, targetCode, execTimeout, execTimeoutUnit, shellCallback)

    @JvmOverloads
    fun executeCommand(
            commands: List<String>,
            useSU: Boolean = false,
            targetCode: Int? = DEFAULT_TARGET_CODE,
            execTimeout: Long = 0,
            execTimeoutUnit: TimeUnit = TimeUnit.MILLISECONDS,
            shellCallback: ShellCallback? = null
    ): CommandResult {
        logger.d("Execute commands: \"$commands\", useSU: $useSU")

        checkDisposed()

        require(commands.isNotEmpty()) { "Nothing to execute" }

        val commands = commands.toMutableList()

        if (useSU) {
            commands.add(0, "su")
            commands.add(1, "-c")
        }

        val commandId = this.commandId.getAndIncrement()
        val commandInfo = CommandInfo(commands)

        if (addToCommandsMap) {
            synchronized(commandsMap) {
                commandsMap.put(commandId, commandInfo)
            }
        }

        val result = execProcess(commands, workingDir, configurator, targetCode, object : ShellCallback {

            override val needToLogCommands: Boolean get() = shellCallback?.needToLogCommands ?: true

            override fun shellOut(from: ShellCallback.StreamType, shellLine: String) {
                logger.d("Output $from: $shellLine")
                shellCallback?.shellOut(from, shellLine)
            }

            override fun processStarted() {
                logger.d("Command \"" + commandInfo.commandsToRun + "\" started")
                shellCallback?.processStarted()
            }

            override fun processStartFailed(t: Throwable?) {
                logger.e("Command \"" + commandInfo.commandsToRun + "\" start failed: $t")
//                commandInfo.setResult(new CommandResult(targetCode, -1, null, null));
                shellCallback?.processStartFailed(t)
            }

            override fun processComplete(exitValue: Int) {
                shellCallback?.processComplete(exitValue)
            }
        }, object : ThreadsCallback {

            override fun onThreadStarted(info: CmdThreadInfo, thread: Thread) {
                synchronized(commandInfo) {
                    commandInfo.startedThreads.put(info, thread)
                }
            }

            override fun onThreadFinished(info: CmdThreadInfo, thread: Thread) {
                synchronized(commandInfo) {
                    commandInfo.startedThreads.remove(info)
                }
            }
        }, execTimeout, execTimeoutUnit)

        synchronized(commandInfo) {
            commandInfo.result = result
            // synchronize in case of threads still not finished (otherwise - ConcurrentModificationException)
            logger.d("Command completed: $commandInfo")
        }

        return result
    }

    private fun checkDisposed() {
        check(!isDisposed) { ShellWrapper::class.java.simpleName + " is disposed" }
    }

    class CommandInfo {

        val commandsToRun: List<String>

        var startedThreads: MutableMap<CmdThreadInfo, Thread>

        var result: CommandResult?

        val isCompleted: Boolean
            get() = with(result) {
                this != null && this.isCompleted
            }

        val isSuccessful: Boolean
            get() = with(result) {
                this != null && this.isSuccessful
            }

        constructor(commandsToRun: List<String>?) {
            this.commandsToRun = if (commandsToRun != null) ArrayList(commandsToRun) else ArrayList()
            this.startedThreads = mutableMapOf()
            this.result = null
        }

        constructor(info: CommandInfo) {
            this.commandsToRun = info.commandsToRun
            this.startedThreads = info.startedThreads
            this.result = info.result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CommandInfo) return false

            if (commandsToRun != other.commandsToRun) return false
            if (startedThreads != other.startedThreads) return false
            if (result != other.result) return false

            return true
        }

        override fun hashCode(): Int {
            var result1 = commandsToRun.hashCode()
            result1 = 31 * result1 + startedThreads.hashCode()
            result1 = 31 * result1 + (result?.hashCode() ?: 0)
            return result1
        }

        override fun toString(): String {
            return "CommandInfo(commandsToRun=$commandsToRun, startedThreads=$startedThreads, result=$result)"
        }
    }
}