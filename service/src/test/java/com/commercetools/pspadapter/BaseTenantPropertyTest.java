package com.commercetools.pspadapter;

import com.commercetools.pspadapter.payone.config.PropertyProvider;
import com.commercetools.pspadapter.tenant.TenantPropertyProvider;
import org.junit.Before;
import org.mockito.Mock;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class BaseTenantPropertyTest {

    protected static final String dummyPropertyValue = "123";
    protected static final String dummyTenantValue = "456";

    @Mock
    protected TenantPropertyProvider tenantPropertyProvider;

    @Mock
    protected PropertyProvider propertyProvider;

    @Before
    public void setUp() throws Exception {
        // 1) inject dummy values to tenant and common properties providers
        // 2) inject propertyProvider ot tenantPropertyProvider
        when(propertyProvider.getProperty(any())).thenReturn(Optional.of(dummyPropertyValue));
        when(propertyProvider.getMandatoryNonEmptyProperty(any())).thenReturn(dummyPropertyValue);

        when(tenantPropertyProvider.getPropertyProvider()).thenReturn(propertyProvider);
        when(tenantPropertyProvider.getTenantProperty(anyString())).thenReturn(Optional.of(dummyTenantValue));
        when(tenantPropertyProvider.getTenantMandatoryNonEmptyProperty(anyString())).thenReturn(dummyTenantValue);
    }
}
