import TestHelper.opts
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kossh.impl.SSH
import java.io.File

class CompressedTransferTest: StringSpec({

    "simple test" {
        val content = "Hello world"
        val testedfile = "testme-tobecompressed.txt"
        val gztestedfile = testedfile+".gz"
        val gztestedfileMD5 = "38570c70a362855368dd8c5f25a157f7"

        fun clean() {
            File(testedfile).delete()
            File(gztestedfile).delete()
        }

        SSH.ftp(opts) { put(content, testedfile) }
        SSH.ftp(opts) { get(testedfile) shouldBe content }
        clean()

        SSH.once(opts) {
            receive(testedfile, testedfile)
            File(testedfile).bufferedReader().readText() shouldBe content
            receiveNcompress(testedfile, testedfile)
            File(gztestedfile).exists() shouldBe true
            localmd5sum(gztestedfile) shouldBe gztestedfileMD5
        }
        clean()

        SSH.shell(opts) {
            ftp {
                receive(testedfile, testedfile)
                File(testedfile).bufferedReader().readText() shouldBe content
                receiveNcompress(testedfile, testedfile)
                File(gztestedfile).exists() shouldBe true
                localmd5sum(gztestedfile) shouldBe gztestedfileMD5
            }
        }
        clean()

        SSH.ftp(opts) {
            receive(testedfile, testedfile)
            File(testedfile).bufferedReader().readText() shouldBe content
            receiveNcompress(testedfile, testedfile)
            File(gztestedfile).exists() shouldBe true
            localmd5sum(gztestedfile) shouldBe gztestedfileMD5
        }
        clean()
    }
})