package com.bnorm.template

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.coroutines.Continuation

@OptIn(InternalSerializationApi::class)
object ContinuationSerializer : KSerializer<Continuation<Any>> {
  override val descriptor: SerialDescriptor
    get() = buildSerialDescriptor("com.bnorm.template.ContinuationSerializer", SerialKind.CONTEXTUAL) {
    }


  override fun deserialize(decoder: Decoder): Continuation<Any> {
    TODO("Not yet implemented")
  }

  override fun serialize(encoder: Encoder, value: Continuation<Any>) {
    TODO("Not yet implemented")
  }
}
