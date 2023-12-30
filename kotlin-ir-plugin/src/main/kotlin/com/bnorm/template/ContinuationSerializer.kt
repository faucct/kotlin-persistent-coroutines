@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.bnorm.template

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.PrimitiveSerialDescriptor
import kotlinx.serialization.internal.StringSerializer
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.coroutines.Continuation
import kotlin.coroutines.jvm.internal.BaseContinuationImpl
import kotlin.coroutines.jvm.internal.DebugMetadata

@OptIn(InternalSerializationApi::class)
class ContinuationSerializer(private val rootContinuation: Continuation<*>) : KSerializer<Continuation<Any?>> {
  override val descriptor: SerialDescriptor
    get() = buildSerialDescriptor("com.bnorm.template.ContinuationSerializer", SerialKind.CONTEXTUAL) {
    }

  override fun deserialize(decoder: Decoder): Continuation<Any?> {
    val descriptor =
      buildSerialDescriptor(Continuation::class.java.typeName, kotlinx.serialization.descriptors.StructureKind.MAP) {
        element("type", PrimitiveSerialDescriptor("type", PrimitiveKind.STRING))
      }
    return decoder.decodeStructure(
      buildSerialDescriptor(Continuation::class.java.typeName, kotlinx.serialization.descriptors.StructureKind.LIST) {
        element("type", descriptor)
      },
    ) {
      var delegate = rootContinuation
      while (decodeElementIndex(descriptor) != CompositeDecoder.DECODE_DONE) {
        decodeInlineElement(descriptor, 0).decodeStructure(descriptor) {
          assert(decodeSerializableElement(descriptor, decodeElementIndex(descriptor), StringSerializer) == "type")
          val type = decodeSerializableElement(descriptor, decodeElementIndex(descriptor), StringSerializer)
          val clazz = ClassLoader.getSystemClassLoader().loadClass(type)
          delegate = clazz.getDeclaredConstructor(Continuation::class.java).newInstance(delegate).cast()
          val debugMetadata = clazz.getAnnotation(DebugMetadata::class.java)
          assert(decodeStringElement(descriptor, decodeElementIndex(descriptor)) == "label")
          val label = decodeSerializableElement(descriptor, decodeElementIndex(descriptor), IntSerializer)
          clazz.getDeclaredField("label").set(delegate, label)
          debugMetadata.indexToLabel.zip(debugMetadata.localNames.zip(debugMetadata.spilled)) { spilledLabel, (localName, spilled) ->
            if (spilledLabel + 1 == label) {
              assert(decodeStringElement(descriptor, decodeElementIndex(descriptor)) == localName)
              clazz.getDeclaredField(spilled).set(
                delegate, decodeStringElement(descriptor, decodeElementIndex(descriptor)).toInt()
              )
            }
          }
        }
      }
      delegate.cast()
    }
  }

  override fun serialize(encoder: Encoder, value: Continuation<Any?>) {
    val descriptor =
      buildSerialDescriptor(value::class.java.typeName, kotlinx.serialization.descriptors.StructureKind.MAP) {
        element("type", PrimitiveSerialDescriptor("type", PrimitiveKind.STRING))
      }
    encoder.encodeStructure(
      buildSerialDescriptor(value::class.java.typeName, kotlinx.serialization.descriptors.StructureKind.LIST) {
        element("type", descriptor)
      },
    ) {
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
          debugMetadata.indexToLabel.zip(debugMetadata.localNames.zip(debugMetadata.spilled)) { spilledLabel, (localName, spilled) ->
            if (spilledLabel + 1 == label) {
              encodeStringElement(descriptor, index++, localName)
              val field = continuation.javaClass.getDeclaredField(spilled)
              encodeStringElement(descriptor, index++, field.get(continuation).toString())
            }
          }
        }
      }

      rec(value)
    }
  }
}
