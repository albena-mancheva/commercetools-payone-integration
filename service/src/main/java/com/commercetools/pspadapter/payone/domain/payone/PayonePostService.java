package com.commercetools.pspadapter.payone.domain.payone;

import com.commercetools.pspadapter.payone.domain.payone.exceptions.PayoneException;
import com.commercetools.pspadapter.payone.domain.payone.model.common.PayoneBaseRequest;

import java.util.Map;

/**
 * Encapsulates the communication with the payone services.
 *
 */
public interface PayonePostService {

    /**
     * Setup connection and execute post request with params.
     * @param requestParams set to request
     * @return Map containing the servers response
     * @throws PayoneException - the payone exception
     */
    Map<String, String> executePost(final PayoneBaseRequest requestParams) throws PayoneException;

}