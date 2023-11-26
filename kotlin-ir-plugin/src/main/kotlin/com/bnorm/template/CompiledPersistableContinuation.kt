package com.bnorm.template

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CompiledPersistableContinuation(
  @get:JvmName("l")
  val persistencePointsLabels: IntArray = [],
)
