package com.bnorm.template

import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ProcessBytecodeAnnotations {
  operator fun invoke(files: List<File>) {
    val methodsContinuations = HashMap<Triple<String?, String?, String?>, Type>()
    val outerClasses = HashMap<String, Triple<String?, String?, String?>>()
    val persistableContinuationClasses = HashSet<String>()
    val methodsDescriptors = HashMap<Pair<String, String?>, HashSet<String?>>()
    for (compiledClassAndResourceFile in files) {
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

    for (compiledClassAndResourceFile in files) {
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
  }
}
