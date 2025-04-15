# Building SimpleClaimSystem

Working commits of this repo are known to build with OpenJDK 21 and 22.

## How-to

Ensure the `./gradlew` has executable permissions.

```shell
chmod +x ./gradlew
```

Compile it:

```shell
./gradlew clean buildFatJar
```

If you have to upgrade your JDK, gradle may still be using the old one. Fixing this may require thoroughly purging caches:
```
rm -rf ~/.gradle/daemon/
rm -rf ~/.gradle/caches/
rm -rf ~/.gradle/java*
rm -rf ~/.gradle/jdks/
```

and if it still does not find the correct JDK, you may have to set the path manually to wherever it is (list potential locations with `ls /usr/lib/jvm`), such as:
```
JAVA_HOME=/usr/lib/jvm/java-22-amazon-corretto
export PATH=$JAVA_HOME/bin:$PATH
```

Then try again to compile.

If successful your plugin is located in:

```
./build/libs/fat.jar
```

Rename put it in your spigot server in the folder plugins,
this is a recommended way to do it:

```
mv ./build/libs/fat.jar <your_spigot_server_location>/plugins/SimpleClaimSystem.jar
```
