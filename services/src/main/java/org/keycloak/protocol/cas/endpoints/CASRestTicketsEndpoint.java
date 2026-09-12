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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.UserSessionManager;

/**
 * Apereo CAS 7 REST protocol ({@code /v1/tickets}).
 */
public class CASRestTicketsEndpoint {

    private final KeycloakSession session;
    private final EventBuilder event;
    private final RealmModel realm;

    public CASRestTicketsEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
        this.realm = session.getContext().getRealm();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createTicketGrantingTicket(@FormParam("username") String username, @FormParam("password") String password) {
        event.event(EventType.LOGIN);
        if (username == null || password == null) {
            event.error(Errors.INVALID_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing username or password").build();
        }

        UserModel user = KeycloakModelUtils.findUserByNameOrEmail(session, realm, username);
        if (user == null || !user.isEnabled() || !user.credentialManager().isValid(UserCredentialModel.password(password))) {
            event.error(Errors.INVALID_USER_CREDENTIALS);
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }

        ClientConnection connection = session.getContext().getConnection();
        UserSessionModel userSession = new UserSessionManager(session).createUserSession(realm, user, user.getUsername(),
                connection.getRemoteAddr(), CASLoginProtocol.LOGIN_PROTOCOL, false, null, null);
        event.user(user).session(userSession).success();

        String tgt = CASLoginProtocol.TICKET_GRANTING_TICKET_PREFIX + userSession.getId();
        URI location = session.getContext().getUri().getAbsolutePathBuilder().path(tgt).build();
        return Response.created(location).entity(location.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    @POST
    @Path("{tgt}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createServiceTicket(@PathParam("tgt") String tgt, @FormParam(CASLoginProtocol.SERVICE_PARAM) String service) {
        event.event(EventType.CODE_TO_TOKEN);
        UserSessionModel userSession = resolveUserSession(tgt);
        if (userSession == null) {
            event.error(Errors.INVALID_CODE);
            return Response.status(Response.Status.NOT_FOUND).entity("Ticket-granting ticket not recognized").build();
        }
        if (service == null || service.isBlank()) {
            event.error(Errors.INVALID_REQUEST);
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing service").build();
        }

        ClientModel client = CASProtocolUtils.findClientByService(session, realm, service);
        if (client == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            return Response.status(Response.Status.BAD_REQUEST).entity("Unknown service").build();
        }

        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        clientSession.setProtocol(CASLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setNote(CASLoginProtocol.SERVICE_PARAM, service);
        clientSession.setRedirectUri(service);

        OAuth2Code codeData = new OAuth2Code(
                SecretGenerator.getInstance().generateSecureID(),
                client.getId(),
                Time.currentTime() + realm.getAccessCodeLifespan(),
                null,
                null,
                null,
                service,
                null,
                null,
                null,
                userSession.getId());
        String ticket = CASLoginProtocol.SERVICE_TICKET_PREFIX + OAuth2CodeParser.persistCode(session, clientSession, codeData);
        event.client(client).user(userSession.getUser()).session(userSession).success();
        return Response.ok(ticket).type(MediaType.TEXT_PLAIN_TYPE).build();
    }

    @DELETE
    @Path("{tgt}")
    public Response deleteTicketGrantingTicket(@PathParam("tgt") String tgt) {
        event.event(EventType.LOGOUT);
        UserSessionModel userSession = resolveUserSession(tgt);
        if (userSession == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        AuthenticationManager.backchannelLogout(session, realm, userSession, session.getContext().getUri(),
                session.getContext().getConnection(), session.getContext().getRequestHeaders(), true);
        event.user(userSession.getUser()).session(userSession).success();
        return Response.ok().build();
    }

    private UserSessionModel resolveUserSession(String tgt) {
        String id = CASProtocolUtils.stripTicketPrefix(tgt);
        return id == null ? null : session.sessions().getUserSession(realm, id);
    }
}
