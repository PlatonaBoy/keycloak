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

package org.keycloak.protocol.cas.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.cas.CASLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;

/**
 * Shared helpers for CAS service matching, ticket formatting, and response types.
 */
public final class CASProtocolUtils {

    private CASProtocolUtils() {
    }

    public static String normalizeService(String service) {
        if (service == null) {
            return null;
        }
        String value = service.trim();
        int hash = value.indexOf('#');
        if (hash >= 0) {
            value = value.substring(0, hash);
        }
        while (value.endsWith("/") && value.length() > 1 && !value.endsWith("://")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static boolean matchesService(String service, Collection<String> redirectUris) {
        if (service == null || redirectUris == null) {
            return false;
        }
        String normalized = normalizeService(service);
        for (String uri : redirectUris) {
            if (uri == null) {
                continue;
            }
            if (uri.endsWith("*")) {
                String prefix = uri.substring(0, uri.length() - 1);
                if (service.startsWith(prefix) || normalized.startsWith(normalizeService(prefix))) {
                    return true;
                }
            } else if (normalizeService(uri).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static ClientModel findClientByService(KeycloakSession session, RealmModel realm, String service) {
        if (service == null || service.isBlank()) {
            return null;
        }
        return realm.getClientsStream()
                .filter(client -> CASLoginProtocol.LOGIN_PROTOCOL.equals(client.getProtocol()))
                .filter(ClientModel::isEnabled)
                .filter(client -> {
                    String verified = RedirectUtils.verifyRedirectUri(session, service, client, false);
                    return verified != null || matchesService(service, client.getRedirectUris());
                })
                .findFirst()
                .orElse(null);
    }

    public static String stripTicketPrefix(String ticket) {
        if (ticket == null) {
            return null;
        }
        if (ticket.startsWith(CASLoginProtocol.SERVICE_TICKET_PREFIX)) {
            return ticket.substring(CASLoginProtocol.SERVICE_TICKET_PREFIX.length());
        }
        if (ticket.startsWith(CASLoginProtocol.TICKET_GRANTING_TICKET_PREFIX)) {
            return ticket.substring(CASLoginProtocol.TICKET_GRANTING_TICKET_PREFIX.length());
        }
        return ticket;
    }

    public static String appendQuery(String url, String name, String value) {
        if (url == null) {
            return null;
        }
        String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + name + "=" + encoded;
    }

    public static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean preferJson(String format, String acceptHeader, String protocolVersion) {
        if (format != null) {
            return CASLoginProtocol.FORMAT_JSON.equalsIgnoreCase(format.trim());
        }
        if (acceptHeader != null && acceptHeader.toLowerCase(Locale.ROOT).contains("application/json")) {
            return true;
        }
        return CASLoginProtocol.VERSION_7_0.equals(protocolVersion);
    }

    public static String casBaseUrl(String serverBaseUri, String realmName) {
        String base = serverBaseUri.endsWith("/") ? serverBaseUri.substring(0, serverBaseUri.length() - 1) : serverBaseUri;
        return base + "/realms/" + realmName + "/protocol/" + CASLoginProtocol.LOGIN_PROTOCOL;
    }
}
