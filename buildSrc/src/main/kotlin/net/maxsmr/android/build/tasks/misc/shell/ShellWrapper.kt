package net.maxsmr.android.build.tasks.misc.shell

import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ShellWrapper(var addToCommandsMap: Boolean = true) {

    private val commandId = AtomicInteger(1)

    private val commandsMap = mutableMapOf<Int, CommandInfo?>()

    var targetCode = DEFAULT_TARGET_CODE

    var workingDir: String = ""

    var configurator: ShellUtils.IProcessBuilderConfigurator? = null

    var isDisposed = false
        private set

    fun getCommandsMap(): Map<Int, CommandInfo> {

        check(!isDisposed) { ShellWrapper::class.java.simpleName + " is disposed" }

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

        check(!isDisposed) { ShellWrapper::class.java.simpleName + " is disposed" }

        synchronized(commandsMap) {
            for (v in commandsMap.values) {
                if (v != null) {
                    v.result = null
                    v.startedThreads = LinkedHashMap()
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

    @Synchronized
    fun executeCommand(command: String, useSU: Boolean): CommandResult {
        return executeCommand(mutableListOf(command), useSU)
    }

    fun executeCommand(commands: MutableList<String>?, useSU: Boolean): CommandResult {
        var commands = commands
        println("Execute commands: \"$commands\", useSU: $useSU")

        check(!isDisposed) { ShellWrapper::class.java.simpleName + " is disposed" }

        require(!(commands == null || commands.isEmpty())) { "Nothing to execute" }

        commands = ArrayList(commands)

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

        val result = ShellUtils.execProcess(commands, workingDir, configurator, targetCode, object : ShellUtils.ShellCallback {

            override fun needToLogCommands(): Boolean {
                return true
            }

            override fun shellOut(from: ShellUtils.ShellCallback.StreamType, shellLine: String) {
                println("Command \"" + commandInfo.commandsToRun + "\" output " + from + ": " + shellLine)
            }

            override fun processStarted() {
                System.out.println("Command \"" + commandInfo.commandsToRun + "\" started")
            }

            override fun processStartFailed(t: Throwable?) {
                System.err.println("Command \"" + commandInfo.commandsToRun + "\" start failed: $t")
                //                commandInfo.setResult(new CommandResult(targetCode, -1, null, null));
            }

            override fun processComplete(exitValue: Int) {

            }
        }, object : ShellUtils.ThreadsCallback {

            override fun onThreadStarted(info: ShellUtils.CmdThreadInfo, thread: Thread) {
                synchronized(commandInfo) {
                    commandInfo.startedThreads.put(info, thread)
                }
            }

            override fun onThreadFinished(info: ShellUtils.CmdThreadInfo, thread: Thread) {
                synchronized(commandInfo) {
                    commandInfo.startedThreads.remove(info)
                }
            }
        })

        synchronized(commandInfo) {
            commandInfo.result = result
            // synchronize in case of threadss not still finished (otherwise - ConcurrentModificationException)
            println("Command completed: $commandInfo")
        }

        return result
    }


    class CommandInfo {

        val commandsToRun: List<String>

        var startedThreads: MutableMap<ShellUtils.CmdThreadInfo, Thread>

        var result: CommandResult? = null

        val isCompleted: Boolean
            get() = result != null && result!!.isCompleted

        val isSuccessful: Boolean
            get() = result != null && result!!.isSuccessful

        constructor(commandsToRun: List<String>?) {
            this.commandsToRun = if (commandsToRun != null) ArrayList(commandsToRun) else ArrayList()
            startedThreads = mutableMapOf()
        }

        constructor(info: CommandInfo) {
            this.commandsToRun = ArrayList(info.commandsToRun)
            this.startedThreads = info.startedThreads
            this.result = info.result
        }

        override fun toString(): String {
            return "CommandInfo{" +
                    "commandsToRun=" + commandsToRun +
                    ", result=" + result +
                    '}'
        }
    }

}