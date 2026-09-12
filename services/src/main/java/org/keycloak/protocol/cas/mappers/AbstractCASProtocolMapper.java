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

import java.util.HashMap;
import java.util.Map;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.cas.CASLoginProtocol;

public abstract class AbstractCASProtocolMapper implements ProtocolMapper, CASAttributeMapper {

    public static final String TOKEN_MAPPER_CATEGORY = "CAS Attribute Mapper";
    public static final String ATTRIBUTE_NAME = "attribute.name";

    @Override
    public String getProtocol() {
        return CASLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public void close() {
    }

    @Override
    public final ProtocolMapper create(KeycloakSession session) {
        throw new RuntimeException("UNSUPPORTED METHOD");
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    protected static ProtocolMapperModel createModel(String name, String protocolMapper, String attributeName, boolean consentRequired) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(protocolMapper);
        mapper.setProtocol(CASLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(ATTRIBUTE_NAME, attributeName);
        mapper.setConfig(config);
        return mapper;
    }

    protected static String attributeName(ProtocolMapperModel mappingModel, String fallback) {
        String name = mappingModel.getConfig() == null ? null : mappingModel.getConfig().get(ATTRIBUTE_NAME);
        return name == null || name.isBlank() ? fallback : name;
    }
}
