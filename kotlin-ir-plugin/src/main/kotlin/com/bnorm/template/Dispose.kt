package com.bnorm.template

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

abstract class Dispose : AbstractCoroutineContextElement(Key) {
  companion object Key : CoroutineContext.Key<Dispose>

  abstract suspend fun dispose()
}
