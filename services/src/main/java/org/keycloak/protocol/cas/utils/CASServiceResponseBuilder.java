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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.util.JsonSerialization;

/**
 * Builds CAS 1.0 text, CAS 2.0/3.0 XML, and CAS 7 JSON validation responses.
 */
public final class CASServiceResponseBuilder {

    public static final String CAS_NAMESPACE = "http://www.yale.edu/tp/cas";

    private CASServiceResponseBuilder() {
    }

    public static String cas10Success(String username) {
        return "yes\n" + nullToEmpty(username) + "\n";
    }

    public static String cas10Failure() {
        return "no\n\n";
    }

    public static String successXml(String username, Map<String, Object> attributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<cas:serviceResponse xmlns:cas='").append(CAS_NAMESPACE).append("'>\n");
        sb.append("  <cas:authenticationSuccess>\n");
        sb.append("    <cas:user>").append(CASProtocolUtils.escapeXml(username)).append("</cas:user>\n");
        if (attributes != null && !attributes.isEmpty()) {
            sb.append("    <cas:attributes>\n");
            attributes.forEach((name, value) -> appendAttributeXml(sb, name, value));
            sb.append("    </cas:attributes>\n");
        }
        sb.append("  </cas:authenticationSuccess>\n");
        sb.append("</cas:serviceResponse>");
        return sb.toString();
    }

    public static String failureXml(CASErrorCode code, String description) {
        return "<cas:serviceResponse xmlns:cas='" + CAS_NAMESPACE + "'>\n"
                + "  <cas:authenticationFailure code=\"" + code.name() + "\">"
                + CASProtocolUtils.escapeXml(description) + "</cas:authenticationFailure>\n"
                + "</cas:serviceResponse>";
    }

    public static String successJson(String username, Map<String, Object> attributes) {
        Map<String, Object> success = new LinkedHashMap<>();
        success.put("user", username);
        if (attributes != null && !attributes.isEmpty()) {
            success.put("attributes", attributes);
        }
        return wrapJson("authenticationSuccess", success);
    }

    public static String failureJson(CASErrorCode code, String description) {
        Map<String, Object> failure = new LinkedHashMap<>();
        failure.put("code", code.name());
        failure.put("description", description);
        return wrapJson("authenticationFailure", failure);
    }

    private static String wrapJson(String key, Object value) {
        Map<String, Object> serviceResponse = new LinkedHashMap<>();
        serviceResponse.put(key, value);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("serviceResponse", serviceResponse);
        try {
            return JsonSerialization.writeValueAsPrettyString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize CAS JSON response", e);
        }
    }

    private static void appendAttributeXml(StringBuilder sb, String name, Object value) {
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                appendAttributeXml(sb, name, item);
            }
            return;
        }
        if (value instanceof Object[] array) {
            for (Object item : array) {
                appendAttributeXml(sb, name, item);
            }
            return;
        }
        sb.append("      <cas:").append(sanitizeElement(name)).append('>')
                .append(CASProtocolUtils.escapeXml(value == null ? "" : String.valueOf(value)))
                .append("</cas:").append(sanitizeElement(name)).append(">\n");
    }

    public static String sanitizeElement(String name) {
        if (name == null || name.isBlank()) {
            return "attribute";
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        if (!Character.isLetter(sb.charAt(0)) && sb.charAt(0) != '_') {
            sb.insert(0, '_');
        }
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
