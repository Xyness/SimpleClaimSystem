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

If successful your plugin is located in:

```
./build/libs/fat.jar
```

Rename put it in your spigot server in the folder plugins,
this is a recommended way to do it:

```
mv ./build/libs/fat.jar <your_spigot_server_location>/plugins/SimpleClaimSystem.jar
```
