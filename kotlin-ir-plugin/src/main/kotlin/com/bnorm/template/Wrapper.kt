package com.bnorm.template

interface Wrapper {
  suspend operator fun <T> invoke(block: suspend () -> T): T
}
