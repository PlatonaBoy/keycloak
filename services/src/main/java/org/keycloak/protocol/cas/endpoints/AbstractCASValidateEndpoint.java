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
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.mappers.CASAttributeMapper;
import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;
import org.keycloak.protocol.cas.utils.CASServiceResponseBuilder;
import org.keycloak.protocol.oidc.utils.OAuth2CodeParser;
import org.keycloak.protocol.oidc.utils.RedirectUtils;

abstract class AbstractCASValidateEndpoint {

    protected final KeycloakSession session;
    protected final EventBuilder event;
    protected final RealmModel realm;

    AbstractCASValidateEndpoint(KeycloakSession session, EventBuilder event) {
        this.session = session;
        this.event = event;
        this.realm = session.getContext().getRealm();
        event.event(EventType.CODE_TO_TOKEN);
    }

    protected ValidationResult validate(String ticket, String service, boolean renew) {
        if (ticket == null || ticket.isBlank() || service == null || service.isBlank()) {
            event.error(Errors.INVALID_REQUEST);
            return ValidationResult.failure(CASErrorCode.INVALID_REQUEST, "Missing required parameter ticket or service");
        }

        String code = CASProtocolUtils.stripTicketPrefix(ticket);
        OAuth2CodeParser.ParseResult parsed = OAuth2CodeParser.parseCode(session, code, realm, event);
        if (parsed.isIllegalCode() || parsed.getClientSession() == null) {
            event.error(Errors.INVALID_CODE);
            return ValidationResult.failure(CASErrorCode.INVALID_TICKET, "Ticket is not recognized");
        }
        if (parsed.isExpiredCode()) {
            event.error(Errors.EXPIRED_CODE);
            return ValidationResult.failure(CASErrorCode.INVALID_TICKET, "Ticket has expired");
        }

        AuthenticatedClientSessionModel clientSession = parsed.getClientSession();
        ClientModel client = clientSession.getClient();
        UserSessionModel userSession = clientSession.getUserSession();
        if (userSession == null || userSession.getUser() == null) {
            return ValidationResult.failure(CASErrorCode.INVALID_TICKET, "Ticket is not recognized");
        }

        String storedService = clientSession.getNote(CASLoginProtocol.SERVICE_PARAM);
        boolean serviceOk = CASProtocolUtils.normalizeService(service).equals(CASProtocolUtils.normalizeService(storedService))
                || RedirectUtils.verifyRedirectUri(session, service, client, false) != null
                || CASProtocolUtils.matchesService(service, client.getRedirectUris());
        if (!serviceOk) {
            event.detail(Details.REDIRECT_URI, service);
            event.error(Errors.INVALID_REDIRECT_URI);
            return ValidationResult.failure(CASErrorCode.INVALID_SERVICE, "Service does not match the ticket");
        }

        if (renew && !Boolean.parseBoolean(clientSession.getNote(CASLoginProtocol.RENEW_PARAM))) {
            return ValidationResult.failure(CASErrorCode.INVALID_TICKET, "Ticket was not issued from an immediate login (renew=true)");
        }

        event.client(client).user(userSession.getUser()).session(userSession).success();
        return ValidationResult.success(userSession.getUser(), client, clientSession, userSession);
    }

    protected Map<String, Object> collectAttributes(ClientModel client, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        client.getProtocolMappersStream().forEach(mapperModel -> applyMapper(attributes, mapperModel, userSession, clientSession));
        return attributes;
    }

    private void applyMapper(Map<String, Object> attributes, ProtocolMapperModel mapperModel,
                             UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        ProtocolMapper mapper = (ProtocolMapper) session.getKeycloakSessionFactory()
                .getProviderFactory(ProtocolMapper.class, mapperModel.getProtocolMapper());
        if (mapper instanceof CASAttributeMapper casMapper) {
            casMapper.setAttribute(attributes, mapperModel, session, userSession, clientSession);
        }
    }

    protected Response validationResponse(boolean success, boolean json, String username, Map<String, Object> attributes,
                                          CASErrorCode errorCode, String description) {
        if (success) {
            String body = json
                    ? CASServiceResponseBuilder.successJson(username, attributes)
                    : CASServiceResponseBuilder.successXml(username, attributes);
            return Response.ok(body).type(json ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML).build();
        }
        String body = json
                ? CASServiceResponseBuilder.failureJson(errorCode, description)
                : CASServiceResponseBuilder.failureXml(errorCode, description);
        return Response.ok(body).type(json ? MediaType.APPLICATION_JSON : MediaType.APPLICATION_XML).build();
    }

    protected record ValidationResult(boolean success, UserModel user, ClientModel client,
                                      AuthenticatedClientSessionModel clientSession, UserSessionModel userSession,
                                      CASErrorCode errorCode, String description) {
        static ValidationResult success(UserModel user, ClientModel client, AuthenticatedClientSessionModel clientSession,
                                        UserSessionModel userSession) {
            return new ValidationResult(true, user, client, clientSession, userSession, null, null);
        }

        static ValidationResult failure(CASErrorCode errorCode, String description) {
            return new ValidationResult(false, null, null, null, null, errorCode, description);
        }
    }
}
