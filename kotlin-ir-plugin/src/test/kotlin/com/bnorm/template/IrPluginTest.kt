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
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.org.objectweb.asm.*
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists

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
  import javaslang.Tuple3

  @PersistableContinuation("bookTrip")
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
    val result = compile(SourceFile.fromPath(File(javaClass.getResource("/Test3.kt")!!.file)))
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    val methodsContinuations = HashMap<Triple<String?, String?, String?>, Type>()
    val outerClasses = HashMap<String, Triple<String?, String?, String?>>()
    val persistableContinuationClasses = HashSet<String>()
    val methodsDescriptors = HashMap<Pair<String, String?>, HashSet<String?>>()
    for (compiledClassAndResourceFile in result.compiledClassAndResourceFiles) {
      if (compiledClassAndResourceFile.extension == "class") {
        val classReader = ClassReader(FileInputStream(compiledClassAndResourceFile).use { it.readAllBytes() })
        classReader.accept(object : ClassVisitor(Opcodes.ASM5) {
          var outerClass: Triple<String?, String?, String?>? = null

          override fun visitOuterClass(owner: String?, name: String?, descriptor: String?) {
            val outerClass = Triple(owner, name, descriptor)
            this.outerClass = outerClass
            val className = classReader.className
            if (className != null) {
              outerClasses[className] = outerClass
            }
            super.visitOuterClass(owner, name, descriptor)
          }

          override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
            if (descriptor == "Lkotlin/coroutines/jvm/internal/DebugMetadata;") {
              return object : AnnotationVisitor(Opcodes.ASM5, super.visitAnnotation(descriptor, visible)) {
                lateinit var clazz: String
                lateinit var method: String
                override fun visit(name: String?, value: Any?) {
                  when (name) {
                    "m" -> {
                      method = value.cast()
                      if (value != outerClass?.second) {
                        outerClass = null
                      }
                    }

                    "c" -> {
                      clazz = value.cast()
                      if (value != outerClass?.first) {
                        outerClass = null
                      }
                    }

                    else -> {}
                  }
                  super.visit(name, value)
                }

                override fun visitEnd() {
                  methodsContinuations[outerClass ?: Triple(clazz, method, null)] = Type.getObjectType(
                    classReader.className.replace('.', '/')
                  )
                  super.visitEnd()
                }
              }
            }
            return super.visitAnnotation(descriptor, visible)
          }

          override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?,
          ): MethodVisitor {
            methodsDescriptors.getOrPut(Pair(classReader.className, name)) { HashSet() }.add(descriptor)
            return object : MethodVisitor(
              Opcodes.ASM5, super.visitMethod(access, name, descriptor, signature, exceptions)
            ) {
              override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                if (descriptor == "Lcom/bnorm/template/CompiledPersistableContinuation;") {
                  persistableContinuationClasses.add(classReader.className)
                }
                return super.visitAnnotation(descriptor, visible)
              }
            }
          }
        }, 0)
      }
    }
    for (method in methodsContinuations.keys.filter { it.third == null }) {
      methodsContinuations[method.copy(third = methodsDescriptors[Pair(method.first, method.second)]!!.single())] =
        methodsContinuations.remove(method)!!
    }

    val outerClassesAnonymousClasses = HashMap<Triple<String?, String?, String?>, HashSet<String>>()
    fun rec(className: String) {
      val outerClass = outerClasses[className]
      if (outerClass != null) {
        val absent = HashSet<String>()
        val prev = outerClassesAnonymousClasses.putIfAbsent(outerClass, absent)
        (prev ?: absent).add(className)
        if (prev == null) {
          rec(outerClass.first!!)
        }
      }
    }
    persistableContinuationClasses.forEach { rec(it) }

    for (compiledClassAndResourceFile in result.compiledClassAndResourceFiles) {
      if (compiledClassAndResourceFile.extension == "class") {
        val classReader = ClassReader(FileInputStream(compiledClassAndResourceFile).use { it.readAllBytes() })
        val classWriter = ClassWriter(0)
        classReader.accept(object : ClassVisitor(Opcodes.ASM5, classWriter) {
          override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?,
          ): MethodVisitor {
            val methodDescriptor = Triple(classReader.className, name, descriptor)
            val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
            val continuationClass = methodsContinuations.remove(methodDescriptor)
            if (continuationClass != null) {
              val annotationVisitor = methodVisitor.visitAnnotation(
                "Lcom/bnorm/template/FunctionContinuation;", true
              )
              annotationVisitor.visit("value", continuationClass)
              annotationVisitor.visitEnd()
            }
            val anonymousClasses = outerClassesAnonymousClasses.remove(methodDescriptor)
            if (anonymousClasses != null) {
              val annotationVisitor = methodVisitor.visitAnnotation("Lcom/bnorm/template/AnonymousClasses;", true)
              val arrayVisitor = annotationVisitor.visitArray("v")
              anonymousClasses.forEach {
                arrayVisitor.visit(null, Type.getObjectType(it.replace('.', '/')))
              }
              arrayVisitor.visitEnd()
              annotationVisitor.visitEnd()
            }
            return methodVisitor
          }
        }, 0)
        FileOutputStream(compiledClassAndResourceFile).use { it.write(classWriter.toByteArray()) }
      }
    }
    assert(outerClassesAnonymousClasses.isEmpty())
    assert(methodsContinuations.isEmpty())

    val declaredMethod = result.classLoader.loadClass("Test3").getDeclaredMethod("main", PersistedString::class.java)
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
