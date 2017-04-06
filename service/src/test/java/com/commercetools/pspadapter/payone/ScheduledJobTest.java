package com.commercetools.pspadapter.payone;

import com.commercetools.pspadapter.payone.domain.ctp.CommercetoolsQueryExecutor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;

/**
 * @author fhaertig
 * @since 07.12.15
 */
@RunWith(MockitoJUnitRunner.class)
public class ScheduledJobTest {

    @Mock
    private IntegrationService integrationService;

    @Mock
    private CommercetoolsQueryExecutor commercetoolsQueryExecutor;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Test
    public void createScheduledJob() throws JobExecutionException {
        final ScheduledJob job = new ScheduledJob() {
            @Override
            protected ZonedDateTime getSinceDateTime() {
                return ZonedDateTime.now().minusMinutes(2);
            }
        };
        final JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduledJob.COMMERCETOOLS_QUERY_EXECUTOR, integrationService);
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(dataMap);
//        when(integrationService.getCommercetoolsQueryExecutor()).thenReturn(commercetoolsQueryExecutor);
//
//        job.execute(jobExecutionContext);
//        verify(integrationService).getCommercetoolsQueryExecutor();

//        verify(commercetoolsQueryExecutor).consumePaymentCreatedMessages(zonedDateTime, any());
//        verify(commercetoolsQueryExecutor).consumePaymentTransactionAddedMessages(zonedDateTime, any());
    }
}
