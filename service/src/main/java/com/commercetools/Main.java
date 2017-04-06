package com.commercetools;

import com.commercetools.pspadapter.payone.IntegrationService;
import com.commercetools.pspadapter.payone.ServiceFactory;
import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.payone.config.ServiceConfig;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws SchedulerException, MalformedURLException {

        final PropertyProvider propertyProvider = new PropertyProvider();
        final ServiceConfig serviceConfig = new ServiceConfig(propertyProvider);

        final IntegrationService integrationService = ServiceFactory.createIntegrationService(propertyProvider, serviceConfig);

        integrationService.start();

        LOG.error("SCHEDULERS ARE NOT STARTED");

        // TODO: finalize schedulers

//        ScheduledJobFactory scheduledJobFactory = new ScheduledJobFactory();
//
//        scheduledJobFactory.setAllScheduledItemsStartedListener(() ->
//                LOG.info(format("%n%1$s%nPayone Integration Service is STARTED%n%1$s",
//                "============================================================"))
//        );

//        scheduledJobFactory.createScheduledJob(
//                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForShortTimeFramePoll()),
//                ScheduledJobShortTimeframe.class,
//                integrationService,
//                serviceFactory.getPaymentDispatcher());
//
//        scheduledJobFactory.createScheduledJob(
//                CronScheduleBuilder.cronSchedule(serviceConfig.getScheduledJobCronForLongTimeFramePoll()),
//                ScheduledJobLongTimeframe.class,
//                integrationService,
//                serviceFactory.getPaymentDispatcher());
    }
}
