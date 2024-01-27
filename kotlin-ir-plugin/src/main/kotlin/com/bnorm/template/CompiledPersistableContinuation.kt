package com.bnorm.template

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CompiledPersistableContinuation(
  @get:JvmName("k")
  val key: String,
  @get:JvmName("p")
  val persistencePoints: Array<PersistencePoint> = [],
  @get:JvmName("v")
  val variables: Array<Variable>,
) {
  annotation class PersistencePoint(
    @get:JvmName("l")
    val lineNumber: Int,
    @get:JvmName("k")
    val serialized: String,
  )
  annotation class Variable(
    @get:JvmName("n")
    val name: String,
    @get:JvmName("t")
    val type: KClass<*>,
    @get:JvmName("k")
    val key: String,
  )
}
