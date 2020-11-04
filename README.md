# assimp

[![Build Status](https://github.com/kotlin-graphics/assimp/workflows/build/badge.svg)](https://github.com/kotlin-graphics/assimp/actions?workflow=build)
[![license](https://img.shields.io/badge/License-MIT-orange.svg)](https://github.com/kotlin-graphics/assimp/blob/master/LICENSE) 
[![Release](https://jitpack.io/v/kotlin-graphics/assimp.svg)](https://jitpack.io/#kotlin-graphics/assimp) 
![Size](https://github-size-badge.herokuapp.com/kotlin-graphics/assimp.svg)
[![Github All Releases](https://img.shields.io/github/downloads/kotlin-graphics/assimp/total.svg)]()
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin) 

JVM porting of [Assimp](https://github.com/assimp/assimp)

This port is being written trying to stick as much as possible close to the C version in order to:

- minimize maintenance to keep it in sync with the original
- minimize differences for people used to dev/work with Assimp

### Code ###
A small example how to load files:
- for java users:
```java
AiScene scene = new Importer().readFile("test/resources/models/OBJ/box.obj");
```
- for kotlin users:
```kotlin
val scene = Importer().readFile("test/resources/models/OBJ/box.obj")
```

[Port Status](https://github.com/java-graphics/assimp/wiki/Status)

[Format priority list](https://github.com/java-graphics/assimp/wiki/Priority-list-of-file-formats)

If you have a format or a feature which is not yet supported, you can use the original assimp (or the lwjgl one) to load the mesh you have and save it in assimp binary format (`.assbin`). Once done, you can load it with this port. 

In case you don't know how to do it, you may open an [issue](https://github.com/kotlin-graphics/assimp/issues) giving the mesh, specifying the options and I'll convert it for you into binary assimp.

Please note that using the binary assimp format is also the fastest way to import meshes into your application.

The development is essentially feature-driver, if you want to express your preference -> [Format wish list](https://github.com/java-graphics/assimp/wiki/wish-list)

### Contributions:

Do not hesitate to offer any help: pushes (java or kotlin, it doesn't matter), testing, website, wiki, etc

### Comparison to a simple binding

Advantages:

- runs entirely on jvm (Garbage Collector)
- lighter import
- written in Kotlin (less code to write, more features, more expressiveness)
- cleaner, more intuitive interface (especially the [Material part](https://github.com/kotlin-graphics/assimp/blob/master/src/main/kotlin/assimp/material.kt#L385-L413))
- plain names, without prefixes
- possibility to set the build-time flags and property (debug/config/log)
- easier to debug
- easier to modify/customize, e.g: textures get automatically loaded and offered via [gli library](https://github.com/kotlin-graphics/glm), you just have to upload them to GL
- matrices are column-major instead of row-major and offered via the [glm library](https://github.com/kotlin-graphics/glm)
- easier to fix (found a couple of bugs on the original assimp, opened an issue, I didn't have to wait for the next releases fix)
- reduced the maintenance at minimum by keeping the same structure as possible during the port
- possible to get the same loading speed, using binary assimp format

Disadvantages:
- code needs to be ported from cpp to java
- code needs to be maintained
- a little slower compared to cpp when loading big meshes if not using assbin
