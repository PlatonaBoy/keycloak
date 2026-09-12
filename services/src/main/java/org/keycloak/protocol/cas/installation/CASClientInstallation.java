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

package org.keycloak.protocol.cas.installation;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.cas.utils.CASProtocolUtils;
import org.keycloak.util.JsonSerialization;

public class CASClientInstallation implements ClientInstallationProvider {

    @Override
    public Response generateInstallation(KeycloakSession session, RealmModel realm, ClientModel client, URI serverBaseUri) {
        String base = CASProtocolUtils.casBaseUrl(serverBaseUri.toString(), realm.getName());
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("casVersion", CASLoginProtocol.protocolVersion(client));
        config.put("serverUrl", base);
        config.put("loginUrl", base + "/login");
        config.put("logoutUrl", base + "/logout");
        config.put("validateUrl", base + "/validate");
        config.put("serviceValidateUrl", base + "/serviceValidate");
        config.put("proxyValidateUrl", base + "/proxyValidate");
        config.put("p3ServiceValidateUrl", base + "/p3/serviceValidate");
        config.put("restTicketsUrl", base + "/v1/tickets");
        config.put("wellKnownUrl", base + "/.well-known/cas");
        config.put("clientId", client.getClientId());
        config.put("serviceUrls", client.getRedirectUris());
        config.put("restEnabled", CASLoginProtocol.isRestEnabled(client));
        config.put("jwtTickets", CASLoginProtocol.isJwtTickets(client));
        try {
            return Response.ok(JsonSerialization.writeValueAsPrettyString(config), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getProtocol() {
        return CASLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getDisplayType() {
        return "CAS 7 JSON";
    }

    @Override
    public String getHelpText() {
        return "JSON file with CAS 7 login, logout, validation, and REST endpoints for this client.";
    }

    @Override
    public String getFilename() {
        return "keycloak-cas.json";
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public boolean isDownloadOnly() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public ClientInstallationProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public String getId() {
        return "keycloak-cas-json";
    }
}
