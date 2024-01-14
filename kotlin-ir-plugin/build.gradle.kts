plugins {
  java
  kotlin("jvm")
  kotlin("kapt")
  kotlin("plugin.serialization")
  id("com.github.gmazzo.buildconfig")
}

dependencies {
  compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.4.1")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
  implementation("com.esotericsoftware:kryo:5.4.0")
  implementation(kotlin("reflect"))

  kapt("com.google.auto.service:auto-service:1.0.1")
  compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

  testImplementation(kotlin("test-junit"))
  testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
  testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
}

buildConfig {
  packageName(group.toString())
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
}
