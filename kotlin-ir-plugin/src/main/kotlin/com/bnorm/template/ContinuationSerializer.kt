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
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.PrimitiveSerialDescriptor
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.serializer
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.coroutines.Continuation
import kotlin.coroutines.jvm.internal.BaseContinuationImpl
import kotlin.coroutines.jvm.internal.DebugMetadata

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
class ContinuationSerializer(private val rootContinuation: Continuation<*>) : KSerializer<Continuation<Any?>> {
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

  override fun deserialize(decoder: Decoder): Continuation<Any?> {
    val descriptor = continuationDescriptor
    return decoder.decodeStructure(continuationsDescriptor) {
      var delegate = rootContinuation
      while (decodeElementIndex(descriptor) != CompositeDecoder.DECODE_DONE) {
        decodeInlineElement(descriptor, 0).decodeStructure(descriptor) {
          assert(decodeSerializableElement(descriptor, decodeElementIndex(descriptor), StringSerializer) == "type")
          val type = decodeSerializableElement(descriptor, decodeElementIndex(descriptor), StringSerializer)
          val clazz = ClassLoader.getSystemClassLoader().loadClass(type)
          val constructor = clazz.declaredConstructors.single()
          delegate = constructor.newInstance(*(constructor.parameters.dropLast(1).map { parameter ->
            val serializer = serializersModule.serializer(parameter.type)
            if (serializer is SingletonKSerializer) {
              serializer.t
            } else {
              decodeSerializableElement(descriptor, decodeElementIndex(descriptor), serializer)
            }
          } + listOf(delegate)).toTypedArray()).cast()
          val debugMetadata = clazz.getAnnotation(DebugMetadata::class.java)
          assert(decodeStringElement(descriptor, decodeElementIndex(descriptor)) == "label")
          val label = decodeSerializableElement(descriptor, decodeElementIndex(descriptor), IntSerializer)
          clazz.getDeclaredField("label").set(delegate, label)
          forEachSpilledLocalNameAndField(debugMetadata, label) { localName, spilled ->
            val field = clazz.getDeclaredField(spilled)
            val serializer = serializersModule.serializer(
              if (localName == "this")
                ClassLoader.getSystemClassLoader().loadClass(debugMetadata.className)
              else
                field.type
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
      delegate.cast()
    }
  }

  override fun serialize(encoder: Encoder, value: Continuation<Any?>) {
    val descriptor = continuationDescriptor
    encoder.encodeStructure(continuationsDescriptor) {
      fun rec(continuation: Continuation<Any?>) {
        if (continuation == rootContinuation) {
          return
        }
        val debugMetadata = continuation.javaClass.getAnnotation(DebugMetadata::class.java)!!
        rec((continuation as BaseContinuationImpl).completion!!)
        val label = continuation.javaClass.getDeclaredField("label").getInt(continuation)
        encodeInlineElement(descriptor, 0).encodeStructure(descriptor) {
          var index = 0
          encodeStringElement(descriptor, index++, "type")
          encodeStringElement(descriptor, index++, continuation::class.java.typeName)
          encodeStringElement(descriptor, index++, "label")
          encodeIntElement(descriptor, index++, label)
          forEachSpilledLocalNameAndField(debugMetadata, label) { localName, spilled ->
            val field = continuation.javaClass.getDeclaredField(spilled)
            val serializer = serializersModule.serializer(
              if (localName == "this")
                ClassLoader.getSystemClassLoader().loadClass(debugMetadata.className)
              else
                field.type
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
