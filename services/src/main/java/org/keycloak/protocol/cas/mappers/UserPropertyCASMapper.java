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
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;

public class UserPropertyCASMapper extends AbstractCASProtocolMapper {

    public static final String PROVIDER_ID = "cas-usermodel-property-mapper";

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        property.setLabel(ProtocolMapperUtils.USER_MODEL_PROPERTY_LABEL);
        property.setHelpText(ProtocolMapperUtils.USER_MODEL_PROPERTY_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(property);

        ProviderConfigProperty attribute = new ProviderConfigProperty();
        attribute.setName(ATTRIBUTE_NAME);
        attribute.setLabel("CAS attribute name");
        attribute.setHelpText("Name of the attribute released in the CAS assertion.");
        attribute.setType(ProviderConfigProperty.STRING_TYPE);
        CONFIG_PROPERTIES.add(attribute);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User Property";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a built-in user property (email, firstName, lastName, username) to a CAS attribute.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void setAttribute(Map<String, Object> attributes, ProtocolMapperModel mappingModel, KeycloakSession session,
                             UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {
        String propertyName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);
        if (propertyName == null || propertyName.isBlank()) {
            return;
        }
        String value = ProtocolMapperUtils.getUserModelValue(userSession.getUser(), propertyName);
        if (value != null && !value.isBlank()) {
            attributes.put(attributeName(mappingModel, propertyName), value);
        }
    }

    public static ProtocolMapperModel create(String name, String userProperty, String attributeName, boolean consentRequired) {
        ProtocolMapperModel mapper = createModel(name, PROVIDER_ID, attributeName, consentRequired);
        mapper.getConfig().put(ProtocolMapperUtils.USER_ATTRIBUTE, userProperty);
        return mapper;
    }
}
