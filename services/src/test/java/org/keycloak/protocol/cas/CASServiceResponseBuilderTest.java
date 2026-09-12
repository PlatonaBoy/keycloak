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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.protocol.cas.representations.CASErrorCode;
import org.keycloak.protocol.cas.utils.CASServiceResponseBuilder;

import org.junit.Assert;
import org.junit.Test;

public class CASServiceResponseBuilderTest {

    @Test
    public void cas10SuccessAndFailure() {
        Assert.assertEquals("yes\nalice\n", CASServiceResponseBuilder.cas10Success("alice"));
        Assert.assertEquals("no\n\n", CASServiceResponseBuilder.cas10Failure());
    }

    @Test
    public void successXmlIncludesEscapedUserAndAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("mail", "alice@example.com");
        attributes.put("groups", List.of("admin", "users"));
        attributes.put("note", "a <b> & 'c'");

        String xml = CASServiceResponseBuilder.successXml("alice", attributes);

        Assert.assertTrue(xml.contains("<cas:user>alice</cas:user>"));
        Assert.assertTrue(xml.contains("<cas:mail>alice@example.com</cas:mail>"));
        Assert.assertTrue(xml.contains("<cas:groups>admin</cas:groups>"));
        Assert.assertTrue(xml.contains("<cas:groups>users</cas:groups>"));
        Assert.assertTrue(xml.contains("<cas:note>a &lt;b&gt; &amp; &apos;c&apos;</cas:note>"));
        Assert.assertTrue(xml.contains("http://www.yale.edu/tp/cas"));
    }

    @Test
    public void failureXmlIncludesCode() {
        String xml = CASServiceResponseBuilder.failureXml(CASErrorCode.INVALID_TICKET, "Ticket is not recognized");
        Assert.assertTrue(xml.contains("code=\"INVALID_TICKET\""));
        Assert.assertTrue(xml.contains("Ticket is not recognized"));
    }

    @Test
    public void successAndFailureJsonUseCas7Envelope() {
        Map<String, Object> attributes = Map.of("mail", "alice@example.com");
        String success = CASServiceResponseBuilder.successJson("alice", attributes);
        Assert.assertTrue(success.contains("\"serviceResponse\""));
        Assert.assertTrue(success.contains("\"authenticationSuccess\""));
        Assert.assertTrue(success.contains("\"alice\""));
        Assert.assertTrue(success.contains("\"mail\""));

        String failure = CASServiceResponseBuilder.failureJson(CASErrorCode.INVALID_SERVICE, "Unknown service");
        Assert.assertTrue(failure.contains("\"authenticationFailure\""));
        Assert.assertTrue(failure.contains("INVALID_SERVICE"));
        Assert.assertTrue(failure.contains("Unknown service"));
    }

    @Test
    public void sanitizeElementReplacesIllegalCharacters() {
        Assert.assertEquals("givenName", CASServiceResponseBuilder.sanitizeElement("givenName"));
        Assert.assertEquals("given_name", CASServiceResponseBuilder.sanitizeElement("given name"));
        Assert.assertEquals("_1id", CASServiceResponseBuilder.sanitizeElement("1id"));
    }
}
