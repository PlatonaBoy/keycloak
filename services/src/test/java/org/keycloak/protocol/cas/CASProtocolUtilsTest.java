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

import java.util.List;

import org.keycloak.protocol.cas.utils.CASProtocolUtils;

import org.junit.Assert;
import org.junit.Test;

public class CASProtocolUtilsTest {

    @Test
    public void normalizeServiceStripsFragmentAndTrailingSlash() {
        Assert.assertEquals("https://app.example.com/cas",
                CASProtocolUtils.normalizeService("https://app.example.com/cas/#/home"));
        Assert.assertEquals("https://app.example.com/cas",
                CASProtocolUtils.normalizeService("https://app.example.com/cas/"));
    }

    @Test
    public void matchesExactAndWildcardServiceUrls() {
        Assert.assertTrue(CASProtocolUtils.matchesService(
                "https://app.example.com/cas",
                List.of("https://app.example.com/cas")));
        Assert.assertTrue(CASProtocolUtils.matchesService(
                "https://app.example.com/cas/callback",
                List.of("https://app.example.com/cas/*")));
        Assert.assertFalse(CASProtocolUtils.matchesService(
                "https://evil.example.com/cas",
                List.of("https://app.example.com/cas")));
    }

    @Test
    public void stripTicketPrefix() {
        Assert.assertEquals("abc.session.client",
                CASProtocolUtils.stripTicketPrefix("ST-abc.session.client"));
        Assert.assertEquals("session-id",
                CASProtocolUtils.stripTicketPrefix("TGT-session-id"));
        Assert.assertEquals("plain", CASProtocolUtils.stripTicketPrefix("plain"));
    }

    @Test
    public void appendQueryHandlesExistingParameters() {
        Assert.assertEquals("https://app.example.com/cas?ticket=ST-1",
                CASProtocolUtils.appendQuery("https://app.example.com/cas", "ticket", "ST-1"));
        Assert.assertEquals("https://app.example.com/cas?x=1&ticket=ST-1",
                CASProtocolUtils.appendQuery("https://app.example.com/cas?x=1", "ticket", "ST-1"));
    }

    @Test
    public void preferJsonForCas7AndExplicitFormat() {
        Assert.assertTrue(CASProtocolUtils.preferJson("JSON", null, CASLoginProtocol.VERSION_2_0));
        Assert.assertFalse(CASProtocolUtils.preferJson("XML", "application/json", CASLoginProtocol.VERSION_7_0));
        Assert.assertTrue(CASProtocolUtils.preferJson(null, "application/json", CASLoginProtocol.VERSION_2_0));
        Assert.assertTrue(CASProtocolUtils.preferJson(null, null, CASLoginProtocol.VERSION_7_0));
        Assert.assertFalse(CASProtocolUtils.preferJson(null, null, CASLoginProtocol.VERSION_2_0));
    }

    @Test
    public void casBaseUrl() {
        Assert.assertEquals("https://auth.example.com/realms/demo/protocol/cas",
                CASProtocolUtils.casBaseUrl("https://auth.example.com/", "demo"));
    }
}
