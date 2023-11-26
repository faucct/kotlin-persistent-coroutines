package com.bnorm.template;

public interface AccountClient {
    void withdraw(String fromAccountId, String referenceId, int amountCents);

    void deposit(String fromAccountId, String referenceId, int amountCents);
}
