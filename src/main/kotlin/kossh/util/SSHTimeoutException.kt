package kossh.util
class SSHTimeoutException(val stdout:String, val stderr: String): Exception("kossh.impl.SSH Timeout")