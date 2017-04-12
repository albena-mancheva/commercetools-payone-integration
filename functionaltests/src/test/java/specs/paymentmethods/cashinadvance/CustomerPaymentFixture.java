package specs.paymentmethods.cashinadvance;

import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.mapping.CustomFieldKeys;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.commands.UpdateActionImpl;
import io.sphere.sdk.payments.*;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.payments.commands.PaymentUpdateCommand;
import io.sphere.sdk.payments.commands.updateactions.AddTransaction;
import io.sphere.sdk.payments.commands.updateactions.SetCustomField;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;
import specs.paymentmethods.BaseNotifiablePaymentFixture;

import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author mht@dotsource.de
 *
 */
@RunWith(ConcordionRunner.class)
public class CustomerPaymentFixture extends BaseNotifiablePaymentFixture {

    public String  createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode,
            final String buyerLastName) throws ExecutionException, InterruptedException, UnsupportedEncodingException {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CASH_ADVANCE,
                        ImmutableMap.<String, Object>builder()
                                .put(CustomFieldKeys.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage())
                                .put(CustomFieldKeys.REFERENCE_FIELD, "<placeholder>")
                                .build()))
                .build();

        final BlockingSphereClient ctpClient = ctpClient();
        Payment payment = ctpClient.executeBlocking(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        final String orderNumber = createCartAndOrderForPayment(payment, currencyCode, buyerLastName);

        ctpClient.executeBlocking(PaymentUpdateCommand.of(
                payment,
                ImmutableList.<UpdateActionImpl<Payment>>builder()
                        .add(AddTransaction.of(TransactionDraftBuilder.of(
                                TransactionType.valueOf(transactionType),
                                monetaryAmount,
                                ZonedDateTime.now())
                                .state(TransactionState.PENDING)
                                .build()))
                        .add(SetCustomField.ofObject(CustomFieldKeys.REFERENCE_FIELD, orderNumber))
                        .build()));

        return payment.getId();
    }

    public Map<String, String> handlePayment(final String paymentName, final String requestType)
            throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfFirstTransaction(payment);
        final String amountAuthorized = (payment.getAmountAuthorized() != null)
                ? MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized())
                : BaseFixture.EMPTY_STRING;
        final String amountPaid = (payment.getAmountPaid() != null)
                ? MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid())
                : BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String>builder()
                .put("statusCode", Integer.toString(response.getStatusLine().getStatusCode()))
                .put("interactionCount", getInteractionRequestCountOverAllTransactions(payment, requestType))
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("version", payment.getVersion().toString()).build();
    }

    @Override
    public boolean receivedNotificationOfActionFor(final String paymentNames, final String txaction) throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNotificationOfActionFor(paymentNames, txaction);
    }

    @Override
    public boolean receivedNextNotificationOfActionFor(String paymentNames, String txaction, String prevTxaction) throws Exception {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.receivedNextNotificationOfActionFor(paymentNames, txaction, prevTxaction);
    }

    @Override
    public String fetchOrderPaymentState(final String paymentName) {
        // we keep this overriding just to easily see which test methods are run in this fixture
        return super.fetchOrderPaymentState(getIdForLegibleName(paymentName));
    }

    public Map<String, String> fetchPaymentDetails(final String paymentName)
            throws InterruptedException, ExecutionException {
        final Payment payment = fetchPaymentByLegibleName(paymentName);

        final String transactionId = getIdOfFirstTransaction(payment);

        final long appointedNotificationCount =
                getTotalNotificationCountOfAction(payment, "appointed");
        final long paidNotificationCount = getTotalNotificationCountOfAction(payment, "paid");

        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        final String amountPaid = (payment.getAmountPaid() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountPaid()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.<String, String>builder()
                .put("transactionState", getTransactionState(payment, transactionId))
                .put("amountAuthorized", amountAuthorized)
                .put("amountPaid", amountPaid)
                .put("appointedNotificationCount", String.valueOf(appointedNotificationCount))
                .put("paidNotificationCount", String.valueOf(paidNotificationCount))
                .put("version", payment.getVersion().toString())
                .build();
    }
}
