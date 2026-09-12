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

package org.keycloak.protocol.cas;

import jakarta.ws.rs.Path;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.cas.endpoints.CASLoginEndpoint;
import org.keycloak.protocol.cas.endpoints.CASLogoutEndpoint;
import org.keycloak.protocol.cas.endpoints.CASRestTicketsEndpoint;
import org.keycloak.protocol.cas.endpoints.CASServiceValidateEndpoint;
import org.keycloak.protocol.cas.endpoints.CASValidateEndpoint;
import org.keycloak.protocol.cas.endpoints.CASWellKnownEndpoint;
import org.keycloak.services.resources.RealmsResource;

import jakarta.ws.rs.core.UriBuilder;

public class CASLoginProtocolService {

    private final KeycloakSession session;
    private final EventBuilder event;

    public CASLoginProtocolService(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
    }

    public static UriBuilder serviceBaseUrl(UriBuilder builder) {
        return builder.path(RealmsResource.class).path("{realm}/protocol/" + CASLoginProtocol.LOGIN_PROTOCOL);
    }

    @Path("login")
    public Object login() {
        return new CASLoginEndpoint(session, event);
    }

    @Path("logout")
    public Object logout() {
        return new CASLogoutEndpoint(session, event);
    }

    @Path("validate")
    public Object validate() {
        return new CASValidateEndpoint(session, event);
    }

    @Path("serviceValidate")
    public Object serviceValidate() {
        return new CASServiceValidateEndpoint(session, event, false);
    }

    @Path("proxyValidate")
    public Object proxyValidate() {
        return new CASServiceValidateEndpoint(session, event, true);
    }

    @Path("p3/serviceValidate")
    public Object p3ServiceValidate() {
        return new CASServiceValidateEndpoint(session, event, false);
    }

    @Path("p3/proxyValidate")
    public Object p3ProxyValidate() {
        return new CASServiceValidateEndpoint(session, event, true);
    }

    @Path("v1/tickets")
    public Object restTickets() {
        return new CASRestTicketsEndpoint(session, event);
    }

    @Path(".well-known/cas")
    public Object wellKnown() {
        return new CASWellKnownEndpoint(session);
    }
}
