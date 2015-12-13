package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.sepa;

import static org.hamcrest.CoreMatchers.is;

import com.commercetools.pspadapter.payone.PaymentTestHelper;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.TransactionExecutor;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionType;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CompletionException;

import static org.junit.Assert.*;

public class SepaDispatcherTest extends PaymentTestHelper {
    private interface CountingTransactionExecutor extends TransactionExecutor {
        int getCount();
    }

    private CountingTransactionExecutor countingTransactionExecutor() {
        return new CountingTransactionExecutor() {
            int count = 0;
            @Override
            public Payment executeTransaction(Payment payment, Transaction transaction) {
                count += 1;
                return payment;
            }
            @Override
            public int getCount() {
                return count;
            }
        };
    }

    private CountingTransactionExecutor returnSuccessTransactionExecutor(final int afterExecutions) {
        return new CountingTransactionExecutor() {
            int count = 0;
            @Override
            public Payment executeTransaction(Payment payment, Transaction transaction) {
                count += 1;
                if (count > afterExecutions) try {
                    return dummyPayment2();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
                else return payment;
            }
            @Override
            public int getCount() {
                return count;
            }
        };
    }

    @Test
    public void usesDefaultExecutor() throws Exception {
        CountingTransactionExecutor countingTransactionExecutor = countingTransactionExecutor();
        SepaDispatcher dispatcher = new SepaDispatcher(countingTransactionExecutor, new HashMap<>());
        dispatcher.dispatchPayment(dummyPayment1());
        assertThat(countingTransactionExecutor.getCount(), is(1));
    }

    @Test
    public void callsCorrectExecutor() throws Exception {
        CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        CountingTransactionExecutor chargeExecutor = countingTransactionExecutor();
        CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final HashMap<TransactionType, TransactionExecutor> executorMap = new HashMap<>();
        executorMap.put(TransactionType.CHARGE, chargeExecutor);
        executorMap.put(TransactionType.REFUND, refundExecutor);
        SepaDispatcher dispatcher = new SepaDispatcher(defaultExecutor, executorMap);
        dispatcher.dispatchPayment(dummyPayment1());
        assertThat(defaultExecutor.getCount(), is(0));
        assertThat(chargeExecutor.getCount(), is(1));
        assertThat(refundExecutor.getCount(), is(0));
    }

    @Test
    public void callsSecondExecutorAfterFirstTransactionStateChanged() throws Exception {
        CountingTransactionExecutor defaultExecutor = countingTransactionExecutor();
        CountingTransactionExecutor chargeExecutor = returnSuccessTransactionExecutor(2);
        CountingTransactionExecutor refundExecutor = countingTransactionExecutor();
        final HashMap<TransactionType, TransactionExecutor> executorMap = new HashMap<>();
        executorMap.put(TransactionType.CHARGE, chargeExecutor);
        executorMap.put(TransactionType.REFUND, refundExecutor);
        SepaDispatcher dispatcher = new SepaDispatcher(defaultExecutor, executorMap);
        dispatcher.dispatchPayment(dispatcher.dispatchPayment(dispatcher.dispatchPayment(dummyPayment1())));
        assertThat(defaultExecutor.getCount(), is(0));
        assertThat(chargeExecutor.getCount(), is(3));
        assertThat(refundExecutor.getCount(), is(1));
    }
}