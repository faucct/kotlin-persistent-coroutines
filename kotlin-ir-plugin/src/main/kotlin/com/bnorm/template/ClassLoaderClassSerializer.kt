package com.bnorm.template

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class ClassLoaderClassSerializer(private val classLoader: ClassLoader) : KSerializer<Class<*>> {
  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("kotlin.String", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Class<*> = classLoader.loadClass(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: Class<*>) {
    encoder.encodeString(value.typeName)
  }
}
