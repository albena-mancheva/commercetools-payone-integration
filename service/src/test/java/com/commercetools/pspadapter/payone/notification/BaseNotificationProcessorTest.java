package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.mapping.order.PaymentToOrderStateMapper;
import com.commercetools.service.OrderService;
import com.commercetools.service.PaymentService;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import org.junit.Before;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import util.PaymentTestHelper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BaseNotificationProcessorTest {
    @Mock
    protected PaymentService paymentService;

    @Mock
    protected OrderService orderService;

    @Mock
    protected PaymentToOrderStateMapper paymentToOrderStateMapper;

    @Mock
    protected ServiceFactory serviceFactory;

    @Mock
    protected ServiceConfig serviceConfig;

    @Mock
    protected Order orderToUpdate;

    @Mock
    protected Order orderUpdated;

    @Captor
    protected ArgumentCaptor<List<UpdateAction<Payment>>> paymentRequestUpdatesCaptor;

    @Captor
    protected ArgumentCaptor<Payment> paymentRequestPayment;

    protected final PaymentTestHelper testHelper = new PaymentTestHelper();

    protected Notification notification;

    @Before
    public void setUp() throws Exception {
        when(serviceFactory.getPaymentService()).thenReturn(paymentService);
        when(paymentService.updatePayment(any(Payment.class), anyObject()))
                .then(answer -> CompletableFuture.completedFuture(answer.getArgumentAt(0, Payment.class)));

        when(serviceFactory.getOrderService()).thenReturn(orderService);
        when(orderService.getOrderByPaymentId(anyString()))
                .then(answer -> CompletableFuture.completedFuture(Optional.of(orderToUpdate)));
        when(orderService.updateOrderPaymentState(any(Order.class), any(PaymentState.class)))
                .then(answer -> CompletableFuture.completedFuture(orderUpdated));

        when(serviceFactory.getPaymentToOrderStateMapper()).thenReturn(paymentToOrderStateMapper);

        when(serviceConfig.isUpdateOrderPaymentState()).thenReturn(true);
    }

    protected List<? extends UpdateAction<Payment>> updatePaymentAndGetUpdateActions(Payment payment) {
        verify(paymentService).updatePayment(paymentRequestPayment.capture(), paymentRequestUpdatesCaptor.capture());

        final Payment updatePayment = paymentRequestPayment.getValue();
        assertThat(updatePayment).isEqualTo(payment);
        return paymentRequestUpdatesCaptor.getValue();
    }

    /**
     * Verify that the processor called expected order service functions when a payment is updated
     * @param payment payment which is updated
     * @param expectedPaymentState expected new payment state of the updated order
     */
    protected void verifyUpdateOrderActions(Payment payment, PaymentState expectedPaymentState) {
        ArgumentCaptor<String> paymentIdForOrderCaptor = ArgumentCaptor.forClass(String.class);
        verify(orderService).getOrderByPaymentId(paymentIdForOrderCaptor.capture());

        // verify the orderService.getOrderByPaymentId() was called with with our payment id
        assertThat(paymentIdForOrderCaptor.getValue()).isEqualTo(payment.getId());

        ArgumentCaptor<Order> orderUpdateCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<PaymentState> paymentStateCaptor = ArgumentCaptor.forClass(PaymentState.class);
        verify(orderService).updateOrderPaymentState(orderUpdateCaptor.capture(), paymentStateCaptor.capture());

        // verify the orderService.updateOrderPaymentState() for the same value we returned in orderService.getOrderByPaymentId()
        assertThat(orderUpdateCaptor.getValue()).isSameAs(orderToUpdate);
        // verify the orderService.updateOrderPaymentState() is called with expected PaymentState
        assertThat(paymentStateCaptor.getValue()).isEqualTo(expectedPaymentState);
    }

    protected void verifyUpdateOrderActionsNotCalled() {
        verify(orderService, never()).getOrderByPaymentId(anyString());
        verify(orderService, never()).updateOrderPaymentState(anyObject(), anyObject());
    }


}
