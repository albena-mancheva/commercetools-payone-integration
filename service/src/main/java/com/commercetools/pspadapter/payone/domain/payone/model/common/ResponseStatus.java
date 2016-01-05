package com.commercetools.pspadapter.payone.domain.payone.model.common;

/**
 * @author fhaertig
 * @date 11.12.15
 */
public enum ResponseStatus {
    REDIRECT("REDIRECT"),
    APPROVED("APPROVED"),
    PENDING("PENDING"),
    ERROR("ERROR");

    private String stateCode;

    ResponseStatus(final String stateCode) {
        this.stateCode = stateCode;
    }
}
