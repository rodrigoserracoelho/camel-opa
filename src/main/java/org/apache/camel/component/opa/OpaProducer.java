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
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;

@Slf4j
public class OpaProducer extends DefaultProducer {

    public OpaProducer(OpaEndpoint endpoint) throws Exception {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {

        OpaEndpoint opaEndpoint = (OpaEndpoint) getEndpoint();
        OpaOperation opaOperation = new OpaOperation();

        if(opaEndpoint.getOperationType() != null) {
            opaOperation.processOperation(opaEndpoint, exchange);
        } else {
            opaOperation.handleException(opaEndpoint, "Error calling OPA endpoint: Missing Operation type", exchange);
        }
    }
}
