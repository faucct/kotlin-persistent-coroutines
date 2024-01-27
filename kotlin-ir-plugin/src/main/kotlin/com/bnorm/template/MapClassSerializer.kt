package com.bnorm.template

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class MapClassSerializer(private val index: Map<Class<*>, String>) : KSerializer<Class<*>> {
  private val invertedIndex = HashMap<String, Class<*>>()

  init {
    for ((key, value) in index) {
      val present = invertedIndex.putIfAbsent(value, key)
      assert(present == null)
    }
  }

  override val descriptor: SerialDescriptor
    get() = PrimitiveSerialDescriptor("kotlin.String", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): Class<*> = invertedIndex[decoder.decodeString()]!!

  override fun serialize(encoder: Encoder, value: Class<*>) = encoder.encodeString(index[value]!!)
}
