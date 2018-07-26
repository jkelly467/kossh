import TestHelper.opts
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import kossh.impl.SSH
import kossh.util.SSHTimeoutException

class SSHTest: StringSpec({

    "simple connectivity test" {
        SSH(opts).execute("echo 'hello'").trim() shouldBe "hello"
    }

//    "timeout test" {
//        val opts = TestHelper.opts.copy(timeout = 7000, connectTimeout = 2000)
//        SSH.once(opts) {
//            executeAndTrim("sleep 4; echo 'ok'") shouldBe "ok"
//            shouldThrow<SSHTimeoutException> {
//                executeAndTrim("sleep 10; echo 'ok'")
//            }
//        }
//    }

//    "timeout test with shell SSH session" {
//        val opts = TestHelper.opts.copy(timeout = 7000, connectTimeout = 2000)
//        SSH.shell(opts) {
//            executeAndTrim("sleep 4; echo 'ok'") shouldBe "ok"
//            shouldThrow<SSHTimeoutException> {
//                executeAndTrim("sleep 10; echo 'ok'")
//            }
//            executeAndTrim("echo 'good'") shouldBe "good"
//        }
//    }
})

