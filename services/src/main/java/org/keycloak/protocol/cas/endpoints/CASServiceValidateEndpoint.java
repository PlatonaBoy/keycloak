/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.protocol.cas.endpoints;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;

/**
 * CAS 2.0/3.0 {@code /serviceValidate} and {@code /proxyValidate}, including CAS 7 JSON responses.
 */
public class CASServiceValidateEndpoint extends AbstractCASValidateEndpoint {

    public CASServiceValidateEndpoint(KeycloakSession session, EventBuilder event, boolean proxy) {
        super(session, event);
    }

    @GET
    public Response validate(@QueryParam(CASLoginProtocol.TICKET_PARAM) String ticket,
                             @QueryParam(CASLoginProtocol.SERVICE_PARAM) String service,
                             @QueryParam(CASLoginProtocol.RENEW_PARAM) String renew,
                             @QueryParam(CASLoginProtocol.FORMAT_PARAM) String format) {
        ValidationResult result = validate(ticket, service, Boolean.parseBoolean(renew));
        String accept = session.getContext().getRequestHeaders().getHeaderString(HttpHeaders.ACCEPT);
        String version = result.client() != null ? CASLoginProtocol.protocolVersion(result.client()) : CASLoginProtocol.VERSION_7_0;
        boolean json = CASProtocolUtils.preferJson(format, accept, version);

        if (!result.success()) {
            return validationResponse(false, json, null, null, result.errorCode(), result.description());
        }

        Map<String, Object> attributes = collectAttributes(result.client(), result.userSession(), result.clientSession());
        return validationResponse(true, json, result.user().getUsername(), attributes, null, null);
    }
}
