package com.bnorm.template

import com.bnorm.template.PersistingWrapper.wrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.coroutines.coroutineContext

suspend fun foo() {
  println("hi")
  coroutineContext[Persistor.Key]!!.persist()
  bar()
  println("then")
  delay(1000)
  yield()
  println("later")
}

suspend fun bar() {
  println("bar")
  coroutineContext[Persistor.Key]!!.persist()
  println("then")
  delay(1000)
  yield()
  println("later")
}

fun main() {
  Json.encodeToString(Json.serializersModule.serializer(), Color(1, 2))
  val message = runBlocking {
    wrapper {
      foo()
      "foo"
    }
  }
  println(message)
}
