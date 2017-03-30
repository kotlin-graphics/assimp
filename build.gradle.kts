//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {

    repositories {
        mavenCentral()
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", "1.1.1"))
//        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

apply {
    plugin("kotlin")
    plugin("maven")
//    plugin("com.github.johnrengelman.shadow")
}

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {
    compile(kotlinModule("stdlib", "1.1.1"))
    testCompile("io.kotlintest:kotlintest:2.0.0")
    compile("com.github.elect86:kotlin-unsigned:3a9f8afc98ed3636b7d8c2bbff1b1223f027fab4")
    compile("com.github.elect86:glm:51f9ee28a3dd327b2a2e63fedf58210950fc61ff")
}

allprojects {
    repositories {
        maven { setUrl("https://jitpack.io") }
    }
}

//the<ShadowJar>().apply {
//    manifest.attributes.apply {
//        put("Implementation-Title", "Gradle Jar File Example")
//        put("Implementation-Version", version)
//        put("Main-Class", "com.mkyong.DateUtils")
//    }
//
//    baseName = project.name + "-all"
//}