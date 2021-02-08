buildscript {
    repositories {
        maven(url = "https://maven.aliyun.com/repository/public/")
        maven(url = "https://maven.aliyun.com/repository/google/")
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin/")
        mavenLocal()
        mavenCentral()
    }
}
plugins {
    application
    id("org.openjfx.javafxplugin") version("0.0.9")
    id("org.beryx.jlink") version "2.23.2"
}

group = "com.cqsongjin"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

javafx {
    version = "15.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}

repositories {
    maven(url = "https://maven.aliyun.com/repository/public/")
    maven(url = "https://maven.aliyun.com/repository/google/")
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin/")
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("junit", "junit", "4.12")
    implementation("com.google.guava:guava:30.1-jre")
    implementation("com.alibaba:fastjson:1.2.75")
    implementation("org.slf4j:slf4j-api:1.7.29")
    implementation("ch.qos.logback:logback-classic:1.2.3")


    //lombok配置
//    compileOnly("org.projectlombok:lombok:1.18.16")
//    annotationProcessor("org.projectlombok:lombok:1.18.16")
//    testCompileOnly("org.projectlombok:lombok:1.18.16")
//    testAnnotationProcessor("org.projectlombok:lombok:1.18.16")
}

val mycopy by tasks.register<Copy>("mycopy") {
    from("${projectDir}/src/main/resources")
    into("${buildDir}/classes/java/main")
    println("project files")
    project.files().forEach {
        println(it.absolutePath)
    }
    println("runtime files")
    project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).files.forEach { println(it.absolutePath) }
}

tasks.named("run") {
    dependsOn(mycopy)
}


application {
    mainClass.set("com.cqsongjin.file.fly.APP")

}
tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Main-Class" to "com.cqsongjin.javafx.lab.HelloFX"
            )
        )
    }
//    println("project files1")
//    project.files().forEach {
//        println(it.absolutePath)
//    }
//    println("runtime files1")
//    project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME).files.forEach { println(it.absolutePath) }
}