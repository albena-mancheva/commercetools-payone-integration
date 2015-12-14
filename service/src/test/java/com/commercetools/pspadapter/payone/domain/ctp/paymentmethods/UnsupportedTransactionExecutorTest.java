package com.commercetools.pspadapter.payone.domain.ctp.paymentmethods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.PaymentTestHelper;
import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.CompletionException;

/**
 * @author Jan Wolter
 */
public class UnsupportedTransactionExecutorTest extends PaymentTestHelper {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private BlockingClient client;

    @InjectMocks
    private UnsupportedTransactionExecutor testee;

    @Test
    public void addsInterfaceInteractionAndSetsStateToFailure() throws Exception {
        final Payment inputPayment = dummyPaymentTwoTransactionsPending();
        final Payment outputPayment = dummyPaymentTwoTransactionsSuccessPending();
        final Transaction transaction = inputPayment.getTransactions().get(0);

        // TODO jw: use more specific matchers
        when(client.complete(argThat(instanceOf(PaymentUpdateCommand.class)))).thenReturn(outputPayment);

        assertThat(testee.executeTransaction(inputPayment, transaction)).isSameAs(outputPayment);
    }

    @Test
    public void throwsCompletionException() throws Exception {
        final Payment inputPayment = dummyPaymentTwoTransactionsSuccessPending();
        final Transaction transaction = inputPayment.getTransactions().get(0);

        // TODO jw: use more specific matchers
        final CompletionException completionException = new CompletionException(new Exception());
        when(client.complete(argThat(instanceOf(PaymentUpdateCommand.class))))
                .thenThrow(completionException);

        final Throwable throwable = catchThrowable(() -> testee.executeTransaction(inputPayment, transaction));

        assertThat(throwable).isSameAs(completionException);
    }

}