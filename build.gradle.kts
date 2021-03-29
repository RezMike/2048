import com.soywiz.korge.gradle.*

buildscript {
    repositories {
        mavenLocal()
        google()
        maven { url = uri("https://dl.bintray.com/korlibs/korlibs") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:2.0.9.1")
    }
}

apply<KorgeGradlePlugin>()

korge {
    id = "io.github.rezmike.2048"
    name = "2048"
    icon = file("src/commonMain/resources/korge.png")
}
