package com.bnorm.template

import kotlinx.coroutines.runBlocking

fun main() {
  var finalized = false
  runBlocking {
    val disposableCoroutine = DisposableCoroutine()
    disposableCoroutine {
      try {
        disposableCoroutine.dispose()
      } finally {
        finalized = true
      }
    }
  }
  assert(!finalized)
}
