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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AbstractLoginProtocolFactory;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.cas.mappers.FullNameCASMapper;
import org.keycloak.protocol.cas.mappers.UserAttributeCASMapper;
import org.keycloak.protocol.cas.mappers.UserPropertyCASMapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.idm.ClientRepresentation;

public class CASLoginProtocolFactory extends AbstractLoginProtocolFactory {

    public static final int UI_ORDER = OIDCLoginProtocolFactory.UI_ORDER - 30;

    static final Map<String, ProtocolMapperModel> builtins = new HashMap<>();
    static final List<ProtocolMapperModel> defaultBuiltins = new ArrayList<>();

    static {
        ProtocolMapperModel email = UserPropertyCASMapper.create("email", "email", "mail", true);
        builtins.put("email", email);
        defaultBuiltins.add(email);

        ProtocolMapperModel username = UserPropertyCASMapper.create("username", "username", "username", true);
        builtins.put("username", username);
        defaultBuiltins.add(username);

        ProtocolMapperModel firstName = UserPropertyCASMapper.create("first name", "firstName", "givenName", true);
        builtins.put("first name", firstName);
        defaultBuiltins.add(firstName);

        ProtocolMapperModel lastName = UserPropertyCASMapper.create("last name", "lastName", "sn", true);
        builtins.put("last name", lastName);
        defaultBuiltins.add(lastName);

        ProtocolMapperModel fullName = FullNameCASMapper.create("full name", "cn", true);
        builtins.put("full name", fullName);
        defaultBuiltins.add(fullName);

        ProtocolMapperModel locale = UserAttributeCASMapper.create("locale", "locale", "locale", false);
        builtins.put("locale", locale);
    }

    @Override
    protected void createDefaultClientScopesImpl(RealmModel newRealm) {
        // CAS clients do not require dedicated default client scopes
    }

    @Override
    protected void addDefaults(ClientModel clientModel) {
        defaultBuiltins.forEach(clientModel::addProtocolMapper);
    }

    @Override
    public Map<String, ProtocolMapperModel> getBuiltinMappers() {
        return builtins;
    }

    @Override
    public Object createProtocolEndpoint(KeycloakSession session, EventBuilder event) {
        return new CASLoginProtocolService(session, event);
    }

    @Override
    public void setupClientDefaults(ClientRepresentation rep, ClientModel newClient) {
        if (rep.getAttributes() == null || !rep.getAttributes().containsKey(CASLoginProtocol.PROTOCOL_VERSION_ATTR)) {
            newClient.setAttribute(CASLoginProtocol.PROTOCOL_VERSION_ATTR, CASLoginProtocol.VERSION_7_0);
        }
        if (rep.getAttributes() == null || !rep.getAttributes().containsKey(CASLoginProtocol.REST_ENABLED_ATTR)) {
            newClient.setAttribute(CASLoginProtocol.REST_ENABLED_ATTR, Boolean.TRUE.toString());
        }
        if (rep.getAttributes() == null || !rep.getAttributes().containsKey(CASLoginProtocol.SLO_ENABLED_ATTR)) {
            newClient.setAttribute(CASLoginProtocol.SLO_ENABLED_ATTR, Boolean.TRUE.toString());
        }
        if (rep.isPublicClient() == null) {
            newClient.setPublicClient(true);
        }
        if (rep.isStandardFlowEnabled() == null) {
            newClient.setStandardFlowEnabled(true);
        }
        if (rep.isFullScopeAllowed() == null) {
            newClient.setFullScopeAllowed(true);
        }
        if (rep.getRedirectUris() == null) {
            newClient.setRedirectUris(java.util.Set.of());
        }
    }

    @Override
    public LoginProtocol create(KeycloakSession session) {
        return new CASLoginProtocol().setSession(session);
    }

    @Override
    public String getId() {
        return CASLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public int order() {
        return UI_ORDER;
    }
}
