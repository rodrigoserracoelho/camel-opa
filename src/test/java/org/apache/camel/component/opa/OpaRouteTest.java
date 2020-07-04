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

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.mockserver.MockServer;

import static org.apache.camel.support.ObjectHelper.contains;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;

@Slf4j
public class OpaRouteTest  {

    private CamelContext camel;
    private ProducerTemplate template;
    private MockServer mockServer;

    private String successMessage = "{\"input\": {\"user\": \"test\", \"access\": \"write\"}}";
    private String successResponseMessage = "{\"result\":true}";
    private String testMethod = "POST";
    private String testPath = "/test";
    private String testHost = "127.0.0.1";
    private int testPort = 1080;

    @Before
    public void setup() throws Exception {
        mockServer = new MockServer(testPort);
        new MockServerClient(testHost, testPort)
                .when(
                    request()
                        .withMethod(testMethod)
                        .withPath(testPath)
                        .withBody(exact(successMessage)), Times.exactly(1))
                .respond(
                    response()
                        .withStatusCode(200)
                        .withBody(successResponseMessage));
        camel = new DefaultCamelContext();
        template = camel.createProducerTemplate();
    }

    @After
    public void tearDown() throws Exception {
        mockServer.stop();
        camel.stop();
    }
    
    @Test
    public void testSuccessCall() throws Exception {
        camel.addRoutes(createRouteBuilder("opa:127.0.0.1:1080/test?secure=false&handleError=true"));
        camel.start();

        Endpoint endpoint = camel.getEndpoint("direct:start");
        Exchange exchange = endpoint.createExchange();

        exchange.getIn().setBody(successMessage);
        Exchange out = template.send(endpoint, exchange);

        assertTrue(contains("Valid", exchange.getIn().getHeader("OPA-RESULT")));
    }

    @Test
    public void testUnauthorizedWithErrorHandling() throws Exception {
        camel.addRoutes(createRouteBuilder("opa:127.0.0.1:1080/test?secure=false&handleError=true"));
        camel.start();

        Endpoint endpoint = camel.getEndpoint("direct:start");
        Exchange exchange = endpoint.createExchange();

        exchange.getIn().setBody("{\"input\": {\"user\": \"fake\", \"access\": \"write\"}}");
        Exchange out = template.send(endpoint, exchange);

        assertTrue(exchange.getIn().getHeader("OPA-Exception") != null);
    }

    @Test
    public void testUnauthorizedWithoutErrorHandling() throws Exception {
        camel.addRoutes(createRouteBuilder("opa:127.0.0.1:1080/test?secure=false&handleError=false"));
        camel.start();

        Endpoint endpoint = camel.getEndpoint("direct:start");
        Exchange exchange = endpoint.createExchange();

        exchange.getIn().setBody("{\"input\": {\"user\": \"fake\", \"access\": \"write\"}}");
        Exchange out = template.send(endpoint, exchange);

        assertTrue(out.getException() != null);
    }

    @Test
    public void testWrongHostWithoutErrorHandling() throws Exception {
        camel.addRoutes(createRouteBuilder("opa:111.111.1.1:1080/test?secure=false&handleError=false&connectTimeout=4"));
        camel.start();

        Endpoint endpoint = camel.getEndpoint("direct:start");
        Exchange exchange = endpoint.createExchange();

        exchange.getIn().setBody("{\"input\": {\"user\": \"fake\", \"access\": \"write\"}}");
        Exchange out = template.send(endpoint, exchange);

        assertTrue(out.getException() != null);
    }

    @Test
    public void testRealServer() throws Exception {
        camel.addRoutes(createRouteBuilder("opa:localhost:8181/v1/data/myapi/policy/allow?secure=false&handleError=true"));
        camel.start();

        Endpoint endpoint = camel.getEndpoint("direct:start");
        Exchange exchange = endpoint.createExchange();

        exchange.getIn().setBody(successMessage);
        Exchange out = template.send(endpoint, exchange);

        assertTrue(contains("Valid", exchange.getIn().getHeader("OPA-RESULT")));
    }

    protected RouteBuilder createRouteBuilder(final String opaEndpointUrl) throws Exception {
        return new RouteBuilder() {
            // START SNIPPET: route
            public void configure() throws Exception {
                from("direct:start").to(opaEndpointUrl);
            }
            // END SNIPPET: route
        };
    }
}
