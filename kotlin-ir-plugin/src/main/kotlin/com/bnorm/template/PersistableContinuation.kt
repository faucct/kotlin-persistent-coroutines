package com.bnorm.template

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PersistableContinuation(
  val key: String,
)
