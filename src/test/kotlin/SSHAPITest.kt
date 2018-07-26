import io.kotlintest.specs.StringSpec
import TestHelper.opts
import io.kotlintest.matchers.contain
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import kossh.impl.SSH
import kossh.impl.SSHOptions
import kossh.util.ExecPart
import kossh.util.ExecResult
import org.slf4j.LoggerFactory
import java.io.File

class SSHAPITest: StringSpec({
    val log = LoggerFactory.getLogger(this.javaClass)

    "One line exec with automatic resource close" {
        SSH.once(opts) { execute("expr 1 + 1").trim() shouldBe "2" }
        SSH.once(opts) { executeAndTrim("expr 1 + 1") shouldBe "2" }
        val year = SSH.once(opts) { executeAndTrim("expr 1 + 10").toInt() }
        year shouldBe 11
    }

    "Execution & file transfers within the same ssh session" {
        SSH.once(opts) {
            val rfile = "HelloWorld.txt"
            val lfile = "/tmp/sshtest.txt"

            File(rfile).delete()
            File(lfile).delete()

            val msg = execute("/bin/echo -n 'Hello ${System.getProperty("user.name", "")}'")

            put(msg, rfile)

            (get(rfile)) shouldBe msg

            receive(rfile,lfile)

            File(lfile).readLines().first() shouldBe msg
        }
    }

    "Execution & file transfers within the same SH and FTP persisted session" {
        SSH.shell(opts) {
            ftp {
                val rfile = "HelloWorld.txt"
                val lfile = "/tmp/sshtest.txt"

                File(rfile).delete()
                File(lfile).delete()

                val msg = execute("/bin/echo -n 'Hello ${System.getProperty("user.name", "")}'")

                put(msg, rfile)

                (get(rfile)) shouldBe msg

                receive(rfile,lfile)

                File(lfile).readLines().first() shouldBe msg
            }
        }
    }

    "shell coherency check" {
        SSH.shell(opts) {
            (1 .. 100).forEach { i ->
                executeAndTrim("echo ta$i") shouldBe "ta$i"
                executeAndTrim("echo ga$i") shouldBe "ga$i"
            }
        }
    }

    "Start a remote process in background" {
        SSH.once(opts) {
            var x = mutableListOf<String>()

            fun receiver(result: ExecResult) {
                if (result is ExecPart) {
                    x.add(result.content)
                }
            }

            val executor = run("for i in 1 2 3 4 5 ; do echo hello${'$'}1 ; done", ::receiver)
            executor.waitForEnd()

            x.forEachIndexed { index, s ->
                log.info("$index : %s")
            }
            x.size shouldBe 5
        }
    }

    "Usage case example for tutorial" {
        SSH.once(opts) {
            val uname = executeAndTrim("uname -a")
            val fsstatus = execute("df -m")
            val fmax = get("/etc/lsb-release")

            shell {
                val hostname = executeAndTrim("hostname")
                val files = execute("find /usr/lib")
            }
            ftp {
                val cpuinfo = get("/proc/cpuinfo")
                val meminfo = get("/proc/meminfo")
            }

            fun receiver(result: ExecResult) {
                if (result is ExecPart) {
                    log.info("received :${result.content}")
                }
            }

            val executor = run("for i in 1 2 3 ; do echo hello${'$'}i ; done", ::receiver)
            executor.waitForEnd()
        }
    }

    "Simplified persistent ssh shell usage" {
        SSH.shell("localhost", "test", "testing") {
            execute("ls -la")
            execute("uname")
        }
    }

    "Simplified persistent ssh shell and ftp usage" {
        SSH.shell(opts) {
            ftp {
                execute("ls -la")
                execute("uname")
                get("/proc/stat")
                get("/proc/vmstat")
            }
        }
    }

    "ssh compression" {
        val testfile = "test-transfer"

        fun withReusedSFTP(filename: String, ssh:SSH, howmany:Int, sizeKb:Int) {
            ssh.ftp {
                for (i in 1 .. howmany) {
                    getBytes(filename)?.size shouldBe (sizeKb*1024)
                }
            }
        }

        fun toTest(thattest: (String, SSH, Int, Int)->Unit,
                   howmany: Int,
                   sizeKb: Int,
                   comments: String,
                   ssh:SSH
        ) {
            ssh.execute("dd count=$sizeKb bs=1024 if=/dev/zero of=$testfile")
            val (d, _) = TestHelper.howLongFor { thattest(testfile, ssh, howmany, sizeKb) }
            log.info("Bytes rate: %.1fMb/s %dMb in %.1fs for %d files - %s".format((howmany*sizeKb*1000L/d/1024).toDouble(), sizeKb*howmany/1024, (d/1000).toDouble(), howmany, comments))
        }

        val withCompress = SSHOptions("localhost", "test", "testing", compress=null)
        val noCompress = SSHOptions("localhost", "test", "testing", compress=9)

        SSH.once(withCompress) {
            toTest(::withReusedSFTP, 1, 100*1024, "byterates using SFTP (max compression)", this)
        }
        SSH.once(noCompress) {
            toTest(::withReusedSFTP, 1, 100*1024, "byterates using SFTP (no compression)", this)
        }
    }

    "tunneling remote->local" {
        SSH.once("localhost", "test", "testing", port=22) {
            remoteToLocal(22022, "localhost", 22)
            SSH.once("localhost", "test",  "testing", port=22022) {
                executeAndTrim("echo 'works'") shouldBe "works"
            }
        }
    }

    "tunneling local->remote" {
        SSH.once("localhost", "test", "testing", port=22) {
            localToRemote(33033, "localhost", 22)
            SSH.once("localhost", "test", "testing", port=33033) {
                executeAndTrim("echo 'works'") shouldBe "works"
            }
        }
    }

    "remote ssh sessions (ssh tunneling ssh)" {
        val rssh = SSH(opts).remote(opts)
        rssh.options.port shouldNotBe 22
        rssh.executeAndTrim("echo 'hello'") shouldBe "hello"
        rssh.close()
    }

    "SCP/SFTP and special system file" {
        SSH.once(opts) {
            val r = get("/dev/null")
            r shouldNotBe null
            r shouldBe ""
        }
    }

    "sharing SSH options" {
        fun common(h: String) = SSHOptions(h, username="test", password="testing")
        SSH.once(common("localhost")) {
            executeAndTrim("echo 'hello'") shouldBe "hello"
        }
    }

    "env command test" {
        SSH.shell(opts) {
            execute("export ABC=1")
            execute("export XYZ=999")
            val envmap = env()

            envmap.keys should (contain("ABC") and contain("XYZ"))
        }
    }

    "exit code tests" {
        SSH.once(opts) {
            val (_, rc) = executeWithStatus("(echo 'toto' ; exit 2)")
            rc shouldBe 2
        }
        SSH.once(opts) {
            val (_, rc) = executeWithStatus("(echo 'toto' ; exit 3)")
            rc shouldBe 3
        }
    }
})