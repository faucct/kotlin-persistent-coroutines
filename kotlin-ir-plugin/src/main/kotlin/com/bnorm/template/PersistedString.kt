package com.bnorm.template

interface PersistedString {
  suspend fun getPersisted(): String?
  suspend fun setPersisted(value: String?)
}
