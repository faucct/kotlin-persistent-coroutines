package com.bnorm.template

object AccountTransfer {
  lateinit var accountClient: AccountClient

  suspend fun persist() {
  }

  suspend fun transfer(fromAccountId: String, toAccountId: String, idempotencyKey: String, amountCents: Int) {
    accountClient.withdraw(fromAccountId, idempotencyKey, amountCents)
    persist()
    accountClient.deposit(toAccountId, idempotencyKey, amountCents)
  }
}
