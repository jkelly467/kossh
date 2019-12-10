package kossh.impl

import com.jcraft.jsch.ChannelExec
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kossh.util.*
import kotlinx.coroutines.*
import java.io.Closeable

class SSHCoExec(cmd: String,
                out: suspend ExecResult.()->Unit,
                err: suspend ExecResult.()->Unit,
                val ssh: SSH): Closeable, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val channel: ChannelExec = ssh.jschsession().openChannel("exec") as ChannelExec
    private val stdout: InputStream
    private val stderr: InputStream
    private val stdin: OutputStream

    init {
        channel.setCommand(cmd.toByteArray())
        stdout = channel.inputStream
        stderr = channel.errStream
        stdin = channel.outputStream
        channel.setPty(ssh.options.execWithPty)
        channel.connect(ssh.options.connectTimeout.toInt())
    }

    private val stdoutJob = launch {
        if (ssh.options.timeout > 0) {
            withTimeoutOrNull(ssh.options.timeout) {
                run(channel, stdout, out, ssh.options.charset.toCharset())
            }
        } else {
            run(channel, stdout, out, ssh.options.charset.toCharset())
        }
    }

    private val stderrJob = launch {
        if (ssh.options.timeout > 0) {
            withTimeoutOrNull(ssh.options.timeout) {
                run(channel, stderr, err, ssh.options.charset.toCharset())
            }
        } else {
            run(channel, stderr, err, ssh.options.charset.toCharset())
        }
    }

    /**
     * Waits for all output to finish.
     */
    suspend fun waitForEnd() {
        stdoutJob.join()
        stderrJob.join()
        close()
    }

    /**
     * Closes the exec channel and all open streams.
     */
    override fun close() {
        stdin.close()
        channel.disconnect()
    }

    private suspend fun run(channel: ChannelExec, input: InputStream,
                    output: suspend ExecResult.()->Unit, charset:Charset) {
        val bufsize = 16*1024
        val binput = input.buffered()
        val bytes = ByteArray(bufsize)
        val buffer = ByteBuffer.allocate(bufsize)
        val appender = StringBuilder()
        var eofreached = false

        try {
            do {
                val howmany = binput.read(bytes, 0, bufsize)
                if (howmany == -1) eofreached = true
                if (howmany > 0) {
                    buffer.put(bytes, 0, howmany)
                    buffer.flip()
                    val cbOut = charset.decode(buffer)
                    buffer.compact()
                    appender.append(cbOut.toString())
                    var s = 0
                    var e: Int
                    do {
                        e = appender.indexOf("\n", s)
                        if (e >= 0) {
                            ExecPart(appender.substring(s, e)).output()
                            s = e + 1
                        }
                    } while (e != -1)
                    appender.delete(0, s)
                }
            } while (isActive && !eofreached)

            if (appender.isNotEmpty()) {
                ExecPart(appender.toString()).output()
            }
            ExecEnd(channel.exitStatus).output()
        } catch (e: InterruptedIOException) {
            ExecTimeout().output()
        } catch (e: InterruptedException) {
            ExecTimeout().output()
        }
    }
}
