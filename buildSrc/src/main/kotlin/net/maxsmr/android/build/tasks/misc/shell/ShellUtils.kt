package net.maxsmr.android.build.tasks.misc.shell

import net.maxsmr.android.build.tasks.misc.shell.ShellUtils.ShellCallback.*
import java.io.*
import java.util.*
import java.util.concurrent.CountDownLatch

class ShellUtils private constructor() {

    init {
        throw AssertionError("no instances.")
    }

    private class StreamConsumeThread internal constructor(private val threadInfo: CmdThreadInfo, internal val `is`: InputStream, internal val sc: ShellCallback?, internal val tc: ThreadsCallback?, internal val latch: CountDownLatch?) : Thread() {

        init {
            this.name = threadInfo.type.name
        }

        override fun run() {
            try {
                val isr = InputStreamReader(`is`)
                val br = BufferedReader(isr)
                var line = br.readLine()
                while (!isInterrupted && line != null) {
                    sc?.shellOut(threadInfo.type, line)
                    line = br.readLine()
                }
            } catch (e: IOException) {
                System.err.println("an IOException occurred: $e")
            }

            latch?.countDown()
            tc?.onThreadFinished(threadInfo, this)
        }
    }

    private class ProcessWaitThread(internal val process: Process, internal val sc: ShellCallback?, internal val latch: CountDownLatch?) : Thread(ProcessWaitThread::class.java.name) {

        private val latchCounts: Long
            get() = latch?.count ?: 0

        override fun run() {

            var exitVal = -1

            if (latch != null && latchCounts > 0) {
                try {
                    latch.await()
                } catch (e: InterruptedException) {
                    System.err.println("an InterruptedException occurred during await(): $e")
                }

            }

            try {
                exitVal = process.waitFor()
            } catch (e: InterruptedException) {
                currentThread().interrupt()
                System.err.println("an InterruptedException occurred during waitFor(): $e")
            }

            process.destroy()

            sc?.processComplete(exitVal)

        }
    }

    class CmdThreadInfo(cmds: List<String>?, private val workingDir: String?, internal val type: StreamType) {

        private val cmds: List<String> = if (cmds != null) ArrayList(cmds) else ArrayList()

        override fun toString(): String {
            return "CmdThreadInfo{" +
                    "cmds=" + cmds +
                    ", workingDir='" + workingDir + '\''.toString() +
                    ", type=" + type +
                    '}'.toString()
        }
    }

    interface ShellCallback {

        enum class StreamType private constructor(val value: String) {
            CMD("cmd"), OUT("out"), ERR("err")
        }

        fun needToLogCommands(): Boolean

        fun shellOut(from: StreamType, shellLine: String)

        fun processStarted()

        fun processStartFailed(t: Throwable?)

        fun processComplete(exitValue: Int)
    }

    interface ThreadsCallback {

        fun onThreadStarted(info: CmdThreadInfo, thread: Thread)

        fun onThreadFinished(info: CmdThreadInfo, thread: Thread)
    }

    interface IProcessBuilderConfigurator {

        fun configure(builder: ProcessBuilder)
    }

    class WrappedShellCallback(
            private val sc: ShellCallback?,
            private val stdOutLines: MutableList<String>,
            private val stdErrLines: MutableList<String>
    ) : ShellCallback {

        var wasStarted: Boolean = false
        var isFinished: Boolean = false

        override fun needToLogCommands(): Boolean {
            return sc != null && sc.needToLogCommands()
        }

        override fun shellOut(from: StreamType, shellLine: String) {
            when (from) {
                StreamType.OUT -> stdOutLines.add(shellLine)
                StreamType.ERR -> stdErrLines.add(shellLine)
            }
            sc?.shellOut(from, shellLine)
        }

        override fun processStarted() {
            wasStarted = true
        }

        override fun processStartFailed(t: Throwable?) {
            isFinished = true
            sc?.processStartFailed(t)
        }

        override fun processComplete(exitValue: Int) {
            isFinished = true
            sc?.processComplete(exitValue)
        }
    }


    companion object {

        private fun createAndStartProcess(cmds: MutableList<String>,
                                          workingDir: String,
                                          configurator: IProcessBuilderConfigurator?,
                                          sc: ShellCallback?,
                                          tc: ThreadsCallback?,
                                          latch: CountDownLatch?): Process? {

            for (i in cmds.indices) {
                cmds[i] = String.format(Locale.US, "%s", cmds[i])
            }

            val pb = ProcessBuilder(cmds)

            if (workingDir.isNotEmpty()) {
                val workingDirFile = File(workingDir)
                if (workingDirFile.exists() && workingDirFile.isDirectory) {
                    pb.directory(workingDirFile)
                } else {
                    System.err.println("working directory $workingDir not exists")
                }
            }

            if (sc != null && sc.needToLogCommands()) {
                val cmdlog = StringBuilder()
                for (cmd in cmds) {
                    cmdlog.append(cmd)
                    cmdlog.append(' ')
                }
                sc.shellOut(StreamType.CMD, cmdlog.toString())
            }

            configurator?.configure(pb)

            var process: Process? = null
            var startEx: IOException? = null

            try {
                process = pb.start()
            } catch (e: IOException) {
                startEx = e
            }

            if (process == null) {
                sc?.processStartFailed(startEx)
                return null
            } else {
                sc?.processStarted()
            }

            val outThreadInfo = CmdThreadInfo(cmds, workingDir, StreamType.OUT)
            val outThread = StreamConsumeThread(outThreadInfo, process.inputStream, sc, tc, latch)
            outThread.start()
            tc?.onThreadStarted(outThreadInfo, outThread)

            val errThreadInfo = CmdThreadInfo(cmds, workingDir, StreamType.ERR)
            val errThread = StreamConsumeThread(errThreadInfo, process.errorStream, sc, tc, latch)
            errThread.start()
            tc?.onThreadStarted(errThreadInfo, errThread)

            return process
        }



        fun execProcessAsync(cmd: String,
                             workingDir: String = "",
                             configurator: IProcessBuilderConfigurator? = null,
                             sc: ShellCallback?,
                             tc: ThreadsCallback?): Boolean {
            return execProcessAsync(mutableListOf(cmd), workingDir, configurator, sc, tc)
        }

        /**
         * @return true if started successfully, false - otherwise
         */
        fun execProcessAsync(cmds: MutableList<String>,
                             workingDir: String= "",
                             configurator: IProcessBuilderConfigurator? = null,
                             sc: ShellCallback?,
                             tc: ThreadsCallback?): Boolean {
            println("execProcessAsync(), cmds=$cmds, workingDir=$workingDir, configurator=$configurator, sc=$sc, tc=$tc")
            val latch = CountDownLatch(2)
            val process = createAndStartProcess(cmds, workingDir, configurator, sc, tc, latch)
            if (process != null) {
                ProcessWaitThread(process, sc, latch).start()
                return true
            }
            return false
        }

        fun execProcess(cmd: String, workingDir: String, sc: ShellCallback?, tc: ThreadsCallback?): CommandResult {
            return execProcess(cmd, workingDir, null, DEFAULT_TARGET_CODE, sc, tc)
        }

        fun execProcess(cmd: String, workingDir: String, configurator: IProcessBuilderConfigurator?, targetExitCode: Int?, sc: ShellCallback?, tc: ThreadsCallback?): CommandResult {
            return execProcess(mutableListOf(cmd), workingDir, configurator, targetExitCode, sc, tc)
        }

        fun execProcess(cmds: MutableList<String>, workingDir: String, sc: ShellCallback?, tc: ThreadsCallback?): CommandResult {
            return execProcess(cmds, workingDir, null, DEFAULT_TARGET_CODE, sc, tc)
        }

        /**
         * @return result code; -1 if start failed or interrupted
         */
        fun execProcess(cmds: MutableList<String>,
                        workingDir: String,
                        configurator: IProcessBuilderConfigurator?,
                        targetExitCode: Int?,
                        sc: ShellCallback?, tc: ThreadsCallback?): CommandResult {
            println("execProcess(), cmds=$cmds, workingDir=$workingDir, configurator=$configurator, targetExitCode=$targetExitCode, sc=$sc, tc=$tc")

            val stdOutLines = ArrayList<String>()
            val stdErrLines = ArrayList<String>()

            val latch = CountDownLatch(2)

            val wrappedCallback = WrappedShellCallback(sc, stdOutLines, stdErrLines)
            val process = createAndStartProcess(cmds, workingDir, configurator, wrappedCallback, tc, latch)

            if (wrappedCallback.wasStarted && !wrappedCallback.isFinished) {
                try {
                    latch.await()
                } catch (e: InterruptedException) {
                    System.err.println("an InterruptedException occurred during await(): $e")
                }
            }

            var exitCode = -1
            try {
                exitCode = process?.waitFor() ?: -1
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                System.err.println("an InterruptedException occurred during waitFor(): $e")
            } finally {
                process?.destroy()
                sc?.processComplete(exitCode)
            }

            return CommandResult(targetExitCode, exitCode, stdOutLines, stdErrLines)
        }
    }
}
