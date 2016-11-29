package com.commercetools.pspadapter.payone.config;

import com.google.common.collect.ImmutableMap;

import java.util.Optional;

/**
 * @author fhaertig
 * @since 15.12.15
 */
public class PropertyProvider {

    public static final String PAYONE_API_VERSION = "PAYONE_API_VERSION";
    public static final String PAYONE_REQUEST_ENCODING = "PAYONE_REQUEST_ENCODING";

    public static final String PAYONE_SOLUTION_NAME = "PAYONE_SOLUTION_NAME";
    public static final String PAYONE_SOLUTION_VERSION = "PAYONE_SOLUTION_VERSION";
    public static final String PAYONE_INTEGRATOR_NAME = "PAYONE_INTEGRATOR_NAME";
    public static final String PAYONE_INTEGRATOR_VERSION = "PAYONE_INTEGRATOR_VERSION";

    public static final String PAYONE_API_URL = "PAYONE_API_URL";
    public static final String PAYONE_SUBACC_ID = "PAYONE_SUBACC_ID";
    public static final String PAYONE_MERCHANT_ID = "PAYONE_MERCHANT_ID";
    public static final String PAYONE_PORTAL_ID = "PAYONE_PORTAL_ID";
    public static final String PAYONE_KEY = "PAYONE_KEY";
    public static final String PAYONE_MODE = "PAYONE_MODE";

    public static final String CT_PROJECT_KEY = "CT_PROJECT_KEY";
    public static final String CT_CLIENT_ID = "CT_CLIENT_ID";
    public static final String CT_CLIENT_SECRET = "CT_CLIENT_SECRET";
    public static final String CT_START_FROM_SCRATCH = "CT_START_FROM_SCRATCH";
    public static final String SHORT_TIME_FRAME_SCHEDULED_JOB_CRON = "SHORT_TIME_FRAME_SCHEDULED_JOB_CRON";
    public static final String LONG_TIME_FRAME_SCHEDULED_JOB_CRON = "LONG_TIME_FRAME_SCHEDULED_JOB_CRON";

    public static final String SECURE_KEY = "SECURE_KEY";

    private final ImmutableMap<String, String> internalProperties;

    public PropertyProvider() {
        internalProperties = ImmutableMap.<String, String>builder()
            .put(PAYONE_API_VERSION, "3.9")
            .put(PAYONE_REQUEST_ENCODING, "UTF-8")
            .put(PAYONE_SOLUTION_NAME, "commercetools-platform")
            .put(PAYONE_SOLUTION_VERSION, "1")
            .put(PAYONE_INTEGRATOR_NAME, "commercetools-payone-integration")
            // TODO set dynamically
            .put(PAYONE_INTEGRATOR_VERSION, "0.1-SNAPSHOT")
            .build();
    }

    /**
     * Gets an optional property.
     * @param propertyName the name of the requested property, must not be null
     * @return the property, an empty Optional if not present; empty values are treated as present
     */
    public Optional<String> getProperty(final String propertyName) {
        final Optional<String> internalProperty = Optional.ofNullable(internalProperties.get(propertyName));
        if (internalProperty.isPresent()) {
            return internalProperty;
        }

        final Optional<String> environmentValue = Optional.ofNullable(System.getenv(propertyName));
        return environmentValue;
    }

    /**
     * Gets a mandatory non-empty property.
     * @param propertyName the name of the requested property, must not be null
     * @return the property value
     * @throws IllegalStateException if the property isn't defined or empty
     */
    public String getMandatoryNonEmptyProperty(final String propertyName) {
        return getProperty(propertyName)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> createIllegalStateException(propertyName));
    }

    private IllegalStateException createIllegalStateException(final String propertyName) {
        return new IllegalStateException("Value of " + propertyName + " is required and can not be empty!");
    }
}
