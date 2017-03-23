package com.commercetools.pspadapter.payone.mapping.order;

import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentStatus;

import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Default implementation for {@link PaymentToOrderStateMapper}:<ul>
 *     <li><i>appointed</i> and <i>underpaid</i> are mapped to {@link PaymentState#PENDING}</li>
 *     <li><i>paid</i>, <i>capture</i> and <i>transfer</i> are mapped to {@link PaymentState#PAID}</li>
 *     <li><i>failed</i> and <i>cancelation</i> are mapped to {@link PaymentState#FAILED}</li>
 *     <li>the rest is mapped to <b>null</b></li>
 * </ul>
 */
public class DefaultPaymentToOrderStateMapper implements PaymentToOrderStateMapper {

    private static final Map<String, PaymentState> MAP;

    static {
        MAP = ImmutableMap.<String, PaymentState>builder()
                .put(NotificationAction.APPOINTED.getTxActionCode(),    PaymentState.PENDING)
                .put(NotificationAction.UNDERPAID.getTxActionCode(),    PaymentState.PENDING)

                .put(NotificationAction.PAID.getTxActionCode(),         PaymentState.PAID)
                .put(NotificationAction.CAPTURE.getTxActionCode(),      PaymentState.PAID)
                .put(NotificationAction.TRANSFER.getTxActionCode(),     PaymentState.PAID)

                .put(NotificationAction.FAILED.getTxActionCode(),       PaymentState.FAILED)
                .put(NotificationAction.CANCELATION.getTxActionCode(),  PaymentState.FAILED)

                .build();
    }

    @Override
    public PaymentState mapPaymentToOrderState(Payment payment) {
        return ofNullable(payment)
                .map(Payment::getPaymentStatus)
                .map(PaymentStatus::getInterfaceCode)
                .map(MAP::get)
                .orElse(null);
    }
}
