package com.bnorm.template

@Target(AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class PersistencePoint(
  val name: String,
)
