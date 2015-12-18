package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;

import java.util.Map;

public class PaymentMethodDispatcher {
    private final TransactionExecutor defaultExecutor;
    private final Map<TransactionType, TransactionExecutor> executorMap;

    public PaymentMethodDispatcher(TransactionExecutor defaultExecutor, Map<TransactionType, TransactionExecutor> executorMap) {
        this.defaultExecutor = defaultExecutor;
        this.executorMap = executorMap;
    }

    public PaymentWithCartLike dispatchPayment(PaymentWithCartLike paymentWithCartLike) {
        // Execute the first Pending Transaction
        return paymentWithCartLike.getPayment()
            .getTransactions()
            .stream()
            .filter(transaction -> transaction.getState().equals(TransactionState.PENDING))
            .findFirst()
            .map(transaction -> {
                PaymentWithCartLike newPaymentWithCartLike = getExecutor(transaction).executeTransaction(paymentWithCartLike, transaction);
                Transaction newTransaction = getUpdatedTransaction(transaction, newPaymentWithCartLike.getPayment());
                if (newTransaction.getState().equals(TransactionState.PENDING)) {
                    return null; // Still Pending, stop (e.g. not approved or no executor) TODO: e.g. throw error when no executor found!
                } else {
                    return dispatchPayment(newPaymentWithCartLike); // Recursively execute next Pending Transaction
                }
            })
            .orElse(paymentWithCartLike); //TODO: strategy for returning valid payments and those where transactions could not be processed!
    }

    private Transaction getUpdatedTransaction(Transaction transaction, Payment newPayment) {
        return newPayment
            .getTransactions()
            .stream()
            .filter(t -> t.getId().equals(transaction.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Transaction can not be found in payment"));
    }

    private TransactionExecutor getExecutor(Transaction transaction) {
        return executorMap.getOrDefault(transaction.getType(), defaultExecutor);
    }
}