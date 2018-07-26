import TestHelper.opts
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kossh.impl.SSH
import org.slf4j.LoggerFactory

class StabilityTest: StringSpec({
    val log = LoggerFactory.getLogger(this.javaClass)

    "stability test" {
        val max = 200
        var failed = 0
        (1..max).forEach { i ->
            try {
                SSH.once(opts) { execute("true") }
            } catch (ex: Exception) {
                log.info("Failed on '$ex' for #$i/$max")
                failed++
            }
        }
        log.info("Failed $failed times for $max attempts (${(failed*100/max)}%)")
        failed shouldBe 0
    }
})