package com.commercetools.service;

import io.sphere.sdk.orders.Order;
import io.sphere.sdk.orders.PaymentState;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * CTP service to work with {@link Order}s
 */
public interface OrderService {

    /**
     * Get an order which has one of {@code paymentInfo.payments.id = paymentId}.
     * <p>
     * This service does not expect we could have multiply orders for one payment
     *
     * @param paymentId CTP payment uuid
     * @return {@link Optional} {@link Order} if exists, empty {@link Optional} if not found.
     */
    CompletionStage<Optional<Order>> getOrderByPaymentId(String paymentId);

    /**
     * Update the order's payment state.
     * @param order <b>non-null</b> {@link Order} to update
     * @param newPaymentState <b>nullable</b> {@link PaymentState} to set to the order
     * @return new {@link CompletionStage<Order>} with the updated order reference.
     */
    CompletionStage<Order> updateOrderPaymentState(Order order, PaymentState newPaymentState);
}
