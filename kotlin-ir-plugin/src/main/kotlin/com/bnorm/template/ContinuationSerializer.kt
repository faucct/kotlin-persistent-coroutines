@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.bnorm.template

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.PrimitiveSerialDescriptor
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.serializer
import kotlin.coroutines.Continuation
import kotlin.coroutines.jvm.internal.BaseContinuationImpl
import kotlin.coroutines.jvm.internal.DebugMetadata

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
class ContinuationSerializer(
  private val classLoader: ClassLoader, private val rootContinuation: Continuation<*>,
  private val classSerializer: KSerializer<Class<*>> = ClassLoaderClassSerializer(classLoader),
) : KSerializer<Continuation<*>> {
  companion object {
    val continuationDescriptor = buildSerialDescriptor(
      Continuation::class.java.typeName, kotlinx.serialization.descriptors.StructureKind.MAP,
    ) {
      element("type", PrimitiveSerialDescriptor("type", PrimitiveKind.STRING))
    }
    val continuationsDescriptor = buildSerialDescriptor(
      Continuation::class.java.typeName, kotlinx.serialization.descriptors.StructureKind.LIST
    ) {
      element("type", continuationDescriptor)
    }
  }

  private fun forEachSpilledLocalNameAndField(
    debugMetadata: DebugMetadata, label: Int, closure: (String, String) -> Unit,
  ) {
    debugMetadata.indexToLabel.zip(
      debugMetadata.localNames.zip(debugMetadata.spilled),
    ) { spilledLabel, (localName, spilled) ->
      if (spilledLabel + 1 == label) {
        closure(localName, spilled)
      }
    }
  }

  override val descriptor: SerialDescriptor
    get() = buildSerialDescriptor("com.bnorm.template.ContinuationSerializer", SerialKind.CONTEXTUAL) {
    }

  override fun deserialize(decoder: Decoder): Continuation<*> {
    val descriptor = continuationDescriptor
    return decoder.decodeStructure(continuationsDescriptor) {
      var delegate = rootContinuation
      while (decodeElementIndex(descriptor) != CompositeDecoder.DECODE_DONE) {
        decodeInlineElement(descriptor, 0).decodeStructure(descriptor) {
          assert(decodeSerializableElement(descriptor, decodeElementIndex(descriptor), StringSerializer) == "type")
          val clazz = decodeSerializableElement(descriptor, decodeElementIndex(descriptor), classSerializer)
          val constructor = clazz.declaredConstructors.single()
          assert(constructor.trySetAccessible())
          delegate = constructor.newInstance(*(constructor.parameters.dropLast(1).map { parameter ->
            val serializer = serializersModule.serializer(parameter.type)
            if (serializer is SingletonKSerializer) {
              serializer.t
            } else {
              decodeSerializableElement(descriptor, decodeElementIndex(descriptor), serializer)
            }
          } + listOf(delegate)).toTypedArray()) as Continuation<*>
          val debugMetadata = clazz.getAnnotation(DebugMetadata::class.java)
          val compiledPersistableContinuationAnnotation =
            classLoader.loadClass(debugMetadata.className).declaredMethods.single {
              it.name == debugMetadata.methodName
            }.getAnnotation(CompiledPersistableContinuation::class.java)
          assert(decodeStringElement(descriptor, decodeElementIndex(descriptor)) == "label")
          val persistencePointKey = decodeStringElement(descriptor, decodeElementIndex(descriptor))
          val persistencePoint = compiledPersistableContinuationAnnotation.persistencePoints.single {
            it.serialized == persistencePointKey
          }
          val labelField = clazz.getDeclaredField("label")
          assert(labelField.trySetAccessible())
          val label = debugMetadata.lineNumbers.indexOfFirst { it - 1 == persistencePoint.lineNumber } + 1
          labelField.set(delegate, label)
          forEachSpilledLocalNameAndField(debugMetadata, label) { localName, spilled ->
            val field = clazz.getDeclaredField(spilled)
            assert(field.trySetAccessible())
            val serializer = serializersModule.serializer(
              if (localName == "this")
                classLoader.loadClass(debugMetadata.className)
              else
                compiledPersistableContinuationAnnotation.variables.single { it.name == localName }.type.java
            )
            field.set(
              delegate, if (serializer is SingletonKSerializer) {
                serializer.t
              } else {
                assert(decodeStringElement(descriptor, decodeElementIndex(descriptor)) == localName)
                decodeSerializableElement(descriptor, decodeElementIndex(descriptor), serializer)
              }
            )
          }
        }
      }
      delegate
    }
  }

  override fun serialize(encoder: Encoder, value: Continuation<*>) {
    val descriptor = continuationDescriptor
    encoder.encodeStructure(continuationsDescriptor) {
      fun rec(continuation: Continuation<*>) {
        if (continuation == rootContinuation) {
          return
        }
        val debugMetadata = continuation.javaClass.getAnnotation(DebugMetadata::class.java)!!
        val compiledPersistableContinuationAnnotation =
          classLoader.loadClass(debugMetadata.className).declaredMethods.single {
            it.name == debugMetadata.methodName
          }.getAnnotation(CompiledPersistableContinuation::class.java)
        rec((continuation as BaseContinuationImpl).completion!!)
        val labelField = continuation.javaClass.getDeclaredField("label")
        assert(labelField.trySetAccessible())
        val label = labelField.getInt(continuation)
        val persistencePoint = compiledPersistableContinuationAnnotation.persistencePoints.single {
          it.lineNumber == debugMetadata.lineNumbers[label - 1] - 1
        }
        encodeInlineElement(descriptor, 0).encodeStructure(descriptor) {
          var index = 0
          encodeStringElement(descriptor, index++, "type")
          encodeSerializableElement(descriptor, index++, classSerializer, continuation.javaClass)
          encodeStringElement(descriptor, index++, "label")
          encodeStringElement(descriptor, index++, persistencePoint.serialized)
          forEachSpilledLocalNameAndField(debugMetadata, label) { localName, spilled ->
            val field = continuation.javaClass.getDeclaredField(spilled)
            assert(field.trySetAccessible())
            val serializer = serializersModule.serializer(
              if (localName == "this")
                classLoader.loadClass(debugMetadata.className)
              else
                compiledPersistableContinuationAnnotation.variables.single { it.name == localName }.type.java
            )
            if (serializer !is SingletonKSerializer) {
              encodeStringElement(descriptor, index++, localName)
              encodeSerializableElement(
                descriptor,
                index++,
                serializer,
                field.get(continuation)
              )
            }
          }
        }
      }

      rec(value)
    }
  }
}
