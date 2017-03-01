//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

buildscript {

    repositories {
        mavenCentral()
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", "1.1.0"))
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
    compile(kotlinModule("stdlib", "1.1.0"))
    testCompile("com.github.elect86:kotlintest:d8878d6da0944ec6bcbcdad6a1540bba021d768d")
    compile("com.github.elect86:kotlin-unsigned:405b68c99309459eeb24c2639f016f26dcf59ba3")
    compile("com.github.elect86:glm:05859e02c3529f7a95b4b8cefbeb16f9b5e0b515")
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