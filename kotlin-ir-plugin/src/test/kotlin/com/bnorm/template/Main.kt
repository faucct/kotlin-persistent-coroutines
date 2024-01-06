package com.bnorm.template

import com.bnorm.template.PersistingWrapper.wrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.coroutines.coroutineContext

var persisting = false
suspend fun persist() {
  persisting = true
  coroutineContext[Persistor.Key]!!.persist()
  if (persisting) {
    throw RuntimeException("")
  }
}

class Main {
  suspend fun foo() {
    val a = 100
    println("hi")
    persist()
    bar()
    println("then $a")
    delay(1000)
    yield()
    println("later")
  }

  suspend fun bar() {
    println("bar")
    persist()
    println("then")
    delay(1000)
    yield()
    println("later")
  }
}

fun main() {
  Json.encodeToString(Json.serializersModule.serializer(), Color(1, 2))
  val main = Main()
  val json = Json {
    serializersModule = SerializersModule {
      contextual(Main::class, SingletonKSerializer(main))
    }
  }
  val message = runBlocking {
    (wrapper(json)) {
      Main().foo()
      "foo"
    }
  }
  println(message)
}
