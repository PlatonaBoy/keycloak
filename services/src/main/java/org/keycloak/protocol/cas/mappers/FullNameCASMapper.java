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

package org.keycloak.protocol.cas.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.ProviderConfigProperty;

public class FullNameCASMapper extends AbstractCASProtocolMapper {

    public static final String PROVIDER_ID = "cas-full-name-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty attribute = new ProviderConfigProperty();
        attribute.setName(ATTRIBUTE_NAME);
        attribute.setLabel("CAS attribute name");
        attribute.setHelpText("Name of the attribute released in the CAS assertion.");
        attribute.setType(ProviderConfigProperty.STRING_TYPE);
        attribute.setDefaultValue("cn");
        CONFIG_PROPERTIES.add(attribute);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User's full name";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the user's first and last name to a single CAS attribute.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, KeycloakSession session,
                             UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String first = user.getFirstName();
        String last = user.getLastName();
        String fullName;
        if (first != null && last != null) {
            fullName = first + " " + last;
        } else if (first != null) {
            fullName = first;
        } else {
            fullName = last;
        }
        if (fullName != null && !fullName.isBlank()) {
            attributes.put(attributeName(mappingModel, "cn"), fullName.trim());
        }
    }

    public static ProtocolMapperModel create(String name, String attributeName, boolean consentRequired) {
        return createModel(name, PROVIDER_ID, attributeName, consentRequired);
    }
}
