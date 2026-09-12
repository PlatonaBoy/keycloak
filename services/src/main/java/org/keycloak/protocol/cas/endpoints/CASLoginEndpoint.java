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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;

public class CASLoginEndpoint extends AuthorizationEndpointBase {

    public CASLoginEndpoint(KeycloakSession session, EventBuilder event) {
        super(session, event);
        event.event(EventType.LOGIN);
    }

    @GET
    public Response loginGet() {
        return process(session.getContext().getUri().getQueryParameters());
    }

    @POST
    public Response loginPost() {
        return process(httpRequest.getDecodedFormParameters());
    }

    private Response process(MultivaluedMap<String, String> params) {
        checkSsl();
        checkRealm();

        String service = params.getFirst(CASLoginProtocol.SERVICE_PARAM);
        boolean renew = Boolean.parseBoolean(params.getFirst(CASLoginProtocol.RENEW_PARAM));
        boolean gateway = Boolean.parseBoolean(params.getFirst(CASLoginProtocol.GATEWAY_PARAM));

        if (service == null || service.isBlank()) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.MISSING_PARAMETER, CASLoginProtocol.SERVICE_PARAM);
        }

        ClientModel client = CASProtocolUtils.findClientByService(session, realm, service);
        if (client == null) {
            event.detail(Details.REDIRECT_URI, service);
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
        }

        String verified = RedirectUtils.verifyRedirectUri(session, service, client, false);
        if (verified == null && !CASProtocolUtils.matchesService(service, client.getRedirectUris())) {
            event.client(client);
            event.detail(Details.REDIRECT_URI, service);
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new ErrorPageException(session, Response.Status.BAD_REQUEST, Messages.INVALID_REDIRECT_URI);
        }

        session.getContext().setClient(client);
        event.client(client);
        event.detail(Details.REDIRECT_URI, service);

        AuthenticationSessionModel authSession = createAuthenticationSession(client, service);
        authSession.setProtocol(CASLoginProtocol.LOGIN_PROTOCOL);
        authSession.setAction(CommonClientSessionModel.Action.AUTHENTICATE.name());
        authSession.setRedirectUri(service);
        authSession.setClientNote(CASLoginProtocol.SERVICE_PARAM, service);
        authSession.setClientNote(CASLoginProtocol.RENEW_PARAM, Boolean.toString(renew));
        authSession.setClientNote(CASLoginProtocol.GATEWAY_PARAM, Boolean.toString(gateway));

        CacheControlUtil.noBackButtonCacheControlHeader(session);
        return handleBrowserAuthenticationRequest(authSession, new CASLoginProtocol().setSession(session), gateway, false);
    }
}
