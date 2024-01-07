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

package com.bnorm.template

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.assertEquals
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.junit.Test

class IrPluginTest {
  @Test
  fun `IR plugin success`() {
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "main.kt", """
fun main() {
  println(debug())
}

fun debug() = "Hello, World!"
"""
      )
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
  }

  @Test
  fun `IR plugin success2`() {
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "main.kt", """
  import kotlinx.coroutines.async
  import kotlinx.coroutines.coroutineScope
  import kotlinx.coroutines.delay
  import java.util.*
  import com.bnorm.template.PersistableContinuation
  import com.bnorm.template.PersistedField
  import com.bnorm.template.PersistencePoint
  import com.bnorm.template.Typed
  import javaslang.Tuple3

  @PersistableContinuation
  suspend fun bookTrip(@PersistedField name: String) {
    @PersistedField val carReservationID = Random().nextInt()
    @PersistencePoint("a") val a = delay(0)
    @PersistencePoint("b") val ignored = delay(0)
//    @PersistencePoint("c") val _10 = persist()
//    rollbackIfThrows({ println("undoing carReservationID") }) {
//      @PersistedField val hotelReservationID = Random().nextInt()
//      @PersistencePoint("d") val _20 = persist()
//      rollbackIfThrows({ println("undoing hotelReservationID") }) {
//        @PersistedField val flightReservationID = Random().nextInt()
//        @PersistencePoint("e") val _30 = persist()
////        Tuple3(carReservationID, hotelReservationID, flightReservationID)
//      }
//    }
  }

  inline fun <T> rollbackIfThrows(rollback: () -> Unit, throwing: () -> T): T {
    try {
      return throwing()
    } catch (suppressing: Throwable) {
      try {
        rollback()
      } catch (suppressed: Throwable) {
        suppressing.addSuppressed(suppressed)
      }
      throw suppressing
    }
  }
"""
      )
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
  }
}

fun compile(
  sourceFiles: List<SourceFile>,
  plugin: ComponentRegistrar = TemplateComponentRegistrar(),
): KotlinCompilation.Result {
  return KotlinCompilation().apply {
    sources = sourceFiles
    useIR = true
    compilerPlugins = listOf(plugin)
    inheritClassPath = true
  }.compile()
}

fun compile(
  sourceFile: SourceFile,
  plugin: ComponentRegistrar = TemplateComponentRegistrar(),
): KotlinCompilation.Result {
  return compile(listOf(sourceFile), plugin)
}
