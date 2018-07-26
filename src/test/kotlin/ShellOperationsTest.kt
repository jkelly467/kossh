import io.kotlintest.shouldBe
import io.kotlintest.matchers.*
import io.kotlintest.matchers.string.beBlank
import io.kotlintest.matchers.string.include
import io.kotlintest.should
import io.kotlintest.specs.StringSpec
import kossh.impl.SSH
import kossh.util.SSHTools
import kossh.util.UnknownOS
import java.util.*

class ShellOperationsTest: StringSpec({
    "helper methods" {
        val testfile = "sshapitest.dummy"
        val testdir = "sshapitest-dummydir"
        val started = Date()

        SSH.shell(TestHelper.opts) {
            //create a dummy file and dummy directory
            execute("echo -n 'toto' > $testfile")

            val homedir = executeAndTrim("pwd")
            val rhostname = executeAndTrim("hostname")

            mkdir(testdir) shouldBe true
            TestHelper.oschoices should contain(uname().toLowerCase())
            TestHelper.oschoices should contain(osname().toLowerCase())
            whoami() shouldBe options.username
            osid() shouldNot beInstanceOf(UnknownOS::class)
            id() should include("test")
            arch() shouldNot beBlank()
            env().size shouldBe gt(0)
            hostname() shouldBe rhostname
            fileSize(testfile) shouldBe 4L
            md5sum(testfile) shouldBe "f71dbe52628a3f83a77ab494817525c6"
            md5sum(testfile) shouldBe SSHTools.md5sum("toto")
            sha1sum(testfile) shouldBe "0b9c2625dc21ef05f6ad4ddf47c5f203837aa32c"
            ls() should contain(testfile)
            cd(testdir)
            pwd() shouldBe "$homedir/$testdir"
            cd()
            pwd() shouldBe homedir
            test("1 = 1") shouldBe true
            test("1 = 2") shouldBe false
            isFile(testfile) shouldBe true
            isDirectory(testfile) shouldBe false
            exists(testfile) shouldBe true
            exists(testdir) shouldBe true
            isExecutable(testfile) shouldBe false
            findAfterDate(".", started).size shouldBe between(1,3)

            val reftime = Date().time
            date().time shouldBe between(reftime-5000, reftime+5000)
            fsFreeSpace("/tmp") should { it != null }
            fileRights("/tmp") should { it != null }
            ps().size shouldBe gt(0)
            du("/bin") shouldBe gt(0)
            cat(testfile) should include("toto")
            rm(testfile)
            notExists(testfile) shouldBe true
            rmdir(testdir) shouldBe true
            mkcd(testdir) shouldBe true
            basename(pwd()) shouldBe testdir
            dirname(pwd()) shouldBe homedir
            touch(listOf("hello"))
            exists("hello") shouldBe true
            rm("hello")
            cd("..")
            rmdir(testdir) shouldBe true
            notExists(testdir) shouldBe true
            echo("hello") shouldBe "hello\n"
            alive() shouldBe true
            which("ls") shouldBe "/bin/ls"
            dirname(which("ls")!!) shouldBe ("/bin")
        }
    }

    "shell disable history test" {
        SSH.shell(TestHelper.opts) {
            if (exists(".bash_history")) {
                val msgBefore = "shell history before test ${Date().time}"
                val msgAfter = "shell history after test ${Date().time}"
                execute("echo $msgBefore")
                execute("history | grep 'shell history'") should include(msgBefore)
                disableHistory()
                execute("echo $msgAfter")
                execute("history | grep 'shell history'") shouldNot include(msgBefore)
                execute("history | grep 'shell history'") shouldNot include(msgAfter)
            }
        }
    }

    "more ls and mkdir tests" {
        SSH.shell(TestHelper.opts) {
            execute("rm -rf ~/test1 ~/test2")

            mkdir("test1") shouldBe true
            ls("test1").size shouldBe 0
            rmdir("test1") shouldBe true
            mkcd("test2") shouldBe true
            pwd().split("/").last() shouldBe "test2"
            cd("..")
            rmdir("test2") shouldBe true
        }
    }

    "catData test" {
        SSH.shell(TestHelper.opts) {
            rm ("checkthat")
            catData("hello\nworld", "checkthat")
            cat("checkthat").trim() shouldBe "hello\nworld"
        }
    }

})