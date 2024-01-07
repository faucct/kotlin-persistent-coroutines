package com.bnorm.template

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CompiledPersistableContinuation(
  @get:JvmName("p")
  val persistencePoints: Array<PersistencePoint> = [],
  val variables: Array<Variable>,
) {
  annotation class PersistencePoint(
    val lineNumber: Int,
    val serialized: String,
  )
  annotation class Variable(
    val name: String,
    val type: KClass<*>,
    val key: String,
  )
}
