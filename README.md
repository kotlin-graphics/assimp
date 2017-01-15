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

###[Port Status](https://github.com/java-graphics/assimp/wiki/Status)

[Format priority list](https://github.com/java-graphics/assimp/wiki/Priority-list-of-file-formats)

The development is essentially feature-driver, if you want to express your preference -> [Format wish list](https://github.com/java-graphics/assimp/wiki/wish-list)

### Contributions:

Do not hesitate to offer any help: pushes (java or kotlin, it doesn't matter), testing, website, wiki, etc
