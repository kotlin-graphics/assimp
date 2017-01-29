//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

// Required Gradle 3.3

buildscript {

    repositories {
        mavenCentral()
        gradleScriptKotlin()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", "1.1.0-beta-17"))
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

apply {
    plugin("kotlin")
    plugin("maven")
    plugin("com.github.johnrengelman.shadow")
}

repositories {
    mavenCentral()
    gradleScriptKotlin()
}

dependencies {
    compile(kotlinModule("stdlib", "1.1.0-beta-17"))
    testCompile("io.kotlintest:kotlintest:1.3.5")
//    testCompile("io.kotlintest:kotlintest:2.0-SNAPSHOT")
    compile("com.github.elect86:kotlin-unsigned:-SNAPSHOT")
    compile("com.github.elect86:glm:30982dc750")
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