package com.commercetools.pspadapter.payone.notification;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.commands.UpdateAction;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddInterfaceInteraction;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionInteractionId;
import io.sphere.sdk.payments.commands.updateactions.ChangeTransactionState;
import io.sphere.sdk.utils.MoneyImpl;

import javax.money.MonetaryAmount;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * @author fhaertig
 * @date 08.01.16
 */
public class AppointedNotificationProcessor implements NotificationProcessor {

    private final BlockingClient client;
    private static final TransactionType authorizationTransactionType = TransactionType.AUTHORIZATION;

    public AppointedNotificationProcessor(
            final BlockingClient client) {
        this.client = client;
    }

    @Override
    public NotificationAction supportedNotificationAction() {
        return NotificationAction.APPOINTED;
    }

    @Override
    public boolean processTransactionStatusNotification(final Notification notification, final Payment payment) {
        if (!notification.getTxaction().equals(supportedNotificationAction())) {
            throw new IllegalArgumentException(String.format("txaction %s is not supported for %s",
                                                                notification.getTxaction(),
                                                                this.getClass().getSimpleName()));
        }

        client.complete(PaymentUpdateCommand.of(payment, createPaymentUpdates(payment, notification)));

        return true;
    }

    @Override
    public ImmutableList<UpdateAction<Payment>> createPaymentUpdates(final Payment payment, final Notification notification) {
        LocalDateTime timestamp = LocalDateTime.ofEpochSecond(Long.valueOf(notification.getTxtime()), 0, ZoneOffset.UTC);

        return payment.getTransactions().stream()
                .filter(t -> t.getType().equals(authorizationTransactionType))
                .findFirst()
                .map(t -> {
                    //map sequenceNr to interactionId in existing transaction
                    final UpdateAction<Payment> changeInteractionId = ChangeTransactionInteractionId.of(notification.getSequencenumber(), t.getId());
                    //set transactionState according to notification
                    final UpdateAction<Payment> changeTransactionState = ChangeTransactionState.of(notification.getTransactionStatus().getCtTransactionState(), t.getId());

                    //add new interface interaction
                    final AddInterfaceInteraction newInterfaceInteraction = AddInterfaceInteraction
                            .ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                                    ImmutableMap.of(
                                            CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                                            CustomTypeBuilder.TRANSACTION_ID_FIELD, t.getId(),
                                            CustomTypeBuilder.NOTIFICATION_FIELD, notification.toString()));

                    return ImmutableList.of(
                            changeInteractionId,
                            changeTransactionState,
                            newInterfaceInteraction
                    );
                })
                .orElseGet(() -> {
                    //create new transaction
                    MonetaryAmount amount = MoneyImpl.of(notification.getPrice(), notification.getCurrency());

                    TransactionDraft transactionDraft = TransactionDraftBuilder.of(authorizationTransactionType, amount)
                            .timestamp(ZonedDateTime.of(timestamp, ZoneId.of("UTC")))
                            .state(notification.getTransactionStatus().getCtTransactionState())
                            .interactionId(notification.getSequencenumber())
                            .build();

                    //add new interface interaction
                    final AddInterfaceInteraction newInterfaceInteraction = AddInterfaceInteraction
                            .ofTypeKeyAndObjects(CustomTypeBuilder.PAYONE_INTERACTION_NOTIFICATION,
                                    ImmutableMap.of(
                                            CustomTypeBuilder.TIMESTAMP_FIELD, ZonedDateTime.of(timestamp, ZoneId.of("UTC")),
                                            // TODO: its impossible to get id of the new transaction, we need to change the customType to reflect this
                                            CustomTypeBuilder.TRANSACTION_ID_FIELD, "",
                                            CustomTypeBuilder.NOTIFICATION_FIELD, notification.toString()));

                    return ImmutableList.of(
                            AddTransaction.of(transactionDraft),
                            newInterfaceInteraction
                    );
                });
    }

}
