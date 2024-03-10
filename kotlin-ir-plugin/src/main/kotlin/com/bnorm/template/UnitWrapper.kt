package com.bnorm.template

interface UnitWrapper {
  suspend operator fun invoke(block: suspend () -> Unit)
}
