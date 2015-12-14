package com.commercetools.pspadapter.payone.domain.payone.model.creditcard;

import com.commercetools.pspadapter.payone.PayoneConfig;
import com.commercetools.pspadapter.payone.domain.payone.model.common.ClearingType;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PreauthorizationRequest;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public class CCPreauthorizationRequest extends PreauthorizationRequest {

    /**
     * pseudo card number
     */
    private String pseudocardpan;

    private String ecommercemode;

    public CCPreauthorizationRequest(final PayoneConfig config, final String pseudocardpan) {
        super(config, ClearingType.PAYONE_CC.getPayoneCode());

        this.pseudocardpan = pseudocardpan;
    }

    //**************************************************************
    //* GETTER AND SETTER METHODS
    //**************************************************************


    public String getPseudocardpan() {
        return pseudocardpan;
    }

    public String getEcommercemode() {
        return ecommercemode;
    }

    public void setEcommercemode(final String ecommercemode) {
        this.ecommercemode = ecommercemode;
    }
}
