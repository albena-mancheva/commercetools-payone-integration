package com.commercetools.pspadapter.payone;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.commercetools.pspadapter.payone.mapping.*;
import org.quartz.CronScheduleBuilder;
import org.quartz.SchedulerException;

import com.commercetools.pspadapter.payone.config.PayoneConfig;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import com.commercetools.pspadapter.payone.domain.ctp.CustomTypeBuilder;
import com.commercetools.pspadapter.payone.domain.ctp.TypeCacheLoader;
import com.commercetools.pspadapter.payone.domain.ctp.paymentmethods.PaymentMethod;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostService;
import com.commercetools.pspadapter.payone.domain.payone.PayonePostServiceImpl;
import com.commercetools.pspadapter.payone.domain.payone.model.common.NotificationAction;
import com.commercetools.pspadapter.payone.notification.NotificationDispatcher;
import com.commercetools.pspadapter.payone.notification.NotificationProcessor;
import com.commercetools.pspadapter.payone.notification.common.AppointedNotificationProcessor;
import com.commercetools.pspadapter.payone.notification.common.CaptureNotificationProcessor;
import com.commercetools.pspadapter.payone.notification.common.DefaultNotificationProcessor;
import com.commercetools.pspadapter.payone.notification.common.PaidNotificationProcessor;
import com.commercetools.pspadapter.payone.notification.common.UnderpaidNotificationProcessor;
import com.commercetools.pspadapter.payone.transaction.PaymentMethodDispatcher;
import com.commercetools.pspadapter.payone.transaction.TransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.common.DefaultChargeTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.common.UnsupportedTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.creditcard.AuthorizationTransactionExecutor;
import com.commercetools.pspadapter.payone.transaction.paymentinadvance.BankTransferInAdvanceChargeTransactionExecutor;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.sphere.sdk.client.BlockingSphereClient;
import io.sphere.sdk.client.SphereClientFactory;
import io.sphere.sdk.payments.TransactionType;
import io.sphere.sdk.types.Type;

/**
 * @author fhaertig
 * @since 02.12.15
 */
public class ServiceFactory {

    private static final long DEFAULT_CTP_CLIENT_TIMEOUT = 10;

    private final ServiceConfig serviceConfig;
    private final PropertyProvider propertyProvider;

    private ServiceFactory(final PropertyProvider propertyProvider) {
        this.propertyProvider = propertyProvider;
        this.serviceConfig = new ServiceConfig(propertyProvider);
    }

    /**
     * Creates a new service factory initialized with a default {@link PropertyProvider}.
     * @return the new factory instance, never null
     */
    public static ServiceFactory create() {
        return ServiceFactory.withPropertiesFrom(new PropertyProvider());
    }

    /**
     * Creates a new service factory initialized with the provided {@code propertyProvider}.
     * @param propertyProvider provides the configuration parameters
     * @return the new service factory instance, never null
     */
    public static ServiceFactory withPropertiesFrom(final PropertyProvider propertyProvider) {
        return new ServiceFactory(propertyProvider);
    }

    public static void main(String [] args) throws SchedulerException, MalformedURLException {
        final PropertyProvider propertyProvider = new PropertyProvider();
        // FIXME get rid of this (by using instance methods...)
        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);
        final PayoneConfig payoneConfig = new PayoneConfig(propertyProvider);
        final ServiceFactory serviceFactory = ServiceFactory.withPropertiesFrom(propertyProvider);
        final BlockingSphereClient commercetoolsClient = serviceFactory.createCommercetoolsClient();
        final LoadingCache<String, Type> typeCache = serviceFactory.createTypeCache(commercetoolsClient);
        final PaymentDispatcher paymentDispatcher = ServiceFactory.createPaymentDispatcher(
                typeCache,
                payoneConfig,
                serviceConfig,
                commercetoolsClient);
        final NotificationDispatcher notificationDispatcher = ServiceFactory.createNotificationDispatcher(
                commercetoolsClient, payoneConfig);

        final IntegrationService integrationService = ServiceFactory.createService(
                new CommercetoolsQueryExecutor(commercetoolsClient),
                paymentDispatcher,
                notificationDispatcher,
                new CustomTypeBuilder(
                        commercetoolsClient,
                        CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(
                                serviceConfig.getStartFromScratch())));

        integrationService.start();

        ScheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForShortTimeFramePoll()),
                ScheduledJobShortTimeframe.class,
                integrationService,
                paymentDispatcher);

        ScheduledJobFactory.createScheduledJob(
                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForLongTimeFramePoll()),
                ScheduledJobLongTimeframe.class,
                integrationService,
                paymentDispatcher);
    }

    /**
     * FIXME return shared instance
     * Creates a new commercetools client instance.
     * @return the client
     */
    public BlockingSphereClient createCommercetoolsClient() {
        final SphereClientFactory sphereClientFactory = SphereClientFactory.of();
        return BlockingSphereClient.of(
                sphereClientFactory.createClient(
                        serviceConfig.getCtProjectKey(),
                        serviceConfig.getCtClientId(),
                        serviceConfig.getCtClientSecret()),
                DEFAULT_CTP_CLIENT_TIMEOUT,
                TimeUnit.SECONDS);
    }

    // FIXME return shared instance
    public LoadingCache<String, Type> createTypeCache(final BlockingSphereClient client) {
        return CacheBuilder.newBuilder().build(new TypeCacheLoader(client));
    }

    public IntegrationService createService() {
        final BlockingSphereClient client = createCommercetoolsClient();
        final LoadingCache<String, Type> typeCache = createTypeCache(client);
        final PayoneConfig payoneConfig = new PayoneConfig(propertyProvider);

        return ServiceFactory.createService(
                new CommercetoolsQueryExecutor(client),
                createPaymentDispatcher(typeCache, payoneConfig, serviceConfig, client),
                createNotificationDispatcher(client, payoneConfig),
                new CustomTypeBuilder(
                        client,
                        CustomTypeBuilder.PermissionToStartFromScratch.fromBoolean(serviceConfig.getStartFromScratch())));
    }

    // FIXME get rid of this static method
    private static IntegrationService createService(
            final CommercetoolsQueryExecutor queryExecutor,
            final PaymentDispatcher paymentDispatcher,
            final NotificationDispatcher notificationDispatcher,
            final CustomTypeBuilder customTypeBuilder) {
        return new IntegrationService(customTypeBuilder, queryExecutor, paymentDispatcher, notificationDispatcher);
    }

    public static NotificationDispatcher createNotificationDispatcher(final BlockingSphereClient client, final PayoneConfig config) {
        final NotificationProcessor defaultNotificationProcessor = new DefaultNotificationProcessor(client);

        final ImmutableMap.Builder<NotificationAction, NotificationProcessor> builder = ImmutableMap.builder();
        builder.put(NotificationAction.APPOINTED, new AppointedNotificationProcessor(client));
        builder.put(NotificationAction.CAPTURE, new CaptureNotificationProcessor(client));
        builder.put(NotificationAction.PAID, new PaidNotificationProcessor(client));
        builder.put(NotificationAction.UNDERPAID, new UnderpaidNotificationProcessor(client));

        return new NotificationDispatcher(defaultNotificationProcessor, builder.build(), client, config);
    }

    /**
     * TODO transform into instance method
     * @param typeCache
     * @param config
     * @param client
     * @return
     */
    public static PaymentDispatcher createPaymentDispatcher(final LoadingCache<String, Type> typeCache, final PayoneConfig config, final ServiceConfig serviceConfig, final BlockingSphereClient client) {
        // TODO jw: use immutable map
        final HashMap<PaymentMethod, PaymentMethodDispatcher> methodDispatcherMap = new HashMap<>();

        final TransactionExecutor defaultExecutor = new UnsupportedTransactionExecutor(client);
        final PayonePostServiceImpl postService = PayonePostServiceImpl.of(config.getApiUrl());

        final ImmutableSet<PaymentMethod> supportedMethods = ImmutableSet.of(
                PaymentMethod.CREDIT_CARD,
                PaymentMethod.WALLET_PAYPAL,
                PaymentMethod.BANK_TRANSFER_SOFORTUEBERWEISUNG,
                PaymentMethod.BANK_TRANSFER_ADVANCE,
                PaymentMethod.BANK_TRANSFER_POSTFINANCE_CARD,
                PaymentMethod.BANK_TRANSFER_POSTFINANCE_EFINANCE
        );

        for (final PaymentMethod paymentMethod : supportedMethods) {
            final PayoneRequestFactory requestFactory = createRequestFactory(paymentMethod, config, serviceConfig);
            final ImmutableMap.Builder<TransactionType, TransactionExecutor> executors = ImmutableMap.builder();
            for (final TransactionType type : paymentMethod.getSupportedTransactionTypes()) {
                // FIXME jw: shouldn't be nullable anymore when payment method is implemented completely
                final TransactionExecutor executor = Optional
                            .ofNullable(createTransactionExecutor(type, typeCache, client, requestFactory, postService, paymentMethod))
                            .orElse(defaultExecutor);

                executors.put(type, executor);
            }
            methodDispatcherMap.put(paymentMethod, new PaymentMethodDispatcher(defaultExecutor, executors.build()));
        }
        return new PaymentDispatcher(methodDispatcherMap);
    }

    private static TransactionExecutor createTransactionExecutor(
            final TransactionType transactionType,
            final LoadingCache<String, Type> typeCache,
            final BlockingSphereClient client,
            final PayoneRequestFactory requestFactory,
            final PayonePostService postService,
            final PaymentMethod paymentMethod) {

        switch(transactionType) {
            case AUTHORIZATION:
                return new AuthorizationTransactionExecutor(typeCache, requestFactory, postService, client);
            case CANCEL_AUTHORIZATION:
                break;
            case CHARGE:
                switch (paymentMethod) {
                    case BANK_TRANSFER_ADVANCE:
                        return new BankTransferInAdvanceChargeTransactionExecutor(typeCache, requestFactory, postService, client);
                    default:
                        return new DefaultChargeTransactionExecutor(typeCache, requestFactory, postService, client);
                }
            case REFUND:
                break;
            case CHARGEBACK:
                break;
        }
        return null;
    }

    private static PayoneRequestFactory createRequestFactory(final PaymentMethod method, final PayoneConfig payoneConfig, final ServiceConfig serviceConfig) {
        switch(method) {
            case CREDIT_CARD:
                return new CreditCardRequestFactory(payoneConfig);
            case WALLET_PAYPAL:
                return new PaypalRequestFactory(payoneConfig);
            case BANK_TRANSFER_SOFORTUEBERWEISUNG:
                return new SofortBankTransferRequestFactory(payoneConfig, serviceConfig);
            case BANK_TRANSFER_POSTFINANCE_CARD:
            case BANK_TRANSFER_POSTFINANCE_EFINANCE:
                return new PostFinanceBanktransferRequestFactory(payoneConfig);
            case BANK_TRANSFER_ADVANCE:
                return new BanktTransferInAdvanceRequestFactory(payoneConfig);
            default:
                throw new IllegalArgumentException(String.format("No PayoneRequestFactory could be created for payment method %s", method));
        }
    }
}
