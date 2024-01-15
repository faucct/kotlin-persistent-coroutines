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
import kotlin.test.assertEquals
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
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

  @Test
  fun `IR plugin success3`() {
    val result = compile(
      sourceFile = SourceFile.kotlin(
        "Main.kt", """
import com.bnorm.template.Color
import com.bnorm.template.PersistingWrapper.wrapper
import com.bnorm.template.Persistor
import com.bnorm.template.SingletonKSerializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.coroutines.coroutineContext
import java.util.*
import com.bnorm.template.PersistableContinuation
import com.bnorm.template.PersistedField
import com.bnorm.template.PersistencePoint

var persisting = false
@PersistableContinuation
suspend fun persist() {
  persisting = true
  @PersistencePoint("persist") val persist = coroutineContext[Persistor]!!.persist()
  if (persisting) {
    throw RuntimeException("")
  }
}

class Main {
  @PersistableContinuation
  suspend fun foo() {
    @PersistedField val a = "a"
    println("hi")
    @PersistencePoint("barring") val barring = persist()
    @PersistencePoint("delaying") val delaying = bar()
    println("then ${'$'}a")
    delay(1000)
    yield()
    println("later")
  }

  @PersistableContinuation
  suspend fun bar() {
    println("bar")
    @PersistencePoint("delaying") val delaying = persist()
    println("then")
    delay(1000)
    yield()
    println("later")
  }

  suspend fun supplier(): Int {
    persist()
    return 0
  }

  suspend fun consumer(a: Int, b: Int) {
    println("bar")
    persist()
    println("then")
    delay(1000)
    yield()
    println("later")
  }

  suspend fun supplierConsumer() {
    consumer(
      Random().nextInt(),
      supplier(),
    )
  }

  companion object {
    @JvmStatic
    fun main() {
      Json.encodeToString(Json.serializersModule.serializer(), Color(1, 2))
      val main = Main()
      val json = Json {
        serializersModule = SerializersModule {
          contextual(Main::class, SingletonKSerializer(main))
        }
      }
      val message = runBlocking @PersistableContinuation {
        (wrapper(Main::class.java.classLoader, json)) @PersistableContinuation {
          @PersistencePoint("fooing") val fooing = Main().foo()
          "foo"
        }
      }
      println(message)
    }
  }
}
"""
      )
    )
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    println(result.classLoader.loadClass("Main").getDeclaredMethod("main").invoke(null))
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
