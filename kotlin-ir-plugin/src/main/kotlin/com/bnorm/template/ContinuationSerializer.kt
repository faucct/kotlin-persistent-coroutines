@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.bnorm.template

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.internal.PrimitiveSerialDescriptor
import kotlin.coroutines.Continuation
import kotlin.coroutines.jvm.internal.BaseContinuationImpl
import kotlin.coroutines.jvm.internal.DebugMetadata

@OptIn(InternalSerializationApi::class)
object ContinuationSerializer : KSerializer<Continuation<Any?>> {
  override val descriptor: SerialDescriptor
    get() = buildSerialDescriptor("com.bnorm.template.ContinuationSerializer", SerialKind.CONTEXTUAL) {
    }


  override fun deserialize(decoder: Decoder): Continuation<Any?> {
    TODO("Not yet implemented")
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
        val debugMetadata: DebugMetadata? = continuation.javaClass.getAnnotation(DebugMetadata::class.java)
        if (debugMetadata === null) {
          return
        }
        rec((continuation as BaseContinuationImpl).completion!!)
        val labelField = continuation.javaClass.getDeclaredField("label")
        labelField.set(continuation, labelField.getInt(continuation))
        val label = labelField.getInt(continuation)
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
              field.set(continuation, field.get(continuation))
              encodeStringElement(descriptor, index++, field.get(continuation).toString())
            }
          }
        }
      }

      rec(value)
    }
  }
}
