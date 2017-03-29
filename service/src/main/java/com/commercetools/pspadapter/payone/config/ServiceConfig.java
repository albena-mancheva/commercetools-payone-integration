package com.commercetools.pspadapter.payone.config;

import io.sphere.sdk.client.SphereClientConfig;

/**
 * Provides the configuration of the integration service.
 *
 * @author fhaertig
 * @author Jan Wolter
 * @since 02.12.15
 */
public class ServiceConfig {

    private final SphereClientConfig sphereClientConfig;

    private final PayoneConfig payoneConfig;

    private final boolean startFromScratch;
    private final String scheduledJobCronShortTimeFrame;
    private final String scheduledJobCronLongTimeFrame;
    private final String secureKey;

    /**
     * Initializes the configuration.
     *
     * @param propertyProvider to get the parameters from
     * @throws IllegalStateException if a mandatory parameter is undefined or empty
     */
    public ServiceConfig(final PropertyProvider propertyProvider) {

        sphereClientConfig = SphereClientConfig.of(
                propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_PROJECT_KEY),
                propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_ID),
                propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_SECRET)
        );

        payoneConfig = new PayoneConfig(propertyProvider);

        scheduledJobCronShortTimeFrame =
                propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("0/30 * * * * ? *");
        scheduledJobCronLongTimeFrame =
                propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON)
                        .map(String::valueOf)
                        .orElse("5 0 0/1 * * ? *");
        startFromScratch = propertyProvider.getProperty(PropertyProvider.CT_START_FROM_SCRATCH)
                .map(Boolean::valueOf)
                .orElse(false);

        secureKey = propertyProvider.getProperty(PropertyProvider.SECURE_KEY)
                .map(String::valueOf)
                .orElse("");
    }

    public SphereClientConfig getSphereClientConfig() {
        return sphereClientConfig;
    }

    public PayoneConfig getPayoneConfig() {
        return payoneConfig;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a shorter time frame.
     * @return the cron expression
     */
    public String getScheduledJobCronForShortTimeFramePoll() {
        return scheduledJobCronShortTimeFrame;
    }

    /**
     * Gets the
     * <a href="http://www.quartz-scheduler.org/documentation/quartz-1.x/tutorials/crontrigger">QUARTZ cron expression</a>
     * for polling commercetools messages with a longer time frame.
     * @return the cron expression
     */
    public String getScheduledJobCronForLongTimeFramePoll() {
        return scheduledJobCronLongTimeFrame;
    }

    /**
     * Gets the flag indicating whether the service shall reset the commerctools project at start up.
     * @return whether the commercetools project shall be reset
     */
    public boolean getStartFromScratch() {
        return startFromScratch;
    }

    /**
     * Gets the secure key which was used for encrypting data with Blowfish.
     * @return the secure key as plain text
     */
    public String getSecureKey() {
        return secureKey;
    }
}
