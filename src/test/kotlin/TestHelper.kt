import kossh.impl.SSHOptions

object TestHelper {
    val opts = SSHOptions("127.0.0.1", username="test", password="testing")
    val oschoices = listOf("linux", "darwin", "aix", "sunos")

    fun <T> howLongFor(what: ()->T): Pair<Long, T> {
        val begin = System.currentTimeMillis()
        val result = what()
        val end = System.currentTimeMillis()
        return Pair(end-begin, result)
    }
}
