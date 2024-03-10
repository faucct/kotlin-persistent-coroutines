/*
 * Copyright (C) 2020 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.bnorm.template

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.test.assertEquals

class IrPluginTest {
  @Test
  fun success() {
    val result = compile(SourceFile.fromPath(File(javaClass.getResource("/UserCode.kt")!!.file)))
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

    ProcessBytecodeAnnotations(result.compiledClassAndResourceFiles)

    val declaredMethod = result.classLoader.loadClass("UserCode").getDeclaredMethod("main", PersistedString::class.java)
    val stringPersistedToFile = StringPersistedToFile(Files.createTempFile(null, null))
    stringPersistedToFile.path.deleteExisting()

    AutoCloseable {
      stringPersistedToFile.path.deleteIfExists()
      stringPersistedToFile.tmpPath.deleteIfExists()
    }.use {
      declaredMethod.invoke(null, stringPersistedToFile)
      assertEquals(
        """[{"type":"main","label":"fooing"},{"type":"foo","label":"barring","a":"a"},{"type":"persist","label":"persist"}]""",
        Files.newInputStream(stringPersistedToFile.path).use { String(it.readAllBytes()) },
      )
      declaredMethod.invoke(null, stringPersistedToFile)
      assertEquals(
        """[{"type":"main","label":"fooing"},{"type":"foo","label":"delaying","a":"a"},{"type":"bar","label":"delaying"},{"type":"persist","label":"persist"}]""",
        Files.newInputStream(stringPersistedToFile.path).use { String(it.readAllBytes()) },
      )
      declaredMethod.invoke(null, stringPersistedToFile)
      assertEquals(
        """[{"type":"main","label":"done"},{"type":"persist","label":"persist"}]""",
        Files.newInputStream(stringPersistedToFile.path).use { String(it.readAllBytes()) },
      )
      declaredMethod.invoke(null, stringPersistedToFile)
      assertEquals(
        """[{"type":"main","label":"done"},{"type":"persist","label":"persist"}]""",
        Files.newInputStream(stringPersistedToFile.path).use { String(it.readAllBytes()) },
      )
    }
  }
}

fun compile(
  sourceFiles: List<SourceFile>,
  plugin: CompilerPluginRegistrar = TemplateComponentRegistrar(),
): KotlinCompilation.Result {
  return KotlinCompilation().apply {
    sources = sourceFiles
    useIR = true
    compilerPluginRegistrars = listOf(plugin)
    inheritClassPath = true
  }.compile()
}

fun compile(
  sourceFile: SourceFile,
  plugin: CompilerPluginRegistrar = TemplateComponentRegistrar(),
): KotlinCompilation.Result {
  return compile(listOf(sourceFile), plugin)
}
