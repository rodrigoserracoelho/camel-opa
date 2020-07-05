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
import org.apache.camel.*;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Perform authorization on Open Policy Agent server.
 */
@Slf4j
@UriEndpoint(firstVersion = "1.5.0", scheme = "opa", title = "OPA", syntax = "opa:endpoint", producerOnly = true, category = {Category.API})
public class OpaEndpoint extends DefaultEndpoint {

    @UriPath
    private boolean secure;

    @UriPath
    private boolean handleError;

    @UriPath
    private int connectTimeout = OpaConstants.DEFAULT_TIMEOUT_VALUE;

    @UriPath
    private int connectionRequestTimeout = OpaConstants.DEFAULT_TIMEOUT_VALUE;

    @UriPath
    private int socketTimeout = OpaConstants.DEFAULT_TIMEOUT_VALUE;

    @UriPath
    private OpaOperationType operationType;

    protected OpaEndpoint(String endpointUri, OpaComponent component) {
        super(endpointUri, component);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new RuntimeCamelException("An OPA Consumer would be the OPA server itself! No such support here");
    }

    @Override
    public Producer createProducer() throws Exception {
        return new OpaProducer(this);
    }

    public boolean getSecure() {
        return secure;
    }

    public boolean getHandleError() {
        return handleError;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * If true the OPA agent will be called on HTTPS
     * @param secure
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * If true OPA Component will send the exception in the In header as OPA-Exception
     * @param handleError
     */
    public void setHandleError(boolean handleError) {
        this.handleError = handleError;
    }

    /**
     * Connect timeout in seconds
     * @param connectTimeout
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Connection Request timeout in seconds
     * @param connectionRequestTimeout
     */
    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    /**
     * Socket timeout in seconds
     * @param socketTimeout
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public OpaOperationType getOperationType() {
        return operationType;
    }

    /**
     * Set the operation type: Query, ACL (access control list) or Policy
     * @param operationType
     */
    public void setOperationType(OpaOperationType operationType) {
        this.operationType = operationType;
    }
}
