package com.commercetools.pspadapter.payone.mapping;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.ctp.PaymentWithCartLike;
import com.commercetools.pspadapter.payone.domain.payone.model.creditcard.CreditCardPreauthorizationRequest;
import com.google.common.base.Preconditions;
import io.sphere.sdk.carts.CartLike;
import io.sphere.sdk.payments.Payment;
import org.javamoney.moneta.function.MonetaryUtil;

/**
 * @author fhaertig
 * @date 13.12.15
 */
public class CreditCardRequestFactory extends PayoneRequestFactory {

    public CreditCardRequestFactory(final PayoneConfig config) {
        super(config);
    }

    @Override
    public CreditCardPreauthorizationRequest createPreauthorizationRequest(
            final PaymentWithCartLike paymentWithCartLike) {

        final Payment ctPayment = paymentWithCartLike.getPayment();
        final CartLike ctCartLike = paymentWithCartLike.getCartLike();
        Preconditions.checkArgument(ctPayment.getCustom() != null, "Missing custom fields on payment!");

        String pseudocardpan = ctPayment.getCustom().getFieldAsString(CustomFieldKeys.PSEUDOCARDPAN_KEY);
        CreditCardPreauthorizationRequest request = new CreditCardPreauthorizationRequest(getConfig(), pseudocardpan);

        if (paymentWithCartLike.getOrderNumber().isPresent()) {
            request.setReference(paymentWithCartLike.getOrderNumber().get());
        }

        request.setAmount(MonetaryUtil.minorUnits().queryFrom(ctPayment.getAmountPlanned()).intValue());
        request.setCurrency(ctPayment.getAmountPlanned().getCurrency().getCurrencyCode());
        request.setNarrative_text(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.NARRATIVETEXT_KEY));
        request.setUserid(ctPayment.getCustom().getFieldAsString(CustomFieldKeys.USERID_KEY));

        MappingUtil.mapCustomerToRequest(request, ctPayment.getCustomer());
        MappingUtil.mapBillingAddressToRequest(request, ctCartLike.getBillingAddress());
        MappingUtil.mapShippingAddressToRequest(request, ctCartLike.getShippingAddress());
        return request;
    }

}
