package com.bnorm.template

import kotlinx.coroutines.delay

class DelayingPersistor: Persistor() {
  override suspend fun persist() {
    delay(1000)
  }
}
