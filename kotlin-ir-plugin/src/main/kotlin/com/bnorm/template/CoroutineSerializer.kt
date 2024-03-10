package com.bnorm.template

interface CoroutineSerializer {
  suspend fun serializer(): suspend () -> String

  suspend fun deserialize(string: String)
}
