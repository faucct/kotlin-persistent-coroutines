package com.bnorm.template

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class SingletonKSerializer<T>(val t: T) : kotlinx.serialization.KSerializer<T> {
  override val descriptor: SerialDescriptor
    get() = throw UnsupportedOperationException()

  override fun deserialize(decoder: Decoder): T = t

  override fun serialize(encoder: Encoder, value: T) = throw UnsupportedOperationException()
}
