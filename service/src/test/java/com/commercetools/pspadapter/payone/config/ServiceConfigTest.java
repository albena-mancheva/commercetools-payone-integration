package com.commercetools.pspadapter.payone.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 03.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceConfigTest {

    private static final String dummyValue = "123";

    @Mock
    private PropertyProvider propertyProvider;

    @Mock
    private PayoneConfig payoneConfig;

    @Before
    public void setUp() {
        when(propertyProvider.getProperty(anyString())).thenReturn(Optional.of(dummyValue));
        when(propertyProvider.getMandatoryNonEmptyProperty(anyString())).thenReturn(dummyValue);
    }

    @Test
    public void getsCtProjectKey() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_PROJECT_KEY)).thenReturn("project X");
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).getSphereClientConfig().getProjectKey()).isEqualTo("project X");
    }

    @Test
    public void throwsInCaseOfMissingCtProjectKey() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.CT_PROJECT_KEY);
    }

    @Test
    public void getsCtClientId() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_ID)).thenReturn("id X");
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).getSphereClientConfig().getClientId()).isEqualTo("id X");
    }

    @Test
    public void throwsInCaseOfMissingCtClientId() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.CT_CLIENT_ID);
    }

    @Test
    public void getsCtClientSecret() {
        when(propertyProvider.getMandatoryNonEmptyProperty(PropertyProvider.CT_CLIENT_SECRET)).thenReturn("secret X");
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).getSphereClientConfig().getClientSecret()).isEqualTo("secret X");
    }

    @Test
    public void getsScheduledJobCronForShortTimeFramePoll() {
        when(propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.of("short cron"));
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).getScheduledJobCronForShortTimeFramePoll())
                .isEqualTo("short cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForShortTimeFramePollIfPropertyIsNotProvided() {
        when(propertyProvider.getProperty(PropertyProvider.SHORT_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.empty());
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).getScheduledJobCronForShortTimeFramePoll())
                .isEqualTo("0/30 * * * * ? *");
    }

    @Test
    public void getsScheduledJobCronForLongTimeFramePoll() {
        when(propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.of("long cron"));
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).getScheduledJobCronForLongTimeFramePoll())
                .isEqualTo("long cron");
    }

    @Test
    public void getsDefaultScheduledJobCronForLongTimeFramePollIfPropertyIsNotProvided() {
        when(propertyProvider.getProperty(PropertyProvider.LONG_TIME_FRAME_SCHEDULED_JOB_CRON))
                .thenReturn(Optional.empty());
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).getScheduledJobCronForLongTimeFramePoll())
                .isEqualTo("5 0 0/1 * * ? *");
    }

    @Test
    public void throwsInCaseOfMissingCtClientSecret() {
        assertThatThrowsInCaseOfMissingOrEmptyProperty(PropertyProvider.CT_CLIENT_SECRET);
    }

    @Test
    public void getsIsUpdateOrderPaymentState() {
        when(propertyProvider.getProperty(PropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("true"));
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(true);

        when(propertyProvider.getProperty(PropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("TRUE"));
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(true);

        when(propertyProvider.getProperty(PropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("false"));
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);

        when(propertyProvider.getProperty(PropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of("FALSE"));
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);

        when(propertyProvider.getProperty(PropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.of(""));
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);

        when(propertyProvider.getProperty(PropertyProvider.UPDATE_ORDER_PAYMENT_STATE)).thenReturn(Optional.empty());
        assertThat(new ServiceConfig(propertyProvider, payoneConfig).isUpdateOrderPaymentState()).isEqualTo(false);
    }

    private void assertThatThrowsInCaseOfMissingOrEmptyProperty(final String propertyName) {
        final IllegalStateException illegalStateException = new IllegalStateException();
        when(propertyProvider.getMandatoryNonEmptyProperty(propertyName)).thenThrow(illegalStateException);

        final Throwable throwable = catchThrowable(() -> new ServiceConfig(propertyProvider, payoneConfig));

        assertThat(throwable).isSameAs(illegalStateException);
    }
}
