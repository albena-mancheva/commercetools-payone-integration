package specs.paymentmethods.creditcard;

import com.commercetools.pspadapter.payone.domain.ctp.BlockingClient;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.neovisionaries.i18n.CountryCode;
import io.sphere.sdk.carts.Cart;
import io.sphere.sdk.carts.CartDraft;
import io.sphere.sdk.carts.CartDraftBuilder;
import io.sphere.sdk.carts.commands.CartCreateCommand;
import io.sphere.sdk.carts.commands.CartUpdateCommand;
import io.sphere.sdk.carts.commands.updateactions.AddLineItem;
import io.sphere.sdk.carts.commands.updateactions.AddPayment;
import io.sphere.sdk.carts.commands.updateactions.SetBillingAddress;
import io.sphere.sdk.carts.commands.updateactions.SetShippingAddress;
import io.sphere.sdk.models.Address;
import io.sphere.sdk.orders.OrderFromCartDraft;
import io.sphere.sdk.orders.PaymentState;
import io.sphere.sdk.orders.commands.OrderFromCartCreateCommand;
import io.sphere.sdk.payments.Payment;
import io.sphere.sdk.payments.PaymentDraft;
import io.sphere.sdk.payments.PaymentDraftBuilder;
import io.sphere.sdk.payments.PaymentMethodInfoBuilder;
import io.sphere.sdk.payments.TransactionDraft;
import io.sphere.sdk.payments.TransactionDraftBuilder;
import io.sphere.sdk.payments.TransactionState;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.payments.commands.PaymentCreateCommand;
import io.sphere.sdk.products.Product;
import io.sphere.sdk.products.queries.ProductQuery;
import io.sphere.sdk.types.CustomFieldsDraft;
import org.apache.http.HttpResponse;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;
import specs.BaseFixture;

import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.format.MonetaryFormats;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * @author fhaertig
 * @date 10.12.15
 */
@RunWith(ConcordionRunner.class)
public class AuthorizationFixture extends BaseFixture {

    public String createPayment(
            final String paymentName,
            final String paymentMethod,
            final String transactionType,
            final String centAmount,
            final String currencyCode) throws ExecutionException, InterruptedException {

        final MonetaryAmount monetaryAmount = createMonetaryAmountFromCent(Long.valueOf(centAmount), currencyCode);

        final List<TransactionDraft> transactions = Collections.singletonList(TransactionDraftBuilder
                .of(TransactionType.valueOf(transactionType), monetaryAmount, ZonedDateTime.now())
                .state(TransactionState.PENDING)
                .build());

        final PaymentDraft paymentDraft = PaymentDraftBuilder.of(monetaryAmount)
                .paymentMethodInfo(PaymentMethodInfoBuilder.of()
                        .method(paymentMethod)
                        .paymentInterface("PAYONE")
                        .build())
                .transactions(transactions)
                .custom(CustomFieldsDraft.ofTypeKeyAndObjects(
                        CustomTypeBuilder.PAYMENT_CREDIT_CARD,
                        ImmutableMap.of(
                                CustomTypeBuilder.CARD_DATA_PLACEHOLDER_FIELD, getUnconfirmedVisaPseudoCardPan(),
                                CustomTypeBuilder.LANGUAGE_CODE_FIELD, Locale.ENGLISH.getLanguage(),
                                CustomTypeBuilder.REFERENCE_FIELD, "myGlobalKey")))
                .build();

        final BlockingClient ctpClient = ctpClient();
        final Payment payment = ctpClient.complete(PaymentCreateCommand.of(paymentDraft));
        registerPaymentWithLegibleName(paymentName, payment);

        // create cart and order with product
        final Product product = ctpClient.complete(ProductQuery.of()).getResults().get(0);

        final CartDraft cardDraft = CartDraftBuilder.of(Monetary.getCurrency(currencyCode)).build();

        final Cart cart = ctpClient.complete(CartUpdateCommand.of(
                ctpClient.complete(CartCreateCommand.of(cardDraft)),
                ImmutableList.of(
                        AddPayment.of(payment),
                        AddLineItem.of(product.getId(), product.getMasterData().getCurrent().getMasterVariant().getId(), 1),
                        SetShippingAddress.of(Address.of(CountryCode.DE)),
                        SetBillingAddress.of(Address.of(CountryCode.DE).withLastName("Test Buyer"))
                )));

        ctpClient.complete(OrderFromCartCreateCommand.of(
                OrderFromCartDraft.of(cart, getRandomOrderNumber(), PaymentState.PENDING)));

        return payment.getId();
    }

    public Map<String, String> handlePayment(final String paymentName,
                                             final String interactionTypeName,
                                             final String requestType) throws ExecutionException, IOException {
        final HttpResponse response = requestToHandlePaymentByLegibleName(paymentName);
        final Payment payment = fetchPaymentByLegibleName(paymentName);
        final String transactionId = getIdOfLastTransaction(payment);
        final String amountAuthorized = (payment.getAmountAuthorized() != null) ?
                MonetaryFormats.getAmountFormat(Locale.GERMANY).format(payment.getAmountAuthorized()) :
                BaseFixture.EMPTY_STRING;

        return ImmutableMap.of(
                "statusCode", Integer.toString(response.getStatusLine().getStatusCode()),
                "interactionCount", getInterfaceInteractionCount(payment, transactionId, interactionTypeName, requestType),
                "transactionState", getTransactionState(payment, transactionId),
                "amountAuthorized", amountAuthorized);
    }

    private String getInterfaceInteractionCount(final Payment payment,
                                                final String transactionId,
                                                final String interactionTypeName,
                                                final String requestType) throws ExecutionException {
        final String interactionTypeId = typeIdFromTypeName(interactionTypeName);
        return Long.toString(payment.getInterfaceInteractions().stream()
                .filter(i -> i.getType().getId().equals(interactionTypeId))
                .filter(i -> transactionId.equals(i.getFieldAsString(CustomTypeBuilder.TRANSACTION_ID_FIELD)))
                .filter(i -> {
                    final String requestField = i.getFieldAsString(CustomTypeBuilder.REQUEST_FIELD);
                    return (requestField != null) && requestField.contains("request=" + requestType);
                })
                .count());
    }
}
