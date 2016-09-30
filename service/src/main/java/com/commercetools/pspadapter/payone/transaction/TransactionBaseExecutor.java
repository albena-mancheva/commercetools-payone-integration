package com.commercetools.pspadapter.payone.transaction;

import com.google.common.cache.LoadingCache;
import io.sphere.sdk.json.SphereJsonUtils;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceCode;
import io.sphere.sdk.payments.commands.updateactions.SetStatusInterfaceText;
import io.sphere.sdk.types.Type;

import java.util.Map;

/**
 * @author mht@dotsource.de
 * Common base class responsible for default paymentupdateactions
 */
public abstract class TransactionBaseExecutor extends IdempotentTransactionExecutor{

    public final static String STATUS = "status";
    public final static String ERROR_CODE = "errorcode";
    public final static String CUSTOMER_MESSAGE = "customermessage";
    public final static String ERROR_MESSAGE = "errormessage";
    public final static String ERROR = "ERROR";

    public TransactionBaseExecutor(LoadingCache<String, Type> typeCache) {
        super(typeCache);
    }

    /**
     * Creates the SetStatusInterfaceCode from the response
     * @param response contains all key that creates the
     * @return the UpdateAction that
     */
    protected SetStatusInterfaceCode setStatusInterfaceCode(final Map<String, String> response){
        final String status = response.get(STATUS);
        final StringBuilder stringBuilder = new StringBuilder(status);
        if(ERROR.equals(status)) {
            //Payone errorcode is required for error case
            stringBuilder.append(" ");
            stringBuilder.append(response.get(ERROR_CODE));
        }
        return SetStatusInterfaceCode.of(stringBuilder.toString());
    }

    /**
     * Creates the SetStatusInterfaceText from the response ErrorMessage in case of Error
     * @param response contains all key that creates the
     * @return the UpdateAction that
     */
    protected SetStatusInterfaceText setStatusInterfaceText(final Map<String, String> response){
        final String status = response.get(STATUS);
        final StringBuilder stringBuilder = new StringBuilder("");
        if(ERROR.equals(status)) {
            stringBuilder.append(response.get(ERROR_MESSAGE));
        }
        else {
            stringBuilder.append(status);
        }
        return SetStatusInterfaceText.of(stringBuilder.toString());
    }

    /**
     * Converts {@code Map<String, String>} response value to valid JSON string.
     * @param response response map to convert
     * @return JSON string with respective {@code response} key-value entries.
     */
    protected final String responseToJsonString(Map<String, String> response) {
        return SphereJsonUtils.toJsonString(response);
    }
}
