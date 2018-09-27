# Server extention for Java Runtime for Anthill Platform

This Runtime extends <a href="https://github.com/anthill-services/anthill-runtime-java">Java Runtime</a>,
adding functionality for Game Server to communicate with
<a href="https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#6-communication-between-game-server-and-controller-service">Controller Service</a>.

## How To Use

1. Override GameServerController class

```java
public class YourServerController extends GameServerController
{
    public YourServerController(String socket)
    {
        super(socket);
    }

    @Override
    protected String getStatus()
    {
        return "ok";
    }
}
```

See <a href="https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#9-the-game-server-instance-status">this</a> for `getStatus()` method reference.

2. Instantiate it in your Game Server instance

```java
ServerController = new YourServerController(sockets);
```

3. Once you Game Server instance initialized, call <a href="https://github.com/anthill-services/anthill-game/blob/master/doc/API.md#initialized-request">Initialized Request</a>:

```java
ServerController.inited(settings, new GameServerController.InitedHandler()
{
    @Override
    public void result(boolean success)
    {
        if (!success)
        {
            System.exit(-1);
        }
    }
});
```

4. Once a Player connected, call `ServerController.joined` and one a Player left, call `ServerController.left`.

## Installation

You would need to install [JZMQ](https://github.com/zeromq/jzmq) first:

Install build tools first (assuming Debian):
```
apt install -y g++ pkg-config libtool autoconf automake make
```

Next, install the zmq library:
```
apt install -y libzmq3-dev 
```

Next, install the jzmq into the local maven:
```
git clone https://github.com/zeromq/jzmq.git
cd jzmq/jzmq-jni/
./autogen.sh
./configure --prefix=/usr
make
make install
cd ..
mvn install -Dgpg.skip=true
```

You would also need to set `JAVA_LIBRARY_PATH` environment variable to 
`/usr/lib` for the application to work.

#### Gradle

1. Add the JitPack repository to your `build.gradle` file

```
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

2. Add the dependency:

```
dependencies {
    compile 'com.github.anthill-platform:anthill-runtime-java-server:0.1.6'
}
```

#### Maven

1. Add the JitPack repository to your `pom.xml` file

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

2. Add the dependency:

```xml
<dependency>
    <groupId>com.github.anthill-platform</groupId>
    <artifactId>anthill-runtime-java-server</artifactId>
    <version>0.1.7</version>
</dependency>
```