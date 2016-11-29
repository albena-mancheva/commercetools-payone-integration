package com.commercetools.pspadapter.payone.config;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

/**
 * @author fhaertig
 * @since 11.12.15
 */
public class PayoneConfig {

    public static final String DEFAULT_PAYONE_API_URL = "https://api.pay1.de/post-gateway/";
    public static final String DEFAULT_PAYONE_MODE = "test";
    public static final String DEFAULT_PAYONE_REQUEST_ENCODING = "UTF-8";

    //assure that properties don't change once service started
    private final String subAccountId;
    private final String merchantId;
    private final String portalId;
    private final String keyAsMd5Hash;
    private final String mode;
    private final String apiUrl;
    private final String apiVersion;
    private final String encoding;
    private final String solutionName;
    private final String solutionVersion;
    private final String integratorName;
    private final String integratorVersion;


    public PayoneConfig(final PropertyProvider propertyProvider) {
        subAccountId = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_SUBACC_ID);
        merchantId = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_MERCHANT_ID);
        portalId = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_PORTAL_ID);
        mode = propertyProvider.getProperty(PropertyProvider.PAYONE_MODE).orElse(DEFAULT_PAYONE_MODE);
        apiUrl = propertyProvider.getProperty(PropertyProvider.PAYONE_API_URL).orElse(DEFAULT_PAYONE_API_URL);
        apiVersion = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_API_VERSION);
        encoding = propertyProvider.getProperty(PropertyProvider.PAYONE_REQUEST_ENCODING).orElse(DEFAULT_PAYONE_REQUEST_ENCODING);
        final String plainKey = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_KEY);
        keyAsMd5Hash = Hashing.md5().hashString(plainKey, Charsets.UTF_8).toString();
        solutionName = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_SOLUTION_NAME);
        solutionVersion = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_SOLUTION_VERSION);
        integratorName = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_INTEGRATOR_NAME);
        integratorVersion = propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.PAYONE_INTEGRATOR_VERSION);
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getSubAccountId() {
        return subAccountId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getKeyAsMd5Hash() {
        return keyAsMd5Hash;
    }

    public String getPortalId() {
        return portalId;
    }

    public String getMode() {
        return mode;
    }

    public String getApiVersion() { return apiVersion; }

    public String getEncoding() { return encoding; }

    public String getSolutionName() { return solutionName; }

    public String getSolutionVersion() { return solutionVersion; }

    public String getIntegratorName() { return integratorName; }

    public String getIntegratorVersion() { return integratorVersion; }


}
