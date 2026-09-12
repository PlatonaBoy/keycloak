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

import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ClientData;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;
import org.keycloak.protocol.oidc.utils.OAuth2Code;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * CAS 1.0/2.0/3.0 login protocol with Apereo CAS 7 compatible extensions
 * (JSON validation, REST tickets, discovery document, and optional JWT assertions).
 */
public class CASLoginProtocol implements LoginProtocol {

    private static final Logger logger = Logger.getLogger(CASLoginProtocol.class);

    public static final String LOGIN_PROTOCOL = "cas";

    public static final String SERVICE_PARAM = "service";
    public static final String TARGET_SERVICE_PARAM = "targetService";
    public static final String RENEW_PARAM = "renew";
    public static final String GATEWAY_PARAM = "gateway";
    public static final String TICKET_PARAM = "ticket";
    public static final String FORMAT_PARAM = "format";
    public static final String PGT_URL_PARAM = "pgtUrl";
    public static final String URL_PARAM = "url";

    public static final String SERVICE_TICKET_PREFIX = "ST-";
    public static final String TICKET_GRANTING_TICKET_PREFIX = "TGT-";

    public static final String PROTOCOL_VERSION_ATTR = "cas.protocol.version";
    public static final String JWT_TICKETS_ATTR = "cas.jwt.tickets";
    public static final String REST_ENABLED_ATTR = "cas.rest.enabled";
    public static final String SLO_ENABLED_ATTR = "cas.slo.enabled";

    public static final String VERSION_1_0 = "1.0";
    public static final String VERSION_2_0 = "2.0";
    public static final String VERSION_3_0 = "3.0";
    public static final String VERSION_7_0 = "7.0";

    public static final String FORMAT_XML = "XML";
    public static final String FORMAT_JSON = "JSON";

    private KeycloakSession session;
    private RealmModel realm;
    private UriInfo uriInfo;
    private HttpHeaders headers;
    private EventBuilder event;

    @Override
    public CASLoginProtocol setSession(KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public CASLoginProtocol setRealm(RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public CASLoginProtocol setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public CASLoginProtocol setHttpHeaders(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public CASLoginProtocol setEventBuilder(EventBuilder event) {
        this.event = event;
        return this;
    }

    @Override
    public Response authenticated(AuthenticationSessionModel authSession, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        String service = authSession.getRedirectUri();
        if (service == null) {
            service = authSession.getClientNote(SERVICE_PARAM);
        }

        OAuth2Code codeData = new OAuth2Code(
                SecretGenerator.getInstance().generateSecureID(),
                authSession.getClient().getId(),
                Time.currentTime() + userSession.getRealm().getAccessCodeLifespan(),
                null,
                authSession.getClientNote("scope"),
                null,
                service,
                null,
                null,
                null,
                userSession.getId());
        String code = OAuth2CodeParser.persistCode(session, clientSession, codeData);
        String ticket = SERVICE_TICKET_PREFIX + code;

        clientSession.setNote(SERVICE_PARAM, service);
        clientSession.setNote(TICKET_PARAM, ticket);
        String renew = authSession.getClientNote(RENEW_PARAM);
        if (renew != null) {
            clientSession.setNote(RENEW_PARAM, renew);
        }

        String redirect = CASProtocolUtils.appendQuery(service, TICKET_PARAM, ticket);
        logger.debugf("CAS login success, redirecting to service with ticket");
        return Response.status(Response.Status.FOUND).location(URI.create(redirect)).build();
    }

    @Override
    public Response sendError(AuthenticationSessionModel authSession, Error error, String errorMessage) {
        String service = authSession.getRedirectUri();
        if (service == null) {
            service = authSession.getClientNote(SERVICE_PARAM);
        }
        if (service != null && (error == Error.PASSIVE_LOGIN_REQUIRED || error == Error.PASSIVE_INTERACTION_REQUIRED
                || error == Error.CANCELLED_BY_USER || error == Error.CONSENT_DENIED)) {
            return Response.status(Response.Status.FOUND).location(URI.create(service)).build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorMessage != null ? errorMessage : error.name())
                .type("text/plain; charset=" + StandardCharsets.UTF_8.name())
                .build();
    }

    @Override
    public ClientData getClientData(AuthenticationSessionModel authSession) {
        return new ClientData(authSession.getRedirectUri(), null, null, authSession.getClientNote(SERVICE_PARAM));
    }

    @Override
    public Response sendError(ClientModel client, ClientData clientData, Error error) {
        if (clientData != null && clientData.getRedirectUri() != null) {
            String redirect = RedirectUtils.verifyRedirectUri(session, clientData.getRedirectUri(), client);
            if (redirect != null) {
                return Response.status(Response.Status.FOUND).location(URI.create(redirect)).build();
            }
        }
        return null;
    }

    @Override
    public Response backchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        clientSession.setAction(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name());
        return Response.ok().build();
    }

    @Override
    public Response frontchannelLogout(UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        clientSession.setAction(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name());
        String service = clientSession.getNote(SERVICE_PARAM);
        if (service == null) {
            return null;
        }
        String logoutUrl = CASProtocolUtils.appendQuery(service, "logoutRequest", userSession.getId());
        return Response.status(Response.Status.FOUND).location(URI.create(logoutUrl)).build();
    }

    @Override
    public Response finishBrowserLogout(UserSessionModel userSession, AuthenticationSessionModel logoutSession) {
        String redirect = logoutSession.getAuthNote(SERVICE_PARAM);
        if (redirect == null) {
            redirect = logoutSession.getAuthNote(URL_PARAM);
        }
        if (redirect != null) {
            return Response.status(Response.Status.FOUND).location(URI.create(redirect)).build();
        }
        return Response.noContent().build();
    }

    @Override
    public boolean requireReauthentication(UserSessionModel userSession, AuthenticationSessionModel authSession) {
        return Boolean.parseBoolean(authSession.getClientNote(RENEW_PARAM));
    }

    @Override
    public void close() {
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public EventBuilder getEvent() {
        return event;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public static boolean isSloEnabled(ClientModel client) {
        String value = client.getAttribute(SLO_ENABLED_ATTR);
        return value == null || Boolean.parseBoolean(value);
    }

    public static boolean isRestEnabled(ClientModel client) {
        String value = client.getAttribute(REST_ENABLED_ATTR);
        if (value == null) {
            return VERSION_7_0.equals(protocolVersion(client));
        }
        return Boolean.parseBoolean(value);
    }

    public static boolean isJwtTickets(ClientModel client) {
        return Boolean.parseBoolean(client.getAttribute(JWT_TICKETS_ATTR));
    }

    public static String protocolVersion(ClientModel client) {
        String version = client.getAttribute(PROTOCOL_VERSION_ATTR);
        return version == null || version.isBlank() ? VERSION_7_0 : version;
    }
}
