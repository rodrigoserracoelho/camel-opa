package org.apache.camel.component.opa;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.InputStreamReader;
import java.io.Reader;

@Slf4j
public class OpaOperation {

    private final Gson gson = new Gson();

    public void processOperation(OpaEndpoint opaEndpoint, Exchange exchange) throws Exception {
        switch(opaEndpoint.getOperationType()) {
            case QUERY:
                query(opaEndpoint, exchange);
                break;
            case POLICY:
                log.trace("To implement Policy");
                break;
            case ACL:
                log.trace("To implement ACL");
        }
    }

    private void query(OpaEndpoint opaEndpoint, Exchange exchange) throws Exception {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

        if(opaEndpoint.getConnectTimeout() > 0) {
            requestConfigBuilder.setConnectTimeout(opaEndpoint.getConnectTimeout() * 1000);
        }

        if(opaEndpoint.getConnectionRequestTimeout() > 0) {
            requestConfigBuilder.setConnectionRequestTimeout(opaEndpoint.getConnectionRequestTimeout() * 1000);
        }

        if(opaEndpoint.getSocketTimeout() > 0) {
            requestConfigBuilder.setSocketTimeout(opaEndpoint.getSocketTimeout() * 1000);
        }

        RequestConfig requestConfig = requestConfigBuilder.build();
        CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

        try {
            String input = exchange.getIn().getBody(String.class);
            log.trace("Request received by OPA Component: {}", input);

            HttpPost httpPost = new HttpPost(transformOpaEndpoint(opaEndpoint));
            HttpEntity httpEntity = new ByteArrayEntity(input.getBytes());
            httpPost.setEntity(httpEntity);

            CloseableHttpResponse httpResponse = closeableHttpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                Reader reader = new InputStreamReader(httpResponse.getEntity().getContent());
                OpaResult opaResult = gson.fromJson(reader, OpaResult.class);
                if (!opaResult.isResult()) {
                    handleException(opaEndpoint, "OPA returned not allowed", exchange);
                }
            } else {
                handleException(opaEndpoint, "Error calling OPA endpoint: " + opaEndpoint.getEndpointUri(), exchange);
            }
            exchange.getIn().setHeader("OPA-RESULT", "Valid");

        } catch(Exception e) {
            handleException(opaEndpoint, e, exchange);
        } finally {
            closeableHttpClient.close();
        }
    }

    private String transformOpaEndpoint(OpaEndpoint endpoint) {
        String opaEndpoint = endpoint.getEndpointBaseUri();

        if(endpoint.getSecure()) {
            return "https://" + opaEndpoint.substring(6);
        } else {
            return "http://" + opaEndpoint.substring(6);
        }
    }

    public void handleException(OpaEndpoint endpoint, Exception exception, Exchange exchange) throws Exception {
        if(endpoint.getHandleError()) {
            exchange.getIn().setHeader("OPA-Exception", exception.getMessage());
        } else {
            exchange.setException(exception);
        }
    }

    public void handleException(OpaEndpoint endpoint, String exception, Exchange exchange) throws Exception {
        if(endpoint.getHandleError()) {
            exchange.getIn().setHeader("OPA-Exception", exception);
        } else {
            exchange.setException(new Exception(exception));
        }
    }


}
