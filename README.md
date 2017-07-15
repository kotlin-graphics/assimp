# assimp

Java (~kotlin) porting of [Assimp](https://github.com/assimp/assimp)

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
