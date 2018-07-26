# KOSSH - Kotlin SSH API  

High level Kotlin SSH API for easy and fast operations on remote servers. This is a Kotlin port of the Scala [JASSH](https://github.com/dacr/jassh) library.

The API is [JSCH](http://www.jcraft.com/jsch/) based.

----

## Hello World script

It requires a local user named "test" with password "testing", remember that you can remove the password, if your public key has been added in authorized_keys file of the test user.

```kotlin
#!/bin/sh
exec java -jar kossh.jar "$0" "$@"
!#
kossh.impl.SSH.once("localhost", "test", "testing") { 
  print(execute("""echo "Hello World from $(hostname)" """))
}
```

## Persisted shell session

```kotlin
#!/bin/sh
exec java -jar kossh.jar "$0" "$@"
!#
kossh.impl.SSH.shell("localhost", "test", "testtest") {
  println("initial directory is $pwd")
  cd("/tmp")
  println("now it is $pwd")
}
```

## Shell session to an SSH enabled  PowerShell Server (windows)
This functions much the same as a regular SSH connection, but many of the unix like commands are not supported and the terminal behaves differently
```kotlin
import kossh.impl.*

val settings = SSHOptions(host = host, username=user, password = pass, prompt = prompt, timeout = timeout)
val session = SSH(settings)

val shell = session.newPowerShell()

println(shell.ls())
println(shell.pwd())
```

## SSH Configuration notes

To turn on/off ssh root direct access or sftp ssh subsystem.
```
    Subsystem       sftp    ...     (add or remove comment)
    PermitRootLogin yes or no       (of course take care of security constraints)
```

AIX SSHD CONFIGURATION :
```
    vi /system/products/openssh/conf/sshd_config
    /etc/rc.d/rc2.d/S99sshd reload
```

LINUX SSHD CONFIGURATION
```
    vi /etc/ssh/sshd_config
    /etc/init.d/sshd reload
```

SOLARIS SSHD CONFIGURATION
```
    vi /usr/local/etc/ssh/sshd_config
    svcadm restart ssh
```

MAC OS X CONFIGURATION
```
    sudo vi /etc/sshd_config
    sudo launchctl load -w /System/Library/LaunchDaemons/ssh.plist
```
