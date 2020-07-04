/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.opa;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@Slf4j
public class OpaProducer extends DefaultProducer {

    private final Gson gson = new Gson();

    public OpaProducer(OpaEndpoint endpoint) throws Exception {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        OpaEndpoint opaEndpoint = (OpaEndpoint) getEndpoint();

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
                handleException(opaEndpoint, "Error calling OPA endpoint: " + getEndpoint().getEndpointUri(), exchange);
            }
            exchange.getIn().setHeader("OPA-RESULT", "Valid");

        } catch(Exception e) {
            handleException(opaEndpoint, e, exchange);
        } finally {
            closeableHttpClient.close();
        }
    }

    private String transformOpaEndpoint(OpaEndpoint endpoint) {
        String opaEndpoint = getEndpoint().getEndpointBaseUri();
        if(endpoint.getSecure()) {
            return "https://" + opaEndpoint.substring(6);
        } else {
            return "http://" + opaEndpoint.substring(6);
        }
    }

    private void handleException(OpaEndpoint endpoint, Exception exception, Exchange exchange) throws Exception {
        if(endpoint.getHandleError()) {
            exchange.getIn().setHeader("OPA-Exception", exception.getMessage());
        } else {
            exchange.setException(exception);
        }
    }

    private void handleException(OpaEndpoint endpoint, String exception, Exchange exchange) throws Exception {
        if(endpoint.getHandleError()) {
            exchange.getIn().setHeader("OPA-Exception", exception);
        } else {
            exchange.setException(new Exception(exception));
        }
    }
}
