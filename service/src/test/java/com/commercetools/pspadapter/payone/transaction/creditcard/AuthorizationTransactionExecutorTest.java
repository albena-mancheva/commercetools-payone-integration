package com.commercetools.pspadapter.payone.transaction.creditcard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.mapping.PayoneRequestFactory;
import com.google.common.cache.CacheBuilder;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.Transaction;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.queries.PagedQueryResult;
import io.sphere.sdk.types.Type;
import io.sphere.sdk.types.queries.TypeQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import util.PaymentTestHelper;

import java.util.Arrays;


/**
 * @author fhaertig
 * @since 13.01.16
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthorizationTransactionExecutorTest {

    @Mock
    private PaymentWithCartLike paymentWithCartLike;

    @Mock
    private PayoneRequestFactory requestFactory;

    @Mock
    private PayonePostService postService;

    @Mock
    private CommercetoolsClient client;

    private final PaymentTestHelper testHelper = new PaymentTestHelper();

    private AuthorizationTransactionExecutor testee;

    @Before
    public void setUp() {
        when(client.complete(any(TypeQuery.class))).then(a -> {
            PagedQueryResult<Type> customTypes = testHelper.getCustomTypes();
            String queryString = Arrays.asList(a.getArguments()).get(0).toString();
            if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE)) {
                return PagedQueryResult.of(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_RESPONSE, customTypes));
            } else if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION)) {
                return PagedQueryResult.of(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION, customTypes));
            } else if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT)) {
                return PagedQueryResult.of(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_REDIRECT, customTypes));
            } else if (queryString.contains(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST)) {
                return PagedQueryResult.of(findCustomTypeByKey(CustomTypeBuilder.PAYONE_INTERACTION_REQUEST, customTypes));
            }
            return PagedQueryResult.empty();
        });

        testee = new AuthorizationTransactionExecutor(
                CacheBuilder.newBuilder().build(new TypeCacheLoader(client)),
                requestFactory,
                postService,
                client
        );
    }

    @Test
    public void pendingAuthorizationNotExecuted() throws Exception {
        Payment payment = testHelper.dummyPaymentOneAuthPending20Euro();
        Transaction transaction = payment.getTransactions().stream().filter(t -> t.getState().equals(TransactionState.PENDING)).findFirst().get();
        when(paymentWithCartLike.getPayment()).thenReturn(payment);

        assertThat(testee.wasExecuted(paymentWithCartLike, transaction)).isFalse();
    }

    @Test
    public void pendingAuthorizationWasExecutedStillPending() throws Exception {
        Payment payment = testHelper.dummyPaymentOneAuthPending20EuroPendingResponse();
        Transaction transaction = payment.getTransactions().stream().filter(t -> t.getState().equals(TransactionState.PENDING)).findFirst().get();
        when(paymentWithCartLike.getPayment()).thenReturn(payment);

        assertThat(testee.wasExecuted(paymentWithCartLike, transaction)).isTrue();
    }

    @Test
    public void pendingAuthorizationCreatedByNotification() throws Exception {
        Payment payment = testHelper.dummyPaymentCreatedByNotification();
        Transaction transaction = payment.getTransactions().stream().filter(t -> t.getState().equals(TransactionState.PENDING)).findFirst().get();
        when(paymentWithCartLike.getPayment()).thenReturn(payment);

        assertThat(testee.wasExecuted(paymentWithCartLike, transaction)).isTrue();
    }


    private Type findCustomTypeByKey(String key, PagedQueryResult<Type> customTypes) {
        return customTypes
            .getResults()
            .stream()
            .filter(p -> p.getKey().equals(key))
            .findFirst()
            .get();
    }
}