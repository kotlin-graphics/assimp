# assimp

Java porting of [Assimp](https://github.com/assimp/assimp)

This port is being written trying to stick as much as possible close to the C version in order to:

- reduce any future maintenance to keep it up with the C version
- reduce any possible difference for peoples used to dev/work with Assimp

#Supported import formats at the moment:

| Format        | status          | %   |
| ------------- |:---------------:|     |
| STL ascii     | done, .stl ext  | 90  |
| STL binary    | to test, stl ext| 80  |
| MD2           | to fix          | 50  |
| Wavefront     | to implement    | 0 |
| Ply           | to implement    | 0 |
| Collada       | to implement    | 0 |
| MD5           | to implement    | 0 |
| FBX           | to implement    | 0 |


[Format priority list](https://github.com/java-graphics/assimp/wiki/Priority-list-of-file-formats)

If you want to express your preference, [Format wish list](https://github.com/java-graphics/assimp/wiki/wish-list)

# Core right now:

- most of the Ai Identity
- Validation (almost, need to finish animation)
- BaseProcess (triangulation mesh in progress)

Missing/incomplete:
- ScenePreprocessor


# Contributions:

Do not hesitate to offer any help: developing, testing, website creation, wiki writing, etc
