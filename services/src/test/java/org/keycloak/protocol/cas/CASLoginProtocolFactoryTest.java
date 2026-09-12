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

import org.junit.Assert;
import org.junit.Test;

public class CASLoginProtocolFactoryTest {

    @Test
    public void factoryIdAndOrder() {
        CASLoginProtocolFactory factory = new CASLoginProtocolFactory();
        Assert.assertEquals(CASLoginProtocol.LOGIN_PROTOCOL, factory.getId());
        Assert.assertEquals(CASLoginProtocolFactory.UI_ORDER, factory.order());
        Assert.assertTrue(factory.getBuiltinMappers().containsKey("email"));
        Assert.assertTrue(factory.getBuiltinMappers().containsKey("username"));
        Assert.assertTrue(factory.getBuiltinMappers().containsKey("full name"));
    }

    @Test
    public void protocolConstants() {
        Assert.assertEquals("cas", CASLoginProtocol.LOGIN_PROTOCOL);
        Assert.assertEquals("7.0", CASLoginProtocol.VERSION_7_0);
        Assert.assertEquals("ST-", CASLoginProtocol.SERVICE_TICKET_PREFIX);
        Assert.assertEquals("TGT-", CASLoginProtocol.TICKET_GRANTING_TICKET_PREFIX);
    }
}
