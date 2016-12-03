# assimp

Java porting of [Assimp](https://github.com/assimp/assimp)

This port is being written trying to stick as much as possible close to the C version in order to:

- minimize maintenance to keep it up with the C version
- minimize differences for peoples used to dev/work with Assimp

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

###[Status](https://github.com/java-graphics/assimp/wiki/Status)

[Format priority list](https://github.com/java-graphics/assimp/wiki/Priority-list-of-file-formats)

If you want to express your preference -> [Format wish list](https://github.com/java-graphics/assimp/wiki/wish-list)

### Contributions:

Do not hesitate to offer any help: developing (java or kotlin is the same), testing, website creation, wiki writing, etc
