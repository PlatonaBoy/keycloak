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

import java.net.URI;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.managers.AuthenticationManager;

public class CASLogoutEndpoint {

    private final KeycloakSession session;
    private final EventBuilder event;

    public CASLogoutEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
        event.event(EventType.LOGOUT);
    }

    @GET
    public Response logoutGet(@QueryParam(CASLoginProtocol.SERVICE_PARAM) String service,
                              @QueryParam(CASLoginProtocol.URL_PARAM) String url) {
        return logout(service, url);
    }

    @POST
    public Response logoutPost() {
        var form = session.getContext().getHttpRequest().getDecodedFormParameters();
        return logout(form.getFirst(CASLoginProtocol.SERVICE_PARAM), form.getFirst(CASLoginProtocol.URL_PARAM));
    }

    private Response logout(String service, String url) {
        String redirect = firstNonBlank(url, service);
        AuthenticationManager.AuthResult authResult = AuthenticationManager.authenticateIdentityCookie(session, session.getContext().getRealm(), false);
        if (authResult == null) {
            return redirectOrNoContent(redirect);
        }

        if (redirect != null) {
            ClientModel client = CASProtocolUtils.findClientByService(session, session.getContext().getRealm(), redirect);
            if (client != null) {
                String verified = RedirectUtils.verifyRedirectUri(session, redirect, client, false);
                if (verified == null && !CASProtocolUtils.matchesService(redirect, client.getRedirectUris())) {
                    redirect = null;
                }
            }
        }

        Response response = AuthenticationManager.browserLogout(session, session.getContext().getRealm(),
                authResult.getSession(), session.getContext().getUri(), session.getContext().getConnection(),
                session.getContext().getRequestHeaders());
        event.user(authResult.getUser()).session(authResult.getSession()).success();
        if (response != null) {
            return response;
        }
        return redirectOrNoContent(redirect);
    }

    private static Response redirectOrNoContent(String redirect) {
        if (redirect != null) {
            return Response.status(Response.Status.FOUND).location(URI.create(redirect)).build();
        }
        return Response.noContent().build();
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
