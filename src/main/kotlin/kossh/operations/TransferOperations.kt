package kossh.operations

import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

interface TransferOperations: CommonOperations {

    /**
     * Returns possible content from a remote file called [remoteFilename] as a string
     */
    fun get(remoteFilename: String): String?

    /**
     * Returns possible content from a remote file called [remoteFilename] as an array of bytes
     */
    fun getBytes(remoteFilename: String): ByteArray?

    /**
     * Copies the remote file called [remoteFilename] to the local [outputStream]
     */
    fun receive(remoteFilename: String, outputStream: OutputStream)

    /**
     * Uploads string [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    fun put(data:String, remoteDestination: String)

    /**
     * Uploads byte array [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    fun putBytes(data:ByteArray, remoteDestination: String)

    /**
     * Uploads [howmany] bytes of input stream [data] to a remote file at [remoteDestination]. If the file exists, it is overwritten.
     */
    fun putFromStream(data: InputStream, howmany:Int, remoteDestination: String)

    /**
     * Copies a [localFile] to a remote file at [remoteDestination]
     */
    fun send(localFile: File, remoteDestination: String)

    /**
     * Copies the remote file called [remoteFilename] to the local [localFile]
     */
    fun receive(remoteFilename: String, localFile: File) {
        receive(remoteFilename, FileOutputStream(localFile))
    }

    /**
     * Copies the remote file called [remoteFilename] to a local file called [localFilename]
     */
    fun receive(remoteFilename: String, localFilename: String) {
        receive(remoteFilename, File(localFilename))
    }

    /**
     * Returns a file copied and compressed from a remote file called [remoteFilename] to a local file called [localFilename]
     */
    fun receiveNcompress(remoteFilename: String, localFilename: String): File {
        val dest = File(localFilename)

        if (dest.isDirectory()) {
            val basename = remoteFilename.split("/+").last()
            return receiveNcompress(remoteFilename, dest, basename)
        } else {
            val destdir = if(dest.parentFile?.exists() ?: false) dest.parentFile else File(".")
            val basename = dest.name
            return receiveNcompress(remoteFilename, destdir, basename)
        }
    }

    /**
     * Returns a file copied and compressed from a remote file called [remoteFilename] to a local file called [localBasename] in directory [localDirectory]
     */
    fun receiveNcompress(remoteFilename: String, localDirectory: File, localBasename: String): File {
        val (outputStream, localFile) = if (compressedCheck(remoteFilename) != null) {
            val local = File(localDirectory, localBasename)
            val output = FileOutputStream(local)
            Pair<OutputStream,File>(output, local)
        } else {
            val destfilename = if (localBasename.endsWith(".gz")) localBasename else localBasename+".gz"
            val local  = File(localDirectory, destfilename)
            val output = FileOutputStream(local)
            val compressedOutput = CompressorStreamFactory().createCompressorOutputStream(
                CompressorStreamFactory.GZIP,
                output
            )
            Pair<OutputStream,File>(compressedOutput, local)
        }

        receive(remoteFilename, outputStream)
        return localFile
    }

    /**
     * Copies the remote file called [remoteFilename] to a local file with the same name
     */
    fun receive(filename:String) {
        receive(filename, File(filename))
    }

    /**
     * Copies a local file at [fromLocalFilename] to a remote file at [remoteDestination]
     */
    fun send(fromLocalFilename:String, remoteDestination: String) {
        send(File(fromLocalFilename), remoteDestination)
    }

    /**
     * Copies a local file at [filename] to a remote file with the same name
     */
    fun send(filename:String) {
        send(File(filename), filename)
    }

    private fun compressedCheck(filename: String): String? {
        val GZ = """.*[.]gz$""".toRegex()
        val XZ = """.*[.]xz$""".toRegex()
        val BZ = """.*[.](?:(?:bz2)|(?:bzip2))""".toRegex()

        val f = filename.toLowerCase()

        if (f.matches(GZ)) {
            return "gz"
        } else if (f.matches(BZ)) {
            return "bz"
        } else if (f.matches(XZ)) {
            return "xz"
        } else {
            return null
        }
    }
}