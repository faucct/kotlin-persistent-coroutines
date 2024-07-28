package com.bnorm.template

import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

object MapClassSerializerFactory {
  operator fun invoke(vararg classes: KClass<*>): KSerializer<Class<*>> {
    val map = HashMap<Class<*>, String>()
    fun rec(clazz: Class<*>) {
      for (declaredMethod in clazz.declaredMethods) {
        val functionContinuation = declaredMethod.getAnnotation(FunctionContinuation::class.java)
        val persistableContinuation = declaredMethod.getAnnotation(PersistableContinuation::class.java)
        if (functionContinuation != null && persistableContinuation != null) {
          map[functionContinuation.value.java] = persistableContinuation.key
        }
        declaredMethod.getAnnotation(AnonymousClasses::class.java)?.value?.forEach { rec(it.java) }
        clazz.declaredClasses.forEach(::rec)
      }
    }
    classes.forEach { rec(it.java) }
    return MapClassSerializer(map)
  }
}
