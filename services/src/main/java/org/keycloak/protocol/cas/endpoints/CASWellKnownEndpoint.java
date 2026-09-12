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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;

/**
 * CAS 7 style discovery document so administrators and clients can copy endpoint URLs.
 */
public class CASWellKnownEndpoint {

    private final KeycloakSession session;

    public CASWellKnownEndpoint(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getConfiguration() {
        String realm = session.getContext().getRealm().getName();
        String base = CASProtocolUtils.casBaseUrl(session.getContext().getUri().getBaseUri().toString(), realm);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("protocol", CASLoginProtocol.LOGIN_PROTOCOL);
        config.put("casVersion", CASLoginProtocol.VERSION_7_0);
        config.put("supportedVersions", List.of(
                CASLoginProtocol.VERSION_1_0,
                CASLoginProtocol.VERSION_2_0,
                CASLoginProtocol.VERSION_3_0,
                CASLoginProtocol.VERSION_7_0));
        config.put("loginEndpoint", base + "/login");
        config.put("logoutEndpoint", base + "/logout");
        config.put("validateEndpoint", base + "/validate");
        config.put("serviceValidateEndpoint", base + "/serviceValidate");
        config.put("proxyValidateEndpoint", base + "/proxyValidate");
        config.put("p3ServiceValidateEndpoint", base + "/p3/serviceValidate");
        config.put("p3ProxyValidateEndpoint", base + "/p3/proxyValidate");
        config.put("restTicketsEndpoint", base + "/v1/tickets");
        config.put("formats", List.of(CASLoginProtocol.FORMAT_XML, CASLoginProtocol.FORMAT_JSON));
        return config;
    }
}
