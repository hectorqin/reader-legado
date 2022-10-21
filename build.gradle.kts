import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin ("jvm") version "1.5.31"
  application
  id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.htmake"
version = "2.7.2"

repositories {
  mavenCentral()
}

val vertxVersion = "4.2.1"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "com.htmake.reader.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
  mainClass.set(launcherClassName)
}

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-web")
  implementation("io.vertx:vertx-lang-kotlin-coroutines")
  implementation("io.vertx:vertx-lang-kotlin")
  implementation(kotlin("stdlib-jdk8"))

  // json
  implementation("com.google.code.gson:gson:2.8.5")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.+")

  // log
  implementation("io.github.microutils:kotlin-logging:1.6.24")
  implementation("uk.org.lidalia:sysout-over-slf4j:1.0.2")

  implementation("com.google.guava:guava:28.0-jre")

  // 网络
  implementation("com.squareup.okhttp3:okhttp:4.9.1")
  implementation("com.squareup.okhttp3:logging-interceptor:4.1.0")
  // Retrofit
  implementation("com.squareup.retrofit2:retrofit:2.6.1")
  implementation("com.julienviet:retrofit-vertx:1.1.3")

  //JS rhino
  // implementation("com.github.gedoor:rhino-android:1.6")
  implementation(fileTree("src/lib").include("rhino-*.jar"))

  // 规则相关
  implementation("org.jsoup:jsoup:1.14.1")
  implementation("cn.wanghaomiao:JsoupXpath:2.5.0")
  implementation("com.jayway.jsonpath:json-path:2.6.0")

  // xml
  // 弃用 xmlpull-1.1.4.0，因为它需要 Java9
  // implementation("org.xmlpull:xmlpull:1.1.4.0")
  implementation(fileTree("src/lib").include("xmlpull-*.jar"))
  // implementation("com.github.stefanhaustein:kxml2:2.5.0")

  //加解密类库
  implementation("cn.hutool:hutool-crypto:5.8.0.M1")

  // 转换繁体
  // implementation("com.github.liuyueyi.quick-chinese-transfer:quick-transfer-core:0.2.1")

  testImplementation("io.vertx:vertx-unit")
  testImplementation("junit:junit:4.13.1")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets["main"].java.srcDir("src/main/kotlin")

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest {
    attributes(mapOf("Main-Verticle" to mainVerticleName))
  }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnit()
  testLogging {
    events = setOf(PASSED, SKIPPED, FAILED)
  }
}

tasks.withType<JavaExec> {
  args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}
