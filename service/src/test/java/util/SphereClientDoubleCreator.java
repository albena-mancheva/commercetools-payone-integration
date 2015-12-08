package util;

import io.sphere.sdk.client.HttpRequestIntent;
import io.sphere.sdk.client.SphereClient;
import io.sphere.sdk.client.SphereClientFactory;

import java.util.function.Function;

/**
 * @author fhaertig
 * @date 07.12.15
 */
public class SphereClientDoubleCreator {

    public static SphereClient getSphereClientWithResponseFunction(final Function<HttpRequestIntent, Object> responseFunction) {
        return SphereClientFactory.createObjectTestDouble(httpRequest -> {
            Object result = null;
            result = responseFunction.apply(httpRequest);
            return result;
        });
    }
}
